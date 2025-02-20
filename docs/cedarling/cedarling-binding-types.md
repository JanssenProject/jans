---
tags:
  - cedarling
  - bindings
---

# Bindings Offered by Cedarling

Cedarling bindings offer ways to integrate Cedarling with apps running in 
various execution environments. 

## Browser apps 

Cedarling can run in a browser alongside JavaScript 
application using [WASM binding](./cedarling-wasm.md). 

## Mobile apps

Cedarling provides [UniFFI interface binding](./uniffi/cedarling-uniffi.md) to integrate with mobile applications running on [Android](./uniffi/cedarling-android.md) 
or [iOS](./cedarling-ios.md).

## Server side applications

Applications running on server can leverage 
Cedarling [sidecar](./cedarling-sidecar.md) to integrate Cedarling. For instance,
a [load balancer](./cedarling-krakend.md) can run sidecar container and use it
to perform server-side authorization checks.

