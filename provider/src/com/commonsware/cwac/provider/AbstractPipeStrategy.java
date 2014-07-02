/***
  Copyright (c) 2013 CommonsWare, LLC
  
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

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractPipeStrategy implements StreamStrategy {
  abstract InputStream getInputStream(Uri uri) throws IOException;

  @Override
  public String getType(Uri uri) {
    return(MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString())));
  }

  @Override
  public boolean canDelete(Uri uri) {
    return(false);
  }

  @Override
  public void delete(Uri uri) {
    throw new UnsupportedOperationException("Cannot delete a stream");
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode)
                                                            throws FileNotFoundException {
    if ("r".equals(mode)) {
      ParcelFileDescriptor[] pipe=null;

      try {
        pipe=ParcelFileDescriptor.createPipe();

        new TransferOutThread(getInputStream(uri),
                              new AutoCloseOutputStream(pipe[1])).start();
      }
      catch (IOException e) {
        Log.e(getClass().getSimpleName(), "Exception opening pipe", e);

        throw new FileNotFoundException("Could not open pipe for: "
            + uri.toString());
      }

      return(pipe[0]);
    }

    throw new IllegalArgumentException("Cannot support writing!");
  }

  @Override
  public String getName(Uri uri) {
    return(uri.getLastPathSegment());
  }

  @Override
  public long getLength(Uri uri) {
    return(-1);
  }

  static class TransferOutThread extends Thread {
    InputStream in;
    OutputStream out;
    byte[] buf=new byte[16384];

    TransferOutThread(InputStream in, OutputStream out) {
      this.in=in;
      this.out=out;
    }

    @Override
    public void run() {
      int len;

      try {
        while ((len=in.read(buf)) >= 0) {
          out.write(buf, 0, len);
        }

        in.close();
        out.close();
      }
      catch (IOException e) {
        Log.e(getClass().getSimpleName(),
              "Exception transferring file", e);
      }
    }
  }
}
