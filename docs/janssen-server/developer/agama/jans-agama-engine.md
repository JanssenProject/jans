---
tags:
  - administration
  - developer
  - agama
---

# Janssen Agama Engine

## Introduction

The Janssen Server implements the [Agama framework](../../../agama/introduction.md) through several components bundled in its authentication server (AS):

- The engine: the piece of software that actually runs the flows and interacts with the user's browser - sort of a small web application. As expected, these flows are mainly targetted at user authentication

- A transpiler: code written in Agama language is transpiled to an intermediate representation which can then be interpreted at runtime by the engine

- The bridge: since the engine itself does not implement an authentication protocol, another piece is required to put flows in the context of a standard authorization framework like OpenId Connect. This is what the bridge does

- The deployer: a piece of software capable of deploying [Agama projects](./projects-deployment.md) to the engine.

The rest of this document describes implementation-specific details of the engine bundled with the Jans Server. These details give shape to the requirements abstractly defined for an Agama-compliant [engine](../../../agama/execution-rules.md).

!!! Note
    Ensure both the Agama engine and the bridge are [enabled](./engine-bridge-config.md#availability) in the Janssen server to effectively use engine's features

## Launching flows

Flows can be launched by sending an (OpenId Connect) authentication request to the user's browser. This usually boils down to making a redirection to a URL looking like `https://<jans-server-name>/jans-auth/restv1/authorize?acr_values=agama_flowQname&scope=...&response_type=...&redirect_uri=https...&client_id=...&state=...`. Check the OpenId Connect [spec](https://openid.net/specs/openid-connect-core-1_0.html) for more details. Note Jans Server is spec-compliant.

Things to highlight:

- The `acr_values` parameter carries the qualified name (identifier) of the flow to launch prefixed with the string `agama_`, for example `acr_values=agama_test.acme.co`

- If the flow to call receives input parameters, this data can be appended to the `acr_values` parameter: use a hyphen to separate the flow name and the parameters expressed in Base64 URL encoded format. For example, if the flow had inputs  `height` and `color`, you would encode the string `{"height": 190, "color": "blue"}` and the resulting value would be `agama_test.acme.co-eyJoZWlnaHQiOiAxOTAsICJjb2xvciI6ICJibHVlIn0`. When a given input variable is not provided, the engine will assign a `null` value automatically

## Authentication and `Finish`

When a top-level flow (i.e. one with no parents) finishes successfully, the selection of the user to authenticate is driven by the `userId` passed in `data`, as in

```
obj = { success: true, data: { userId: "john_doe" } }
// map literals cannot be passed directly to Finish - a variable can be used instead
Finish obj 
```

By default `userId` maps to the `uid` attribute that generally all user entries already have in the database of Jans Server, in other words, the "user name". This attribute is configurable though via property `finish_userid_db_attribute` of the [bridge script](./engine-bridge-config.md#bridge-configuration). In some cases, you would like to change that to `mail` in order to pass things like `{ success: true, data: { userId: "john_doe@jd.me" } }` for instance. 

!!! Important
    If the database lookup does not produce exactly one user entry, this is treated as a failed authentication attempt by the authentication server.

When the authentication succeeds, the whole contents of `data` are stored in the authentication server's session of the given user under the key `agamaData`. Contents are serialized to a JSON string previously.

## Crashes, timeouts, and failures

[Execution rules](../../../agama/execution-rules.md) define several possible [flow states](../../../agama/execution-rules.md#flows-lifecycle). For crashed, timed out, and finished failed flows, the engine will present proper error pages to users. These are configurable by properties `crashErrorPage`, `interruptionErrorPage`, and `finishedFlowPage` respectively, of the [engine-configuration](./engine-bridge-config.md#engine-configuration).

### How timeouts work

Authentication flows are normally short-lived. They usually span no more than a few minutes. In Agama, the maximum amount of time an end-user can take to fully complete a flow is driven by the [configuration of the authentication server](../../config-guide/auth-server-config/jans-authorization-server-config.md), specifically the `sessionIdUnauthenticatedUnusedLifetime` property which is measured in seconds. As an example, if this value is 120, any attempt to authenticate taking more than two minutes will throw the given error page.

Moreover, when a flow specifies its own timeout in the [header](../../../agama/language-reference.md#header-basics) the effective timeout is the smallest value between `sessionIdUnauthenticatedUnusedLifetime` and the value in the header.

Depending on specific needs, `sessionIdUnauthenticatedUnusedLifetime` may have to be set to a higher value than the server's default. This may be the case where flows send e-mail notifications with temporary codes, for instance.

## Logging

There are three relevant sources of log data:

- The engine. It emits information related to flows transpilation, projects deployment, and flow crashes - generally low-level information
- [Log](../../../agama/language-reference.md#logging) instructions. These are statements originated directly from the Agama code
- [Call](#foreign-calls) directives. Foreign code can issue logging statements as well as any other code a `Call` may depend on. See the [FAQ](./faq.md#how-to-add-log-statements)

The following table details the location of log data. Paths are relative to directory `/opt/jans/jetty/jans-auth/log`:

|Source|Destination file|
|-|-|
|Engine|`jans-auth.log`|
|`Log` instructions|`jans-auth_script.log`|
|Foreign code|`jans-auth.log`|

Depending on the specificity required, you may have to change the logging level so more or less details appear in the logs. This can be done by altering the `loggingLevel` property of the [auth server configuration](../../config-guide/auth-server-config/jans-authorization-server-config.md). `DEBUG` usually suffices for troubleshooting.

The available levels for statements issued with the `Log` instruction are:

|Level name|shortcut|
|-|-|
|`error`|e|
|`warn`|w|
|`info`|i|
|`debug`|d|
|`trace`|t|

For instance, these two instructions are equivalent: `Log "@e Universe collapsed"` and `Log "@error Universe collapsed"`

The engine will use `info` when the level is not specified explicitly, as in `Log "Look ma!"`.

## RFAC and Callback URL

Engine's callback URL is `https://<your-server-name>/jans-auth/fl/callback`. This resource is only be available for a given browser session while `RFAC` is in execution. Once the callback is visited or the flow times out (whichever occurs first), subsequent requests will respond with an HTTP 404 error.

The mechanism used for redirection is a "302 Found" HTTP redirect that entails a subsequent GET request to the external site. In cases where a POST is expected, the 3-param version of [RRF](./flows-navigation-ui.md#3-param-variant) can be useful.

## RRF, navigation, and assets handling 

Understanding how `RRF` works in the engine is key to writing meaningful flows. This topic is fully covered [here](./flows-navigation-ui.md).

## Foreign calls

In the Jans Agama engine, Java or Groovy code can be called by means of the `Call` instruction. Specifically, public methods from public classes or static methods from public interfaces.

!!! Note
    Java language background is required for this section.
    
The following exemplifies different kind of usages of Java from Agama code:
 
<table>
	<tr><th>Example</th><th>Notes</th></tr>
	<tr>
<td>

```
Call java.lang.Integer#parseInt "FF" 16
```

</td>
		<td>Invokes the <code>parseInt</code> method of <code>Integer</code> class passing the given arguments (i.e. conversion of hexadecimal string to a primitive <code>int</code>). The returned value is ignored</td>
	</tr>
	<tr>
<td>

```
numbers = [ 2, -2, 0, 3, -3, 4 ]
small = Call java.util.Collections#min numbers
```

</td>
		<td>Invokes the <code>min</code> method of the <code>Collections</code> class.<br/>Supplies a list of numbers as argument.<br/>The smallest number is stored in variable <code>small</code></td>
	</tr>
	<tr>
<td>

```
jLocale = Call java.util.Locale#getDefault
localeName = jLocale.displayName
```

</td>
		<td>Computes the display name of the JVM default locale</td>
	</tr>
	<tr>
<td>

```
car = { brand: "Ford", model: 1963 }
sidecar = Call java.util.Map#copyOf car
```

</td>
		<td>Makes a deep clone of a map</td>
	</tr>
	<tr>
<td>

```
number | E = Call java.lang.Integer#parseInt "AGA" 16
When E is not null
    Log "An error occurred:" E.message 
```

</td>
		<td>Similar to the first example.<br/>If an exception is thrown by the invocation, it's caught and assigned to variable <code>E</code>.<br/>Note both checked and unchecked exceptions are caught</td>
	</tr>
	<tr>
<td>

```
| E = Call com.acme.Worker#notifyExternalSystem
//Do something with E
//... 
```

</td>
		<td>If exception catching is required when calling a method that returns <code>void</code>, there is no need to put a variable before the pipe</td>
	</tr>
	<tr>
<td>

```
p1 = Call java.awt.Point#new
p2 = Call java.awt.Point#new 1 3
Call p2 translate -1 1
```

</td>
		<td>Creates instances of class <code>java.awt.Point</code> with different constructors. For <code>p1</code> the no-args constructor is used.<br/>Method <code>translate</code> is invoked on the <code>p2</code> instance. Note usage of a space here instead of hash (<code>#</code>)</td>
	</tr>
	<tr>
<td>

```
cls1 = Call java.lang.CharSequence#class
cls2 = Call java.lang.Integer#class
Call cls2 parseInt "FF" 16
```

</td>
		<td>Stores in <code>cls1</code> a reference to interface class <code>CharSequence.class</code>.<br/>Stores in <code>cls2</code> a reference to <code>Integer.class</code>.<br/>The 3rd line statement achieves the same effect of <code>Call java.lang.Integer#parseInt "FF" 16</code></td>
	</tr>
	<tr>
<td>

```
L = [ "A", "B", "C" ]
S = Call java.util.Set#of 0 2 4 6
map = { numbers: S, letters: L  }
hasOne = Call map.numbers contains 1
```

</td>
		<td>Calls the <code>contains</code> method of the <code>java.util.Set</code> object stored in <code>map.numbers</code> passing <code>1</code> as argument and storing the result in <code>hasOne</code></td>
	</tr>
</table>

The usage of a hash sign (or spaces) before a method name helps disambiguate whether the invocation is on a static class method or an object method. This is so because an expression like `hey.You` may reference the class `You` on package `hey`, or the value of key `You` in a map named `hey`.

### Highlights

Any method that meets the conditions mentioned (public or interface static) and that is reachable in the JVM [classpath](#classpath) can be called; developers are not restricted solely to `java.*` packages. 

When using `Call`, the method to execute is picked based on the name (e.g. after the `#` sign) and the number of arguments supplied. If a class/interface exhibits several methods with the same name and arity (number of parameters), the method that best matches the dataypes of the arguments with respect to its signature is selected. Sometimes this requires to perform arguments [conversions](#arguments-conversion) and they may fail. In such case, the second best suited method is tried and so on. 

When all attempts fail or there are no candidate methods to choose from, the `Call` simply throws a `NoSuchMethodException`.

For non-static method invocations, i.e. no hash sign, the class used for method lookup is that of the instance passed (the first parameter in the `Call` directive). This includes all associated superclasses too, as expected. When the instance does not hold a Java but an Agama value, the following is used to pick a class:

|Agama type|Java class for method lookup|
|-|-|
|`string`|`String`|
|`boolean`|`Boolean`|
|`number`|`Double`|
|`list`|`java.util.List`|
|`map`|`java.util.Map`|

**Limitations:**

- _list_ and _map_ literals cannot be passed as arguments to method calls directly. This means the following is illegal: `Call co.Utils#myMethod { key: [ 1, 2 , 3] } [ "Yeeha!" ]`. To achieve the same effect assign the literal value to a variable and pass that instead

- `Call`ing a method that mutates one or more of the arguments passed will not work properly if the corresponding parameters in the method signature have type information attached. For example, copying a list into another using `java.util.Collections#copy​(List<? super T> dest, List<? extends T> src)` may not behave as expected. Conversely, calling `java.lang.reflect.Array#set​(Object array, int index, Object value)` works fine because `array` does not have a parameterized type. The practice of mutating passed arguments is unusual and sometimes discouraged in programming

### Exception handling

As seen in the examples Agama engine can deal with Java exceptions, however, this feature should be used sparingly. When exception handling adds undesired complexity to your code, create wrapper methods in Java and do the processing there instead of delegating that to the DSL.

### Arguments conversion

[Agama types](../../../agama/language-reference.md#data-types) do not match Java types. This means passing a "native" Agama value as parameter in a method `Call` requires some form of compatibility with the target (Java) type in the method signature.

An argument (Agama value) is compatible with a method parameter if it can be "converted" successfully. As we'll see, conversion feels pretty natural in practice. If this process fails a `java.lang.IllegalArgumentException` is thrown and the flow will crash unless the exception is caught. Note however the recommended practice is to let flows [crash](./agama-best-practices.md#about-crashes).

The following lists some of the most common successful conversions:

|Agama value|Can be converted to|
|-|-|
|`string`|`String` or `char[]`|
|`boolean`|`Boolean` or primitive equivalent|
|`number`|`Double`/`Float`/`Integer`/`Long`/`Short`/`Byte` or primitive equivalent|
|`null`|Any non-primitive|
|`list`|Array or class implementing `Collection<T>` as long as items can be converted to type `T`|
|`map`|Class implementing `Map<K, V>` as long as keys and values can be converted to types `K` and `V`, respectively|
|`map`|Java bean. Unrecognized properties in Agama value are ignored|

The below table shows some examples of interesting and handy conversions:

|Agama value|Param data type in target Java method|Argument value (Agama)|Received param value (in method)|Notes|
|-|-|-|-|-|
|Positive `number` having fractional part|`Integer`/`Long`/`Short`/`Byte` or primitive equivalent|2.6|2|Integer part kept|
|Negative `number` having fractional part|`Integer`/`Long`/`Short`/`Byte` or primitive equivalent|-2.4|2|Integer part kept|
|Integer `number`|`Float`/`Double` or primitive equivalent|1|1.0||
|`list` of `number`s|`List<Integer>`|[1, 2.0, 3.1, -4.2]|[1, 2, 3, -4]|Only integer parts kept|
|`list` of integer `number`s|`List<Float>`/`List<Double>`|[1, 2, 3, -4]|[1.0, 2.0, 3.0, -4.0]||
|`string` of length 1|`Character` or primitive equivalent|a|a|Passing a zero, two, or more lengthed string will make the call fail|

When the argument is not an Agama but a Java object/primitive, the following rules apply:

- If the value can be cast to the target type, no conversion is needed, otherwise
- If it is an instance of `java.lang.Number` and the type is a numeric primitive or wrapper (e.g. `Integer`), the value is truncated if required, otherwise
- The value is serialized to JSON - if possible - and then an attempt to create a Java instance based on the given JSON contents is made. For this purpose, the FasterXML Jackson library is used

This is powerful because it allows to send data of similar shape/structure when data types do not necessarily match. Consider the following example:

```
s = "a man's gotta do what a man's gotta do"
jStrArr = Call s split " "
words = Call java.util.Collections#unmodifiableSet jStrArr
```

This Agama snippet creates the (Java) `Set` of different words found in a given (Agama) `string`. Note `jStrArr` is of type `String[]` and is passed directly to method `unmodifiableSet` which originally expects an instance of `Set` as parameter.

### From Java to Agama

We just saw how Agama values are treated in Java code. Here we make an analysis in the reverse direction: from Java to Agama where Java values are obtained through `Call` invocations.

The following table relates Java types to Agama types:

|Java value (`x`)|Agama equivalent|Notes|
|-|-|-|
|`x` is a Java array or implements `java.util.List`|list|Changing the list contents (includes updates on x.length) can only be achieved if the Java list is modifiable|
|`x` implements `java.util.Map` and keys are of type` java.lang.String`|map|Changing the map contents can only be achieved if the Java map is modifiable|
|`x` is a `java.lang.(Double|Float|Long|Integer|Short|Byte)` or an equivalent primitive|number||
|`x` is a (non-null) `java.lang.Boolean` or equivalent primitive|boolean||
|`x` is a `java.lang.String`|string (limited)|Neither indexing nor `x.length` can be used|
|null|null||

**Except** for maps, the following holds for a value `x` obtained through a Java `Call`:

- If `x`'s class has getters and/or setters for some fields, they can be called. As an example if `age` and `name` are available fields, `x.age` can be used to get the value of `age` 

- Likewise, setting a value is possible too: `x.name = "Larry"` (if a suitable setter exists) 

- In general, a no-args method starting with `get` or `is` can be invoked. This is useful for instance to get the (Java) class name of your object (`x.class.name`) or determine if a list has no elements (`x.empty`)

### Classpath

A class/interface is accessible to Agama code as long as it is part of:

- `jans-auth.war > WEB-INF/lib/*.jar` or,
- `jans-auth.war > WEB-INF/classes` or,
- `/opt/jans/jetty/jans-auth/custom/libs/*.jar` (may require edition of `jans-auth.xml` descriptor)   

Additionally, it is possible to upload source code on the fly to augment the classpath. Any valid Java or Groovy file is accepted and must be located under `/opt/jans/jetty/jans-auth/agama/scripts`. For instance, a class named `com.acme.Person` must reside in folder `/opt/jans/jetty/jans-auth/agama/scripts/com/acme`.

!!! Important
    Only files with extensions `.java` or `.groovy` are accounted

The `scripts` directory provides automatic "hot" reloading. This is a valuable time saver for developers because there is no need to restart the jans-auth webapp when sources are modified. This feature has some limitations which are explained in the following.

#### Limitations of code added "on the fly"

Classes in `scripts` directory can only be accessed through `Call` directives. As an example suppose you added classes `A` and `B` to `scripts`, and `A` depends on `B`. `Call`s using class `A` will work and any change to files `A` and/or `B` will be picked automatically. On the contrary, trying to load this kind of classes using `Class.forName` either from a jar file in `custom/libs` or from Agama itself will degenerate in `ClassNotFoundException`. Note `A` and `B` can also depend on classes found at any of the locations listed at the beginning of this section.

Note Java sources are actually interpreted as Groovy code. 99% of times this is not a concern since Groovy can be considered a superset of Java, however, there are some minor discrepancies that may exhibit unexpected behaviors. These differences are described [here](https://groovy-lang.org/differences.html).

We consider the following to be remarkable:

- Array literals in shorthand syntax not allowed, e.g. `int[] array = {1, 2, 3}`. Use `int[] array = new int[] {1, 2, 3}` instead
- String interpolation: `"hello $mark"` evaluates the value of variable `mark` and prepends `hello ` to it!. To avoid this use a backslash, like in `"hello \$mark"`. Learn more about interpolation [here](http://groovy-lang.org/syntax.html#_string_interpolation). In general, prepend ocurrences of `$` in your string literals with a `\`
- Usage of `==` operator actually calls the `equals` method. Most of times this is fine but can be a problem when you are overriding `equals` in your class and make use of `==`. This will introduce a recursive call in your implementation and may degenerate in a stack overflow. If possible, use the `===` operator or the `is` method in these cases

### OOP prose warning

See the recommended [practices](./agama-best-practices.md#oop-prose-warning) to learn more about this topic.
