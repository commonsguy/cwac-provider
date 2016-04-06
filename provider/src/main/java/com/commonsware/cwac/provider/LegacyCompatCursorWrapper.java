/***
 Copyright (c) 2015-2016 CommonsWare, LLC

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

import android.database.Cursor;
import android.database.CursorWrapper;
import java.util.Arrays;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;

public class LegacyCompatCursorWrapper extends CursorWrapper {
  final int fakeDataColumn;
  final int fakeMimeTypeColumn;
  final private String mimeType;

  public LegacyCompatCursorWrapper(Cursor cursor) {
    this(cursor, null);
  }

  public LegacyCompatCursorWrapper(Cursor cursor, String mimeType) {
    super(cursor);

    if (cursor.getColumnIndex(DATA)>=0) {
      fakeDataColumn=-1;
    }
    else {
      fakeDataColumn=cursor.getColumnCount();
    }

    if (cursor.getColumnIndex(MIME_TYPE)>=0) {
      fakeMimeTypeColumn=-1;
    }
    else if (fakeDataColumn==-1) {
      fakeMimeTypeColumn=cursor.getColumnCount();
    }
    else {
      fakeMimeTypeColumn=fakeDataColumn+1;
    }

    this.mimeType=mimeType;
  }

  @Override
  public int getColumnCount() {
    int count=super.getColumnCount();

    if (!cursorHasDataColumn()) {
      count+=1;
    }

    if (!cursorHasMimeTypeColumn()) {
      count+=1;
    }

    return(count);
  }

  @Override
  public int getColumnIndex(String columnName) {
    if (!cursorHasDataColumn() && DATA.equalsIgnoreCase(
      columnName)) {
      return(fakeDataColumn);
    }

    if (!cursorHasMimeTypeColumn() && MIME_TYPE.equalsIgnoreCase(
      columnName)) {
      return(fakeMimeTypeColumn);
    }

    return(super.getColumnIndex(columnName));
  }

  @Override
  public String getColumnName(int columnIndex) {
    if (columnIndex==fakeDataColumn) {
      return(DATA);
    }

    if (columnIndex==fakeMimeTypeColumn) {
      return(MIME_TYPE);
    }

    return(super.getColumnName(columnIndex));
  }

  @Override
  public String[] getColumnNames() {
    if (cursorHasDataColumn() && cursorHasMimeTypeColumn()) {
      return(super.getColumnNames());
    }

    String[] orig=super.getColumnNames();
    String[] result=Arrays.copyOf(orig, getColumnCount());

    if (!cursorHasDataColumn()) {
      result[fakeDataColumn]=DATA;
    }

    if (!cursorHasMimeTypeColumn()) {
      result[fakeMimeTypeColumn]=MIME_TYPE;
    }

    return(result);
  }

  @Override
  public String getString(int columnIndex) {
    if (!cursorHasDataColumn() && columnIndex==fakeDataColumn) {
      return(null); // yes, we have no _data, we have no _data today
    }

    if (!cursorHasMimeTypeColumn() && columnIndex==fakeMimeTypeColumn) {
      return(mimeType);
    }

    return(super.getString(columnIndex));
  }

  @Override
  public int getType(int columnIndex) {
    if (!cursorHasDataColumn() && columnIndex==fakeDataColumn) {
      return(Cursor.FIELD_TYPE_STRING);
    }

    if (!cursorHasMimeTypeColumn() && columnIndex==fakeMimeTypeColumn) {
      return(Cursor.FIELD_TYPE_STRING);
    }

    return(super.getType(columnIndex));
  }

  private boolean cursorHasDataColumn() {
    return(fakeDataColumn==-1);
  }

  private boolean cursorHasMimeTypeColumn() {
    return(fakeMimeTypeColumn==-1);
  }
}
