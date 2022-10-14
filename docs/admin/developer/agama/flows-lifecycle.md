---
tags:
  - administration
  - developer
  - agama
---

# Flows lifecycle

!!! Important
    The reader is supposed to have checked the [DSL basics](./dsl.md) before proceeding with this page.
    
From a programming perspective, a flow is a lot like a function or subroutine in a regular program. Once a flow is triggered, it is executed from top to bottom until a `Finish` statement is encountered. There are no asynchronous calls, threads, event listeners or callbacks to deal with. There are no requirements on flow initialization or flow structure either.

The `RRF` construct makes code look totally fluid despite there is a "pause" once the response (markup) is sent. When this occurs there is no server-side processing taking place at all. Only after the user submits data to the server the flow is "resumed" and execution continues. With `RFAC` a similar pause happens except resumption is triggered by means of a browser redirect.

The above makes the **lifecycle** of a flow very simple: after triggered, it **advances** and terminates when it is **finished**, **aborted**, or **times out**. 

## Flow advance and navigation

Once a web page (or a response in general) is replied to a web client (browser), a POST is required to make the flow proceed, see [RRF](./dsl-full.md#RRF). The POST is expected to be sent to the current URL only, otherwise, a 404 error will be thrown. The engine will then respond with a redirect (usually 301) so the client will GET the next URL to be rendered. This pattern of navigation is known as "POST-REDIRECT-GET".

There is a clear correspondence of the "current URL" with the physical path of the template rendered. As an example, if the browser location shows `https://<your-host>/jans-auth/fl/foo/bar.fls`, the involved template is stored at `/opt/jans/jetty/jans-auth/agama/ftl/foo` and has name `bar`. This makes flows more predictable and easier to reason about.

Note however URLs are not manipulable: an attempt to set the browser location to a URL corresponding to a different template will not make that template be rendered or provoke any unexpected jump in the flow control. Instead, an error page is shown that allows users to re-take where they left off or to restart from scratch. In other words, navigation honors the "current flow URL" avoiding attempts to re-visit past stages or make unexpected moves to future ones.

Additionally, the engine by default sends responses with proper HTTP headers so page contents are not cached. This is key to prevent manipulation and allows a safe usage of the browser's back button, where it will not be possible to visit past stages. 

## Finishing flows

Carefully decide how use the [`Finish`](./dsl-full.md#flow-finish) directive in a flow. Specially when terminating sucessfully, many times developers would like to attach the identity of the user in question, as in `Finish userId`. This results in a successful authentication event and makes sense, but this is not always desired. Sometimes due to decomposition practices (in order to favor re-use and better organization), small flows can arise that should not carry the user identifier.

As an example, suppose several flows exist for OTP (one-time passcode) authentication, like SMS, e-mail, token-based, etc. These would receive the user identifier as an input and act accordingly by verifying the passcode the user has entered at the browser. A parent flow can be used to prompt for a username and password first, and then forward the user to the OTP flow that better matches the user's preferences. This sounds fine, however, since any **enabled** flow can be triggered by means of an authentication request, a skilled individual might try to launch one of the OTP flows directly passing proper parameters. This would result in authentications using a single factor (i.e. no password) which is undesirable.

Thus, it is recommended to include `userId` in `Finish` only when there is a reason to do so, that is, when the authentication carried out by the flow is strong enough. This largely depends on the defined organization policies, but using a two-factor authentication is often a good sign of strength. Another approach is disabling flows that should not be triggered from a browser directly. Disabled flows can still be used as subflows from other flows.

Recall the simplest way to express a positive authentication outcome is just `Finish true`.

## Cancellation

This is a feature that in conjuction with [template overrides](./dsl-full.md#template-overrides) allows developers to implement backtracking or alternative routing. Suppose a flow is designed to reuse two or more existing subflows. As expected these subflows are neither aware of each other nor of its parent. How can the parent make so that once the user has landed at a page belonging to a given subflow A be presented the alternative to take another route, say, to subflow B?

Clearly a page at flow A can be overriden, however, how to abort A and make it jump to B? The answer is cancellation. Through flow cancellation, a running flow can be aborted and the control returned to one of its parents for further processing. This can achieved by overriding a template so that the POST to the current URL includes a form field named `_abort`.

POSTing this way will provoke the associated `Trigger` call to return a value like `{ aborted: true, data: ... }` where `data` is a _map_ consisting of the payload (form fields) sent with the POST. Thus, developers can build custom pages and add for example a button to provoke the cancellation. Then, back in the flow implementation take the user to the desired path.

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
    <button type="submit" class="btn btn-link" id="_abort" name="_abort" value="">
        Didn't get an SMS?, send the passcode to my e-mail</button>
</form>
```

Ideas for a good experience:

- `SmsOTP` and `EmailOTP` should account for retries in their implementation, e.g. give the user 2-3 opportunities to enter an OTP. This can be parameterized, for instance.

- These flows should not contain `data: { userId: ... }` when they finish successfully. If any of them are used standalone - not as subflows - the result will be a successful authentication for the given user. This means someone can have access to the protected resource (e.g. target application) with just entering a valid OTP and knowledge a valid user identifier. This is often weak in terms of security. An extra authentication factor should be required, like a password as in the example above

### Cancellation bubble-up

In order to override a page, the path to the corresponding template can be easily derived from the URL seen at the browser's address bar when the subflow is `Trigger`ed. Note the page may not necessarily belong directly to the subflow  triggered but probably to another flow lying deep in a chain of `Trigger` invocations. 

As an example suppose you are interested in building a flow A that reuses flow B. You identify a page shown that needs to be overriden. It might happen this page is actually rendered by C - a flow that B in turn reuses. In scenarios like this cancellation still works transparently and developers need not be aware of flows dependencies. In practice, when cancellation occurs at A, it bubbles up to B and then to C, which is the target of this process. 

Note that even flow B (as is) may also be overriding A's templates. Resolution of a template path takes place from the inner to the outer flow, so it occurs this way in the example:

1. `path` is as found in A's `RRF` instruction

1. `path` is looked up on the list provided in B's `Override templates`. If a match is found, `path` is updated accordingly 

1. `path` is looked up on the list provided in C's `Override templates`. If a match is found, `path` is updated accordingly

1. The page referenced by `path` is rendered

When a page POSTs a cancellation as described earlier, the flow to return control to is determined by the path of the template that issued the given POST. Similarly a lookup on B's `Override templates` takes place followed by other on A's. 

## Timeouts

Authentication flows are normally short-lived. They usually span no more than a few minutes. In Agama, the maximum amount of time an end-user can take to fully complete a flow is driven by the [configuration of the authentication server](../../config-guide/jans-cli/im/im-jans-authorization-server.md), specifically the `sessionIdUnauthenticatedUnusedLifetime` property which is measured in seconds. As an example, if this value is 120, any attempt to authenticate taking more than two minutes will throw an error page.

Moreover, a flow may specify its own timeout in the [header](./dsl-full#header-basics). In practice, the effective timeout is the smallest value between `sessionIdUnauthenticatedUnusedLifetime` and the value supplied in the header, if any.

Depending on specific needs, `sessionIdUnauthenticatedUnusedLifetime` may have to be set to a higher value than the server's default. This may be the case where flows send e-mail notifications with temporary codes.
