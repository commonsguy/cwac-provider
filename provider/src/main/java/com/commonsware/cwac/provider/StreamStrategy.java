/***
 * Copyright (c) 2013 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may
 * not use this file except in compliance with the License. You
 * may obtain
 * a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software
 * distributed under the License is distributed on an "AS IS"
 * BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * See the License for the specific language governing
 * permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.provider;

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.FileNotFoundException;

public interface StreamStrategy {
  public String getType(Uri uri);

  public boolean canDelete(Uri uri);

  public void delete(Uri uri);

  public ParcelFileDescriptor openFile(Uri uri, String mode)
    throws FileNotFoundException;

  public String getName(Uri uri);

  public long getLength(Uri uri);

  boolean hasAFD(Uri uri);

  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException;
}
