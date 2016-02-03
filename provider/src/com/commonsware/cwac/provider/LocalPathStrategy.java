/***
  Copyright (C) 2013 The Android Open Source Project
  Portions Copyright (c) 2013 CommonsWare, LLC
  
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
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LocalPathStrategy implements StreamStrategy {
  private File root=null;

  public LocalPathStrategy(File root) {
    this.root=root.getAbsoluteFile();
  }

  @Override
  public String getType(Uri uri) {
    final File file=getFileForUri(uri);
    final int lastDot=file.getName().lastIndexOf('.');
    
    if (lastDot >= 0) {
      final String extension=file.getName().substring(lastDot + 1);
      final String mime=
          MimeTypeMap.getSingleton()
                     .getMimeTypeFromExtension(extension);
      
      if (mime != null) {
        return(mime);
      }
    }

    return(null);
  }

  @Override
  public boolean canDelete(Uri uri) {
    return(getFileForUri(uri).exists());
  }

  @Override
  public void delete(Uri uri) {
    getFileForUri(uri).delete();
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
                                                            throws FileNotFoundException {
    final File file=getFileForUri(uri);
    final int fileMode=modeToMode(mode);
    
    return(ParcelFileDescriptor.open(file, fileMode));
  }

  @Override
  public boolean hasAFD(Uri uri) {
    return(false);
  }

  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException {
    throw new IllegalStateException("Not supported");
  }

  @Override
  public String getName(Uri uri) {
    return(getFileForUri(uri).getName());
  }

  @Override
  public long getLength(Uri uri) {
    return(getFileForUri(uri).length());
  }

  private File getFileForUri(Uri uri) {
    String path=uri.getEncodedPath();

    final int splitIndex=path.indexOf('/', 1);

    path=Uri.decode(path.substring(splitIndex + 1));

    if (root == null) {
      throw new IllegalArgumentException(
                                         "Unable to find configured root for "
                                             + uri);
    }

    File file=new File(root, path);

    try {
      file=file.getCanonicalFile();
    }
    catch (IOException e) {
      throw new IllegalArgumentException(
                                         "Failed to resolve canonical path for "
                                             + file);
    }

    if (!file.getPath().startsWith(root.getPath())) {
      throw new SecurityException(
                                  "Resolved path jumped beyond configured root");
    }

    return(file);
  }

  /**
   * Copied from ContentResolver.java
   */
  private static int modeToMode(String mode) {
    int modeBits;
    
    if ("r".equals(mode)) {
      modeBits=ParcelFileDescriptor.MODE_READ_ONLY;
    }
    else if ("w".equals(mode) || "wt".equals(mode)) {
      modeBits=
          ParcelFileDescriptor.MODE_WRITE_ONLY
              | ParcelFileDescriptor.MODE_CREATE
              | ParcelFileDescriptor.MODE_TRUNCATE;
    }
    else if ("wa".equals(mode)) {
      modeBits=
          ParcelFileDescriptor.MODE_WRITE_ONLY
              | ParcelFileDescriptor.MODE_CREATE
              | ParcelFileDescriptor.MODE_APPEND;
    }
    else if ("rw".equals(mode)) {
      modeBits=
          ParcelFileDescriptor.MODE_READ_WRITE
              | ParcelFileDescriptor.MODE_CREATE;
    }
    else if ("rwt".equals(mode)) {
      modeBits=
          ParcelFileDescriptor.MODE_READ_WRITE
              | ParcelFileDescriptor.MODE_CREATE
              | ParcelFileDescriptor.MODE_TRUNCATE;
    }
    else {
      throw new IllegalArgumentException("Invalid mode: " + mode);
    }
    
    return(modeBits);
  }
}
