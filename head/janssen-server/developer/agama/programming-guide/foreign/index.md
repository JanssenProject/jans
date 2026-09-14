# Calling foreign code

Earlier, in other examples, it was mentioned low-level details and computations of flows are delegated to an underlying foreign language. Agama itself does not offer means to write business logic of much use - it's only focused on the "skeleton" of flows. Engines are in charge of providing mechanisms to "bridge" Agama to more powerful tools of computation.

The [`Call`](https://docs.jans.io/stable/agama/language-reference/#foreign-routines) directive is the vehicle engines use to implement such kind of "bridge" to interface with external languages. In the case of the Janssen engine, for instance, [Java/Groovy](https://docs.jans.io/stable/janssen-server/developer/agama/jans-agama-engine/#foreign-calls) code can be invoked via `Call`.

**Note**: Acquaintance with Java or Groovy is not strictly required to study the examples shown in this page. Code shown is simple and explained wherever possible. A thorough understanding of the foreign calls is not necessary to figure out the purpose of the computations presented.

## Basic examples

Flow [`com.acme.basic.foreign_1`](./project/code/com.acme.basic.foreign_1.flow) is divided into several sections that exemplify simple calls.

### Concatenation

The below code shows how to concatenate some strings:

```
strings = [ "Better", "safe", "than", "sorry" ]
proverb = Call java.lang.String#join " " strings
Log "A proverb says: '%'" proverb
```

Here a *list* of Agama *string*s are built. Then the static method `join` of class `java.lang.String` is invoked by passing a whitespace and the list.

Method `join` returns a (Java) `String` composed by joining the `String`s passed using the first one as separator. Thus, the output in the log would read: `A proverb says: 'Better safe than sorry'`.

More exactly, the [method signature](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#join(java.lang.CharSequence,java.lang.CharSequence...)) is `public static String join​(CharSequence delimiter, CharSequence... elements)`. When using `Call`, Agama *string*s are interpreted as Java `String`s. `String` objects implement the `CharSequence` interface, so data types are compatible here.

The `String` returned is assigned to variable `proverb` which is an Agama *string*.

### Finding the smallest

Here is how to find the smallest number in a list:

```
numbers = [ 2, -2, 0, 3, -3, 4 ]
smallest = Call java.util.Collections#min numbers
Log "Smallest number is:" smallest
```

An Agama *list* of *number*s  is passed to the [min](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#min(java.util.Collection)) method of class `java.util.Collections`. Internally, an object compatible with `Collection<Integer>` based on the Agama list is built so the invocation can be made.  

The output in the log would read: `Smallest number is: -3`.

### Pseudo-random number generation

So far, only static methods have been invoked. In this example, a Java object will be created and an instance method will be called:

```
sr = Call java.security.SecureRandom#new
randNumber = Call sr nextInt 4
```

Firstly, an instance of class `java.security.SecureRandom` is created. This is done by issuing a call similar to a static method invocation using `new` this time.

Object `sr` allows to generate cryptographically strong pseudo-random numbers now. In this case, method [`nextInt`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Random.html#nextInt(int)) is called on it. 4 is passed as parameter so `randNumber` will be assigned any of 0, 1, 2, or 3.

Note syntax differ from static calls. When invoking instance methods, the object is passed followed by the method name and then the arguments.

### void returning methods

Calling methods that return nothing are handled the same way than methods that return an actual value. Here's an example:

```
lil = Call java.util.LinkedList#new
Call lil add 3.1416
```

A [`LinkedList`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html) is created and then a number added to it via the [`add`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html#add(E)) method.

Note methods that return something do not necessarily have to have an assignment variable to "receive" the value - it can be simply ignored.

### Exception handling

Handling errors is important in flow development. In the case of Janssen engine, flows crash (terminate abruptly) when a Java exception occurs. To avoid this, exceptions (those events that occur when something abnormal or unexpected happens) can be catched for further processing:

```
number | E = Call java.lang.Integer#parseInt "Aha!" 16
Log "@e An error occurred." E.message
```

Here, the static method [`parseInt`]( https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html#parseInt(java.lang.String,int)) of class `java.lang.Integer` is called. The idea is trying to parse the string Aha! assuming it is a base-16 representation. Clearly, `h` and `!` are not in the hex numbering system so this throws a `java.lang.NumberFormatException`.

The `| variable` indicates that if an exception occurs (even if [unchecked](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/RuntimeException.html)), it is caught and assigned to the given variable.   

In this case, `number` will be `null` and `E` will be an instance of `NumberFormatException`. The output in the log would read: `An error occurred. For input string: "Aha!" under radix 16`. Here, `E.message` is the equivalent of doing `getMessage()` on the Java exception. If instead of "Aha!", "Aba" was used, `number` would have a value of 2746 while `E` would be `null`.
 
If a method that returns nothing throws an exception, it can be caught like `| E = Call ...`, i.e. the assignment variable is not present.   

## Intermediate examples

Flow [`com.acme.basic.foreign_2`](./project/code/com.acme.basic.foreign_2.flow) contains `Call` examples which are a bit more advanced and requires background in Java. Developers are encouraged to take a look at this flow. Explanations are found in the form of comments there.

Ideally, Java developers would like to go over the [Foreign Calls](https://docs.jans.io/stable/janssen-server/developer/agama/jans-agama-engine/#foreign-calls) section of the Janssen's Agama engine documentation page for a deeper, more formal insight on how calls work. Important topics like rules of conversion between Agama and Java data types are covered. 

<!--
difficult to explain:

found = Call numbers contains 0
Log "Zero in the list:" found

found = Call numbers contains 1
Log "One in the list:" found

empty = Call numbers isEmpty
Log "Nothing in the list:" empty

-->

## About Real-world calls

The examples covered so far illustrate how to interface with Java, however, these computations are dummy because real flows need to deal with complex logic where resources like services, databases, files, etc., are involved. In practice, flow developers use specialized libraries instead of just plain Java SE APIs to solve their problems. The patterns for calling code remain the same of course.  

One thing to watch out for is piling up of `Calls`. Situations like the below must be avoided:

```
x = … // A java object obtained in some way
y = … // A java object obtained in some way
Call x methodA arg1 arg2 …
Call x methodB arg1 arg2 arg3 …
z = Call y methodC …
Call x methodD … z …
```

Most likely these lines can be grouped into a single method invocation which should be thoroughly implemented in Java. More than three consecutive `Call`s in a flow may indicate something is not proper in terms of flow design. Do not use Agama to do Java programming. The same applies for any other engine/language combination.
