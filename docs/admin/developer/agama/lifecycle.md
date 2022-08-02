# Development lifecycle

In this page an overview of the flow development process is presented. In short the following are the steps required to run a flow:

- Design and code the flow
- Add the flow to the server
- Upload flow assets (templates, images, Java libs and classes, etc.)
- Trigger an authentication request

As usual, several iterations will take place until you get it right.

!!! Note
    Throughout this document it is assumed you have a single VM standard Janssen installation working
    
## Design and code

It is up to developers how to design. This will normally require identifying the several "steps" that make up a flow and the conditions upon which "branching" takes place. Also it is important to check already existing flows in the server that may be reused for the purpose.

Agama DSL was made to structure flows only, not for doing general purpose programming. This means developers have to use Java for doing low-level computations. This way, the resulting implementation (in DSL) serves as a depiction of the flow itself, hiding most of the internal details.      

Knowledge of the DSL is a requirement as consequence of the above. Fortunately agama is small and very easy to learn. Check the DSL basics [here](./dsl.md). Also the ["Hello World"](./quick-start.md#hello-world-sample-flow) sample flow will give you a first impresssion on the language.  

Currently there are no IDE/editor plugins for coding in agama available. We hope to deliver tools in the future to ease the development experience.

### About crashes

As a flow executes things can go wrong for reasons that developers cannot foresee. A database may have crashed, a connection to an external system may have failed, the flow engine may have some bug, etc. When an abnormal situation is presented, a flow simply crashes.

If a flow crashes, its parent flow (or flows) if they exist, crash as well. Trying to handle crashes involves a lot of planning and work which is too costly and will unlikely account for the so many things that might fail in a real setting.  Thus, coding defensively is not recommended. While in Agama is possible to deal with Java exceptions, that feature should be used sparingly.

## Creating a flow in Janssen

TODO, , retrieve compilation errors

## Upload required assets

!!! Note
    For convenience, references to the directory `/opt/jans/jetty/jans-auth/agama` will be replaced by `<AGAMA-DIR>` here onwards.

The `Basepath` directive determines where flow assets reside. Ensure to create the given directory under `<AGAMA-DIR>/ftl`. Probably the same has to be done under `<AGAMA-DIR>/fl`. The difference between `ftl` and `fl` is subtle but important: the former directory must hold Freemarker templates while the latter assets like stylesheets, images and javascript code. This separation avoids Jetty server to expose the raw source code of your templates whose corresponding URL would be quite easy to guess.

As an example, suppose your `Basepath` is `foo` and you have the instructions `RRF index.ftlh` and `RRF bar/index2.ftlh` somewhere in your code. Then your local `<AGAMA-DIR>/ftl` should look like:

```
foo
|- index.ftlh
+- bar
   \- index2.ftlh

```

Say `index.ftlh` has markup like `<img src="bar/me.png">` and `index2.ftlh` has `<link href="my/style.css" rel="stylesheet">` somewhere. This is how `<AGAMA-DIR>/fl` should look like:

```
foo
+- bar
   |- me.png
   \- my
      \- style.css
```
 
### Correspondence to URLs

In practice, assets will map directly under the URL `https://<your-host>/jans-auth/fl`. This means that to access `me.png` per the example above, is a matter of hitting `https://<your-host>/jans-auth/fl/foo/bar/me.png` in your browser. Trying to get access to templates in `<AGAMA-DIR>/ftl` directly is not possible. 

## Running a flow

The quick start guide exemplifies how to [run a flow](./quick-start.md#craft-an-authentication-request) and provides links to [sample applications](./quick-start.md#client-application) that can be used to play around with authentication  in any OpenId Connect-compliant server like Janssen. If necessary, contact your server administrator for the settings required to trigger authentication requests using OpenId Conect (OIDC).

Thinks to keep in mind when testing flows:

- Once you can successfully start your first flow, it is recommended to take a look at the log statements your flow may have produced via the `Log` instruction. Click [here](./logging.md) to learn more. 

- When a flow crashes for some reason, a page is shown summarizing the error details. Sometimes this is enough to fix the problems, however logs tend to offer quite a better insight.

- Authentication flows are usually short-lived. This means the "journey" has to finish within a defined period of time. If exceeded, users will land at an error page. To learn more about this behavior and how to tweak, visit [Flows lifetime](./flows-lifecycle.md#timeouts).

- The engine often prevents manipulation of URLs so end users cannot mess with the navigation and provoke inconsistent states. This sometimes occurs in web applications when the browser's back button is used. Click [here](./flows-lifecycle.md#flow-advance-and-navigation) to learn more about flow navigation.
