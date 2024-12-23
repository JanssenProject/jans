---
tags:
  - administration
  - developer
  - agama
---

# Frequently asked questions

## Common errors

### Unable to find a suitable constructor with arity ... in class ...

A Java invocation of the form `Call package.className#new ...` is passing an incorrect number of arguments or their data types do not match the signature of any of the constructors for that class. 

### Unable to find a suitable method called ... with arity ... in class ...

A java invocation is attempting to call a method that is not part of the given class, the number of arguments passed for the method is not correct, or the arguments could not be converted to make a successful call.

### No Finish instruction was reached

This occurs when no `Finish` statement has been found in the execution of a flow and there are no remaining instructions.

### Serialization errors

Agama engine saves the state of a flow every time an [RRF](../../../agama/language-reference.md#rrf) or [RFAC](../../../agama/language-reference.md#rfac) instruction is reached. The _State_ can be understood as the set of all variables defined in a flow and their associated values up to certain point. 

Most of times, variables hold basic Agama [values](../../../agama/language-reference.md#data-types) like strings, booleans, numbers, lists or maps, however, more complex values may appear due to Java `Call`s. The engine does its best  to properly serialize these Java objects, nonetheless, this is not always possible. In these cases, the flow crashes and errors will appear on screen as well as in the server logs.

Use the information in the logs to detect the problematic Java class. This would normally allow you to identify the variable that is causing the issue. Now you have several options to proceed:

- Check if the value held by the variable is needed for the given RRF/RFAC or some upcoming statement. If that's not the case, simply set it to `null` before RRF/RFAC occurs
- Extract only the necessary pieces from the variable, that is, grab only the fields from the object which are of use for the rest of the flow. If they are simple values like strings or numbers, serialization will succeed. Ensure to nullify or overwrite the original variable
- Adjust the given class so it is "serialization" friendlier. Sometimes, adding a no-args constructor fixes the problem. In other cases, making the class implement the `java.io.Serializable` interface will make it work. The error in the log should provide a hint 
- Tweak the engine serialization [rules](./engine-bridge-config.md#serialization-rules) so an alternative type of serialization can be used for this particular object 
- Modify your Java code so an alternative class is used instead

In general, a good practice is to avoid handling complex variables in flows. Letting big object graphs live in the state has a negative impact on performance and also increases the risk of serialization issues.   

## Libraries and classes added on the fly

### What Groovy and Java versions are supported?

Groovy 4.0 and Java 8 or higher. The runtime is Amazon Corretto 17.

### How to add third party libraries?

You can include jar files in the `lib` directory of a [project](../../../agama/gama-format.md#anatomy-of-a-project). This applies only for VM-based installation of Janssen. Onboarding the jar files require a restart of the authentication server.

<!--
### A class does not "see" other classes in its own package

This is a limitation of the scripting engine. Here, classes have to be imported even if they belong to the same package, or the fully qualified name used.
-->
### A class is still available after removing the corresponding file

This is because the JVM does not support unloading: even if a given source file is removed, its corresponding class will still be accessible - it remains in the classpath. The classpath will be clean again after a service restart.

### How to add log statements?

The Jans server uses [slf4j](https://slf4j.org) and [log4j2](https://logging.apache.org/log4j/2.x/) logging frameworks. Below is a simple usage example for Java/Groovy code:

```
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

...
Logger logger = LoggerFactory.getLogger(your.class);
logger.info("ahoy, ahoy");
```

!!! Note
    The logging descriptor used by the server can be found [here](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/resources/log4j2.xml)

Depending on the package `your` class belongs to, the message may not appear in the server log. In this case, you have two choices:

- Supply a [custom log4j](../../auth-server/logging/custom-logs.md) descriptor
- Use a class in package `io.jans` when calling `getLogger`. A good example would be using `io.jans.agama.model.Flow`

### How to append data to a flow's log directly?

Call method `log` of class `io.jans.agama.engine.script.LogUtils`. This method receives a variable number of arguments as DSL's `Log` does. Thus you can do `LogUtils.log("@w Today is Friday %th", 13)`, as in the logging [examples](../../../agama/language-reference.md#logging).

### How to use Contexts and Dependency Injection (CDI)?

Jans server uses Weld (a CDI reference implementation), and as such makes heavy use of managed beans, scopes, dependency injection, events, etc. Unless the code added is part of a jar [library](#how-to-add-third-party-libraries), annotations related to scopes or dependency injection won't take any effect in your code. This is because the Java container does a thorough scanning of classes upon start, but the source code files in `lib` directory are compiled upon use and modification, as expected in a scripting scenario.

Java/Groovy files can however reuse any of the (application-scoped) managed beans available in the server's classpath. To obtain a reference to a bean, use a call like:

```
import io.jans.service.cdi.util.CdiUtil;
...
ref = CdiUtil.bean(managedBean.class);
```

More advanced bean lookup capabilities are provided by method `instance` of [this](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/engine/src/main/java/io/jans/agama/engine/service/ManagedBeanService.java) class where you can supply qualifiers.

## Templates

### How to implement localization?

This topic is covered [here](./advanced-usages.md#localization-and-internationalization).

### How to modify the built-in error pages?

Agama features some error pages out-of-the-box. They can be customized by editing the corresponding files referenced in the [configuration](./engine-bridge-config.md#engine-configuration) doc page.  

### Is Javascript supported?

Yes, the template engine is not concerned about the kind of contents linked in markup. It does not even care if your markup makes any sense. For FreeMarker, templates just produce a textual output. 

### A flow page is not rendered as HTML, the browser just shows the source code as text

This is due to lack of proper `Content-Type` response header. This occurs if your template has filename extension other than `ftlh`. If you don't want to rename your template, insert `<#ftl output_format="HTML">` at the beginning of the file.

### How to produce JSON instead of HTML?

In your templates use `<#ftl output_format="JSON">` in the first line. Take a look at the templates whose name start with `json_` in folder `/opt/jans/jetty/jans-auth/agama`. Particularly check how JSON content can be escaped - unfortunately FreeMarker does not support escaping for JSON out-of-the-box.

## Development tools

### Are there any IDE or editor plugins available for coding flows and manage projects?

Not plugins but you can use [Agama Lab](https://agama-lab.gluu.org) for projects and flows authoring. 

### How to debug flows?

We plan to offer a debugger in the future. In the meantime, you can do `printf`-like debugging using the `Log` instruction. See [Agama logging](./jans-agama-engine.md#logging).

## Miscellaneous

### Does the engine support AJAX?

If you require a flow with no page refreshes, it could be implemented using AJAX calls as long as they align to the [POST-REDIRECT-GET](./advanced-usages.md#flow-advance-and-navigation) pattern, where a form is submitted, and as response a 302/303 HTTP redirection is obtained. Your Javascript code must also render UI elements in accordance with the data obtained by following the redirect (GET). Also, care must be taken in order to process server errors, timeouts, etc. In general, this requires a considerable amount of effort.

If you require AJAX to consume a resource (service) residing in the same domain of your server, there is no restriction - the engine is not involved. Interaction with external domains may require to setup CORS configuration appropriately in the authentication server.  

### How to launch a flow?

A flow is launched by issuing an authentication request in a browser as explained [here](./jans-agama-engine.md#launching-flows). 

### Does flow execution timeout?

Yes. The maximum amount of time an end-user can take to fully complete a flow is driven by the configuration of the authentication server and can be constrained even more in the flow itself. Read about timeouts [here](./jans-agama-engine.md#how-timeouts-work).

### How to prevent launching a flow directly from the browser?

This can be configured in the project's [metadata file](../../../agama/gama-format.md#metadata).

### Updates in a flow's code are not reflected in its execution

When a project is re-deployed, a flow remains unchanged if it comes with errors in the provided `.gama` file. The tool used for deployment will let you know which flows have errors and their descriptions. 

### Why are the contents of a list or map logged partially?

This is to avoid traversing big structures fully. You can increase the value of `maxItemsLoggedInCollections` in the [engine configuration](./engine-bridge-config.md#engine-configuration).

### How to add two numbers or compare numeric values in Agama?

Agama only provides operators for equality/inequality check in conditional statements. Comparisons like "less-than", "greater-than-or-equal", etc. require Java usage, however, the structure of an authentication flow will rarely have to deal with this kind of computations.

_Hint_: methods like `addExact`, `incrementExact`, etc. in `java.lang.Math` may help you to do some arithmetic.  

### How to concatenate strings in Agama?

See the previous answer. A two-lines solution could be:

```
strings = [ s1, s2, ... ]
Call java.lang.String#join "" strings
```

### How to know the index of a given loop iteration?

See the examples in the Looping section of the [language reference](../../../agama/language-reference.md#looping).

### How to know the number of iterations carried out by a loop once it has finished?

You can assign this value to a variable at the top of your loop declaration. See the examples in the Looping section of the [language reference](../../../agama/language-reference.md#looping).

### How to access localization labels from Java code?

In Freemarker, [localization](./advanced-usages.md#localization-and-internationalization) labels are accessed using the `${labels(key, ...)}` notation. The following would be a Java equivalent:

```
import io.jans.agama.engine.service.LabelsService;
...

lbls = io.jans.service.cdi.util.CdiUtil.bean(LabelsService.class);
String label = lbls.get("<label key>", ... optional extra params);
```

Note the localization context (language, country, etc.) used in such a call is based on the HTTP request that is taking place when the code is invoked.

### Can Agama code be called from Java?

No. These two languages are supposed to play roles that should not be mixed, check [here](./agama-best-practices.md#about-flow-design).
