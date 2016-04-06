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
import com.commonsware.cwac.provider.StreamProvider;
import org.junit.Assert;
import org.junit.Before;
import java.io.File;

public class StandardFilesProviderTest extends AbstractReadWriteProviderTest {
  @Before
  public void setUp() {
    File testDir=
      new File(InstrumentationRegistry.getContext().getFilesDir(), "foo");

    testDir.mkdirs();
  }

  @Override
  public String getPrefix() {
    return("test-standard");
  }

  @Override
  void assertFileExists(String filename) {
    File testDir=
      new File(InstrumentationRegistry.getContext().getFilesDir(), "foo");
    File testFile=new File(testDir, filename);

    Assert.assertTrue(testFile.exists());
  }

  @Override
  void assertUriBuild(String filename, Uri original) {
    File testDir=
      new File(InstrumentationRegistry.getContext().getFilesDir(), "foo");
    File testFile=new File(testDir, filename);
    Uri test=StreamProvider.getUriForFile(original.getAuthority(),
      testFile);

    Assert.assertNotNull(test);
    Assert.assertEquals(original, test);
  }
}
