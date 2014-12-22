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
        url "https://repo.commonsware.com.s3.amazonaws.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:provider:0.2.+'
}
```

Or, if you cannot use SSL, use `http://repo.commonsware.com` for the repository
URL.

NOTE: The JAR name, as of v0.2.1, has a `cwac-` prefix, to help distinguish it from other JARs.

Usage
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

### Limitations

Compared to `FileProvider`, `StreamProvider` has the following limitations:

- You can only use one authority in the `android:authorities` attribute, not
a semi-colon delimited list

- There is no `getUriForFile()` utility method, as not everything served by
`StreamProvider` is a `File`

- `FileProvider` has support for an additional, undocumented metadata element;
`StreamProvider` drops support for that element

Dependencies
------------
This project has no dependencies.

Version
-------
This is version v0.2.2 of this module, meaning it is brand new.

Demo
----
In the `demo/` sub-project you will find a sample project demonstrating the use
of `StreamProvider`. The `tests/` sub-project holds a JUnit instrumentation test suite.

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

Do not ask for help via Twitter.

Also, if you plan on hacking
on the code with an eye for contributing something back,
please open an issue that we can use for discussing
implementation details. Just lobbing a pull request over
the fence may work, but it may not.

Release Notes
-------------
- v0.2.2: updated for Android Studio 1.0 and new AAR publishing system
- v0.2.1: updated Gradle, fixed manifest for merger, added `cwac-` prefix to JAR
- v0.2.0: migrated to Gradle, published AAR
- v0.1.0: initial release

Who Made This?
--------------
<a href="http://commonsware.com">![CommonsWare](http://commonsware.com/images/logo.png)</a>

