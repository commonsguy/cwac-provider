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
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

abstract class AbstractReadOnlyProviderTest extends AndroidTestCase {
  abstract public InputStream getOriginal() throws IOException;
  abstract public Uri getStreamSource(Uri root);

  static final Uri[] ROOTS={
    Uri.parse("content://"+BuildConfig.APPLICATION_ID+".fixed/"+FixedPrefixStreamProvider.PREFIX)
  };

  public void testRead() throws NotFoundException, IOException {
    for (Uri root : ROOTS) {
      InputStream testInput=
        getContext().getContentResolver().openInputStream(
          getStreamSource(root));
      InputStream testComparison=getOriginal();

      assertTrue(isEqual(testInput, testComparison));
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
