---
tags:
  - administration
  - developer
  - agama
---

# Frequently asked questions

## Common errors

### Source code of flow ... has errors

Details [here](./lifecycle.md#about-syntax-errors).

### Source code of flow ... has not been parsed yet

This may happen when a flow is launched right after its creation and [transpilation](./dsl.md#language-compiler) has not occurred yet. Try again in the next few seconds.

### Unable to find a constructor with arity ... in class ...

A Java invocation of the form `Call package.className#new ...` is passing a number of parameters that does not match any of the existing constructors for that class. 

### Unable to find a method called ... with arity ... in class ...

A java invocation is attempting to call a method that is not part of the given class or the number of parameters passed for the method is not correct.

### No Finish instruction was reached

This occurs when no `Finish` statement has been found in the execution of a flow and there are no remaining instructions.

### Serialization errors

Agama engine saves the state of a flow (see *continuations* [here](./hello-world-closer.md)) every time an [RRF](./dsl.md#rrf) or [RFAC](./dsl-full.md#rfac) instruction is reached. For this purpose the [KRYO](https://github.com/EsotericSoftware/kryo) library is employed. If kryo is unable to serialize a variable in use by your flow, a serialization error will appear in the screen or in the logs. Normally the problematic (Java) class is logged and this helps reveal the variable that is causing the issue. Note variables that hold "native" Agama values like strings or maps are never troublesome; the problems may originate from values obtained via [Call](./dsl-full.md#java-interaction).

To fix a serialization problem, try some of the following:

- Check if the value held by the variable is needed for RRF/RFAC or some upcoming statement. If that's not the case, simply set it to `null` before RRF/RFAC occurs
- Adjust the given class so it is "serialization" friendlier. With kryo, classes are not required to implement the `java.io.Serializable` interface 
- Find a replacement for the problematic class
- As a last resort, set `serializerType` property of the [engine](./engine-config.md) to `null`. Note this will switch to standard Java serialization. This setting applies globally for all your flows

## Classes added on the fly

### A class does not "see" other classes in its own package

This is a limitation of the scripting engine. Here, classes have to be imported even if they belong to the same package, or the fully qualified name used.

### How to append data to a flow's log directly?

Call method `log` of class `io.jans.agama.engine.script.LogUtils`. This method receives a variable number of arguments as DSL's `Log` does. Thus you can do `LogUtils.log("@w Today is Friday %th", 13)`, as in the logging [examples](./dsl-full.md#logging).

### What Groovy and Java versions are supported?

Groovy 4.0 and Java 11. The runtime is Amazon Corretto 11.

## Templates

### How to implement localization?

Store all messages that your templates may show in the jans-auth resource bundle. This can be done by creating files named `jans-auth_xx.properties` or `jans-auth_xx_YY.properties` in `/opt/jans/jetty/jans-auth/custom/i18n`. As an example, use `jans-auth_ja.properties` to hold the messages translated to Japanese, or `jans-auth_de_CH.properties` for German localized to Switzerland. 

The syntax of these files adhere to that of Java properties files. The suffixes `xx` and `YY` are driven by RFC 4647 and RFC 5646.

Agama engine will pick the messages from the bundle that best matches the language settings of the end-user browser. Normally, this is supplied through the HTTP header `Accept-Language`.

A file with no suffix, i.e. `jans-auth.properties` is used as fallback when a message cannot be found in specific language/country combination. To learn how to reference a message in a template, visit [this](./ui-pages.md#data-model) page.

### How to modify the built-in error pages?

Agama features some error pages out-of-the-box. They can be customized by editing the corresponding files referenced in the [configuration](./engine-config.md) doc page.  

### Is Javascript supported?

Yes, the template engine is not concerned about the kind of contents linked in markup. It does not even care if your markup makes any sense. For FreeMarker, templates just produce a textual output. 

### A flow page is not rendered as HTML, the browser just shows the source code as text

This is due to lack of proper `Content-Type` response header. This occurs if your template has filename extension other than `ftlh`. If you don't want to rename your template, insert `<#ftl output_format="HTML">` at the beginning of the file.

### How to produce JSON instead of HTML?

In your templates use `<#ftl output_format="JSON">` in the first line. Take a look at the templates whose name start with `json_` in folder `/opt/jans/jetty/jans-auth/agama`. Particularly check how JSON content can be escaped - unfortunately FreeMarker does not support escaping for JSON out-of-the-box.

## Development tools

### Are there any IDE or editor plugins available for coding flows?

Not yet unfortunately. We plan to offer tools in the future to ease the development process.

### How to debug flows?

We plan to offer a debugger in the future. In the meantime, you can do `printf`-like debugging using the `Log` instruction. See [Agama logging](./logging.md).

## About Agama engine

### Does it support AJAX?

If you require a flow with no page refreshes, it could be implemented using AJAX calls as long as they align to the [POST-REDIRECT-GET](./flows-lifecycle.md#flow-advance-and-navigation) pattern, where a form is submitted, and as response a 302/303 HTTP redirection is obtained. Your Javascript code must also render UI elements in accordance with the data obtained by following the redirect (GET). Also, care must be taken in order to process server errors, timeouts, etc. In general, this requires a considerable amount of effort.

If you require AJAX to consume a resource (service) residing in the same domain of your server, there is no restriction - the engine is not involved. Interaction with external domains may require to setup CORS configuration appropriately in the authentication server.  

### I want/need to understand the internals, where to start?

The quick start guide is a must, followed by [A closer look to Hello world flow](./hello-world-closer.md). In the end a complete sweep over all the docs is needed.

## Miscellaneous

### Does flow execution timeout?

Yes. The maximum amount of time an end-user can take to fully complete a flow is driven by the configuration of the authentication server and can be constrained even more in the flow itself. Read about timeouts [here](./flows-lifecycle.md#timeouts).

### How to prevent launching a flow directly from the browser?

Disable the flow. It will still be callable from other flows. 

### Updates in a flow's code are not reflected in its execution

Ensure the engine is [enabled](./quick-start.md#enable-the-engine). Use the REST API (PUT method) to [update](./lifecycle.md#flow-updates) the flow's code. Wait one minute and then retrieve (GET) this flow's data. The property `codeError` in the response should have the cause.   

### Why are the contents of a list or map logged partially?

This is to avoid traversing big structures fully. You can increase the value of `maxItemsLoggedInCollections` in the [engine configuration](./engine-config.md).

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

See the examples in the Looping section of the DSL [full reference](./dsl-full.md#looping).

## How to know the number of iterations carried out by a loop once it has finished?

You can assign this value to a variable at the top of your loop declaration. See the examples in the Looping section of the DSL [full reference](./dsl-full.md#looping).

### Can Agama code be called from Java?

No. These two languages are supposed to play roles that should not be mixed, check [here](./dsl.md#introduction) and [here](./lifecycle.md#design-and-code).
