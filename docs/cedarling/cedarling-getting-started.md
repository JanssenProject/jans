---
tags:
  - cedar
  - cedarling
---

# Getting Started with Cedarling

**Cedarling** is a lightweight, embeddable Policy Decision Point (PDP) that enables fast, fine-grained, and decentralized access control across modern applications. Built on the Rust-based [Cedar](https://cedarpolicy.com/) engine, Cedarling is designed for both client-side and server-side use, supporting environments like browsers, mobile apps, cloud-native services, and API gateways.

Cedarling supports both [Token-Based Access Control (TBAC)](./cedarling-overview.md#token-based-access-control-tbac) using JWTs and unsigned authorization requests. In both cases, it enforces policies locally for low-latency and consistent Zero Trust security.

You can integrate Cedarling into your application using the following language libraries:

- [Rust](./cedarling-rust)
- [Python](./cedarling-python)
- [Kotlin](./cedarling-kotlin)
- [Swift](./cedarling-swift)

Alternatively, you can use the [Cedarling Sidecar](./cedarling-overview.md) for a drop-in deployment.

From here, you can either jump directly to the language-specific examples above or continue reading for a high-level overview of how Cedarling works.

---

## Cedarling Interfaces

The main way you will interact with Cedarling are through the following interfaces

- [Initialization](#initialization)
- [Authorization](#authorization)
- [Logging](#logging)

### Initialization

The initialization or `init` interface is how you will initialize Cedarling. Initialization involves loading:

**Bootstrap Configuration**
: A set of properties that will tells how Cedarling behaves within your application.
: Learn more in the [bootstrap properties guide](./cedarling-properties.md).

**Policy Store**
: A JSON file containing the schema, policies, trusted issuers, and token metadata schema used for making authorization decisions.
: Learn more in the [policy store guide](./cedarling-policy-store.md).

The bootstrap configuration and policy store directly influence how Cedarling performs [authorization](#authorization).


### Authorization

The authorization, or `authz`, interface is used to evaluate access control decisions by answering the question:

> Is this **Action**, on this **Resource**, in this **Context**, allowed for these **Principals**?

When using Cedarling, **Action** and **Resource** are typically defined in the [policy store](./cedarling-policy-store.md), while **Principal** and **Context** are supplied at runtime via the `authz` interface.

Cedarling currently provides two modes of authorization:

**Standard (Token-based) Interface**
: Extracts the **Principal** from a JWT.
: Accepts the **Context** as a structured map (format might vary by language).

**Unsigned Authorization**
: Accepts the **Principal** directly without requiring a JWT.
: This makes authorization decisions by passing a set of **Principals** directly.
: Similar to the standard interface, the **Context** is passed in as-is in a map-like structure.

### Logging

Cedarling supports logging of both **decision** and **system** events, useful for auditing and troubleshooting. Logging is optional and can be configured (or disabled) via the [bootstrap properties](./cedarling-properties.md).

---

## What's next?

You're now ready to dive deeper into Cedarling. From here, you could either:

- [Pick a language](#getting-started-with-cedarling) and start building with the Cedarling library.
- Use the [Cedarling Sidecar](./cedarling-overview.md) for a quick, zero-code deployment.
- Learn more about [why Cedarling exists](./cedarling-overview.md) and the problems it solves.
