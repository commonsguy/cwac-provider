/***
  Copyright (c) 2014 CommonsWare, LLC
  
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

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.IOException;

abstract class AFDStrategy extends AbstractPipeStrategy {
  abstract AssetFileDescriptor getAssetFileDescriptor(Uri uri)
      throws IOException;

  @Override
  public long getLength(Uri uri) {
    AssetFileDescriptor afd;
    long result=super.getLength(uri);

    try {
      afd=getAssetFileDescriptor(uri);
      result=afd.getLength();
      afd.close();
    }
    catch (Exception e) {
      Log.w(getClass().getSimpleName(), "Exception getting asset length", e);
    }

    return(result);
  }

  @Override
  public boolean hasAFD(Uri uri) {
    return(true);
  }

  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException {
    try {
      return(getAssetFileDescriptor(uri));
    }
    catch (IOException e) {
      throw new IllegalStateException("Attempted to open uri failed for "+uri.toString(), e);
    }
  }
}
