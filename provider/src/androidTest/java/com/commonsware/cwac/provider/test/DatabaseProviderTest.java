/***
  Copyright (c) 2013 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.provider.test;

import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import com.commonsware.cwac.provider.StreamProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class DatabaseProviderTest {
  private static final String[] COLUMNS= {
    OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };

  static final String RESPECT_MAH_AUTHORITAY=
    BuildConfig.APPLICATION_ID+".db";
  static final Uri ROOT=
    Uri.parse("content://"+RESPECT_MAH_AUTHORITAY+"/"+
      StreamProvider.getUriPrefix(RESPECT_MAH_AUTHORITAY));

  @Test
  public void testRead() throws NotFoundException, IOException {
    File dbpath=getDatabasePath();

    dbpath.getParentFile().mkdirs();

    SQLiteDatabase db=
      SQLiteDatabase.openOrCreateDatabase(dbpath, null);

    db.close();

    Uri source=getStreamSource(ROOT);

    InputStream testInput=
      InstrumentationRegistry
        .getContext()
        .getContentResolver()
        .openInputStream(source);
    InputStream testComparison=getOriginal();

    Assert.assertTrue(isEqual(testInput, testComparison));

    Cursor c=InstrumentationRegistry
      .getContext()
      .getContentResolver()
      .query(source, COLUMNS, null, null, null);

    Assert.assertNotNull(c);
    Assert.assertEquals(4, c.getColumnCount());
    Assert.assertEquals(1, c.getCount());

    c.moveToFirst();

    int nameCol=c.getColumnIndex(COLUMNS[0]);
    int sizeCol=c.getColumnIndex(COLUMNS[1]);

    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB) {
      Assert.assertTrue(
        c.getType(nameCol)==Cursor.FIELD_TYPE_STRING);
      Assert.assertNotNull(c.getString(nameCol));
      Assert.assertTrue(
        c.getType(sizeCol)==Cursor.FIELD_TYPE_INTEGER);
      Assert.assertTrue(c.getInt(sizeCol)>0);
    }
  }

  private Uri getStreamSource(Uri root) {
    return(root.buildUpon().appendPath("test-db").build());
  }

  private InputStream getOriginal() throws FileNotFoundException {
    return(new FileInputStream(getDatabasePath()));
  }

  private File getDatabasePath() {
    return(InstrumentationRegistry.getContext().getDatabasePath("test.db"));
  }

  // from http://stackoverflow.com/a/4245881/115145

  static boolean isEqual(InputStream i1, InputStream i2)
    throws IOException {
    byte[] buf1=new byte[1024];
    byte[] buf2=new byte[1024];

    try {
      DataInputStream d2=new DataInputStream(i2);
      int len;
      while ((len=i1.read(buf1)) > 0) {
        d2.readFully(buf2, 0, len);
        for (int i=0; i < len; i++)
          if (buf1[i] != buf2[i]) {
            Log.w("ExternalProviderTest",
                  String.format("Bytes disagreed at %d: %x %x", i,
                                buf1[i], buf2[i]));
            return false;
          }
      }
      return d2.read() < 0; // is the end of the second file
                            // also.
    }
    catch (EOFException ioe) {
      Log.w("ExternalProviderTest", "EOFException", ioe);
      return false;
    }
    finally {
      i1.close();
      i2.close();
    }
  }
}
