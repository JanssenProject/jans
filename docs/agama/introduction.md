---
tags:
  - administration
  - developer
  - agama
---

# Agama overview

Agama is a framework that consists of:

- A DSL (domain-specific language) purposedly designed for writing web flows
- A set of rules that drive the behavior of such flows when they are executed
- The specification of a file format - known as `.gama` - useful for sharing Agama flows. Flows have the `.flow` file extension.  

Here, a web flow is understood as a process composed by one or more stages, where at each stage an actor - normally a person - provides some kind of data or response by using a web browser or similar client. Throughout the process only a single actor is involved.

## DSL

About the Agama language is worth to mention that:

- It helps depicting the structure of web flows in a natural way
- It is closer to human language than general-purpose programming languages
- It has a clean, concise, non-distracting syntax
- It has limited computational power by design, forcing computations to occur in a more formal, general-purpose language like Java, C++, etc.

Intrinsic properties of the language include:

- It follows the imperative paradigm mainly and assumes a traditional sequential execution. It only makes use of a few declarative elements
- Flows can be treated as functions (reusable routines with well-defined inputs)
- It provides dedicated contructs for common patterns in web flows like:
    - "show a page" and "retrieve the data user provided in that page"
    - "redirect a user to an external site" and later "retrieve the data provided at a callback URL"
- It supports typical language elements like assignments, conditionals, loops, etc.

Find the complete Agama DSL reference [here](./language-reference.md).

## Behavioral rules of execution

These are aspects that concrete implementations of the framework must adhere to. Rules may include for instance:

- How and when a flow terminates 
- If and how errors are handled in a flow
- How flows' assets, i.e. UI pages, images, stylesheets, etc., are organized or laid out
- How delegation of business logic execution occur in a language other than Agama

A "concrete implementation" as referred above is known as an _engine_: a software component capable of running flows written in Agama DSL.

More information on execution rules can be found [here](./execution-rules.md).

## `.gama` file format

Check [this](./gama-format.md) page for more information.
