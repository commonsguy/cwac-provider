# Extending `StreamProvider`

You are welcome to create subclasses of `StreamProvider`, to 
extend its capabilities for things that you may want to do in
your app. This page outlines the basics of this.

## Customizing the Uri Prefix

`StreamProvider`, starting with 0.4.0, automatically places a prefix
in each `Uri`, to help prevent against certain kinds of attacks.
The default behavior is for the first request for a `Uri` to
generate a UUID, which is saved in a custom `SharedPreferences`
instance and used thereafter for this provider.

In your subclass, you have three options for changing this
behavior:

- If you want a per-install value, but just not a UUID,
override `buildUriPrefix()` and return your own generated
`String`

- If you want a fixed prefix, to be used for all installs of this
provider, override `getUriPrefix()` and return your constant

- If you do not want a prefix, override `getUriPrefix()` and
return `null`

## Supporting Other Stream Locations

You may have content located in directories other than what
`StreamProvider` supports out of the box, such as the path for
SQLite databases. To handle that, you can add support for
new XML elements in the `<paths>` element (e.g., `<database-path>`
for serving up databases).

To do this, in your `StreamProvider` subclass, override
`buildStrategy()` and return a `StreamStrategy` implementation
that is configured for your scenario. For files located in unusual
spots, `LocalPathStrategy` should work.

In the `androidTest/` sourceset, you will find a `DatabaseProvider`
that, at the time of this writing, looks like this:

```java
public class DatabaseProvider extends StreamProvider {
  private static final String TAG="database-path";
  
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
```

The parameters to `buildStrategy()` are:

- a `Context`, should you need one (though do not assume it is
any particular sort of `Context`)

- the tag we encountered (e.g., `database-path`)

- the value of the `name` attribute, which all of these need to 
have, as that is how we determine which `StreamStrategy` handles
this request

- the value of the `path` attribute, which can be `null`

- a `HashMap` of all attributes, in case you wish to have some
custom ones

Either return your own `StreamStrategy` instance based off of
this information or chain to the superclass' implementation, so
`StreamProvider` can handle the stock tags.

## Supporting Other Stream Strategies

You may have content located in things that
are not files, such as `BLOB` columns in a database. In theory,
you can create a custom `StreamStrategy` implementation
that handles this. In practice... there are probably some things
that will be needed to be added to this library to support
`StreamStrategy` implementations. Examine the built-in ones
(e.g., `AssetStrategy`, `LocalPathStrategy`) and their superclasses
(e.g., `AbstractPipeStrategy`) to see how to implement these things.
Then file feature requests if you cannot implement what you need.
While `StreamProvider` is not going to support every possible
source of data "out of the box", it should offer enough hooks
to let you implement arbitrary data sources yourself, and if it
does not, that may represent a worthwhile enhancement.

## Adding Columns to `query()`

You may wish to add other columns in response to a `query()`
call, beyond the `OpenableColumns` that `StreamProvider` handles
itself and the `_DATA` and `MIME_TYPE` columns added by
`LegacyCompatCursorWrapper`.

To do that, override `getValueForQueryColumn()` in your `StreamProvider`
subclass. This is supplied the `Uri` of the content and the name
of the column requested by the client. You can return an `Object`
suitable for stuffing into a `MatrixCursor` to send back &mdash;
typically, this will be a `String`, `int`, or `long`.

## Totally Overhauling Uri Handling

`StreamProvider` itself holds onto a `CompositeStreamStrategy`,
delegating all operations to it. If you wish to extend
`CompositeStreamStrategy` and do things differently, also override
`buildCompositeStrategy()` on your `StreamProvider` subclass, 
to return the instance of the `CompositeStreamStrategy` that you
want the `StreamProvider` to use.

## Overriding Standard Methods

You can override standard `ContentProvider` methods (e.g., `getType()`)
if needed.

Alternatively, you can override the methods on a `StreamStrategy`,
then use that alternative `StreamStrategy` implementation in
your `buildStrategy()` method.

## Adding Support for insert() and update()

By default, none of the `StreamStrategy` implementations support
`insert()` or `update()`. However, your custom `StreamStrategy`
can, whether you are extending one of the stock strategy classes
or are implementing your own from scratch.

First, override `canInsert()` and/or `canUpdate()`, returning
`true` for those operations you do support. Then, you can
override `insert()` and `update()`, which have the same method
signatures on `StreamStrategy` as they do on `ContentProvider`.
There, you can do what you wish.
