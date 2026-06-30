# Software Statements

Software Statement is defined by OAuth dynamic client registration RFC [7591](https://datatracker.ietf.org/doc/html/rfc7591#section-1.2) as

```
A digitally signed or MACed JSON Web Token (JWT) 
that asserts metadata values about the client software.
```

Janssen Server supports usage of software statements during dynamic client registration.

## Use During Dynamic Client Registration

Janssen Server supports dynamic client registration using software statements. It can be [used as software statements](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/client-registration/#using-software-statement) or [as software statement assertions (SSA)](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/client-registration/#special-mention-about-fapi) to register client dynamically.

Janssen Server also provides [SSA endpoint](https://docs.jans.io/nightly/janssen-server/auth-server/endpoints/ssa/index.md) to create and manage SSAs on the server.

Please see SSA Creation with TUI at [Create SSA with TUI](https://docs.jans.io/nightly/janssen-server/config-guide/auth-server-config/ssa-config/#ssa-screen)

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
