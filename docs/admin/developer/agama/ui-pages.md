---
tags:
  - administration
  - developer
  - agama
---

# Writing UI pages

Creating the pages that make up an Agama flow is rather straightforward for a developer. Depending on the sophistication required for the UI/UX, more effort might be demanded, however with Agama, developers focus more on writing HTML markup than dealing with complexities of a UI framework. Here, there is considerably less wrestling in comparison to using big frameworks like JSF.

## Template engine

To generate all output to be sent to the browser, the [Apache FreeMarker](https://freemarker.apache.org) template engine is used. This is a lightweight, versatile, and easy-to-learn open-source Java library. With FreeMarker any kind of text output can be produced, so Agama flows are not restricted to HTML markup exclusively.

Developers are encouraged to take a peep at the [FreeMarker manual](https://freemarker.apache.org/docs/index.html) before trying to write their first pages. The key concept there is `Template + data-model = output` which is pretty natural. The sumation in this equation is generally referred to as "rendering a template" in this documentation.

The following resources are useful as introduction as well:

- The Hello World flow presented in the [quick start](./quick-start.md#flow-code) guide. Pay attention to the `RRF` instruction there
- Agama [sample flows](./sample-flows.md)

As you will see, the task boils down to produce the desired markup plus adding some placeholders for information that is dynamic.

## Data model

In Agama, every template is generally "injected" some data, see [RRF](./dsl.md#rrf). This data in FreeMarker terms is known as "data-model". In practice, this will be an Agama [map](./dsl-full.md#maps) which can be accessed from the templates using standard FreeMarker notation. This _map_ is attached a couple of additional keys for developer's convenience:

- `webCtx`. It gives easy access to often needed bits like current path, locale, etc. This is a Java object you can inspect [here](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/engine/src/main/java/io/jans/agama/engine/service/WebContext.java). Take a look at the getters; writing `${webCtx.contextPath}` in a template will insert the result of calling method `getContextPath` - normally the string `/jans-auth`.

- `msgs`. It gives access to the localized messages of jans-auth application. Some developers might know this as the "internationalization labels" or "resource bundle" of an application. This is a collection of `.properties` files where common UI-related messages in different languages can be found. A message (label) is identified by a key, so in a template `${msgs.<KEY>}` could be used. As most keys in resource bundles have dot characters, the alternative notation `${webCtx["KEY"]}` works better for FreeMarker, for example `${msgs["login.errorMessage"]}`.

## Output encoding

The character encoding of the response sent to browser is by default `UTF-8`. To specify a different encoding the `ftl` directive can be used, for instance, `<#ftl encoding="ISO-8859-5">`. Place this at the top of the template. 

## Reusable templates

A widespread practice in web page authoring is to compose pages based on reusable pieces. As an example, the need for a common header and footer is ubiquitous in HTML projects. With FreeMarker, composition can be achieved by means of [macros](https://freemarker.apache.org/docs/ref_directive_macro.html). These are the equivalent to functions in programming, they can generate output based on parameters passed and can be called anywhere in a template.

Agama already makes use of macros for this purpose. Take a look at the `ftlh` files found at `/opt/jans/jetty/jans-auth/agama`. These templates are used to present errors, like timeouts, flow crashes, etc.

### Example

Here, two pages will be built to demostrate the concept of composition in FreeMarker templates: a homepage and an "about us" page. These will be made up of a header, a sidebar, and their respective main content. Assume the sidebar should be shown only for the home page. 

!!! Note
    FreeMarker comments are of the form `<#-- This won't be printed in the output -->`

One way to structure the solution is the following:
    
```
<#-- aside.ftlh -->

<#macro sidebar>

<aside>
  <h2>Recommended tracks</h2>
  <nav>
    <ul>
      <li><a href="#">Efilnikufesin</a></li>
      <li><a href="#">P.O.N.X.</a></li>
      <!--li><a href="#">Planet caravan</a></li-->
    </ul>
  </nav>
</aside>

</#macro>
```

`aside.ftlh` has static markup for a sidebar. It is defined inside a macro called `sidebar`.

```
<#-- commons.ftlh -->

<#import "aside.ftlh" as sbar>

<#macro header>

<header>
  <h1>Welcome</h1>
  <nav>
    <ul>
      <li><a href="#">Home</a></li>
      <li><a href="#">About</a></li>
      <li><a href="#">Sign Up</a></li>
    </ul>
  </nav>
</header>

</#macro>

<#macro main useSidebar=false>

<!DOCTYPE html>
<html>
  <body>
    <@header/>
    <#if useSidebar>
       <@sbar.sidebar/>
    </#if>
    <#nested>
  </body>
</html>

</#macro>
```

`commons.ftlh` template imports `aside.ftlh` associating it with the shortname `sbar`. Additionally:

- It defines two macros: `header` and `main`. The macro `header` generates a static navigation menu

- `main` macro is the skeleton of a very simple HTML page 

- `main` has a parameter named `useSidebar` whose default is `false` 

- The `sidebar` macro is called using `<@sbar.sidebar/>` while `header` with `<@header/>` (local macro) 

```
<#-- index.ftlh -->

<#import "commons.ftlh" as com>
<@com.main useSidebar=true>

<article>
  <h1>This is the index page!</h1>
  <p>Temporibus ut nisi quibusdam iusto vitae similique laudantium. Minima cumque ducimus sit ut dolores. Autem quam soluta illo et omnis expedita voluptas magnam. Sit aperiam laboriosam magnam et amet deleniti. Sit et velit unde quibusdam esse ullam voluptatem. Enim sint blanditiis dolores. Laborum velit eos dolor ad quaerat. Quo tempora excepturi enim dolor harum sunt ipsa. Quis sit dolorem harum ipsa fuga voluptatem commodi.
  </p>
</article>

</@com.main>
```

`index.ftlh` is the homepage: 

- Template `commons.ftlh` is imported and its macro `main` called passing `true` for `useSidebar`

- The markup between `<@com.main...` and `</@com.main>` is the content of the homepage, which is "inserted" by the `main` macro when the `<#nested>` directive is reached  

```
<#-- about.ftlh -->

<#import "commons.ftlh" as com>
<@com.main>

<article>
  <h1>About us</h1>
  <p>We don't know ourselves very well.</p>
</article>

</@com.main>
```

`about.ftlh` is the "about us" page. It works like the homepage except the sidebar will not be shown.

## Assets

Web pages need their assets, be they stylesheets, images, or javascript. The template engine has no restrictions in this regard. The main concern is producing proper markup. Keep in mind assets are not stored alongisde the templates but in the parallel folder `/opt/jans/jetty/jans-auth/agama/fl`. Check [here](./lifecycle.md#upload-required-assets) to learn more.

## Template overrides

[Template overrides](./dsl-full.md#template-overrides) is a mechanism that allows templates customization and promotes flows reuse. If an existing flow serves well the needs of a new flow you are writing, with this feature you can homogenize your UI for a more pleasant user experience. 

To start, use [`Trigger`](./dsl.md#subflows) to play around with the existing flow - as is - from the flow you are creating. Collect the URLs of the pages you are not comfortable with: grab them directly from the browser's address bar. Then proceed as follows with every URL to locate the actual templates physically:

1. Remove the `https://.../fl/` portion of the URL
1. Split the URL obtained into two pieces: a *folder* name and a *remainder*. The remainder starts after the last slash found in the URL, and usually ends in `.fls`
1. In your Janssen server, locate the directory `/opt/jans/jetty/jans-auth/agama/ftl`
1. `cd` to the directory specified by the *folder*
1. List the contents of the directory and pick the *filename* that best matches the *remainder*

Save a copy of every selected file in the directory associated to your flow (`basePath` directive) or in a subdirectory of it. Rename the files if desired too.

Now, build the `Override templates` directive passing several pairs of strings separated by lines or spaces. Every pair consists of a string specifying the original template location, that is, `<folder>/<filename>` followed by the path to the new template version relative to the base path of your flow. Check [here](./dsl-full.md#template-overrides) for an example.
