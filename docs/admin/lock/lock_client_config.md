## Lock Client

Lock client is Jans Message API consumer. This is middle point of join all Lock parts together. This part can be deployed as server or service. It subscribes to specified channels, process messages, send data to OPA or other PDP, send policies to OPA or other PDP and allows to customize data which server send to PDP with custom scripts.

Lock client has 2 deployment models. It can be deployed as separate server or as Jans Auth server plugin. Second method is recommended deployment method for cloud deployment. It allows to reduce memory needed for both services. In this method Jans Lock added as plugin to Jans Auth as custom library.
All functionality is the same in both deployment modes.

Since Lock is separated server it has own configuration in `ou=jans-lock,ou=configuration,o=jans`

The main part of it is generic for all Jans projects. These parts have Lock specific properties:

```
   "tokenChannels":[
         "id_token"
      ],
   "opaConfiguration": {
        "baseUrl" : "http://localhost:8181/v1/",
        "accessToken" : ""
   },

   "policiesJsonUris": [
   ],
   "policiesJsonUrisAccessToken" : "",

   "policiesZipUris": [
   ],
   "policiesZipUrisAccessToken" : "",

   "messageConsumerType": "OPA",
   "policyConsumerType": "OPA"
```

"tokenChannels" – list of token channel names. It’s array to allows add more token types in future without changing property type. Now it has only one value “id_token”.

"opaConfiguration" section has 2 properties to define OPA API base URL and specify OPA Bearer access token if OPA server started with it.

Next properties "policiesJsonUris" and "policiesZipUris" specify sources which Lock should check periodically to get new PDP policies. There are more details is in [OPA](./lock_opa.md) section. This service runs periodically with 30 seconds interval.

`messageConsumerType` and `policyConsumerType` specify PDP for data and PDP for policies. In most cases both should specify same provider. Different types can be used together with extension script to put for example data in few different PDP.

Lock is not strictly depends on OPA. It’s possible to override OPA with custom script or use another PDP provided in custom jar. There are more details is in [Lock PDP plugin](./lock_pdp_plugin.md) section.

There is no limitation for count of Lock client instances. Each Lock instance subscribes to specified in configuration PubSub server and receive messages.

Extension script has next methods:

```
	void beforeDataPut(Object messageNode, Object dataNode, Object context);
	void beforeDataRemoval(Object messageNode, Object context);

	void beforePolicyPut(String sourceUri, List<String> policies, Object context);
	void beforePolicyRemoval(String sourceUri, Object context);
```

`Object context` in methods is instance of [xternalLockContext](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/lock/LockExtensionType.java)
`Object messageNode` is JsonNode instance with message received from PubSub
`Object dataNode` is ObjectNode instance with data message for PubSub

`ExternalLockContext` contains loaded `TokenEntity` from DB. Also it has 2 properties to control flow execution `cancelPdpOperation` and `cancelNextScriptOperation`

Since it’s possible to add more than one custom script context property `cancelNextScriptOperation` allows to stop executing next script after current script execution.
Another flow property `cancelPdpOperation` allows to override default data/policy update rules. If script set this property to `true` this means that it should be responsible of adding data/policies into PDP.

Data expiration Lock do automatically. For this it uses expiration stored in DB. Also it can do immediate expiration on remove data PubSub message.

Lock sends only few token attributes to OPA to save memory. Default data entry which Lock send to PDP contains next attributes: `scope`, `creationDate`, `expirationDate`, `userId`, `clientId`. It’s possible to customize attributes which Lock send to PDP from script.
