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

import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import org.junit.BeforeClass;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadOnlyFileProviderTest extends
    AbstractReadOnlyProviderTest {
  @BeforeClass
  static public void initTestFile() throws IOException {
    InputStream original=
      InstrumentationRegistry.getContext().getAssets().open("ic_launcher.png");
    File ro=new File(InstrumentationRegistry.getContext().getFilesDir(), "ro");

    ro.mkdirs();

    File dest=new File(ro, "ic_launcher.png");
    copy(original, dest);
  }

  @Override
  public InputStream getOriginal() throws IOException {
    return(InstrumentationRegistry.getContext().getAssets().open("ic_launcher.png"));
  }

  @Override
  public Uri getStreamSource(Uri root) {
    Uri result=root.buildUpon().appendEncodedPath("test-read-only/ic_launcher.png").build();

    return(result);
  }

  static private void copy(InputStream in, File dst) throws IOException {
    FileOutputStream out=new FileOutputStream(dst);
    byte[] buf=new byte[1024];
    int len;

    while ((len=in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }

    in.close();
    out.close();
  }
}
