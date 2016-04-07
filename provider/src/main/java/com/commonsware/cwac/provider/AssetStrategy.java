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
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete StreamStrategy implementation for mapping Uri
 * values to assets.
 */
class AssetStrategy extends AFDStrategy {
  private String path=null;
  private Context appContext=null;

  /**
   * Constructor.
   *
   * @param ctxt any Context will do; strategy holds onto
   *             Application
   * @param path directory or filename within assets, where all served
   *             assets must reside, or null if we should be
   *             serving everything in assets
   */
  AssetStrategy(Context ctxt, String path) {
    this.path=path;
    appContext=ctxt.getApplicationContext();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  InputStream getInputStream(Uri uri) throws IOException {
    return(appContext.getAssets().open(getAssetPath(uri)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AssetFileDescriptor getAssetFileDescriptor(Uri uri)
      throws IOException {
    return(appContext.getAssets().openFd(getAssetPath(uri)));
  }

  /**
   * @param uri original Uri from client
   * @return path within assets/ where this content should reside
   */
  private String getAssetPath(Uri uri) {
    List<String> segments=new ArrayList<String>(uri.getPathSegments());

    if (path==null) {
      segments.remove(0);
    }
    else {
      segments.set(0, path);
    }

    return(TextUtils.join("/", segments));
  }
}
