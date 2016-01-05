CWAC-Provider: Helping to Make Content Providers Sane
======================================================

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
    compile 'com.commonsware.cwac:provider:0.3.+'
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

  Notably, the provider *must not* be exported, *must* have
  `android:grantUriPermissions="true"`, and *must* have the `<meta-data>`
  element pointing to your XML metadata.

- Use `FLAG_GRANT_READ_URI_PERMISSION` and `FLAG_GRANT_WRITE_URI_PERMISSION`
in `Intent` objects you use to have third parties use the files the
`StreamProvider` serves, to allow those apps selective, temporary access to
the file.

### Metadata Elements

Google's `FileProvider` supports:

- `<files-path>` for serving files from your app's `getFilesDir()`

- `<external-path>` for serving files from
`Environment.getExternalStoragePublicDirectory()`

- `<cache-path>` for serving files from your app's `getCacheDir()`

Each of those take a `name` attribute, indicating the first path segment of the `Uri`
that should identify this particular source of files. For example, a
`name` of `foo` would mean that `content://your.authority.here/foo/...` would
look for a `...` file in that particular element's source of files.

Each of those optionally take a `path` attribute, indicating a subdirectory
under the element-defined root to use as the source of files, rather than
the root itself. So, a `<files-path>` with a `path="stuff"` attribute would
serve files from the `stuff/` subdirectory within `getFilesDir()`. Note
that `path` can point to a file as well, to limit access to a single file
rather than a directory.

`StreamProvider` adds support for:

- `<external-files-path>` for serving files from `getExternalFilesDir(null)`

- `<external-cache-path>` for serving files from `getExternalCacheDir()`

- `<raw-resource>` for serving a particular raw resource, where the `path`
is the name of the raw resource (without file extension)

- `<asset>` for serving files from `assets/`

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

### Limitations

Compared to `FileProvider`, `StreamProvider` has the following limitations:

- You can only use one authority in the `android:authorities` attribute, not
a semi-colon delimited list

- There is no `getUriForFile()` utility method, as not everything served by
`StreamProvider` is a `File`

Usage: LegacyCompatCursorWrapper
-----
For Google's `FileProvider`, or other `ContentProvider` implementations
that also have a need for a fake `MediaStore.MediaColumns.DATA` in the
`query()` result, this library offers `LegacyCompatCursorWrapper`. Just
wrap your `Cursor` in the `LegacyCompatCursorWrapper` (e.g.,
`new LegacyCompatCursorWrapper(cursor)`), and return the `LegacyCompatCursorWrapper`.
It will automatically add the fake `MediaStore.MediaColumns.DATA`
column, delegating all other requests to the underlying `Cursor`.

Many thanks to Stefan Rusek for [pioneering the basic approach](http://stackoverflow.com/a/25020642/115145).

Dependencies
------------
This project has no dependencies.

Version
-------
This is version v0.3.0 of this module, meaning it is pretty new.

Demo
----
In the `demo/` sub-project you will find a sample project demonstrating the use
of `StreamProvider`.

License
-------
The code in this project is licensed under the Apache
Software License 2.0, per the terms of the included LICENSE
file.

Questions
---------
If you have questions regarding the use of this code, please post a question
on [StackOverflow](http://stackoverflow.com/questions/ask) tagged with
`commonsware-cwac` and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[commonsware-cwac]+streamprovider). Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please post an [issue](https://github.com/commonsguy/cwac-provider/issues).
Be certain to include complete steps for reproducing the issue.
The [contribution guidelines](CONTRIBUTING.md)
provide some suggestions for how to create a bug report that will get
the problem fixed the fastest.

Do not ask for help via Twitter.

Also, if you plan on hacking
on the code with an eye for contributing something back,
please open an issue that we can use for discussing
implementation details. Just lobbing a pull request over
the fence may work, but it may not.
Again, the [contribution guidelines](CONTRIBUTING.md) provide a bit
of guidance here.

Release Notes
-------------
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

