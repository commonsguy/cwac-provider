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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public class StreamProvider extends ContentProvider {
  private static final String[] COLUMNS= {
      OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE };
  private static final String META_DATA_FILE_PROVIDER_PATHS=
      "com.commonsware.cwac.provider.STREAM_PROVIDER_PATHS";
  private static final String META_DATA_USE_LEGACY_CURSOR_WRAPPER=
      "com.commonsware.cwac.provider.USE_LEGACY_CURSOR_WRAPPER";
  private static final String TAG_FILES_PATH="files-path";
  private static final String TAG_CACHE_PATH="cache-path";
  private static final String TAG_EXTERNAL="external-path";
  private static final String TAG_EXTERNAL_FILES="external-files-path";
  private static final String TAG_EXTERNAL_CACHE_FILES=
      "external-cache-path";
  private static final String TAG_RAW="raw-resource";
  private static final String TAG_ASSET="asset";
  private static final String ATTR_NAME="name";
  private static final String ATTR_PATH="path";

  private CompositeStreamStrategy strategy;
  private boolean useLegacyCursorWrapper=false;

  @Override
  public boolean onCreate() {
    return(true);
  }

  @Override
  public void attachInfo(Context context, ProviderInfo info) {
    super.attachInfo(context, info);

    checkSecurity(info);

    try {
      strategy=parseStreamStrategy(context, info.authority);
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse "
          + META_DATA_FILE_PROVIDER_PATHS + " meta-data", e);
    }
  }

  protected void checkSecurity(ProviderInfo info) {
    // Sanity check our security
    if (info.exported) {
      throw new SecurityException("Provider must not be exported");
    }

    if (!info.grantUriPermissions) {
      throw new SecurityException("Provider must grant Uri permissions");
    }
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
                      String[] selectionArgs, String sortOrder) {
    if (projection == null) {
      projection=COLUMNS;
    }

    String[] cols=new String[projection.length];
    Object[] values=new Object[projection.length];
    int i=0;

    for (String col : projection) {
      if (OpenableColumns.DISPLAY_NAME.equals(col)) {
        cols[i]=OpenableColumns.DISPLAY_NAME;
        values[i++]=strategy.getName(uri);
      }
      else if (OpenableColumns.SIZE.equals(col)) {
        cols[i]=OpenableColumns.SIZE;
        values[i++]=strategy.getLength(uri);
      }
    }

    cols=copyOf(cols, i);
    values=copyOf(values, i);

    final MatrixCursor cursor=new MatrixCursor(cols, 1);

    cursor.addRow(values);

    if (!useLegacyCursorWrapper) {
      return(cursor);
    }

    return(new LegacyCompatCursorWrapper(cursor, getType(uri)));
  }

  @Override
  public String getType(Uri uri) {
    String result=strategy.getType(uri);

    return(result == null ? "application/octet-stream" : result);
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    throw new UnsupportedOperationException("No external inserts");
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
                    String[] selectionArgs) {
    throw new UnsupportedOperationException("No external updates");
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    if (strategy.canDelete(uri)) {
      strategy.delete(uri);
      return(1);
    }

    return(0);
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
                                                            throws FileNotFoundException {
    return(strategy.openFile(uri, mode));
  }

  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException {
    if (strategy.hasAFD(uri)) {
      return(strategy.openAssetFile(uri, mode));
    }

    return(super.openAssetFile(uri, mode));
  }

  private CompositeStreamStrategy parseStreamStrategy(Context context,
                                                      String authority)
                                                                       throws IOException,
                                                                       XmlPullParserException {
    final CompositeStreamStrategy result=new CompositeStreamStrategy();
    final ProviderInfo info=
        context.getPackageManager()
               .resolveContentProvider(authority,
                                       PackageManager.GET_META_DATA);

    useLegacyCursorWrapper=info.metaData.getBoolean(META_DATA_USE_LEGACY_CURSOR_WRAPPER, true);

    final XmlResourceParser in=
        info.loadXmlMetaData(context.getPackageManager(),
                             META_DATA_FILE_PROVIDER_PATHS);

    if (in == null) {
      throw new IllegalArgumentException("Missing "
          + META_DATA_FILE_PROVIDER_PATHS + " meta-data");
    }

    int type;

    while ((type=in.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
      if (type == org.xmlpull.v1.XmlPullParser.START_TAG) {
        final String tag=in.getName();

        if (!"paths".equals(tag)) {
          final String name=in.getAttributeValue(null, ATTR_NAME);

          if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name must not be empty");
          }

          String path=in.getAttributeValue(null, ATTR_PATH);
          StreamStrategy strategy=buildStrategy(context, tag, path);

          if (strategy != null) {
            result.add(name, strategy);
          }
          else {
            throw new IllegalArgumentException(
                                               "Could not build strategy for "
                                                   + tag);
          }
        }
      }
    }

    return(result);
  }

  protected StreamStrategy buildStrategy(Context context, String tag,
                                         String path) {
    StreamStrategy result=null;

    if (TAG_RAW.equals(tag)) {
      return(new RawResourceStrategy(context, path));
    }
    else if (TAG_ASSET.equals(tag)) {
      return(new AssetStrategy(context, path));
    }
    else {
      result=buildLocalStrategy(context, tag, path);
    }

    return(result);
  }

  protected StreamStrategy buildLocalStrategy(Context context,
                                              String tag, String path) {
    File target=null;

    if (TAG_FILES_PATH.equals(tag)) {
      target=buildPath(context.getFilesDir(), path);
    }
    else if (TAG_CACHE_PATH.equals(tag)) {
      target=buildPath(context.getCacheDir(), path);
    }
    else if (TAG_EXTERNAL.equals(tag)) {
      target=buildPath(Environment.getExternalStorageDirectory(), path);
    }
    else if (TAG_EXTERNAL_FILES.equals(tag)) {
      target=buildPath(context.getExternalFilesDir(null), path);
    }
    else if (TAG_EXTERNAL_CACHE_FILES.equals(tag)) {
      target=buildPath(context.getExternalCacheDir(), path);
    }

    if (target != null) {
      return(new LocalPathStrategy(target));
    }

    return(null);
  }

  private static File buildPath(File base, String... segments) {
    File cur=base;

    for (String segment : segments) {
      if (segment != null) {
        cur=new File(cur, segment);
      }
    }

    return(cur);
  }

  private static String[] copyOf(String[] original, int newLength) {
    final String[] result=new String[newLength];

    System.arraycopy(original, 0, result, 0, newLength);

    return(result);
  }

  private static Object[] copyOf(Object[] original, int newLength) {
    final Object[] result=new Object[newLength];

    System.arraycopy(original, 0, result, 0, newLength);

    return(result);
  }
}
