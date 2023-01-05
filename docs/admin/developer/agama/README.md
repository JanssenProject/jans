---
tags:
  - administration
  - developer
  - agama
---

# Agama introduction


## Overview

Agama is a component of the Janssen authentication server that offers an alternative way to build web-based authentication flows. Typically, person authentication flows are defined in the server by means of jython scripts that adhere to a predefined API. With Agama, flows are coded using a DSL (domain specific language) designed for the sole purpose of writing web flows. 

![image of agama token](../../../assets/agama-token.png)

!!! Important
    Here, a web flow is understood as a process composed by one or more stages, where at each stage an actor - normally a person - provides some kind of data or response by using a web browser or similar client. Throughout the process only a single actor is involved

Some of the advantages of using Agama include:

1. Ability to express authentication flows in a clean and concise way
1. Flow composition is supported out-of-the-box: reuse of an existing flow in another requires no effort
1. Reasoning about flows behavior is straightforward (as consequence of points 1 and 2). This makes flow modifications easy
1. Small cognitive load. Agama DSL is a very small language with simple, non-distracting syntax
1. Friendly UI templating engine. No complexities when authoring web pages - stay focused on writing HTML markup

## Scope and intended audience

Agama brings a lot of power to the table. In general, developers will be able to write arbitrary flows, ranging from very simple to multi-step non-linear dynamic flows. The effort required varies widely and depends on the underlying complexity and developer ability.

According to the current status of this project, the minimum skills required are:

- Principles of the Java programming language
- Basic knowledge of web authoring: HTML, CSS, Javascript. Required for browser-based flows only
- HTTP dialect. A tool like curl, postman, HTTPie, etc.
- Starter knowledge of any of the [OpenId Connect](https://openid.net/specs/openid-connect-core-1_0.html) authorization flows 

As expected, the more sophisticated the flow, the more specialized knowlege is demanded from the developer. In the future, we are hoping to bring visual tools to lower the barriers and make flow building a more expedite experience.

## Jython flows vs. Agama 

With respect to jython-based flows, Agama differs radically in two aspects:

- A DSL is used to structure flows where business logic and computations are delegated to Java
- Flow execution proceeds in a traditional, sequential manner. With jython scripts, an inversion of control (IoC) mechanism is employed where control jumps from one method to another in a script

Despite the above, if you have coded jython authentication scripts in the past, this may represent an advantage since you will probably have acquaintance with some of the auth-server Java APIs.

## Agama engine

The Agama engine (a.k.a the "_engine_") is the piece of software in charge of reading flows written in Agama DSL and put them into action. Among others, the engine does the following:

- Compile flows' DSL code and issue associated errors if any 
- Render (e.g. convert to HTML markup) UI templates and send them to the user's browser
- Maintain flows' state
- Properly drive flows navigation (in HTTP terms)
- Locate and invoke Java code involved 

Note the engine itself does not implement an authentication protocol. It only offers a safe way for short-lived web journeys to materialize. Hence, another piece is required to put flows in the context of a standard authorization framework like OpenId Connect. This where the "_bridge_" pitches in. 

The "bridge" is a regular jython script that temporarily hands control to the engine and receives control back once the Agama flow has finished. This script is in charge of doing the actual user authentication and leads to the completion of the process by taking the user's browser to a predetermined URL - the so-called `redirect_uri` where an access token or a code can be obtained.

Account that by default, the engine is disabled in the authentication server. The [quick start](./quick-start.md) page explains how to enable it. 

## Where to start?

Readers are encouraged to begin at the [quick start](./quick-start.md) guide to learn the basics of Agama. The [FAQ](./faq.md) page provides useful information for beginners as well.
