---
tags:
  - administration
  - developer
  - agama
---

# Flows execution rules

This document regards flow execution details. Engines implementing the Agama framework must account these aspects fully. 

!!! Important
    The concept of "_top-level_" flow is used in several places throughout this page. It refers to a flow which has been directly launched from the user browser and hence has no parents (no callers).    

## Flows lifecycle

From a programming perspective, a flow is a lot like a function or subroutine in a regular program. Once a flow is triggered, it must be executed from top to bottom until a `Finish` statement is encountered. There are no special requirements on flow initialization or flow structure either.

In some cases, the `Finish` instruction is not reached because the flow:

- has crashed
- has been cancelled
- has timed out

More on these conditions below. Otherwise, the flow is said to have **finished** and depending on the actual arguments passed to `Finish`, it can be said the flow finished _successfully_  or the flow _failed_. 

## Successful flows

When a flow finishes successfully, control returns to the caller (parent flow) and execution continues. In the case of a _top-level_ flow, it is up to the concrete engine what to do next. Normally, the arguments passed to the `Finish` directive will drive the specific behavior, which could for instance authenticate a person.

## Failed flows

When a flow fails, control returns to the caller (parent flow) and execution continues. In the case of a _top-level_ flow, it is up to the concrete engine what to do next. Displaying an error page would be generally appropriate. The arguments passed to the `Finish` directive could be of use here.

## Crashed flows

A flow is said to have crashed if any of the below occur:

- Invalid code (syntactically wrong) was tried to be executed
- The last instruction was reached and `Finish` was not encountered
- An attempt to access a property or index of a `null` variable was made
- The invocation of a foreign routine, i.e. through `Call`, raised an error condition, and the error was not caught
- Any unexpected runtime error was raised 

When a flow crashes, the caller flow (if any) is said to have crashed too if it did not catch the given error. This rule applies recursively until the _top-level_ flow is reached.

When a _top-level_ flow crashes, engines must:

- Show an error with a concise descriptive error description
- Append a fuller error message to whatever logging system is in place
- Terminate the flow execution to allow the user start again the flow later in a safe manner 

## Flows timeout

The `Timeout` directive specifies a maximum allowable execution time for a _top-level_ flow. When a flow exceeds this execution time, engines should display an error page accordingly.

## Cancelled flows

Cancellation allows a flow to early interrupt the execution of a given subflow thus enabling the implementation of alternative routing without the need of re-writing subflows. It can only take place upon the execution of a given `RRF` instruction part of a subflow that has been `Trigger`ed.

This feature is better understood via [examples](../janssen-server/developer/agama/advanced-usages.md#cancellation) - note the link provided is specific to the Janssen Server engine only. Other engines may implement cancellation in a different way, the only requirement is to preserve the convention that the returned value of a cancelled flow must be of the form: `{ aborted: true, data: ..., url: ... }`. 

## Launching flows

Engines must provide specific mechanisms to launch a given flow in the user's browser and document how to pass input parameters to it and the formats allowed.

## Logging

Engines must maintain at least one logging destination (file, stream, etc.) to accumulate the messages passed in `Log` directives. Additionally, it should advertise the logging levels supported, their abbreviations if any, and the default logging level.

## RFAC and Callback URL

Engines must provide and maintain a fixed single URL that "users" of `RFAC` can supply to external systems in order to implement integrations with third-party sites that employ browser redirections. This URL should only be available for a given browser session while `RFAC` is in execution. Once the callback is visited or the flow times out (whichever occurs first), subsequent requests will respond with an HTTP 404 error.

The mechanism used by the engine to make the redirect to the external site is implementation specific.

## Assets management

Engines must have an internal mechanism to store assets following a hierarchical (filesystem-like) structure. Such structure must have a defined "root" that in conjunction with a flow `Basepath` will allow the engine to resolve (locate) the paths to specific flow assets.

Engines may support one or more templating technologies and file formats for rendering the UI pages.

## Foreign calls

### Languages support

At least one programming language should be supported by an engine. This feature is critical because Agama DSL was designed to force developers use a distinct, more powerful language when the task at hand cannot be implemented by simple data manipulation or comparison of values. 

### Routines lookup

Engines should define a clear mechanism to lookup the specific routine to be invoked when using the `Call` directive.

Actually, the syntax of `Call` fits well into an object-oriented style. The table bellow illustrates this fact:

|Example|Potential semantics|
|-|-|
|`Call a B c d`|On object instance `a`, invoke method `B` passing `c` and `d` as parameters|
|`Call x.y.z#S d`|Invoke method `S` belonging to class `x.y.z` passing `d` as parameter. This variant maps to a "static" method invocation, where `S` does not require a specific instance to run on|

In OOP, it is not uncommon to have a method `S` with several different signatures. The lookup mechanism should account disambiguation techniques, if possible.

### Errors

In the execution of the call, if an error occurs, the engine should raise an error catchable in Agama code. The structure or data type of this error is an engine-specific detail. Ideally the error should be easily inspected in Agama code so any required further processing is feasible in a flow. 

### Types compatibility

The arguments conversion/compatibility is also an important topic. Most likely [Agama types](./language-reference.md#data-types) will not match the (foreign) target language types. This means passing a "native" Agama value as parameter in a method `Call` requires some form of compatibility with the target type in the  routine (method) signature. When compatibility does not make sense, seems too complex, or impossible, invocation should "crash" by raising some form of error.

The same analysis has to be done in the other direction: from the target language to Agama. This is for the case where the `Call` returns a value. Such value should be "manipulable" in Agama code. 

### Other considerations

Engine implementers may consider to offer the ability for developers to supply routines without the need of engine restarts. This aims for an agile development experience.
