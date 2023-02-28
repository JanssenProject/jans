---
tags:
  - administration
  - developer
  - agama
---

# Hello world flow: a closer look

This document revisits the hello world flow of the [quick start guide](./quick-start.md) using an approach that serves as an introduction to the Agama engine internals. The course of this flow, and generally of any other, can be studied in three stages:

1. From authentication request to bridge script
1. Within Agama engine
1. From bridge script to authentication server

## Stage 1

In normal circumstances the flow is launched through a web browser via an [OIDC authentication request](./quick-start.md#craft-an-authentication-request) which in turn activates the Agama [bridge](README.md#agama-engine) script. Its associated (facelet) page is `agama.xhtml` that invokes the `prepareForStep` method of that script. This method performs some initialization and makes a redirection to the URL `/jans-auth/fl/agama.fls` to hand the control to the Agama engine.

## Stage 2

!!! Note
    A preliminary reading of concepts presented in [flows lifecycle](./flows-lifecycle.md) is recommended before proceeding

### First request cycle

The GET request for `agama.fls` is processed by [this servlet](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/engine/src/main/java/io/jans/agama/engine/servlet/ExecutionServlet.java). Most of logic takes place at method `startFlow` of [FlowService](
https://github.com/JanssenProject/jans/blob/main/agama/engine/src/main/java/io/jans/agama/engine/service/FlowService.java) bean. Here, the associated data of the flow in question is retrieved from the database. Its transpiled code in addition to utility code ([util.js](https://github.com/JanssenProject/jans/blob/main/agama/transpiler/src/main/resources/util.js)) is loaded and the main entry point called - see invocation of `callFunctionWithContinuations`. This will throw a `ContinuationPending` exception once the equivalent instruction to `RRF` (or `RFAC`) is found in the transpiled code.

The above means that at line 6 of [Hello world](https://github.com/JanssenProject/jans/blob/main/docs/admin/developer/agama/test#L6), the DSL code execution is interrupted: method `processPause` collects the required data for template rendering, like template location and its associated data model. This is saved to permanent storage in addition to the "state of the program", that is the values of all variables defined so far in the flow - this is known as the "continuation".

Finally the `ExecutionServlet` sends a response consisting of a redirect. This completes the first request/response of this flow.

### Second request cycle

As a consequence of the HTTP redirect received, the browser will request the server a URL like `/jans-auth/fl/hello/index.fls`. Note how this is correlated to the template path defined in the flow. Here the GET handler of `ExecutionServlet` takes control again, this time calling the `sendPageContents` method defined in the parent class [BaseServlet](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/agama/engine/src/main/java/io/jans/agama/engine/servlet/BaseServlet.java). `sendPageContents` performs the actual page rendering - specifically for `hello/index.ftlh` in this case - and the produced markup is sent to browser.

So far we have "covered" the first two letters of the RRF instruction.

### Third cycle

Once the browser displays the page contents and the user hits the submit button, a new request cycle begins, this time POSTing to `/jans-auth/fl/hello/index.fls`. The method `continueFlow` of `ExecutionServlet` is called by the POST handler. Here the parameters of the payload are parsed and the `continueFlow` method of `FlowService` called in turn.

At `FlowService` the continuation is restored and the transpiled flow code resumed (see `resumeContinuation` usage). This means that statements after the `RRF` instruction are all executed until another `RRF` or `RFAC` call is reached. However in this case, there are only a couple of statements left: `Log` and `Finish`. The value supplied for the latter, namely `"john_doe"` - a shortcut for `{ success: true, data: { userId: "john_doe" } }` - is wrapped in the `NativeObject` instance resulting from the `resumeContinuation` call.

!!! Note
    If we had another `RRF` instruction in this flow, a `ContinuationPending` exception would have been thrown, as in the [first request](#first-request-cycle), and a redirection sent to browser.

Finally, this result is persisted to database, and a finalization page sent to the browser, see `sendFinishPage` in `ExecutionServlet`. The page consists of an auto-submitting form that makes a POST to `/postlogin.htm`, a server URL employed to resume Jython-based authentication flows.

This is how stage 2 ends. The engine now hands control back to the bridge.

## Stage 3

The POST to `/postlogin.htm` provokes a call to the `authenticate` method of the bridge. Here, if the process finished successfully, the user gets authenticated in the server, and the browser is taken to the `redirect_uri` supplied in the original authentication request. There, the requesting party (RP) can make further processing for the user to get access according to the OpenId Connect protocol.

## Transpiled code

The engine has some timers running in the background. One of them [transpiles code](https://github.com/JanssenProject/jans/blob/main/agama/engine/src/main/java/io/jans/agama/timer/Transpilation.java) when a change is detected in a given flow's source (written in Agama DSL). The transpilation process generates vanilla Javascript code runnable through [Mozilla Rhino](https://github.com/mozilla/rhino) by using a transformation chain like  (DSL) flow code -> (ANTLR4) parse tree -> (XML) abstract syntax tree -> JS. 

The transformation chain guarantees that a flow written in Agama DSL cannot:

- Access Java classes/instances not specified in the original flow code (i.e. the only bridge to Java world is via `Call`s)
- Access/modify the standard javascript built-in objects directly
- Conflict with javascript keywords

**Notes**

- You can find the (ANTLR4) DSL grammar [here](https://github.com/JanssenProject/jans/blob/main/agama/transpiler/src/main/antlr4/io/jans/agama/antlr/AuthnFlow.g4).
- The last step of the transformation chain is carried out by means of [this](https://github.com/JanssenProject/jans/blob/main/agama/transpiler/src/main/resources/JSGenerator.ftl) Freemarker transformer

## Additional notes

- The engine does not use asynchronous paradigms: no events, callbacks, extra threads, etc. All computations remain in the classic request/response servlet lifecycle familiar to most Java developers
- _Continuations_ allow to express a flow as if it were a straight sequence of commands despite there are actual pauses in the middle: the gaps between an HTTP response and the next request
- Currently Mozilla Rhino seems to be the only mechanism that brings continuations into the Java language
- In order to preserve server statelessness, continuations are persisted to storage at every flow pause. This way the proper state can be restored when the continuation is resumed in the upcoming HTTP request
