# Quick start

In this page a high-level overview of the flow development process is presented. Readers will be able to take a peep at the overall experience through a simple "hello world" example.

## The basics

These are key concepts to keep in mind before starting.

### Enable the engine

Ensure the agama engine is enabled in your installation. Do the following:

- Enable Agama bridge script: TODO (cli?)
- Update `enabled` property in agama configuration: TODO (cli patch?)

### Flow data

Every flow has some associated information. At minimum this is required:

- Qualified name: The flow identifer. This is normally expressed using an Internet domain reverse notation, e.g. `co.acme.SmsOTP`
- Enabled status: Determines if the flow can be effectively used
- Display name: A short descriptive name of the flow intended for humans, e.g. "passwordless authentication"
<!-- - Properties: A JSON document often used to supply configuration parameters. If unsure, just use an empty object: `{}` -->
- Source code: The flow implementation using agama DSL

TODO: The above can be supplied using using the CLI. More on this later.

### Assets

These are elements used to build the user interface such as templates, images, stylesheets, and javascript code.

Templates are written using [FreeMarker Template Language](https://freemarker.apache.org/docs/index.html) (FTL). This is a simple and highly productive language to produce HTML and other forms of output. By convention templates generating HTML markup in agama have the extension `ftlh`.

In agama, templates must reside in the filesystem under `/opt/jans/jetty/jans-auth/agama/ftl` directory.

All the other assets (CSS, JS, etc.) are expected to be under `/opt/jans/jetty/jans-auth/agama/fl`.

### Java classes

Agama DSL supports calling Java code - it is designed to force developers use Java when tasks cannot be implemented by simple data manipulation or comparison of values. Any public class or interface in the classpath of jans-auth webapp can be used for this purpose. Additionally the classpath can be augmented by uploaded source files on the fly. Click [here](./java-classpath.md) to learn more. 

### Client application

This is the target application that end-users will get access to after a successful authentication. In OpenId Connect terms, a "Relying Party" or RP. It is advisable to have this application ready before proceeding to the next section of this document.

Depending on the tools and acquaintance with OpenId Connect protocol, this may take some time for developers. Note it may also require to apply configurations at the (Janssen) server for this purpose.

A low resistance path we recommend is trying the stripped down [Javascript client example](https://nat.sakimura.org/2014/12/10/making-a-javascript-openid-connect-client/) by Nat Sakimura. Here, the discovery URL is your server's URL. 

Another alternative is trying [mod_auth_openidc](https://github.com/zmartzone/mod_auth_openidc), an Apache 2 server module that implements RP functionality.

## Hello world sample flow

Your first taste of agama will be through a dummy "hello world" flow. Here, the end-user will be presented a salutation page with a submit button which once pressed, will finish the process. For the sake of simplicity, the user to be authenticated will be a hardcoded one. This way we avoid gathering data at the browser and any other further processing in order to keep the example as short and simple as possible.

!!! Note
    While in most cases flows need to externally "receive" data and configuration parameters to properly drive their behavior, this is not the case here. The flow will only show a static salutation message and will terminate logging in a certain user, if existing.

### Flow code

The source code (written in agama DSL dialect) is found [here](TODO). Note the absence of parenthesis and semicolons - in general the syntax is very lightweight.

![hello world flow](./hello_world.png)

A line-by-line description follows:

- Line 2: every flow starts with the `Flow` keyword followed by a qualified name (think of it as the flow identifier)

- Line 3: `Basepath` specifies the directory where the assets of this flow reside. Note this is part of an indented block. There are more directives that may go here but they are beyond scope right now.  `Basepath` is always mandatory.

- Line 4: empty. There can be any number of empty lines in a source file

- Line 5: an assignment to a variable. Agama is dynamically typed and variables are not declared. Here `in` is a map (a collection of key/value pairs). Note its resemblance to JSON. `in` has no special meaning, we could have used `x`, `Soup`, or `whatever_123`.

- Line 6: RRF is used to send a response to the user's browser: it takes the path to a template (`hello/index.ftlh`) and injects a value into it (`in` in this case). The produced (**R**endered) markup is sent (**R**eplied) to the browser. Finally, the result of the interaction of the user with the page can be retrieved (**F**etched), however, this is skipped here because we are no capturing anything at the client side.

- The contents of [index.ftlh](TODO) should be familiar to web developers. The `${...}` notation is used to dynamically insert values in the markup: the text `John` in this case. Expressions like this are integral part of Freemarker. Once form submission occurs, flow execution continues at line 8.

- Line 8: a logging statement. This appends the text `Done!` to the flow's log. The `Log` instruction is pretty versatile; it is used in its simplest form here.

- Line 9: marks the flow ending. `Finish` has several forms but here a shorthand notation is used where we always report a positive success and supply the identifier of a user. If it turns out your local user database contains a user identified by `john_doe`, this will be the subject that will get authenticated by the server, otherwise an error page will be shown.

This flow is extremely static and unrealistic but showcases minimal key elements for flow building. Please **do not** try this flow in any of your production servers.

### Add the flow to the server

TODO (create client? retrieve basic data of all flows, check it is not already defined, then use the easier option to add it, explain enabled/disabled flow, upload index.ftl)

### Craft an authentication request

This section assumes your [client application](#client-application) is ready, or at least you have made the configurations required so that you can trigger an (OpendId Connect) authentication request.

This usually boils down to create and launch a URL looking like `https://<your-host>/jans-auth/restv1/authorize?acr_values=agama&customParam1=test&scope=...&response_type=...&redirect_uri=https...&client_id=...&state=...`. You may like to check the [spec](https://openid.net/specs/openid-connect-core-1_0.html) for more details, however, keep in mind that:

- To trigger an agama flow, the `acr_values` parameter must be equal to `agama`

- The identifier (qualified name) of the flow to trigger is passed using an already registered custom parameter. By now it suffices to say that `customParam1` will work in most installations. Note the parameter value in the example equals the name given to the Hello World flow

- If the flow to call receives input parameters, their values can be passed in the custom parameter as well. Use a hyphen to separate the flow name and the parameters expressed in JSON object format. For example, if the flow had inputs  `height` and `color`, you can use `test-{"height": 190, "color": "blue"}` for the value of `customParam1`. Ensure to apply proper URL-encoding beforehand. In this case, the actual value would be `test-%7B%22height%22%3A+190%2C+%22color%22%3A+%22blue%22%7D`. If certain inputs are not provided, `null` values will be assigned for them

### Testing

Launch the authentication request in a web browser. You will be taken to a plain HTML page with a salutation and a "continue" button. After submission, a quick "Redirecting you" page will be shown and you will be taken probably to an error page showing "Unable to determine identity of user". That's expected.

Let's start by changing the salutation. (TODO)

Generate an authentication request again and launch it. You will be able to see the changes. Feel free to edit `index.ftlh` and re-upload - templates changes are picked up immediately.

Now pick an existing username from your user base and alter the flow's code so that such user gets authenticated. Edit line 9. (TODO)

If things went fine, after the form submission your browser should have been taken to the `redirect_uri` defined for the authentication request. Depending on how evolved your client application is, it may have created a session for such user and obtained profile data already!.

Logs often bring useful information while testing or troubleshooting. Check this [page](./logging.md) to learn how check the logs. 

## Next steps

We have barely scratched the surface so far. There is lots more to learn in order to unveil the real power of Agama. The following topics may be of your interest:

- [Development lifecycle](./lifecycle.md): a quick reference on how flows can be setup and run. Some of these steps were already performed for Hello World, however they are presented in a more formal, detailed manner there. 

- [DSL basics](./dsl.md): an introduction and quick reference to agama language.

- [Agama logging](./logging.md)

- [Writing UI pages](./ui-pages.md)

- [Flows lifecycle](./flows-lifecycle.md)

- [Engine configuration](./engine-config.md)

- [Sample flows](./samples.md)
