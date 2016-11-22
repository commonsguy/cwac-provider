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

import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Concrete StreamStrategy implementation that handles
 * files on the local filesystem. Requires that the app actually
 * be able to read and write those files.
 */
public class LocalPathStrategy implements StreamStrategy {
  private final File root;
  private final String name;
  private final boolean readOnly;

  /**
   * Constructor.
   *
   * @param name name of first path segment of Uri values (not
   *             counting the prefix, if any)
   * @param root directory or file from which to serve
   * @param readOnly  true if should only allow read access, false otherwise
   * @throws IOException
   */
  public LocalPathStrategy(String name, File root, boolean readOnly)
    throws IOException {
    this.root=root.getCanonicalFile();
    this.name=name;
    this.readOnly=readOnly;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canInsert(Uri uri) {
    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    throw new UnsupportedOperationException("Um, this should not have been called");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canUpdate(Uri uri) {
    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int update(Uri uri, ContentValues values,
                    String selection, String[] selectionArgs) {
    throw new UnsupportedOperationException("Um, this should not have been called");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canDelete(Uri uri) {
    return(!readOnly && getFileForUri(uri).exists());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(Uri uri) {
    if (!readOnly) {
      getFileForUri(uri).delete();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
    throws FileNotFoundException {
    if (readOnly && !"r".equals(mode)) {
      throw new FileNotFoundException("Invalid mode for read-only content");
    }

    final File file=getFileForUri(uri);
    final int fileMode=modeToMode(mode);
    
    return(ParcelFileDescriptor.open(file, fileMode));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAFD(Uri uri) {
    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException {
    throw new IllegalStateException("Not supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName(Uri uri) {
    return(getFileForUri(uri).getName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLength(Uri uri) {
    return(getFileForUri(uri).length());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean buildUriForFile(Uri.Builder b, File file) {
    try {
      String fpath=file.getCanonicalPath();
      String rpath=root.getCanonicalPath();

      if (fpath.startsWith(rpath)) {
        b
          .appendPath(name)
          .appendPath(fpath.substring(rpath.length() + 1));

        return(true);
      }
    }
    catch (IOException e) {
      throw new
        IllegalArgumentException("Invalid file: "+file.toString(), e);
    }

    return(false);
  }

  /**
   * @param uri the Uri for the content
   * @return a File pointing to where that content should reside,
   * if the Uri is valid
   */
  protected File getFileForUri(Uri uri) {
    String path=uri.getEncodedPath();

    final int splitIndex=path.indexOf('/', 1);

    path=Uri.decode(path.substring(splitIndex + 1));

    if (root == null) {
      throw new IllegalArgumentException(
                                         "Unable to find configured root for "
                                             + uri);
    }

    File file;

    if (root.isDirectory()) {
      file=new File(root, path);
    }
    else {
      file=root;
    }

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
