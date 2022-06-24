# Java classpath

Any public method from a public class or static method from a public interface that belongs to the classpath of jans-auth webapp can be used in Agama flows. This means a class/interface is accessible as long as it is part of:

- `jans-auth.war > WEB-INF/lib/*.jar` or,
- `jans-auth.war > WEB-INF/classes` or,
- `/opt/jans/jetty/jans-auth/custom/libs/*.jar` (may require edition of `jans-auth.xml`)   

Additionally, it is possible to upload source code on the fly to augment the classpath. Any valid Java or Groovy file is accepted and must be located under `/opt/jans/jetty/jans-auth/agama/scripts`. A class named `com.acme.Person` for instance, must reside in `com/acme/Person` under the `scripts` directory.

This "hot" reloading feature is a big time saver while developing flows because there is no need to restart the jans-auth webapp. Also, only the files that get modified are effectively compiled in the background.
