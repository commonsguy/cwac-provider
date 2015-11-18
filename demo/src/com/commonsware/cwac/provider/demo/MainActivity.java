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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class MainActivity extends Activity {
  private static final Uri PROVIDER=
      Uri.parse("content://com.commonsware.cwac.provider.demo");
  private static final String[] ASSET_PATHS={
    "assets/help.pdf",
    "assets/test.pdf",
    "assets/test.ogg",
    "assets/test.mp4"
  };
  private Spinner assetSpinner;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.main);

    assetSpinner=(Spinner)findViewById(R.id.assets);
    ArrayAdapter<String> adapter=
      new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item,
        getResources().getStringArray(R.array.assets));

    adapter
      .setDropDownViewResource(
        android.R.layout.simple_spinner_dropdown_item);
    assetSpinner.setAdapter(adapter);
  }

  public void viewAsset(View v) {
    String path=ASSET_PATHS[assetSpinner.getSelectedItemPosition()];
    Intent i=
        new Intent(Intent.ACTION_VIEW, PROVIDER.buildUpon()
                                               .path(path)
                                               .build());

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

    share.putExtra(Intent.EXTRA_STREAM,
      PROVIDER.buildUpon().path(path).build());
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    startActivity(Intent.createChooser(share, "Share Asset"));
  }
}