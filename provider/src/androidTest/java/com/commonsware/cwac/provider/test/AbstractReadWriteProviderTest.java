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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.util.Log;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RunWith(AndroidJUnit4.class)
abstract class AbstractReadWriteProviderTest {
  abstract public String getPrefix();
  abstract void assertFileExists(String filename);
  abstract void assertUriBuild(String filename, Uri original);

  @Test
  public void testWriteAndRead() throws NotFoundException, IOException {
    doWriteAndRead("ic_launcher.png", "__test_output.png");
  }

  @Test
  public void testWriteAndReadLarge() throws NotFoundException, IOException {
    doWriteAndRead("test.mp4", "__test_output.mp4");
  }

  public void doWriteAndRead(String original, String out)
    throws NotFoundException, IOException {
    for (Uri root : AbstractReadOnlyProviderTest.ROOTS) {
      Uri output=
        root.buildUpon().appendPath(getPrefix())
          .appendEncodedPath(out).build();

      try {
        OutputStream testOutput=
          InstrumentationRegistry
            .getContext()
            .getContentResolver()
            .openOutputStream(output);

        Assert.assertNotNull(testOutput);
        copy(InstrumentationRegistry
          .getContext()
          .getResources()
          .getAssets()
          .open(original), testOutput);
        assertFileExists(out);
        assertUriBuild(out, output);
        compareStreamToAsset(output, original);
      }
      finally {
        InstrumentationRegistry
          .getContext()
          .getContentResolver()
          .delete(output, null, null);
      }
    }
  }

  public void compareStreamToAsset(Uri stream, String assetName)
    throws NotFoundException, IOException {
    InputStream testInput=
      InstrumentationRegistry
        .getContext()
        .getContentResolver()
        .openInputStream(stream);

    Assert.assertNotNull(testInput);

    InputStream testCompare=
      InstrumentationRegistry
        .getContext().getResources().getAssets().open(assetName);

    Assert.assertTrue(isEqual(testInput, testCompare));
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

  static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf=new byte[1024];
    int len;

    while ((len=in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }

    in.close();
    out.close();
  }
}
