---
tags:
  - administration
  - developer
  - agama
---

# Frequently asked questions

## Common errors

### Unable to find a constructor with arity ... in class ...

A Java invocation of the form `Call package.className#new ...` is passing a number of parameters that does not match any of the existing constructors for that class. 

### Unable to find a method called ... with arity ... in class ...

A java invocation is attempting to call a method that is not part of the given class or the number of parameters passed for the method is not correct.

### No Finish instruction was reached

This occurs when no `Finish` statement has been found in the execution of a flow and there are no remaining instructions.

### Serialization errors

Agama engine saves the state of a flow every time an [RRF](../../../agama/language-reference.md#rrf) or [RFAC](../../../agama/language-reference.md#rfac) instruction is reached. For this purpose the [KRYO](https://github.com/EsotericSoftware/kryo) library is employed. If kryo is unable to serialize a variable in use by your flow, a serialization error will appear in the screen or in the logs. Normally the problematic (Java) class is logged and this helps reveal the variable that is causing the issue. Note variables that hold "native" Agama values like strings or maps are never troublesome; the problems may originate from values obtained via [Call](../../../agama/language-reference.md#foreign-routines).

To fix a serialization problem, try some of the following:

- Check if the value held by the variable is needed for RRF/RFAC or some upcoming statement. If that's not the case, simply set it to `null` before RRF/RFAC occurs
- Adjust the given class so it is "serialization" friendlier. With kryo, classes are not required to implement the `java.io.Serializable` interface 
- Find a replacement for the problematic class
- As a last resort, set `serializerType` property of the [engine](./engine-bridge-config.md#engine-config) to `null`. Note this will switch to standard Java serialization. This setting applies globally for all your flows

## Classes added on the fly

<!--
### A class does not "see" other classes in its own package

This is a limitation of the scripting engine. Here, classes have to be imported even if they belong to the same package, or the fully qualified name used.
-->
### A class is still available after removing the corresponding file

This is because the JVM does not support unloading: even if a given source file is removed, its corresponding class will still be accessible - it remains in the classpath. The classpath will be clean again after a service restart.

### How to append data to a flow's log directly?

Call method `log` of class `io.jans.agama.engine.script.LogUtils`. This method receives a variable number of arguments as DSL's `Log` does. Thus you can do `LogUtils.log("@w Today is Friday %th", 13)`, as in the logging [examples](../../../agama/language-reference.md#logging).

### What Groovy and Java versions are supported?

Groovy 4.0 and Java 11. The runtime is Amazon Corretto 11.

## Templates

### How to implement localization?

This topic is covered [here](./advanced-usages.md#localization).

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

When a project is re-deployed, a flow remains unchanged if it comes with errors in the provided `.gama` file. The response obtained by the deployment API will let you know which flows have errors and their descriptions. 

### Why are the contents of a list or map logged partially?

This is to avoid traversing big structures fully. You can increase the value of `maxItemsLoggedInCollections` in the [engine configuration](./engine-bridge-config.md#engine-config).

### How to add two numbers or compare numeric values in Agama?

Agama only provides operators for equality check in conditional statements. The structure of an authentication flow will rarely have to deal with computations/comparisons of numbers, strings, etc. In case this is needed, developers have to resort to Java.

_Hint_: some methods like `addExact`, `incrementExact`, etc. in `java.lang.Math` might help.  

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

### Can Agama code be called from Java?

No. These two languages are supposed to play roles that should not be mixed, check [here](./recommended-practices.md#about-flow-design).
