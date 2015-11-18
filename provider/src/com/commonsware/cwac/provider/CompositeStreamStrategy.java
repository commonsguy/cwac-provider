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

package com.commonsware.cwac.provider;

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class CompositeStreamStrategy implements StreamStrategy {
  private Map<String, StreamStrategy> strategies=
      new HashMap<String, StreamStrategy>();

  void add(String name, StreamStrategy strategy) {
    strategies.put(name, strategy);
  }

  @Override
  public String getType(Uri uri) {
    String result=null;
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      result=strategy.getType(uri);
    }

    return(result);
  }

  @Override
  public boolean canDelete(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.canDelete(uri));
    }

    return(false);
  }

  @Override
  public void delete(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      if (strategy.canDelete(uri)) {
        strategy.delete(uri);
      }
    }
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
                                                            throws FileNotFoundException {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.openFile(uri, mode));
    }

    return(null);
  }

  @Override
  public boolean hasAFD(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.hasAFD(uri));
    }

    return(false);
  }

  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.openAssetFile(uri, mode));
    }

    return(null);
  }

  @Override
  public String getName(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.getName(uri));
    }

    return(null);
  }

  @Override
  public long getLength(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.getLength(uri));
    }

    return(-1);
  }

  private StreamStrategy getStrategy(Uri uri)
                                             throws IllegalArgumentException {
    String path=uri.getPath();
    Map.Entry<String, StreamStrategy> best=null;

    for (Map.Entry<String, StreamStrategy> entry : strategies.entrySet()) {
      if (path.startsWith("/"+entry.getKey())) {
        if (best == null
            || best.getKey().length() < entry.getKey().length()) {
          best=entry;
        }
      }
    }

    if (best == null) {
      throw new IllegalArgumentException(
                                         "Unable to find configured strategy for "
                                             + uri);
    }

    return(best.getValue());
  }
}
