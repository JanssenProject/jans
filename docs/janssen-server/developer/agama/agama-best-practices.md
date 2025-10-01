---
tags:
  - administration
  - developer
  - agama
---

# Recommended practices in Agama development

## Project reuse and customizations

Agama was designed with reusability in mind. If a project provides functionalities of your interest but you find it does not quite achieve what you want, do not try to modify the project. Instead, create a new separate project reusing the flows and code found in the original project.

It might be tempting to just take an Agama project archive, apply some editions, add files to it, repack, and redeploy it. This practice is not recommended. Think of a project as a library used to develop software. In normal situations you will not to hack or patch a library but use wrapper code to override or tailor its behavior.

Agama provides a couple of vehicles to manipulate behavior and appearance:

- [Template overrides](./advanced-usages.md#template-overrides)
- [Flow cancellation](./advanced-usages.md#cancellation)

These allow you to preserve encapsulation and keep project intrusiveness controlled.

## About flow design

It is up to developers how to design a flow. This will normally require identifying the several "steps" that make up the "journey" and the conditions upon which "branching" takes place. Also it is important to check already existing flows in the server that may be reused for the purpose.

Agama language was made to structure flows only, not for doing general purpose programming. This means developers have to use Java for doing low-level computations in the Janssen engine. This way, the resulting implementation (in DSL) serves as a depiction of the flow itself, hiding most of the internal details.

## About crashes

As a flow executes things can go wrong for reasons that developers cannot foresee. A database may have crashed, a connection to an external system may have failed, the flow itself may have some bug, etc. When an abnormal situation is presented, a flow simply crashes.

If a flow crashes, its parent flows (or flow) if they exist, crash as well. Trying to handle crashes involves a lot of planning and work which is too costly and will unlikely account for the so many things that might fail in a real setting. Thus, coding defensively is not recommended. While it is possible to deal with Java exceptions (product of `Call`s) and other abnormalities when `Trigger`ing flows, these features should be used sparingly.

## Internationalization labels

When using [localization and internationalization](./advanced-usages.md#localization-and-internationalization) use meaningful label keys and prefix all of them with the name of the project in question. If your project is called `magicAuthnJourneys`, then `magicAuthnJourneys.image.caption` and `magicAuthnJourneys.start_button_label` are good examples. This is a strategy that avoids possible name collisions in labels. 

## OOP prose warning

Java support adds the ability to execute pieces of business logic required to build up a flow. These “pieces of logic” match well to Java methods, however situations like this must be avoided:

```
x = … // A java object obtained in some way
y = … // A java object obtained in some way
Call x methodA arg1 arg2 …
Call x methodB arg1 arg2 arg3 …
z = Call y methodC …
Call x methodD … z …
```

If all those calls represent a meaningful unit of work they should be abstracted out and grouped into a single method invocation which should be thoroughly implemented in Java. Note Agama should not be used to do object-oriented programming but to make a clear, concise representation of a flow. As a rule of thumb, let Java do the heavy work; this is wiser, safer, and faster.

## Variable naming

Camel case is recommended. Also, in real-world flows developers would like to prefix a variable name with `j` when its value originates from a Java call and does not match any of Agama types directly. Example: `jCustomerDetail`.

## Finishing flows

Carefully decide how use the [`Finish`](../../../agama/language-reference.md#flow-finish) directive in a flow. Specially when terminating sucessfully, many times developers would like to attach the identity of the user in question, as in `Finish userId`. This results in a successful authentication event and makes sense, but this is not always desired. Sometimes due to decomposition practices (in order to favor re-use and better organization), small flows can arise that should not carry the user identifier.

As an example, suppose several flows exist for OTP (one-time passcode) authentication, like SMS, e-mail, token-based, etc. These would receive the user identifier as an input and act accordingly by verifying the passcode the user has entered at the browser. A parent flow can be used to prompt for a username and password first, and then forward the user to the OTP flow that better matches the user's preferences. This sounds fine, however, since any flow can be triggered by means of an authentication request by default, a skilled individual might try to launch one of the OTP flows directly passing proper parameters. This would result in authentications using a single factor (i.e. no password) which is undesirable.

Thus, it is recommended to include `userId` in `Finish` only when there is a reason to do so, that is, when the authentication carried out by the flow is strong enough. This largely depends on the defined organization policies, but using a two-factor authentication is often a good sign of strength. Another approach is explicitly state which flows  should not be triggered from a browser directly. This can be done in the Agama [project descriptor](../../../agama/gama-format.md#metadata), `project.json`. 

Recall the simplest way to express a positive authentication outcome is just `Finish true`.
