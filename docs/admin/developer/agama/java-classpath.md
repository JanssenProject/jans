---
tags:
  - administration
  - developer
  - agama
---

# Java classpath

Any public method from a public class or static method from a public interface that belongs to the classpath of jans-auth webapp can be used in Agama flows. This means a class/interface is accessible as long as it is part of:

- `jans-auth.war > WEB-INF/lib/*.jar` or,
- `jans-auth.war > WEB-INF/classes` or,
- `/opt/jans/jetty/jans-auth/custom/libs/*.jar` (may require edition of `jans-auth.xml`)   

Additionally, it is possible to upload source code on the fly to augment the classpath. Any valid Java or Groovy file is accepted and must be located under `/opt/jans/jetty/jans-auth/agama/scripts`. A class named `com.acme.Person` for instance, must reside in `com/acme/Person` under the `scripts` directory.

Specifically, classes in `scripts` directory can only be accessed through `Call` directives. As an example suppose you added classes `A` and `B` to `scripts`, and `A` depends on `B`. `Call`s using class `A` will work and any change to files `A` and/or `B` will be picked automatically. On the contrary, trying to load this kind of classes using `Class.forName` either from a jar file in `custom/libs` or from Agama itself will degenerate in `ClassNotFoundException`. Note `A` and `B` can also depend on classes found in any of the three locations listed above.

This "hot" reloading feature can be a big time saver while developing flows because there is no need to restart the jans-auth webapp. Also, only the files that get modified are effectively re-compiled.
