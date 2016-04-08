/***
 Copyright (c) 2016 CommonsWare, LLC

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

package com.commonsware.cwac.provider.test;

import android.content.Context;
import com.commonsware.cwac.provider.LocalPathStrategy;
import com.commonsware.cwac.provider.StreamProvider;
import com.commonsware.cwac.provider.StreamStrategy;
import java.io.IOException;
import java.util.HashMap;

public class DatabaseProvider extends StreamProvider {
  private static final String TAG="database-path";
  @Override
  protected String getUriPrefix() {
    return(null);
  }

  @Override
  protected StreamStrategy buildStrategy(Context context,
                                         String tag, String name,
                                         String path,
                                         HashMap<String, String> attrs)
    throws IOException {
    if (TAG.equals(tag)) {
      return(new LocalPathStrategy(name,
        context.getDatabasePath(path)));
    }

    return(super.buildStrategy(context, tag, name, path, attrs));
  }
}
