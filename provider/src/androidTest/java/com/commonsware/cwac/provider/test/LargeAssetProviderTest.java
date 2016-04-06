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
import java.io.IOException;
import java.io.InputStream;

public class LargeAssetProviderTest extends
    AbstractReadOnlyProviderTest {
  @Override
  public InputStream getOriginal() throws IOException {
    return(InstrumentationRegistry.getContext().getAssets().open("test.mp4"));
  }

  @Override
  public Uri getStreamSource(Uri root) {
    return(root.buildUpon().appendPath("test-largeasset")
                    .appendPath("test.mp4").build());
  }
}
