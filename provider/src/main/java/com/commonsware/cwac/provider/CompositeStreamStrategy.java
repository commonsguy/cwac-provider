/***
  Copyright (c) 2013-2016 CommonsWare, LLC
  
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

import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the composite pattern for a collection
 * of StreamStrategy instances.
 */
public class CompositeStreamStrategy implements StreamStrategy {
  private Map<String, StreamStrategy> strategies=
      new HashMap<String, StreamStrategy>();

  /**
   * Adds a strategy to be considered.
   *
   * @param name unique name of this strategy, forms first segment
   *             of Uri (after the prefix, if any)
   * @param strategy the strategy associated with this name
   */
  void add(String name, StreamStrategy strategy) {
    strategies.put(name, strategy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType(Uri uri) {
    String result=null;
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      result=strategy.getType(uri);
    }

    return(result);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canInsert(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.canInsert(uri));
    }

    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      if (strategy.canInsert(uri)) {
        return(strategy.insert(uri, values));
      }
    }

    throw new UnsupportedOperationException("Um, this should not have been called");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canUpdate(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.canUpdate(uri));
    }

    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int update(Uri uri, ContentValues values,
                    String selection, String[] selectionArgs) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      if (strategy.canUpdate(uri)) {
        return(strategy.update(uri, values, selection, selectionArgs));
      }
    }

    throw new UnsupportedOperationException("Um, this should not have been called");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canDelete(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.canDelete(uri));
    }

    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      if (strategy.canDelete(uri)) {
        strategy.delete(uri);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
    throws FileNotFoundException {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.openFile(uri, mode));
    }

    return(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAFD(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.hasAFD(uri));
    }

    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.openAssetFile(uri, mode));
    }

    return(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.getName(uri));
    }

    return(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLength(Uri uri) {
    StreamStrategy strategy=getStrategy(uri);

    if (strategy != null) {
      return(strategy.getLength(uri));
    }

    return(-1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean buildUriForFile(Uri.Builder b, File file) {
    for (StreamStrategy strategy : strategies.values()) {
      if (strategy.buildUriForFile(b, file)) {
        return(true);
      }
    }

    return(false);
  }

  /**
   * Uses the first path segment (after the already-removed prefix,
   * if any) to find the strategy to use for this Uri.
   *
   * @param uri the Uri for the content
   * @return the StreamStrategy that we think handles this content
   * @throws IllegalArgumentException
   */
  public StreamStrategy getStrategy(Uri uri)
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
      throw new IllegalArgumentException("Unable to find configured strategy for "
                                             + uri);
    }

    return(best.getValue());
  }
}
