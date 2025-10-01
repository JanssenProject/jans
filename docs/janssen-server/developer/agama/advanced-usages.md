---
tags:
  - developer
  - agama
---

# Advanced usages

## UI templates

### Template overrides

[Template overrides](../../../agama/language-reference.md#template-overrides) is a mechanism that allows templates customization and promotes flows reuse. If an existing flow serves well the needs of a new flow you are writing, with this feature you can homogenize your UI for a more pleasant user experience. 

To start, use [`Trigger`](../../../agama/language-reference.md#subflows) to play around with the existing flow - as is - from the flow you are creating. Collect the URLs of the pages you are not comfortable with: grab them directly from the browser's address bar. Then proceed as follows with every URL to locate the actual templates physically:

1. Remove the `https://.../fl/` portion of the URL
1. Split the URL obtained into two pieces: a *folder* name and a *remainder*. The remainder starts after the last slash found in the URL, and usually ends in `.fls`
1. In your Janssen server, locate the directory `/opt/jans/jetty/jans-auth/agama/ftl`
1. `cd` to the directory specified by the *folder*
1. List the contents of the directory and pick the *filename* that best matches the *remainder*

Save a copy of every selected file in the directory associated to your flow (`basePath` directive) or in a subdirectory of it. Rename the files if desired too.

Now, build the `Override templates` directive passing several pairs of strings separated by lines or spaces. Every pair consists of a string specifying the original template location, that is, `<folder>/<filename>` followed by the path to the new template version relative to the base path of your flow.

### Output encoding

By default, the engine sends rendered pages using `UTF-8` character encoding. To specify a different encoding the `ftl` directive can be used in pages, for instance, `<#ftl encoding="ISO-8859-5">`. Always place this at the top of a template.

### Localization and internationalization

To allow templates render text according to users' language or country, you can add a file named `labels.txt` to the `web` directory of a project. There you can place the so-called "internationalization labels" (common UI-related messages) as per your needs. The file is expected to have a structure as follows:

```
key = value

... more key/value pairs

[ locale ID 1 ]

key/value pairs for this locale

[ locale ID 2 ]

key/value pairs for this locale

...
[ locale ID n ]

key/value pairs for this locale

```

Note how the file is split into sections for every locale to support. The first section (no locale) will contain the "default" labels - more on this later. One locale conveys mostly the same concept of a Java locale (class `java.util.Locale`). In this case, a locale represents a language, country/region, and variant combination. Examples of locale IDs are:

- `de_CH`: German as used in Switzerland
- `ca_ES_VALENCIA`: Catalan as used in Spain (province of Valencia)
- `th_TH_TH`: Thai as used in Thailand together with Thai digits
- `ja`: Japanese (in general)

There may be several sections regarding the same language, like `de_CH`, `de_DE`, `de_LU`, and plain `de`. Agama engine will pick the text from the section that best maches the settings of the end-user browser (normally, this is supplied through the `Accept-Language` HTTP header). As an example, if the browser's preferred language is German from Luxembourg, the lookup is made in `de_LU` section. If no text is found there, the `de` section is tried. The opening "no locale" section is used as a last resort - that's why it is said to contain the "default" labels.

To reference a label from a template an expression like `${labels("<KEY>")}` can be used. This will retrieve the proper text using the rules just described. When the lookup fails thoroughly (no way to found a label matching the key), the four character string `null` is returned. This helps to easily locate template issues if any.

A value (message) associated to a key can contain placeholders where data can be injected. Suppose the following key/value pair:

```
disk.count = The disk {1} contains {0} file(s)
```

Using `${labels("disk.count")}` will just retrieve the message as is, however, positional arguments can be passed. As an example, `${labels("disk.count", 7, "SSD-NVME-123")}` will generate `The disk SSD-NVME-123 contains 7 file(s)`. Note parameters are not restricted to literals (fixed values) like `0`, `false`, or `"world"`. Variables can also be used. It is recommended to employ literals/variables whose data types are string, number, date, or boolean only.

Labels are global: not only can they be accessed from the project where they are originally defined but from any other project. This allows [template overrides](#template-overrides) to work smoothly, however, it brings the possibility of key collisions, that is, two or more projects defining labels with the same keys. To mitigate this problem, it is recommended to prefix all keys with the name/identifier of the originating project, like:

```
myproject.salutation = hello!

[es]
myproject.salutation = hola!

[it]
myproject.salutation = ciao!

```

When leaving `salutation` alone, it is likely other projects added to the server in the future may contain their own version of `salutation` leading to unexpected results. 

Additional notes:

- Language codes should follow the ISO 639 alpha-2 standard
- Country/region codes should be driven by ISO 3166 alpha-2 or UN M.49 numeric-3 area code
- Except for the locale section headings, the syntax of `labels.txt` adheres to that of Java properties files. This is a human-friendly, low-surpise format: a label key starts a line and its value comes after an equal sign or a colon. For readability one or more empty lines can be used between contiguous key/value pairs. Any line starting with `!` or `#`  is ignored and thus can be used as a comment or note 
- When positional arguments are passed, Java class `java.text.MessageFormat` is internally employed. This allows  powerful formatting and proper handling of plurals. If for some reason, formatting fails, the resulting string will be `error!`. Check the server logs and try to simplify your message in this case

#### Access to AS labels

The jans-auth server comes with a series of files known as "resource bundles" that follow the naming `jans-auth_xx.properties` where `xx` represents an ISO 639 language code. Bundles can be added or overridden by placing suitable properties files in directory `/opt/jans/jetty/jans-auth/custom/i18n`. Strings stored in these bundles can be accessed from templates using expressions like `${msgs.<KEY>}`. As most keys in resource bundles contain dot characters, the alternative notation `${msgs["KEY"]}` works better for FreeMarker, for example `${msgs["login.errorMessage"]}`.

Whenever possible, it is recommended to place your internationalization labels in the `web` folder of the project and use the `${labels(...` notation instead. Note `msgs` has no support for parameterized placeholders: strings are all static.

### Reusable templates

A widespread practice in web page authoring is to compose pages based on reusable pieces. As an example, the need for a common header and footer is ubiquitous in HTML projects. With FreeMarker, composition can be achieved by means of [macros](https://freemarker.apache.org/docs/ref_directive_macro.html). These are the equivalent to functions in programming, they can generate output based on parameters passed and can be called anywhere in a template.

Agama already makes use of macros for this purpose. Take a look at the `ftlh` files found at `/opt/jans/jetty/jans-auth/agama`. These templates are used to present errors, like timeouts, flow crashes, etc.

**Example:**

Here, two pages will be built to demonstrate the concept of composition in FreeMarker templates: a homepage and an "about us" page. These will be made up of a header, a sidebar, and their respective main content. Assume the sidebar should be shown only for the home page. 

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

- The markup inside `<@com.main...` tag is the content to be "inserted" when the `<#nested>` directive is reached  

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

## Cancellation

!!! Important
    Ensure you have previously gone through the contents of this [page](./flows-navigation-ui.md) before proceeding

This is a feature that in conjuction with [template overrides](#template-overrides) allows developers to implement alternative routing and backtracking. Suppose a flow is designed to reuse two or more existing subflows. As expected these subflows are neither aware of each other nor of its parent. How can the parent make so that once the user has landed at a page belonging to a given subflow A be presented the alternative to take another route, say, to subflow B?

Clearly a page at flow A can be overriden, however, how to abort A and make it jump to B? The answer is cancellation. Through flow cancellation, a running flow can be aborted and the control returned to one of its parents for further processing. This can achieved by overriding a template so that the POST to the current URL includes a form field named `_abort`.

POSTing this way will provoke the associated `Trigger` call to return a value like `{ aborted: true, data: ..., url: ... }` where `data` is a _map_ consisting of the payload (form fields) sent with the POST. Thus, developers can build custom pages and add for example a button to provoke the cancellation. Then, back in the flow implementation take the user to the desired path. The `url` property will hold the URL where cancellation took place relative to `https://your-server/jans-auth/fl/`.

As an example, suppose there exists two flows that allow users to enter and validate a one-time passcode (OTP), one flow sends the OTP via e-mail while the other through an SMS. Assume these flows receive a user identifier as input and render a single UI page each to enter the received OTP. If we are interested in building a flow that prompts for username/password credentials and use the SMS-based OTP flow with a customization that consists of showing a link like "Didn't get an SMS?, send the passcode to my e-mail", the following is a sketch of an implementation:

```
...
//validate username/password
...

result = Trigger co.acme.SmsOTP userId
    Override templates "path/to/enter_otp.ftlh" "cust_enter_otp.ftlh"

When result.aborted is true
    //The user clicked on "send the passcode to my e-mail"
    result = Trigger co.acme.EmailOTP userId

When result.success is true
	result.data = { userId: userId }

Finish result

```

The overriden template `cust_enter_otp.ftlh` would have a form like:

```
...
<form method="post" enctype="application/x-www-form-urlencoded">
    <button type="submit" name="_abort">
        Didn't get an SMS?, send the passcode to my e-mail</button>
</form>
```

Note you cannot make cancellation occur at an arbitrary point of a flow. It can only happen when a page has been rendered, that is, an `RRF` directive is in execution. When a flow is aborted and the control returned back to a parent, there is no way to "resume" execution of the flow target of the cancellation. 

### Cancellation bubble-up

In order to override a page, the path to the corresponding template can be easily derived from the URL seen at the browser's address bar when the subflow is `Trigger`ed. Note the page may not necessarily belong directly to the subflow  triggered but probably to another flow lying deep in a chain of `Trigger` invocations. 

As an example suppose you are interested in building a flow A that reuses flow B. You identify a page shown that needs to be overriden. It might happen this page is actually rendered by C - a flow that B in turn reuses. In scenarios like this cancellation still works transparently and developers need not be aware of flows dependencies. In practice, when cancellation occurs at C, it bubbles up to B and then to A, which is the target of this process. 

Note that even flow B (as is) may also be overriding C's templates. Resolution of a template path takes place from the inner to the outer flow, so it occurs this way in the example:

1. `path` is as found in C's `RRF` instruction

1. `path` is looked up on the list provided in B's `Override templates`. If a match is found, `path` is updated accordingly 

1. `path` is looked up on the list provided in A's `Override templates`. If a match is found, `path` is updated accordingly

1. The page referenced by `path` is rendered

## Engine internals

Here we provide insight on some behavioral aspects of the engine that may result of interest to developers.

### Flow advance and navigation

Once a web page (or a response in general) is replied to a client (e.g. web browser), a POST is required to [make the flow proceed](./flows-navigation-ui.md#user-data-retrieval). The POST is expected to be sent to the current URL only, otherwise, a 404 error will be thrown. The engine will then respond with a redirect (usually 301) so the client will GET the next URL to be rendered. This pattern of navigation is known as "POST-REDIRECT-GET".

There is a clear correspondence of the "current URL" with the physical path of the template rendered. As an example, if the browser location shows `https://<your-host>/jans-auth/fl/foo/bar.fls`, the involved template is stored at `/opt/jans/jetty/jans-auth/agama/ftl/foo` and has name `bar`. This makes flows more predictable and easier to reason about.

Note however URLs are not manipulable: an attempt to set the browser location to a URL corresponding to a different template will not make that template be rendered or provoke any unexpected jump in the flow control. Instead, an error page is shown that allows users to re-take where they left off or to restart from scratch. In other words, navigation honors the "current flow URL" avoiding attempts to re-visit past stages or make unexpected moves to future ones.

Additionally, the engine by default sends responses with proper HTTP headers so page contents are not cached. This is key to prevent manipulation and allows a safe usage of the browser's back button, where it will not be possible to visit past stages. 

### Code transpilation

The engine has some timers running in the background. One of them [transpiles code](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/engine/src/main/java/io/jans/agama/timer/Transpilation.java) when a change is detected in a given flow's source (written in Agama language). The transpilation process generates vanilla Javascript code runnable through [Mozilla Rhino](https://github.com/mozilla/rhino) by using a transformation chain like  (DSL) flow code -> (ANTLR4) parse tree -> (XML) abstract syntax tree -> JS. 

The transformation chain guarantees that a flow written in Agama DSL cannot:

- Access Java classes/instances not specified in the original flow code (i.e. the only bridge to Java world is via `Call`s)
- Access/modify the standard javascript built-in objects directly
- Conflict with javascript keywords

**Notes**

- You can find the (ANTLR4) DSL grammar [here](https://github.com/JanssenProject/jans/blob/main/agama/transpiler/src/main/antlr4/io/jans/agama/antlr/AuthnFlow.g4).
- The last step of the transformation chain is carried out by means of [this](https://github.com/JanssenProject/jans/blob/main/agama/transpiler/src/main/resources/JSGenerator.ftl) Freemarker transformer

### Other engine characteristics

Some interesting facts for the curious:

- The engine does not use asynchronous paradigms: no events, callbacks, extra threads, etc. All computations remain in the classic request/response servlet lifecycle familiar to most Java developers
- _Continuations_ allow to express a flow as if it were a straight sequence of commands despite there are actual pauses in the middle: the gaps between an HTTP response and the next request
- Currently Mozilla Rhino seems to be the only mechanism that brings continuations into the Java language
- In order to preserve server statelessness, continuations are persisted to storage at every flow pause. This way the proper state can be restored when the continuation is resumed in the upcoming HTTP request
