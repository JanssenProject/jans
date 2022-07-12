# Frequently asked questions

## Common errors


### Source code of flow ... has not been parsed yet or has errors

TODO

### Unable to find a constructor with arity ... in class ...

A Java invocation of the form `Call package.className#new ...` is passing a number of parameters that does not match any of the existing constructors for that class. 

### Unable to find a method called ... with arity ... in class ...

A java invocation is attempting to call a method that is not part of the given class or the number of parameters passed for the method is not correct.

## Classes added on the fly

### A class does not "see" other classes in its own package

This is a limitation of the scripting engine. Here, classes have to be imported even if they belong to the same package, or the fully qualified name used.

### What Groovy and Java versions are supported?

Groovy 4.0 and Java 11. The runtime is Amazon Corretto 11.

## Templates

### How to implement localization?

Store all messages that your templates may show in the jans-auth resource bundle. This can be done by creating files named `jans-auth_xx.properties` or `jans-auth_xx_YY.properties` in `/opt/jans/jetty/jans-auth/custom/i18n`. As an example, use `jans-auth_ja.properties` to hold the messages translated to Japanese, or `jans-auth_de_CH.properties` for German localized to Switzerland. 

The syntax of these files adhere to that of Java properties files. The suffixes `xx` and `YY` are driven by RFC 4647 and RFC 5646.

Agama engine will pick the messages from the bundle that best matches the language settings of the end-user browser. Normally, this is supplied through the HTTP header `Accept-Language`.

A file with no suffix, i.e. `jans-auth.properties` is used as fallback when a message cannot be found in specific language/country combination. To learn how to reference a message in a template, visit [this](./ui-pages.md#data-model) page.

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

## Miscellaneous

### Does flow execution times out?

Yes. The maximum amount of time an end-user can take to fully complete a flow is driven by the configuration of the authentication server and can be constrained even more in the flow itself. Read about timeouts [here](./flows-lifecycle.md#timeouts). 

### Why are the contents of a list or map logged partially?

This is to avoid traversing big structures fully. You can increase the value of `maxItemsLoggedInCollections` in the [engine configuration](./engine-config.md).

## How to add two numbers or compare numeric values in Agama?

Agama only provides operators for boolean comparison in conditional statements. The structure of an authentication flow will rarely have to deal with computations/comparisons of numbers, strings, etc. In case this is needed, developers have to resort to Java.

## How to concatenate strings in Agama?

See the previous answer.

## How to know the index of a given loop iteration?

See the examples in the Looping section of the DSL [full reference](./dsl-full.md#looping).
