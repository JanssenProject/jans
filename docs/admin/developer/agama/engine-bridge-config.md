---
tags:
  - administration
  - developer
  - agama
---

# Engine and bridge configurations

## Availability

The engine and the bridge are two of the components part of the Agama Framework implementation in Janssen. The [engine](./jans-agama-engine.md) is a piece of software in charge of parsing flows written in Agama DSL and put them into action. The "bridge" is a regular jython script that temporarily hands control to the engine when an Agama flow is started, and receives control back once the flow has finished. This script is in charge of completing the authentication process for the user.

By default, both components are disabled. To activate them do the following:

- Open [TUI](../../config-guide/jans-tui/README.md)
- Navigate to `Auth Server` > `properties` > `agamaConfiguration` > check `enabled` > `save`
- Navigate to `Scripts` > Search 'agama' > Select the script and hit enter > check `enabled` > `save` 

## Engine configuration

Some aspects of the engine are configurable and they are integral part of the Jans authentication server's JSON configuration - specifically the section labeled `agamaConfiguration`. To learn how to perform changes in the server's configuration click [here](../../config-guide/jans-cli/cli-jans-authorization-server.md).

The properties of Agama engine configuration are described in the following:

- `enabled`: A boolean value that specifies if the engine is enabled. Read more about [engine availability](#engine-availability) above 

- `templatesPath`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` that serves as the root of Agama flow pages. Default value is `/ftl`

- `scriptsPath`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` that serves as the root of the hierarchy of (Java/Groovy) classes added on the fly. Default value is `/scripts`

- `serializerType`: A low-level property related to [continuations](./advanced-usages.md#other-engine-characteristics) serialization. Set this to `null` if your flows present crashes due to issues with Java serialization. Default value is `KRYO`

- `maxItemsLoggedInCollections`: When a list or map is [logged](../../../language-reference.md#logging) in a flow, only the first few items are included in the output. You can use this property to increase that limit. Default value is `9`

- `pageMismatchErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when an unexpected URL is requested while a flow is in course. Default value is `mismatch.ftlh`

- `interruptionErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when a user exceeds the amount of time allowed to take a flow to completion. Note that in order to preserve resources, the engine holds references to unfinished flows only for a small period of time (usually less than two minutes). Once the reference is lost, the error page regarded here won't be shown but `pageMismatchErrorPage`. Default value is `timeout.ftlh`

- `crashErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when an error has occured while running the flow. It contains a brief description of the problem for troubleshooting. Default value is `crash.ftlh`

- `finishedFlowPage`:  A path relative to `/opt/jans/jetty/jans-auth/server/agama` containing the location of the page shown when a flow has finished (whether successfully or not) in the phase handled exclusively by the engine. This page features an auto-submitting form that users won't notice in practice. This page will rarely need modifications. Default value is `finished.ftlh`

- `bridgeScriptPage`: This is a facelets (JSF) page the bridge needs for proper operation. This page resides in the authentication server WAR file and will rarely need modifications. Default value is `agama.xhtml`

<!--
- `defaultResponseHeaders`: A JSON object : {
            "Expires": "0"
        }-->        

## Bridge configuration

There are a few configuration properties admins can set to modify the behavior of the bridge:

- `cust_param_name`: The name of the request parameter - in the authentication request - that will carry the name of the flow to launch. Ensure to register the given parameter name in the [server configuration](../../config-guide/jans-cli/cli-jans-authorization-server.md) (property `authorizationRequestCustomAllowedParameters`) beforehand

- `default_flow_name`: If the relying party (RP) is not able to send custom parameters or omits the flow name in the authentication request, the value of this property will be assumed to be the flow to launch by default

- `finish_userid_db_attribute`: It is used to map the identity of the user to login in the case of sucessfully finished flows. The value of this property will contain a physical database attribute that will be correlated with the `userId` passed in the `Finish` instruction of the flow
