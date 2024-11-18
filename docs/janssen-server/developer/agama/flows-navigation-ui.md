---
tags:
  - administration
  - developer
  - agama
---

# Flows navigation, UI pages and assets

[RRF](../../../agama/language-reference.md#rrf) is a powerful construct in Agama. Its syntax follows this pattern:

(_assignment-var_ =)? `RRF` "path-to-UI-template" _map-variable_?

Execution involves several steps which can be summarized as: location of template, rendering, and user-data retrieval. The process is explained in the following.

## Template location

!!! Note
    For convenience, references to the server directory `/opt/jans/jetty/jans-auth/agama` will be replaced by `<AGAMA-DIR>` from here onwards.

A path is built by concatenating the `Basepath` of the flow this `RRF` belongs to and the actual template path, i.e. _path-to-UI-template_ as in the introductory parapraph. Then the engine's templates path root is prepended to it. The "root" is the value set in the [engine configuration](./engine-bridge-config.md#engine-configuration) for the property `templatesPath`. Thus, in a default installation, the value to prepend would be `<AGAMA-DIR>/ftl`.

If there is no such file, the flow will crash right away, otherwise, processing continues.

### How to build templates?

Templates are expected to be written using [Apache FreeMarker](https://freemarker.apache.org/docs/index.html). This is a simple and highly productive language to produce HTML and other forms of output. By convention templates generating HTML markup in Agama should have the extension `ftlh`. Practically they will look and behave as regular HTML files.  

Developers are encouraged to take a peep at the [FreeMarker manual](https://freemarker.apache.org/docs/index.html) before trying to write their first pages. It usually boils down to produce the desired markup plus adding some placeholders for information that is dynamic. The key concept there is `Template + data-model = output` which is pretty natural. The sumation in this equation is generally referred to as "rendering a template" in this documentation.

## Rendering

Rendering is the process of "injecting" the variable passed (_map-variable_ in the above) into a template. If no variable is present in the instruction, it is assumed an empty _map_ was passed, i.e. `{ }`. 

The variable injected ("data model" in FreeMarker terms) always has to be an Agama _map_, Java bean or object implementing the `java.util.Map` interface. This will allow access to the contents of such variable from within the template.

Here is a simple example. Suppose you want to ask for a username and password in a page called `login.ftl` and that such page will be stored in `<AGAMA-DIR>/ftl/myflow/pages/login.ftl`. Assume the flow has the header directive `Basepath "myflow"`. Also, let's say we want to pass a custom salutation message to be shown at the top of the page. Here is how the Agama code would look like:

```
...
obj = { message: "Hey ho!, let's go" }
RRF "pages/login.ftl" obj
...
```

And here the UI page contents:

```
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
    ...
    <body>
    
        <h1>${message}</h1>			

        <form method="post" enctype="application/x-www-form-urlencoded">
            <div>
                <label for="username">Username</label>
        		<input type="text" name="username">
            </div>
            <div>
                <label for="password">Password</label>
                <input type="password" name="password">
            </div>
			<input type="submit" value="Login">
        </form>

    </body>
</html>
```

### Extended data model

The data model (injected _map_) is attached some additional keys for convenience:

- `webCtx`. It gives easy access to often needed bits like current path, locale, etc. This is a Java object you can inspect [here](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/engine/src/main/java/io/jans/agama/engine/service/WebContext.java). Take a look at the getters; writing `${webCtx.contextPath}` in a template will insert the result of calling method `getContextPath` - normally the string `/jans-auth`

- `cache`. Allows developers to retrieve values already stored in the Jans configured cache, e.g. `${cache.myKey}`. To avoid template errors in case of a cache miss, you can use Freemarker's safeguard [expressions](https://freemarker.apache.org/docs/dgui_template_exp.html#dgui_template_exp_missing). Note you cannot store/modify values in the cache from templates

- `labels` and `msgs`. These give access to localized messages. They are useful when templates have to render different texts depening on user context, such as country and language. Learn more [here](./advanced-usages.md#localization-and-internationalization)

### Assets handling

Clearly, templates can link other local web resources like Javascript code, images, etc. The location of this kind of files is expected to be in the filesystem under `<AGAMA-DIR>/fl` directory. Note this is not the same directory of templates. Doing so would allow users to retrieve the code of templates by simple URL manipulation.

As an example, suppose your flow `Basepath` is `foo` and you have the instructions `RRF index.ftlh` and `RRF bar/index2.ftlh` somewhere in your code. Then your local `<AGAMA-DIR>/ftl` would look like:

```
foo
|- index.ftlh
+- bar
   \- index2.ftlh

```

Say `index.ftlh` has markup like `<img src="bar/me.png">` and `index2.ftlh` has `<link href="my/style.css" rel="stylesheet">` somewhere. This is how `<AGAMA-DIR>/fl` would look like:

```
foo
+- bar
   |- me.png
   \- my
      \- style.css
```

## User-data retrieval

Once the rendered page is shown in the browser, the flow execution is literally paused. If the user stands idly at this page, nothing will happen. To make the flow proceed, an HTTP POST must be made to the current URL. This is exactly what `login.ftl` of the above example tries to do: it provides a button that submits the form via POST for the flow to resume execution.  

Once the flow continues, an Agama _map_ is built using all form fields received at the server and bound to the variable used in the assignment of the RRF instruction (the variable referred as _assignment-var_ in the introduction of this page). This only applies when `RRF` has an assignment associated, of course.  

If the earlier example is modified to

```
...
obj = { message: "Hey ho!, let's go" }
credentials = RRF "pages/login.ftl" obj
...
```

the form values can then be referenced as `credentials.username` and `credentials.password` in the flow. In other words, the keys of the resulting map will correspond to the form field names. The values will all be _strings_.

## 3-param variant

In the Jans Agama engine, `RRF` can be passed a third parameter: `RRF templatePath variable boolean`. When the boolean value is `true` the callback URL will be available while `RRF` is in execution (as in [RFAC](./jans-agama-engine.md#rfac-and-callback-url)). In this case, if the callback is visited, data passed to it will be set as the result of the `RRF`. If a POST to the current URL is received first, i.e. callback not hit, behavior will be as in the two-param `RRF` invocation. This is also the case when a `false` value is passed for the third parameter.

The three-param variant of `RRF` can be useful when:

- The decision to redirect to an external site can only be done from the browser itself
- The external site expects to receive an HTTP POST. In this case, the rendered template may contain a form with fields as needed plus auto-submission logic in Javascript to perform the actual POST. This typically occurs in inbound-identity flows where identity providers require authentication requests serialized in `application/x-www-form-urlencoded` format as is the case of SAML HTTP POST binding, for example
