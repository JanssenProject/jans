# Key language traits

This page does not feature any example, instead, it highlights important aspects of the language which are key to write valid programs in Agama. Readers are encouraged not to skip this topic.

Technically Agama is a DSL (domain-specific language) for depicting web flows. It's simple, compact, and expressive enough to cope with intricate flows despite the number of language [constructs](https://docs.jans.io/stable/agama/language-reference/#language-keywords) is small.

At first, developers may find the following features unexpected:

- Only basic logical and equality operators are supported (no bitwise, arithmetic, concatenating operators, or the like)
- Absence of parenthesis
- Parameters passed to directives are separated by spaces - not commas

In practice, instead of being limiting factors, these aspects help developers write code that clearly expresses flow structure and introduces bias in favor of writing low-level logic in a foreign language instead of Agama itself.

An upcoming example will cover how large or complex flows can be better tackled via decomposition. 

## About parameters

An important syntactical restriction is that *list*s and *map*s can be passed as parameters only in the form of variables. This forces developers write succint directive statements but may require adding some extra preceding lines of code. In general this tends to produce shorter, easier-to-read, less cluttered lines of code. 

As an example, suppose there exists a directive called `Centroid` that computes the geometric centroid of a triangle. This directive would receive three params with the (_x_, _y_) coordinates of the triangle. In this case, the following would be syntactically illegal:

```
point = Centroid { x: 0, y: 0 }  { x: 0, y: 1 }  { x: 1, y: 0 }
```

Instead, developers are forced to use a form like:

```
point = Centroid a b c
```

Where `a`, `b`, and `c` are variables that already have the values of interest. The following is also considered valid:

```
point = Centroid someVar.key anotherVar[0] superVar.key[myNumber].p
```

The above parameters are not including any spaces, commas, or other distracting characters making the statement easy to read.

These restrictions apply only to params which are *list*s or *map*s. When they are literals (*number*s, *boolean*s, *string*s, or just `null`), they can be passed directly. So in general, things like the below are legal:

```
// 7 params passed here 
val = SomeDirective "hi" true -3.1416 null myVar myList[0] myMap.key
// myVar, myList[0], and myMap.key can belong to any type  
```

## About composite calls

Aligned to the syntactic "style" described so far, another restriction is the inability to make composite invocations in a single line, that is, supplying the output of a directive as an input for another. It is common in general-purpose language to find calls like `f(g(h(x)))` where *f*, *g*, and *h* are functions (procedures, methods, routines, etc.). This is not allowed in Agama.

As an example, if a directive named `DistanceFromOrigin` measures the Euclidean distance between an (_x_, _y_) point to the (0, 0) point, the following is **not** valid: `DistanceFromOrigin Centroid a b c`. Even if `Centroid` received a single parameter (a list of points), it is also invalid to write: `DistanceFromOrigin Centroid points`. A correct way to write this computation is: 

```
point = Centroid a b c
d = DistanceFromOrigin point
```

<!--
Some features that set Agama aside from typical languages include:

Being primarily focused on expressing (flow) structure, many idioms featured in s are unnecessary
This has proved to be enough for writing flows!.
-->
