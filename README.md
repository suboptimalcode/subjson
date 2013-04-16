# subjson

A lightweight, easy-to-use, json parser and printer.

This library was designed with two main considerations in mind: 

    - The API should be as easy as calling a public static read() method that returns an in-memory representation of the json value just parsed.
    - The parser should read a character at a time off of a stream, and leave the stream at the first character after a json value has been read.
    
The former is simply a nice way to use a json parser when your needs are simple. The latter is something that can sometimes be required for certain uses, as it was when it was written. Additionally, it would be nice if the library was efficient and easy to use from other JVM languages. Some of these goals are clearly in tension with one another, but SubJson tries to strike a balance.

The parser is invoked with a call to `SubJson.read()`, passing in either a `String` or `Reader`. By default, it maps json lists and maps to Java `ArrayList`s and `HashMap`s, with strings and booleans mapped to the `java.lang` equivalents. Nulls are mapped to `null` object references, and numbers are parsed as either `Long` or `Double` depending on the value in the json source (arbitrary precision is currently not supported). If a `Reader` is given to `read`, then after `read` successfully returns, the `Reader` given as input will be positioned on the first character after a json value has been read. The one ambiguity would be from numbers: "12345" can be parsed as a single json value in 5 ways, from "1" to "12345"; SubJson always parses the longest json value that is correctly formatted.

An additional argument can be passed to `read`, an object implementing the `FromJsonPolicy` interface. By implementing this interface, custom mappings can be set up for application- or language-specific types. Since all that is required to customize the mapping is the implementation of an interface, it is hoped that interoperation with other JVM languages should be painless. There is a built-in Clojure implementation included.

The parsing adheres strictly to the [json standard](http://www.ietf.org/rfc/rfc4627.txt?number=4627), with no options for common extensions like trailing commas or comments. When it encounters a parsing error, it throws an exception with an informative error message. While being "the fastest json parser on the JVM" or any such thing is not a goal, in synthetic benchmarks SubJson ranges from "almost the same speed as Jackson" to "about 30% slower than Jackson," depending on the input. Results will vary, of course, but performance so close to Jackson suggests that for light use, the library is efficient.

SubJson will also print the objects it has parsed (or objects with the same types/format) back to json using the `SubJson/write()` call. Pass in an `Appendable`, such as a `Writer` or `StringBuilder`, and a java object in the format returned from `read()` (that is, `ArrayList`s, `HashMap`s, `Number`s, `String`s, `Boolean`s, and `null`). By default, the json will be pretty-printed into an indented format. An optional third argument can be passed with the value `false` to turn off pretty-printing and instead return json that is very compactly formatted, with minimal whitespace. Finally, a fourth argument can contain an object that implements the `ToJsonPolicy` interface; this object will direct how Java values are mapped back into json values, and can be used to create application- or language-specific bindings.

The javadoc should be referenced for details; it can be generated at the command line by issuing the command

```
lein javadoc
```

and viewing the file generated at `javadoc/index.html`.

The library is not suited to certain perfectly valid use cases. Jackson is probably a good starting point for solving these:
    - If you need the absolute maximum level of performance
    - If you want to use json as a serialization format for java classes
    - If you want to parse or partially parse gigantic blobs of json in a stream-oriented manner without building objects up in memory

## Usage

### Java

```java
import su.boptim.al.SubJson;
SubJson.parse("[1,2,3,4,5]");
```

### Clojure

```clojure
(require '[su.boptim.al.subjson :as subjson])
(subjson/read "[1,2,3,4,5]")
```

## Obtaining

Add the following to your project.clj file or profiles:

```clojure
:dependencies [[su.boptim.al/subjson "0.2.0"]]
```

## License

Copyright © 2013 David Santiago

Distributed under the Eclipse Public License, the same as Clojure.
