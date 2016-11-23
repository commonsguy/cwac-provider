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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import com.commonsware.cwac.provider.StreamProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
abstract class AbstractReadOnlyProviderTest {
  private static final String[] COLUMNS= {
    OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
  abstract public InputStream getOriginal() throws IOException;
  abstract public Uri getStreamSource(Uri root);

  static final Uri[] ROOTS={
    Uri.parse("content://"+BuildConfig.APPLICATION_ID+".fixed/"+
        StreamProvider.getUriPrefix(BuildConfig.APPLICATION_ID+".fixed")),
    Uri.parse("content://"+BuildConfig.APPLICATION_ID+".plain/"+
        StreamProvider.getUriPrefix(BuildConfig.APPLICATION_ID+".plain")),
    Uri.parse("content://"+BuildConfig.APPLICATION_ID+".no")
  };

  @BeforeClass
  static public void resetPrefs() {
    SharedPreferences prefs=InstrumentationRegistry.getTargetContext().getSharedPreferences(
      com.commonsware.cwac.provider.BuildConfig.APPLICATION_ID,
      Context.MODE_PRIVATE);

    prefs.edit().clear().commit();
  }

  @Test
  public void testRead() throws NotFoundException, IOException {
    for (Uri root : ROOTS) {
      Uri source=getStreamSource(root);

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
  }

  @Test
  public void testCannotWrite() throws IOException {
    for (Uri root : ROOTS) {
      Uri source=getStreamSource(root);

      try {
        ParcelFileDescriptor pfd=
          InstrumentationRegistry
            .getContext()
            .getContentResolver()
            .openFileDescriptor(source, "rw");
        pfd.close();
        Assert.fail("expected exception, did not get one");
      }
      catch (FileNotFoundException e) {
        if (!e.getMessage().equals("Not a whole file") &&
          !e.getMessage().equals("Invalid mode for read-only content")) {
          throw e;
        }
      }
    }
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
