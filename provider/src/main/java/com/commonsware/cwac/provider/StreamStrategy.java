/***
 Copyright (c) 2013-2016 CommonsWare, LLC

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

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Interface for all strategy implementations for streams supported
 * by this library. Basically, any stream source should work,
 * so long as you can figure out a way to map a Uri to it and
 * can craft a ParcelFileDescriptor to provide the content
 * itself (e.g., via a pipe).
 *
 * If you are looking to implement a strategy for which you
 * do not have a simple file, and therefore need a pipe, consider
 * subclassing AbstractPipeStrategy instead.
 */
public interface StreamStrategy {
  /**
   * @param uri the Uri of the content
   * @return the MIME type of the content, or null if you do not
   * know the MIME type
   */
  String getType(Uri uri);

  /**
   * @param uri the Uri of the content
   * @return true if this content can be deleted, false otherwise
   */
  boolean canDelete(Uri uri);

  /**
   * Called to delete a piece of content. Will only be called
   * if you returned true from canDelete() for the same Uri.
   *
   * @param uri the Uri of the content
   */
  void delete(Uri uri);

  /**
   * @param uri the Uri of the content
   * @param mode the Unix-style mode string ("r", "rw", "rwt")
   * @return a ParcelFileDescriptor pointing to the content
   * itself, on which streams can be built
   * @throws FileNotFoundException
   */
  ParcelFileDescriptor openFile(Uri uri, String mode)
    throws FileNotFoundException;

  /**
   * @param uri the Uri of the content
   * @return a display name for the content (if you do not have
   * a separate "display name", return a filename or something else
   * that the user might recognize)
   */
  String getName(Uri uri);

  /**
   * @param uri the Uri of the content
   * @return the length in bytes of the content (i.e., if we were
   * to create an InputStream to read in the content, how many bytes
   * should we get?)
   */
  long getLength(Uri uri);

  /**
   * This method is to get a "native" AssetFileDescriptor on
   * the content identified by the Uri, for stream sources where
   * the Android SDK provides direct access to get an
   * AssetFileDescriptor. If all you can get is a
   * ParcelFileDescriptor, do not wrap that in an AssetFileDescriptor
   * yourself; we will let Android handle that. But, if whatever
   * you are using to get the stream gives you an option of
   * getting an AssetFileDescriptor, then return true here and
   * return the AssetFileDescriptor from openAssetFile().
   *
   * @param uri the Uri of the content
   * @return true if you are in position to return an
   * AssetFileDescriptor on the content, false otherwise
   */
  boolean hasAFD(Uri uri);

  /**
   * Called to return an AssetFileDescriptor on the content.
   * Will only be called if you returned true from hasAFD()
   * for the same Uri previously.
   *
   * @param uri the Uri of the content
   * @param mode the Unix-style mode string ("r", "rw", "rwt")
   * @return an AssetFileDescriptor pointing to the content
   * itself, on which streams can be built
   * @throws FileNotFoundException
   */
  AssetFileDescriptor openAssetFile(Uri uri, String mode)
    throws FileNotFoundException;

  /**
   * Given a File, build the rest of the Uri that will
   * resolve to that file. In other words, the Uri that will
   * be built by the Uri.Builder ought to satisfy all the other
   * method calls on this interface to be able to access this
   * File.
   *
   * If this File is not one of yours, or your strategy does
   * not involve actual files on the filesystem, return false and
   * leave the Uri.Builder alone.
   *
   * @param b a Uri.Builder on which you should append the path
   *          necessary to resolve to the file
   * @param file the File that we seek a Uri for
   * @return true if you identified the File and filled out the
   * builder, false otherwise
   */
  boolean buildUriForFile(Uri.Builder b, File file);
}
