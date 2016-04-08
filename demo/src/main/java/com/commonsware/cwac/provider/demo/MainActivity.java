/***
  Copyright (c) 2013 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
 */

package com.commonsware.cwac.provider.demo;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import com.commonsware.cwac.provider.StreamProvider;

public class MainActivity extends Activity
  implements AdapterView.OnItemSelectedListener {
  private static final String AUTHORITY=
    "com.commonsware.cwac.provider.demo";
  private static final Uri PROVIDER=
      Uri.parse("content://"+AUTHORITY);
  private static final String[] ASSET_PATHS={
    "assets/help.pdf",
    "assets/test.pdf",
    "assets/test.ogg",
    "assets/test.mp4"
  };
  private static final String[] PROJECTION={
    OpenableColumns.DISPLAY_NAME,
    MediaStore.MediaColumns.DATA,
    MediaStore.MediaColumns.MIME_TYPE,
    OpenableColumns.SIZE
  };
  private Spinner assetSpinner;
  private TableLayout cursorTable;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.main);

    cursorTable=(TableLayout)findViewById(R.id.cursor);

    assetSpinner=(Spinner)findViewById(R.id.assets);
    assetSpinner.setOnItemSelectedListener(this);

    ArrayAdapter<String> adapter=
      new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item,
        getResources().getStringArray(R.array.assets));

    adapter
      .setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item);
    assetSpinner.setAdapter(adapter);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view,
                             int position, long id) {
    populateCursorTable();
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // no-op
  }

  public void viewAsset(View v) {
    Intent i=new Intent(Intent.ACTION_VIEW, getUri());

    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(i);
  }

  public void sendAsset(View v) {
    String path=ASSET_PATHS[assetSpinner.getSelectedItemPosition()];
    Intent share=new Intent(Intent.ACTION_SEND);
    String extension=null;
    int i=path.lastIndexOf('.');

    if (i>0) {
      extension=path.substring(i+1);
    }

    if (extension!=null) {
      share.setType(
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(
          extension));
    }

    share.putExtra(Intent.EXTRA_STREAM, getUri());
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    startActivity(Intent.createChooser(share, "Share Asset"));
  }

  private Uri getUri() {
    String path=ASSET_PATHS[assetSpinner.getSelectedItemPosition()];

    return(PROVIDER
      .buildUpon()
      .appendPath(StreamProvider.getUriPrefix(AUTHORITY))
      .appendPath(path)
      .build());
  }

  private void populateCursorTable() {
    Cursor c=getContentResolver().query(getUri(), PROJECTION,
      null, null, null);
    LayoutInflater inflater=getLayoutInflater();

    c.moveToFirst();
    cursorTable.removeAllViews();

    for (int i=0;i<c.getColumnCount();i++) {
      View row=inflater.inflate(R.layout.row, cursorTable, false);
      TextView name=(TextView)row.findViewById(R.id.name);
      TextView value=(TextView)row.findViewById(R.id.value);

      name.setText(c.getColumnName(i));

      switch (c.getType(i)) {
        case Cursor.FIELD_TYPE_STRING:
          String text=c.getString(i);

          if (text==null) {
            value.setText("null");
            value.setTypeface(null, Typeface.ITALIC);
          }
          else {
            value.setText(text);
          }

          break;

        case Cursor.FIELD_TYPE_INTEGER:
          value.setText(Integer.toString(c.getInt(i)));
          break;

        default:
          value.setText("**UNEXPECTED TYPE**: "+
            Integer.toString(c.getType(i)));
          value.setTypeface(null, Typeface.ITALIC);
          break;
      }

      cursorTable.addView(row);
    }
  }
}