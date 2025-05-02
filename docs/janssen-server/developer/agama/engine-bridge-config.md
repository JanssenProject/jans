---
tags:
  - administration
  - developer
  - agama
---

# Engine and bridge configurations

## Availability

The engine and the bridge are two of the components part of the Agama Framework implementation in Janssen. The [engine](./jans-agama-engine.md) is a piece of software in charge of parsing flows written in Agama DSL and put them into action. The "bridge" is a regular jython script that temporarily hands control to the engine when an Agama flow is started, and receives control back once the flow has finished. This script is in charge of completing the authentication process for the user.

By default, the bridge is disabled. To activate it do the following:

- Open [TUI](../../config-guide/config-tools/jans-tui/README.md)
- Navigate to `Scripts` > Search 'agama' > Select the script and hit enter > check `enabled` > `save` 

## Engine configuration

Some aspects of the engine are configurable and they are integral part of the Jans authentication server's JSON configuration - specifically the section labeled `agamaConfiguration`. To learn how to perform changes in the server's configuration click [here](../../config-guide/auth-server-config/jans-authorization-server-config.md).

The properties of Agama engine configuration are described in the following:

- `enabled`: A boolean value that specifies if the engine is enabled. To disable the engine, open [TUI](../../config-guide/config-tools/jans-tui/README.md) and navigate to `Auth Server` > `properties` > `agamaConfiguration`. Then uncheck `enabled` and hit `save`

- `templatesPath`: A path relative to `/opt/jans/jetty/jans-auth/agama` that serves as the root of Agama flow pages. Default value is `/ftl`

- `scriptsPath`: A path relative to `/opt/jans/jetty/jans-auth/agama` that serves as the root of the hierarchy of (Java/Groovy) classes added on the fly. Default value is `/scripts`

- `maxItemsLoggedInCollections`: When a list or map is [logged](../../../agama/language-reference.md#logging)
  in 
  a flow, only the first few items are included in the output. You can use this property to increase that limit. Default value is `9`

- `pageMismatchErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/agama` containing the location of the page shown when an unexpected URL is requested while a flow is in course. Default value is `mismatch.ftlh`

- `interruptionErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/agama` containing the location of the page shown when a user exceeds the amount of time allowed to take a flow to completion. Note that in order to preserve resources, the engine holds references to unfinished flows only for a small period of time (usually less than two minutes). Once the reference is lost, the error page regarded here won't be shown but `pageMismatchErrorPage`. Default value is `timeout.ftlh`

- `crashErrorPage`: A path relative to `/opt/jans/jetty/jans-auth/agama` containing the location of the page shown when an error has occured while running the flow. It contains a brief description of the problem for troubleshooting. Default value is `crash.ftlh`

- `finishedFlowPage`:  A path relative to `/opt/jans/jetty/jans-auth/agama` containing the location of the page shown when a flow has finished (whether successfully or not) in the phase handled exclusively by the engine. This page features an auto-submitting form that users won't notice in practice. This page will rarely need modifications. Default value is `finished.ftlh`

- `serializeRules`: A JSON object specifying the serialization rules, see below. It is not recommended to remove items from the out-of-the-box rules. Adding items is fine

### Serialization rules

At certain points in the course of a flow, serialization of all its variables is required. The engine employs two mechanisms for this purpose: standard Java serialization and [KRYO](https://github.com/EsotericSoftware/kryo) serialization. Depending on the type of (Java) object to be serialized, administrators can specify when a mechanism is preferred over the other through a set of simple rules.

This can be better explained with an example. Suppose the following configuration:

```
"serializeRules": {
  "JAVA": ["ice", "com.acme"],
  "KRYO": [ "com.acme.bike" ]
}
```

- If the object to serialize belongs to class `com.acme.bike.SuperSonic`, both lists are traversed for the best package match. Here KRYO wins because it has a perfect match with respect to the package of the class

- If the class were `com.acme.bike.mega.SuperSonic`, KRYO still wins because it has the closest match to the package of the class

- In case of `ice.cream.Salty`, JAVA is chosen (best match)

- In case of `org.buskers.Singer`, no matches are found, however, KRYO is chosen - it's the **fallback** method

- In case of `com.acmeMan`, no matches are found. KRYO is picked as in the previous case

Please account additional behaviors:

- If the object's class is in the default package (unnamed package), KRYO is used
- If the exact class name is found in one of the lists, the method represented by such list is employed
- If the object is a (Java) exception, JAVA is used unless the full class name appears listed in the KRYO rules

## Bridge configuration

Administrators can modify the behavior of the bridge by setting the `finish_userid_db_attribute` configuration property of the script. This is used to map the identity of the user to login in the case of sucessfully finished flows. The value of this property will contain a physical database attribute that will be correlated with the `userId` passed in the `Finish` instruction of the flow.
