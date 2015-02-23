/***
 Copyright (c) 2015 CommonsWare, LLC

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

public class LegacyCompatCursorWrapper extends CursorWrapper {
  private boolean hasDataColumn=false;

  public LegacyCompatCursorWrapper(Cursor cursor) {
    super(cursor);

    hasDataColumn=(cursor.getColumnIndex(DATA)>=0);
  }

  @Override
  public int getColumnCount() {
    if (hasDataColumn) {
      return(super.getColumnCount());
    }

    return(super.getColumnCount()+1);
  }

  @Override
  public int getColumnIndex(String columnName) {
    if (hasDataColumn || DATA.equalsIgnoreCase(columnName)) {
      return(super.getColumnCount());
    }

    return(super.getColumnIndex(columnName));
  }

  @Override
  public String getColumnName(int columnIndex) {
    if (!hasDataColumn && columnIndex==super.getColumnCount()) {
      return(DATA);
    }

    return(super.getColumnName(columnIndex));
  }

  @Override
  public String[] getColumnNames() {
    if (hasDataColumn) {
      return(super.getColumnNames());
    }

    String[] orig=super.getColumnNames();
    String[] result=Arrays.copyOf(orig, orig.length + 1);

    result[orig.length]=DATA;

    return(result);
  }

  @Override
  public String getString(int columnIndex) {
    if (!hasDataColumn && columnIndex==super.getColumnCount()) {
      return(null); // yes, we have no _data, we have no _data today
    }

    return(super.getString(columnIndex));
  }

  @Override
  public int getType(int columnIndex) {
    if (!hasDataColumn && columnIndex==super.getColumnCount()) {
      return(Cursor.FIELD_TYPE_STRING);
    }

    return(super.getType(columnIndex));
  }
}
