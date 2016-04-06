/***
  Copyright (c) 2013-2014 CommonsWare, LLC
  
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

package com.commonsware.cwac.provider;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;

class RawResourceStrategy extends AFDStrategy {
  private int resourceId=-1;
  private Context appContext=null;

  RawResourceStrategy(Context ctxt, String path) {
    resourceId=
        ctxt.getResources().getIdentifier(path, "raw",
                                          ctxt.getPackageName());
    appContext=ctxt.getApplicationContext();
  }

  @Override
  InputStream getInputStream(Uri uri) {
    return(appContext.getResources().openRawResource(resourceId));
  }

  @Override
  public AssetFileDescriptor getAssetFileDescriptor(Uri uri)
      throws IOException {
    return(appContext.getResources().openRawResourceFd(resourceId));
  }
}
