---
tags:
  - administration
  - developer
  - agama
---

# Agama engine configuration

The [engine](README.md#agama-engine) is a piece of software in charge of parsing flows written in Agama DSL and put them into action. Some aspects of the engine are configurable and this is integral part of the general authentication server's JSON configuration. To learn how to perform changes in the server's configuration click [here](../../config-guide/jans-cli/im/im-jans-authorization-server.md).

The properties of Agama engine configuration are described in the following:

- `enabled`: A boolean value that specifies if the engine is enabled. Default value is `false` 

- `templatesPath`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` that servers as the root of Agama flow pages. Default value is `/ftl`

- `scriptsPath`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` that servers as the root of the hierarchy of (Java/Groovy) classes added on the fly. Default value is `/scripts`

- `serializerType`: A low-level property related to [continuations](./hello-world-closer.md#stage-2) serialization. Set this to `null` if your flows present crashes due to issues with Java serialization. Default value is `KRYO`

- `maxItemsLoggedInCollections`: When a list or map is [logged](./dsl.md#logging) in a flow, only the first few items are included in the output. You can use this property to increase that limit. Default value is `3`

- `pageMismatchErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when an unexpected URL is requested while a flow is in course. Default value is `mismatch.ftlh`

- `interruptionErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when a user exceeds the amount of time allowed to take a flow to completion. Note that in order to preserve resources, the engine holds references to unfinished flows only for a small period of time (usually less than two minutes). Once the reference is lost, the error page regarded here won't be shown but `pageMismatchErrorPage`. Default value is `timeout.ftlh`

- `crashErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when an error has occured while running the flow. It contains a brief description of the problem for troubleshooting. Default value is `crash.ftlh`

- `finishedFlowPage`:  A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when a flow has finished (whether successfully or not) in the phase handled exclusively by the engine (see Stage 2 [here](./hello-world-closer.md)). This page features an auto-submitting form that users don't notice in practice. This page will rarely need modifications. Default value is `finished.ftlh`

- `bridgeScriptPage`: This is a facelets (JSF) page the bridge uses (see Stage 1 [here](./hello-world-closer.md)). This page resides in the authentication server WAR file and will rarely need modifications. Default value is `agama.xhtml`

<!--
- `defaultResponseHeaders`: A JSON object : {
            "Expires": "0"
        }-->
        