CWAC-Provider: Helping to Make Content Providers Sane
======================================================

**UPDATE 2021-05-08**: This project is discontinued. This repository will be removed from public access on or after 1 December 2021.

This project
offers a `StreamProvider`, based on Google's
[`FileProvider`](https://developer.android.com/reference/android/support/v4/content/FileProvider.html).
Like `FileProvider`, `StreamProvider` is designed to serve up files,
for reading and writing, through the `ContentProvider` interface
(`content://` `Uri` values). `StreamProvider` offers:

- Serving files from assets and raw resources
- Serving files from `getExternalFilesDir()` and `getExternalCacheDir()`

in addition to `FileProvider`'s support for serving files from `getFilesDir()`,
`getCacheDir()`, and `Environment.getExternalStoragePublicDirectory()`.

You can simply use the `StreamProvider` directly, without creating your own
subclass &mdash; everything can be handled through configuration rather than
coding.

This Android library project is 
[available as a JAR](https://github.com/commonsguy/cwac-provider/releases)
or as an artifact for use with Gradle. To use that, add the following
blocks to your `build.gradle` file:

```groovy
repositories {
    maven {
        url "https://s3.amazonaws.com/repo.commonsware.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:provider:0.5.3'
}
```

Or, if you cannot use SSL, use `http://repo.commonsware.com` for the repository
URL.

NOTE: The JAR name, as of v0.2.1, has a `cwac-` prefix, to help distinguish it from other JARs.

Usage: StreamProvider
-----
Once you add the JAR or artifact to your project, it works much along the lines of
`FileProvider`:

- Define an XML metadata file with a `<paths>` root element, containing
one or more elements describing what you want the provider to serve
(described in greater detail below)

- Define a `<provider>` in your manifest as follows:

```xml
<provider
	android:name="com.commonsware.cwac.provider.StreamProvider"
	android:authorities="..."
	android:exported="false"
	android:grantUriPermissions="true">
	<meta-data
		android:name="com.commonsware.cwac.provider.STREAM_PROVIDER_PATHS"
		android:resource="@xml/..."/>
</provider>
```

  (where you fill in your desired authority name and reference to your XML
  metadata from step #1)

  Notably, the provider *must* have the `<meta-data>`
  element pointing to your XML metadata.

- Use `FLAG_GRANT_READ_URI_PERMISSION` and `FLAG_GRANT_WRITE_URI_PERMISSION`
in `Intent` objects you use to have third parties use the files the
`StreamProvider` serves, to allow those apps selective, temporary access to
the file.

### Exporting and Usage Patterns

If your `StreamProvider` is exported, all of your streams will be considered
read-only, regardless of any other configuration. Mostly, this mode is here
for cases where you need a streaming provider and cannot grant `Uri`
permissions (e.g., [implementing a `ChooserTargetService`](http://stackoverflow.com/q/43895664/115145)).

If your `StreamProvider` is not exported, and it has
`android:grantUriPermissions` set, then you can control, on a per-`Uri`
basis, which clients get access to your streams. This works identically
to how `FileProvider` works. Whether a particular source of streams is
read-only or read-write will depend on whether the stream is a file and
your metadata configuration.

Wherever possible, elect to not export the provider and use
`FLAG_GRANT_READ_URI_PERMISSIONS` or similar techniques to selectively grant
access to your content.

Note that the exported-and-read-only rule is on a per-provider basis. If
you have some content that needs to be published globally and others that
are not:

- Use `StreamProvider` and one `<provider>` element for one set of content,
with one authority and `android:exported` setting

- Subclass `StreamProvider` and have a separate `<provider>` element for the
other set of content, with a separate authority and `android:exported` setting

### Metadata Elements

Google's `FileProvider` supports:

- `<files-path>` for serving files from your app's `getFilesDir()`

- `<external-path>` for serving files from
`Environment.getExternalStoragePublicDirectory()`

- `<cache-path>` for serving files from your app's `getCacheDir()`

- `<external-files-path>` for serving files from `getExternalFilesDir()`

- `<external-cache-path>` for serving files from `getExternalCacheDir()`

Each of those take a `name` attribute, indicating the first path segment of the `Uri`
that should identify this particular source of files. For example, a
`name` of `foo` would mean that `content://your.authority.here/foo/...` would
look for a `...` file in that particular element's source of files.

Each of those optionally take a `path` attribute, indicating a subdirectory
under the element-defined root to use as the source of files, rather than
the root itself. So, a `<files-path>` with a `path="stuff"` attribute would
serve files from the `stuff/` subdirectory within `getFilesDir()`. Note
that `path` can point to a file as well, to limit access to a single file
rather than a directory. Note that `path` is required for `<files-path>`,
so you do not accidentally serve everything under `getFilesDir()`.

Also, each can optionally take a `readOnly` attribute. If this is set to
`true`, then the files will be readable, but not writeable.

`<external-files-path>` also can take an optional `dir` attribute. If
missing, the files are served from `getExternalFilesDir()`. If a valid
value of `dir` is supplied, that value is passed into `getExternalFilesDir()`.
As such, `dir` is limited to be one of the `Environment.DIRECTORY_*` constants:
                             
 - `Alarms`
 - `DCIM`
 - `Documents`
 - `Download`
 - `Movies`
 - `Music`
 - `Notifications`
 - `Pictures`
 - `Podcasts`
 - `Ringtones`

However, you cannot have *both* `<external-files-path>` with no `dir`
(indicating that you are serving from `getExternalFilesDir(null)`)
*and* one or more `<external-files-path>` elements with `dir` values,
as they will conflict.

`StreamProvider` adds support for:

- `<raw-resource>` for serving a particular raw resource, where the `path`
is the name of the raw resource (without file extension)

- `<asset>` for serving files from `assets/`

- `<dir-path>`, for serving files from locations identified by `getDir()`

- `<external-public-path>`, for serving files from locations identified by
`Environment.getExternalStoragePublicDirectory()`

In the case of `<dir-path>`, two attributes are required:

- `dir`, which indicates what directory to serve (this is passed into `getDir()`)

- `path`, which serves its normal role, to determine what to serve from the
directory identified by `dir`

In the case of `<external-public-path>`, `dir` is required. It needs to be
the string value of one of the `Environment.DIRECTORY_*` constants, listed
above.

Of course, your metadata can have one or more of each of these types as needed
to declare what you want to be served.

### Supporting Legacy Apps

Some apps assume that any `content://` `Uri` that they get must be from
the `MediaStore` or otherwise have a `MediaStore.MediaColumns.DATA`
column that can be queried. This, of course, was never the case, and is
less the case nowadays. But, it sometimes takes firms a while to get with
the program, and in the meantime, `StreamProvider` could have issues
working with such apps.

Adding another `<meta-data>` element to the `<provider>`
can help improve compatibility:

```xml
<meta-data
  android:name="com.commonsware.cwac.provider.USE_LEGACY_CURSOR_WRAPPER"
  android:value="true"/>
```

This tells `StreamProvider` to include a fake `MediaStore.MediaColumns.DATA`
in the result set, with a `null` value, to try to cajole these legacy
apps into using the `Uri` as they are supposed to: via `ContentResolver`
and `openInputStream()`.

Similarly, you can add this `<meta-data>` element to the `<provider>`:

```xml
<meta-data
  android:name="com.commonsware.cwac.provider.USE_URI_FOR_DATA_COLUMN"
  android:value="true"/>
```

Clients of a streaming `ContentProvider` should not be assuming that they
can `query()` for a `_DATA` column. Alas, some developers still do, thinking
that all `content:` `Uri` values come from the `MediaStore`. By default,
`StreamProvider` returns `null` for the `_DATA` column, should somebody
`query()` for it. However, with the above `<meta-data>` element, `StreamProvider`
will return the `Uri` used for the `query()` as the value for `_DATA`.

### Gradle Settings

Starting with version 0.3.0 of the library, for files you are looking
to share from your app's `assets/`, you will need to teach the build
system to avoid compressing those files. On the plus side, we can
now use `AssetFileDescriptor` for those, and greatly improve compatibility
with apps using our streams.

To do this, add the following closure to your `android` closure
in your module's `build.gradle` file:

```groovy
aaptOptions {
    noCompress 'pdf', 'mp4', 'ogg'
}
```

(here, the file extensions are from the demo app &mdash; you would
list the file extensions that you are looking to share)

### Getting Uri Values

For files served through `StreamProvider` (as opposed to assets
or raw resources), `StreamProvider` offers a static `getUriForFile()`
method that works akin to its equivalent on `FileProvider`.
It takes two parameters:

- The authority name of the provider you are interested in

- The `File` object that you want to serve

It returns a `Uri` pointing to that file or `null` if the `File`
does not seem to be served by that provider.

For anything else, you have to assemble the `Uri` yourself:

```java
private static final String AUTHORITY=
    "com.commonsware.cwac.provider.demo";
private static final Uri PROVIDER=
      Uri.parse("content://"+AUTHORITY);

private Uri buildUri(String path) {
    return(PROVIDER
          .buildUpon()
          .appendPath(StreamProvider.getUriPrefix(AUTHORITY))
          .appendPath(path)
          .build());
}
```

### Uri Prefixes

To help defeat some security attacks, `StreamProvider`,
starting with 0.4.0, by
default, puts a per-install UUID into every `Uri`, as the first
path segment after the authority name. So, for example, in the
following `Uri`, `some-prefix` is the prefix:

    content://com.commonsware.hithere/some-prefix/foo/bar.txt

If you are constructing a `Uri` supported by a `StreamProvider`
&mdash; and you cannot use `getUriForFile()` (e.g., you are serving
assets or raw resources) &mdash; call the static `getUriPrefix()`
method, passing in the authority name of the provider. If it
returns a non-`null` value, that is the prefix to put into the
`Uri`. If `getUriPrefix()` returns `null`, there is no prefix.

### Extending StreamProvider

You are welcome to create custom subclasses of `StreamProvider`,
to handle cases that are not covered by `StreamProvider` itself.
This process is covered
[in a separate documentation page](https://github.com/commonsguy/cwac-provider/blob/master/docs/EXTENDING.markdown).

### Limitations

Compared to `FileProvider`, `StreamProvider` has the following limitations:

- `FileProvider` has support for an additional, undocumented metadata element;
`StreamProvider` drops support for that element.

- `StreamProvider` no longer allows you to serve everything
from `getFilesDir()`, for security reasons. The `path` attribute
is required.

### Upgrading to 0.5.0+ From Earlier Versions

If you created your own subclass of `StreamProvider` and overrode
`buildStrategy()`, note that the method signature has changed and is now:

```java
protected StreamStrategy buildStrategy(Context context,
                                     String tag, String name,
                                     String path, boolean readOnly,
                                     HashMap<String, String> attrs)
```

`readOnly` is a boolean indicating if this content should be treated as read-only
(`true`) or read-write (`false`).

Also, if you are using `LegacyCompatCursorWrapper`, it now has an additional,
three-parameter constructor:

```java
public LegacyCompatCursorWrapper(Cursor cursor, String mimeType,
                                   Uri uriForDataColumn)
```

That third parameter should be the `Uri` to use for the value of the `_DATA`
column, should somebody attempt to request that column from this `Cursor`.
The default value is `null`. A likely alternative would be whatever `Uri`
generated this `Cursor` (e.g., from the provider's `query()` implementation).

Also, if you are using `LegacyCompatCursorWrapper`, its fields are now marked
`final private`. If you had been referencing those fields, and this now breaks
your code, please file an issue and explain your use case, so an appropriate
API can be added to `LegacyCompatCursorWrapper`.

### Upgrading to 0.4.0+ From Earlier Versions

If you are upgrading an existing `StreamProvider` implementation
to 0.4.0 or higher, please note the new `Uri` prefix discussed
[earlier in the documentation](https://github.com/commonsguy/cwac-provider#uri-prefixes).
Your provider's `Uri` values will
have this prefix by default, and you need to include the
prefix in any `Uri` values that you publish.

Usage: LegacyCompatCursorWrapper
-----
Some consumers of `content://` `Uri` values make unfortunate assumptions,
that they can `query()` on that `Uri` and get columns back other
than those in `OpenableColumns`. Of note, one or more popular consumers
request:

- `MediaStore.MediaColumns.DATA` (erroneously thinking that the
`Uri` must be known to the `MediaStore`)

- `MediaStore.MediaColumns.MIME_TYPE` (rather than calling `getType()`
on a `ContentResolver`, the way talented developers would)

For Google's `FileProvider`, or other `ContentProvider` implementations
that also have a need for these fake columns in the
`query()` result, this library offers `LegacyCompatCursorWrapper`. Just
wrap your `Cursor` in the `LegacyCompatCursorWrapper` (e.g.,
`new LegacyCompatCursorWrapper(cursor)`), and return the `LegacyCompatCursorWrapper`.
It will automatically add the fake columns for queries that
request them, delegating all other requests to the underlying `Cursor`.

Many thanks to Stefan Rusek for [pioneering the basic approach](http://stackoverflow.com/a/25020642/115145).

Dependencies
------------
This project has no dependencies.

Version
-------
This is version v0.5.3 of this module, meaning it is pretty new.

Demo
----
In the `demo/` sub-project you will find a sample project demonstrating the use
of `StreamProvider`.

Additional Documentation
------------------------
[JavaDocs are available](http://javadocs.commonsware.com/cwac/provider/index.html).

[The Busy Coder's Guide to Android Development](https://commonsware.com/Android)
contains a section dedicated to `StreamProvider`. It also uses
`LegacyCompatCursorWrapper` in all of its `FileProvider` samples.

License
-------
The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file.

Questions
---------
If you have questions regarding the use of this code, please post a question
on [Stack Overflow](http://stackoverflow.com/questions/ask) tagged with
`commonsware-cwac` and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[commonsware-cwac]+streamprovider). Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please post an [issue](https://github.com/commonsguy/cwac-provider/issues).
Be certain to include complete steps for reproducing the issue.
The [contribution guidelines](CONTRIBUTING.md)
provide some suggestions for how to create a bug report that will get
the problem fixed the fastest.

You are also welcome to join
[the CommonsWare Community](https://community.commonsware.com/)
and post questions
and ideas to [the CWAC category](https://community.commonsware.com/c/cwac).

Do not ask for help via social media.

Also, if you plan on hacking
on the code with an eye for contributing something back,
please open an issue that we can use for discussing
implementation details. Just lobbing a pull request over
the fence may work, but it may not.
Again, the [contribution guidelines](CONTRIBUTING.md) provide a bit
of guidance here.

Release Notes
-------------
- v0.5.3: upgraded to Android Gradle Plugin 3.0.0
- v0.5.2: added [exported read-only support](https://github.com/commonsguy/cwac-provider/issues/22), [published JavaDoc/source JARs in repo](https://github.com/commonsguy/cwac-provider/issues/17)
- v0.5.1: fixed [bug](https://github.com/commonsguy/cwac-provider/issues/30) blocking use of multiple authorities in a single provider
- v0.5.0:
  - Added support for `Environment.getExternalStoragePublicDirectory()`
  - Added support for `getDir()`
  - Added support for non-`null` versions of `getExternalFilesDir()`
  - Added `com.commonsware.cwac.provider.USE_URI_FOR_DATA_COLUMN` option per [issue #20](https://github.com/commonsguy/cwac-provider/issues/20)
- v0.4.4: fix for [`CompositeStreamStrategy` bug](https://github.com/commonsguy/cwac-provider/issues/18)
- v0.4.3: exposed [yet another method for extending `StreamProvider`](https://github.com/commonsguy/cwac-provider/issues/15#issuecomment-238407330) 
- v0.4.2: exposed [a few more methods for extending `StreamProvider`](https://github.com/commonsguy/cwac-provider/issues/15#issuecomment-238405362) 
- v0.4.1: fix for [`CompositeStreamStrategy` bug](https://github.com/commonsguy/cwac-provider/issues/12)
- v0.4.0: added Uri prefix, clearer subclassing support, refactored into Android Studio project structure, etc.
- v0.3.1: fixed local path bug, added support for `MediaStore.MediaColumns.MIME_TYPE` to `LegacyCompatCursorWrapper`
- v0.3.0: switched to `openAssetFile()` where possible for better compatibility
- v0.2.5: pulled out permissions check into separate method
- v0.2.4: added `LegacyCompatCursorWrapper` and `USE_LEGACY_CURSOR_WRAPPER`
- v0.2.3: resolved issue #8, supporting actual length for assets and raw resources
- v0.2.2: updated for Android Studio 1.0 and new AAR publishing system
- v0.2.1: updated Gradle, fixed manifest for merger, added `cwac-` prefix to JAR
- v0.2.0: migrated to Gradle, published AAR
- v0.1.0: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

