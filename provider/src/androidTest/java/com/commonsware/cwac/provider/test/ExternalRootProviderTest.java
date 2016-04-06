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
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import com.commonsware.cwac.provider.StreamProvider;
import org.junit.Assert;
import org.junit.Before;
import java.io.File;

public class ExternalRootProviderTest extends AbstractReadWriteProviderTest {
  private File testDir=null;

  @Before
  public void setUp() throws Exception {
    testDir=
        new File(Environment.getExternalStorageDirectory(),
                 "StreamProviderTests");

    if (!testDir.exists()) {
      testDir.mkdirs();
    }
  }

  @Override
  public String getPrefix() {
    return("test-external-root");
  }

  @Override
  void assertFileExists(String fileName) {
    File testFile=new File(testDir, fileName);
    
    Assert.assertTrue(testFile.exists());
  }

  @Override
  void assertUriBuild(String filename, Uri original) {
    File testFile=new File(testDir, filename);
    Uri test=
      StreamProvider.getUriForFile(original.getAuthority(),
        testFile);

    Assert.assertNotNull(test);
    Assert.assertEquals(original, test);
  }
}
