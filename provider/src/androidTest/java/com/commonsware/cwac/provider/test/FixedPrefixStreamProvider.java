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

import com.commonsware.cwac.provider.StreamProvider;

public class FixedPrefixStreamProvider extends StreamProvider {
  public static final String PREFIX=
    "581ea1e3-f054-4bf9-8c97-afc02ec6866d";

  @Override
  protected String buildUriPrefix() {
    return(PREFIX);
  }
}
