---
tags:
- technical
- cedarling
- Architectural Decision Records
---

# Cedarling Technical Overview

This document serves as a high-level overview of Cedarling's current internals and should help with onboarding, restructuring, or future feature implementation.

For feature-specific plans, refer to the [Cedarling Nativity Plan](https://github.com/JanssenProject/jans/wiki/Cedarling-Nativity-Plan).


Powered by the Cedar Rust engine, the Cedarling validates JWTs and maps the JSON payload claims to Cedar entities, enabling authorization via the `Cedarling::authorize` method. It also offers a secondary method, `Cedarling::authorize_unsigned`, which accepts pre-constructed Cedar entities.

The Cedarling performs other tasks associated with a Policy Decision Point, or "PDP", like logging, policy store management, workload identity registration, and communication with enterprise IT infrastructure. In order to perform these services, the Cedarling runs an in-memory KV store with built-in record expiration.


## Key Modules

* `authz`: Policy evaluation logic
* `bootstrap_config`: Loads config structs via Serde::Deserialize
* `common`: Shared data structures across the app
* `entity_builder`: Builds Cedar entities from JWTs or manually for unsigned auth
* `http`: HTTP helpers (multi-target safe)
* `init`: Initializes the services using the bootstrap configs
* `jwt`: JWT parsing and validation
* `lock`: Background task logic to communicate with the Lock Server
* `log`: Logger setup
* `tests`: End-to-end tests

!!! Note
    The `init` module may be over-abstracted. Consider simplifying the service factory logic.


## Cedarling Flow

### Startup Requirements
1. [Bootstrap Properties](../../../cedarling/cedarling-properties.md)
2. [Policy Store](../../../cedarling/cedarling-policy-store.md)

### Authorization Flow

* **Policy**
* **Action**
* **Resource**
* **Context** (e.g. headers, timestamps, user posture)

Typical JWT-based flow:

1. Call `Cedarling::authorize`
2. Validate JWT via `JwtService`
3. Build entities using `EntityBuilder`.
4. Evaluate entities against policies in `authz`.
5. Return `Ok(AuthorizeResult)` or `Err(AuthorizeError)`

Alternative: Use `Cedarling::authorize_unsigned` for direct entity input.

### JWT Validation

Handled via `JwtService` in the `jwt` module. JWTs from untrusted issuers are rejected with a warning.

Criteria for valid JWT:
    * Comes from a trusted issuer:
    * Has a defined [token metadata](../../../cedarling/cedarling-policy-store.md#token-metadata-schema)



### Entity Mapping

Handled via `EntityBuilder` in the `entity_builder` module. JWTs from untrusted issuers are rejected with an error.

JWT claims are mapped to Cedar attributes (1:1 by default).

Mappings are configured via the Token Entity Metadata Schema's `claim mapping` field in the [Policy Store](../../../cedarling/cedarling-policy-store.md#claim-mapping).


!!! Note
    JWTs might contain claims that aren't in the Cedar Entity's Schema. Cedarling should be able to ignore these and not fail immediately.

    This is probably the slowest step in the authorization procedure since we have to continually re-check the schema against the input. Improvements must be made.






## Multi-Target Support

Handled via `JwtService` in the `jwt` module. JWTs from untrusted issuers are rejected with a warning.

Criteria for a valid JWT:

* Comes from a trusted issuer
* Has a defined token metadata

Refer to [wasm-bindgen](https://crates.io/crates/wasm-bindgen) for bridging native/WASM features.

Bindings code is separated by language in `jans-cedarling/bindings`. They each should have a README.md, which contains setup instructions.


## Lock Server

Cedarling communicates with the Lock Server for:

* Logs
* Health checks (WIP)
* Telemetry (WIP)
* Policy updates (WIP)

### Connecting

Uses Dynamic Client Registration (DCR) to obtain access tokens. Logic is in the `lock` module.

Use the `CEDARLING_LOCK_SSA_JWT` to provide the Software Statement Assertion JWT. This is optional, as stated in the spec, but may be required by the auth server.


### Local Setup Notes

Use the [head](https://docs.jans.io/head/) branch of the Jans server. Earlier versions (e.g. 1.5) had bugs.

Download the latest builds from: [Dynamic Download](../../../janssen-server/install/vm-install/dynamic-download.md).


Note that the other installation directions will not give you the latest build even if you're in the `head` branch of the docs, so make sure to use the dynamic download.

### Authorization Speed


The target speed is sub-100 ms per call to `Cedarling::authorize`.

We once tried to implement JWT caching only in the JWT module, but it didn't present any significant gains.

After profiling using [pprof](https://crates.io/crates/pprof), we found out that the biggest bottleneck is creating the Cedar entities from the JWTs. Caching the whole request might improve the speed, but just caching the JWTs won't.

This should output an SVG named `cedarling_profiling_flamegraph`.

```
cargo run --example profiling
```

## Known Quirks & Things to Watch Out For

* The `entity_builder` relies heavily on the structure of the token metadata unexpected schema changes might break parsing silently.
* WASM build will sometimes fail even if it works on native. One example is you can't just spawn a new task using `tokio::spawn`. There are some helper functions in the `http_utils` module to solve the usual tasks.
* The Lock Server will not recognize the client immediately after DCR. Expect receive some unauthorized responses for the first few seconds if you're testing locally.


## Relevant Docs & Specs

* [Cedar Policy](https://docs.cedarpolicy.com/)
* [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)
* [OpenID Connect Discovery](https://openid.net/specs/openid-connect-discovery-1_0.html)
* [OIDC Dynamic Client Registration](https://openid.net/specs/openid-connect-registration-1_0.html)
* [JSON Web Token (RFC 7519)](https://datatracker.ietf.org/doc/html/rfc7519)
* [OAuth2 DCR (RFC 7591)](https://datatracker.ietf.org/doc/html/rfc7591)

