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
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class AssetStrategy extends AFDStrategy {
  private String path=null;
  private Context appContext=null;

  AssetStrategy(Context ctxt, String path) {
    this.path=path;
    appContext=ctxt.getApplicationContext();
  }

  @Override
  InputStream getInputStream(Uri uri) throws IOException {
    return(appContext.getAssets().open(getAssetPath(uri)));
  }

  @Override
  public AssetFileDescriptor getAssetFileDescriptor(Uri uri)
      throws IOException {
    return(appContext.getAssets().openFd(getAssetPath(uri)));
  }

  private String getAssetPath(Uri uri) {
    boolean isFirst=true;
    StringBuilder fullPathBuilder=new StringBuilder();

    if (path != null) {
      fullPathBuilder.append(path);
      fullPathBuilder.append('/');
    }

    List<String> segments=new ArrayList<String>(uri.getPathSegments());

    segments.remove(0); // get rid of first segment

    for (String segment : segments) {
      if (isFirst) {
        isFirst=false;
        fullPathBuilder.append(segment);
      }
      else if (!"..".equals(segment)) {
        fullPathBuilder.append('/');
        fullPathBuilder.append(segment);
      }
    }

    return(fullPathBuilder.toString());
  }
}
