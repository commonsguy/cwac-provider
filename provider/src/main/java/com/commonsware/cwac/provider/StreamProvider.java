/***
  Copyright (C) 2013 The Android Open Source Project
  Portions Copyright (c) 2013-2016 CommonsWare, LLC
  
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
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
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
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
  private static final String PREF_URI_PREFIX="uriPrefix";

  private static ConcurrentHashMap<String, SoftReference<StreamProvider>> INSTANCES=
    new ConcurrentHashMap<String, SoftReference<StreamProvider>>();
  private CompositeStreamStrategy strategy;
  private boolean useLegacyCursorWrapper=false;
  private SharedPreferences prefs;

  private static void putInstance(StreamProvider provider)
    throws PackageManager.NameNotFoundException {
    PackageManager pm=provider.getContext().getPackageManager();
    PackageInfo pkg=
      pm.getPackageInfo(provider.getContext().getPackageName(),
        PackageManager.GET_PROVIDERS);

    for (ProviderInfo pi : pkg.providers) {
      if (provider.getClass().getCanonicalName().equals(pi.name)) {
        for (String authority : pi.authority.split(";")) {
          INSTANCES.put(authority,
            new SoftReference<StreamProvider>(provider));
        }
      }
    }
  }

  public static Uri getUriForFile(String authority, File file) {
    SoftReference<StreamProvider> ref=INSTANCES.get(authority);
    Uri result=null;

    if (ref!=null) {
      result=ref.get().getUriForFileImpl(authority, file);
    }

    return(result);
  }

  @Override
  public boolean onCreate() {
    try {
      putInstance(this);

      prefs=
        getContext()
          .getSharedPreferences(BuildConfig.APPLICATION_ID,
            Context.MODE_PRIVATE);

      return(true);
    }
    catch (PackageManager.NameNotFoundException e) {
      Log.e(getClass().getSimpleName(), "Exception caching self", e);
    }

    return(false);
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
    uri=normalize(uri);

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
    String result=strategy.getType(normalize(uri));

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
    uri=normalize(uri);

    if (strategy.canDelete(uri)) {
      strategy.delete(uri);
      return(1);
    }

    return(0);
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
    throws FileNotFoundException {
    return(strategy.openFile(normalize(uri), mode));
  }

  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException {
    Uri normalized=normalize(uri);

    if (strategy.hasAFD(normalized)) {
      return(strategy.openAssetFile(normalized, mode));
    }

    return(super.openAssetFile(uri, mode));
  }

  private CompositeStreamStrategy parseStreamStrategy(Context context,
                                                      String authority)
    throws IOException, XmlPullParserException {
    final CompositeStreamStrategy result=buildCompositeStrategy();
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
          StreamStrategy strategy=buildStrategy(context, tag, name, path);

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

  protected String getUriPrefix() {
    String prefix=prefs.getString(PREF_URI_PREFIX, null);

    if (prefix==null) {
      prefix=buildUriPrefix();
      prefs.edit().putString(PREF_URI_PREFIX, prefix).apply();
    }

    return(prefix);
  }

  protected String buildUriPrefix() {
    return(UUID.randomUUID().toString());
  }

  protected StreamStrategy buildStrategy(Context context, String tag,
                                         String name, String path)
    throws IOException {
    StreamStrategy result=null;

    if (TAG_RAW.equals(tag)) {
      return(new RawResourceStrategy(context, path));
    }
    else if (TAG_ASSET.equals(tag)) {
      return(new AssetStrategy(context, path));
    }
    else {
      result=buildLocalStrategy(context, tag, name, path);
    }

    return(result);
  }

  protected StreamStrategy buildLocalStrategy(Context context,
                                              String tag, String name,
                                              String path)
    throws IOException {
    File target=null;

    if (TAG_FILES_PATH.equals(tag)) {
      if (TextUtils.isEmpty(path)) {
        throw new
          SecurityException("Cannot serve files from all of getFilesDir()");
      }

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
      return(new LocalPathStrategy(name, target));
    }

    return(null);
  }

  protected CompositeStreamStrategy buildCompositeStrategy() {
    return(new CompositeStreamStrategy());
  }

  protected Uri getUriForFileImpl(String authority, File file) {
    Uri.Builder b=new Uri.Builder();

    b.scheme("content").authority(authority);

    String prefix=getUriPrefix();

    if (prefix!=null) {
      b.appendPath(prefix);
    }

    if (strategy.buildUriForFile(b, file)) {
      return(b.build());
    }

    return(null);
  }

  private Uri normalize(Uri input) {
    String prefix=getUriPrefix();

    if (prefix==null) {
      return(input);
    }

    List<String> segments=new ArrayList<String>(input.getPathSegments());

    if (getUriPrefix().equals(segments.get(0))) {
      segments.remove(0);

      return(input
        .buildUpon()
        .path(TextUtils.join("/", segments))
        .build());
    }

    throw new IllegalArgumentException("Unrecognized Uri: "+input.toString());
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
