# Interception Scripts (or custom scripts)

Interception scripts (or custom scripts) allow you to define custom business logic for various features offered by the OpenID Provider (Jans-auth server). Some examples of features which can be customized are - implementing a 2FA authentication method, consent gathering, client registration, adding business specific claims to ID token or Access token etc. Scripts can easily be upgraded and doesn't require forking the Jans Server code or re-building it.

## Types of Interception scripts in Jans server

Listed below, are custom scripts classified into various types, each of which represents a feature of the Jans server that can be extended as per the business need. Each script type is described by a java interface whose methods should be overridden to implement your business case.

| Script Name                                                                                                                          | Purpose                                                                                                                                                                                                                                                    |
| ------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Person Authentication](https://docs.jans.io/nightly/script-catalog/person_authentication/person-authentication/index.md)            | Allows the definition of multi-step authentication workflows, including adaptive authentication - where the number of steps varies depending on the context.                                                                                               |
| [Consent Gathering](https://docs.jans.io/nightly/script-catalog/consent_gathering/consent-gathering/index.md)                        | Allows exact customization of the authorization (or consent) process. By default, the OP will request authorization for each scope, and display the respective scope description.                                                                          |
| [Link Interception (Link Interception)](https://docs.jans.io/nightly/script-catalog/link_interception/link-interception.md)          | Allows implementing custom business logic in existing link interception script.                                                                                                                                                                            |
| [Client Registration](https://docs.jans.io/nightly/script-catalog/client_registration/client-registration/index.md)                  | Allows implementing custom business logic during dynamic client registration, including validating SSA's and granting scopes.                                                                                                                              |
| [Dynamic Scopes](https://docs.jans.io/nightly/script-catalog/dynamic_scope/dynamic-scope/index.md)                                   | Enables admin to generate scopes on the fly, for example by calling external APIs                                                                                                                                                                          |
| [ID Generator](https://docs.jans.io/nightly/script-catalog/id_generator/id-generator/index.md)                                       |                                                                                                                                                                                                                                                            |
| [Update Token](https://docs.jans.io/nightly/script-catalog/update_token/update-token/index.md)                                       | Enables transformation of claims and values in id_token, Access token and Refresh tokens; allows the setting of token lifetime; allows the addition or removal of scopes to / from tokens; allows the addition of audit logs each time a token is created. |
| Session Management                                                                                                                   |                                                                                                                                                                                                                                                            |
| [Token Exchange](https://docs.jans.io/nightly/script-catalog/token_exchange/token-exchange/index.md)                                 | Token Exchange custom script which allows to perform custom validation, send error response and modify existing response if needed.                                                                                                                        |
| [SCIM](https://docs.jans.io/nightly/script-catalog/scim/scim/index.md)                                                               |                                                                                                                                                                                                                                                            |
| [Introspection](https://docs.jans.io/nightly/script-catalog/introspection/index.md)                                                  | Introspection scripts allows to modify response of Introspection Endpoint spec and present additional meta information surrounding the token.                                                                                                              |
| [Post Authentication](https://docs.jans.io/nightly/script-catalog/post_authn/post-authentication/index.md)                           |                                                                                                                                                                                                                                                            |
| [Client Authentication](https://docs.jans.io/nightly/script-catalog/client_authn/client-authn/index.md)                              |                                                                                                                                                                                                                                                            |
| [Authorization Challenge](https://docs.jans.io/nightly/script-catalog/authorization_challenge/authorization-challenge/index.md)      |                                                                                                                                                                                                                                                            |
| [Authorization Detail](https://docs.jans.io/nightly/script-catalog/authz_detail/authz-detail/index.md)                               |                                                                                                                                                                                                                                                            |
| [Access Evaluation](https://docs.jans.io/nightly/script-catalog/access_evaluation/access-evaluation/index.md)                        | Access Evaluation custom script for Access Evaluation Endpoint (AuthZEN)                                                                                                                                                                                   |
| [Access Evaluation Discovery](https://docs.jans.io/nightly/script-catalog/access_evaluation/access-evaluation-discovery/index.md)    | Access Evaluation Discovery custom script for `/.well-known/authzen-configuration` Access Evaluation Discovery Endpoint (AuthZEN)                                                                                                                          |
| [Select Account](https://docs.jans.io/nightly/script-catalog/select_account/select-account/index.md)                                 |                                                                                                                                                                                                                                                            |
| [Resource Owner Password Credentials](https://docs.jans.io/nightly/script-catalog/resource_owner_password_credentials/ropc/index.md) |                                                                                                                                                                                                                                                            |
| [UMA 2 RPT Authorization Policies](https://docs.jans.io/nightly/script-catalog/uma_rpt_policy/uma-rpt/index.md)                      |                                                                                                                                                                                                                                                            |
| [UMA 2 Claims-Gathering](https://docs.jans.io/nightly/script-catalog/uma_claims_gathering/uma-claims-web/index.md)                   |                                                                                                                                                                                                                                                            |
| [UMA RPT Claims](https://docs.jans.io/nightly/script-catalog/uma_rpt_claims/uma-claims-jwt/index.md)                                 |                                                                                                                                                                                                                                                            |
| [Fido2 Extension](https://docs.jans.io/nightly/script-catalog/fido2_extension/fido2-extension/index.md)                              | Extension of attestation and assertion endpoints                                                                                                                                                                                                           |
| [Discovery](https://docs.jans.io/nightly/script-catalog/discovery/discovery/index.md)                                                | OpenID discovery response modification                                                                                                                                                                                                                     |
| [Logout Status JWT](https://docs.jans.io/nightly/script-catalog/logout_status_jwt/logout-status-jwt/index.md)                        | Logout Status JWT modification: lifetime and claims                                                                                                                                                                                                        |
| [PAR](https://docs.jans.io/nightly/script-catalog/par/par/index.md)                                                                  | PAR Script to modify PAR before persistence or/and change response from `/par` endpoint                                                                                                                                                                    |
| [Transaction Token](https://docs.jans.io/nightly/script-catalog/tx_token/txtoken/index.md)                                           | TxToken Script to modify TxToken payload, response from endpoint or lifetime.                                                                                                                                                                              |
| [Cookie](https://docs.jans.io/nightly/script-catalog/cookie/cookie/index.md)                                                         | Cookies attributes modification script                                                                                                                                                                                                                     |
| [CIBA - End User notification](https://docs.jans.io/nightly/script-catalog/ciba/ciba/index.md)                                       |                                                                                                                                                                                                                                                            |
| [Configuration API](https://docs.jans.io/nightly/script-catalog/config_api/config-api/index.md)                                      |                                                                                                                                                                                                                                                            |
| [IDP](https://docs.jans.io/nightly/script-catalog/idp/idp-extension/index.md)                                                        |                                                                                                                                                                                                                                                            |
| [End Session](https://docs.jans.io/nightly/script-catalog/end_session/end-session/index.md)                                          |                                                                                                                                                                                                                                                            |
| [Persistence Extension](https://docs.jans.io/nightly/script-catalog/persistence_extension/persistence/index.md)                      |                                                                                                                                                                                                                                                            |
| [Revoke Token](https://docs.jans.io/nightly/script-catalog/revoke_token/revoke-token/index.md)                                       |                                                                                                                                                                                                                                                            |
| [Application Session](https://docs.jans.io/nightly/script-catalog/application_session/application-session/index.md)                  |                                                                                                                                                                                                                                                            |
| [Spontaneous Scope](https://docs.jans.io/nightly/script-catalog/spontaneous_scope/spontaneous-scope/index.md)                        |                                                                                                                                                                                                                                                            |
| [Create User](https://docs.jans.io/nightly/script-catalog/create_user/create-user/index.md)                                          |                                                                                                                                                                                                                                                            |
| SSA Response Modification                                                                                                            |                                                                                                                                                                                                                                                            |
| [Health Check](https://docs.jans.io/nightly/script-catalog/health_check/health-check/index.md)                                       |                                                                                                                                                                                                                                                            |
| Lock Extension                                                                                                                       |                                                                                                                                                                                                                                                            |

## Implementation languages - Jython or pure Java

Interception scripts are written in **[Jython](http://www.jython.org/)** or in **pure Java**, enabling Java or Python libraries to be imported.

______________________________________________________________________

## Implementation in Pure Java

A script in Java refers to a java source file (e.g. `Discovery.java`) which is compiled by AS and executed at runtime.

Some rules:

- The java class file containing the script should not have a package set.
- The name of the class must match to the name set in [CustomScriptType source code](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/CustomScriptType.java) (e.g. for discovery script it is "Discovery")
- Scripts must implement predefined interface which can be found against the [CustomScriptType](https://github.com/JanssenProject/jans/tree/main/jans-core/script/src/main/java/io/jans/model/custom/script/type). For e.g. if you are writing a Person authentication script then your class should implement the [following interface](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/auth/PersonAuthenticationType.java)
- All libraries available at runtime to server are available also to pure java script
- To log to `jans-auth_script.log` use `scriptLogger`
- Normal log will log in to `jans-auth.log`

**Example pure java script**

```
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.discovery.DiscoveryType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
public class Discovery implements DiscoveryType {

    private static final Logger log = LoggerFactory.getLogger(Discovery.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Discovery Java custom script");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Discovery Java custom script");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Destroy of Discovery Java custom script");
        return true;
    }

    @Override
    public int getApiVersion() {
        log.info("getApiVersion Discovery Java custom script: 11");
        return 11;
    }

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        scriptLogger.info("write to script logger");
        JSONObject response = (JSONObject) responseAsJsonObject;
        response.accumulate("key_from_java", "value_from_script_on_java");
        return true;
    }
}
```

### Using Java libraries in a script:

**Steps:**

1. Add library jars to `/opt/jans/jetty/jans-auth/custom/libs/`
1. Edit /opt/jans/jetty/jans-auth/webapps/jans-auth.xml and add the following line replacing the word `library-name` with the actual name of the library:

```
<Set name="extraClasspath">/opt/jans/jetty/jans-auth/custom/libs/library-name.jar</Set>
```

3. Restart jans-auth service `systemctl restart jans-auth`

______________________________________________________________________

## Implementation in Jython

The example below is only meant to convey the concept, we will cover the details in later parts of the documentation. Suppose, we are implementing an Openbanking Identity platform and we have to add business specific claims say `openbanking_intent_id` to the ID token. The custom script which will help us accomplish our goal is of the type `UpdateTokenType` where the `modifyIdToken` method has to be implemented. A sample custom script with this business logic will be as stated below:

```
class UpdateToken(UpdateTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        < initialization code comes here >
        return True

    def destroy(self, configurationAttributes):
        < clean up code comes here>
        return True

    def getApiVersion(self):
        return <version number>

   def modifyIdToken(self, jsonWebResponse, context):

       # Step1: <get openbanking_intent_id from session >
              sessionId = context.getSession()
              openbanking_intent_id = sessionId.getSessionAttributes().get("openbanking_intent_id ")   

       # Step2: <add custom claims to ID token here>
              jsonWebResponse.getClaims().setClaim("openbanking_intent_id ", openbanking_intent_id )
```

### Using Java libraries in a Jython script:

**Steps:**

1. Add library jars to `/opt/jans/jetty/jans-auth/custom/libs/`
1. Edit /opt/jans/jetty/jans-auth/webapps/jans-auth.xml and add the following line replacing the word `library-name` with the actual name of the library:

```
<Set name="extraClasspath">/opt/jans/jetty/jans-auth/custom/libs/library-name.jar</Set>
```

3. Restart jans-auth service `systemctl restart jans-auth`

### Using Python libraries in a script:

1. You can only use libraries (packages and modules) that are written in **Pure Python**. Importing a Python class which is a wrapper around a library written in C is not supported by the Jans server. As an example, the psycopg2 library used to connect to PostgreSQL from Python. Since it is a C wrapper around libpq, it won't work with Jython.
1. Python 3 packages / modules are not supported.

**Steps:**

1. Pure Python libraries should be added to `/opt/jans/python/libs`

1. Using pip to install additional Python packages:

1. Find out about your Jython version first. cd into the /opt directory in your Jans Server container and run ls. A directory named jython- should be listed too where will correspond to the Jython version. Note the version.

1. Open the file `/etc/jans/conf/jans.properties` and look for the line starting with `pythonModulesDir=`. Append the value `/opt/jython-<version>/Lib/site-packages` to any existing value. Each value is separater by a colon (:). It should look something like this `pythonModulesDir=/opt/jans/python/libs:/opt/jython-2.7.2a/Lib/site-packages` Run the following command `/opt/jython-<version>/bin/jython -m ensurepip` Install your library with `/opt/jython-<version>/bin/pip install <library_name>` where is the name of the library to install.

1. Restart the jans-auth service : `systemctl restart jans-auth`

### Debugging a Jython script

1. This [article](https://docs.jans.io/nightly/janssen-server/developer/scripts/interception-scripts-debug-ce/index.md) covers the details for debugging a script in a developer environment (CE).
1. This [article](https://docs.jans.io/nightly/janssen-server/developer/scripts/interception-scripts-debug/index.md) covers the details for debugging a script in a CN environment.

______________________________________________________________________

## Unit testing custom scripts

You don't need to start the Authorization Server to verify your script logic. The interception interfaces (e.g. `DiscoveryType`, `UpdateTokenType`, `IntrospectionType`) are plain Java interfaces, so a script class can be instantiated and its methods called directly from a unit test.

### Recommended approach

The `ExecutionContext` (and other context objects passed to interception methods) holds references to most server-side objects at runtime. **You only need to populate the parts of the context your script actually reads.** This keeps tests focused and fast.

Cover both **positive** and **negative** scenarios for each branch of script logic:

- **Positive** — provide the inputs the script expects and assert the observable outcome (response was modified, return value is `true`, etc.).
- **Negative** — provide missing/empty inputs and assert the failure mode (`NullPointerException`, `false` return, response left untouched, etc.). These tests document the script's preconditions.

Two mocking styles work well:

1. **Plain construction** — build a real `ExecutionContext`, set what you need (`context.setClient(client)`), and pass it. Easiest to read.
1. **Mockito** — already used heavily in jans-auth-server tests. Useful when the real object is heavy or pulls in too many collaborators. Stub only the methods the script calls.

### Example: Discovery script

Given a `Discovery` script that does not touch context:

```
public boolean modifyResponse(Object responseAsJsonObject, Object context) {
    JSONObject response = (JSONObject) responseAsJsonObject;
    response.accumulate("key_from_java", "value_from_script_on_java");
    return true;
}
```

A bare `new ExecutionContext()` is enough — there is nothing to set up:

```
DiscoveryType script = new Discovery();
JSONObject response = new JSONObject();

assertTrue(script.modifyResponse(response, new ExecutionContext()));
assertEquals(response.getString("key_from_java"), "value_from_script_on_java");
```

If the same script is changed to read the client from the context:

```
public boolean modifyResponse(Object responseAsJsonObject, Object context) {
    ExecutionContext executionContext = (ExecutionContext) context;
    JSONObject response = (JSONObject) responseAsJsonObject;
    response.accumulate("client_id_from_script", executionContext.getClient().getClientId());
    return true;
}
```

…the negative test is to pass an empty context and assert NPE; the positive test sets the client (either with a real `Client` or a Mockito mock):

```
// negative — empty context, client is null
assertThrows(NullPointerException.class,
        () -> script.modifyResponse(new JSONObject(), new ExecutionContext()));

// positive — explicit setter
Client client = new Client();
client.setClientId("test_id");
ExecutionContext context = new ExecutionContext();
context.setClient(client);

JSONObject response = new JSONObject();
assertTrue(script.modifyResponse(response, context));
assertEquals(response.getString("client_id_from_script"), "test_id");
```

### Full sample test

A complete, runnable example covering context-free, client-aware, and opt-out (returns `false`) scenarios — both with explicit construction and with Mockito — lives in the jans-auth-server test suite:

[`jans-auth-server/server/src/test/java/io/jans/as/server/scripts/DiscoveryScriptTest.java`](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/test/java/io/jans/as/server/scripts/DiscoveryScriptTest.java)

The same pattern applies to every other script type — substitute the type's interface and its context object (`ExternalUpdateTokenContext`, `ExternalIntrospectionContext`, etc.).

______________________________________________________________________

### Mandatory methods to be overridden

This is the [base class of all custom script types](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/BaseExternalType.java) and all custom scripts should implement the following methods. * `init(self, customScript, configurationAttributes)` : This method is only called once during the script initialization (or jans-auth service restarts). It can be used for global script initialization, initiate objects etc

- `destroy(self, configurationAttributes)`: This method is called when a custom script fails to initialize or upon jans-auth service restarts. It can be used to free resource and objects created in the init() method
- `getApiVersion(self, configurationAttributes, customScript)` : The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10

______________________________________________________________________

## Configurable properties of a custom script

| Property              | Description                                                                                                                                                                                                                                                                                                                                                        |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Name                  | unique identifier(name) for the custom script e.g. person_authentication_google                                                                                                                                                                                                                                                                                    |
| Description           | Description Text                                                                                                                                                                                                                                                                                                                                                   |
| Programming Languages | Python/Java                                                                                                                                                                                                                                                                                                                                                        |
| Level                 | Used in Person Authentication script type, the strength of the credential is a numerical value assigned to the custom script that is tied to the authentication method. The higher the value, the stronger it is considered. Thus, if a user has several credentials enrolled, he will be asked to present the one of them having the highest strength associated. |
| Location type         | - Database - Stored in persistence (MYSQL or PLSQL whichever applicable ) - File - stored as a file                                                                                                                                                                                                                                                                |
| Interactive           | - Web - web application - native - mobile application - both                                                                                                                                                                                                                                                                                                       |
| Custom Properties     | Key - value pairs for configurable parameters like Third Party API keys, location of configuration files etc                                                                                                                                                                                                                                                       |

______________________________________________________________________

### Building business logic in a custom script

Jans-auth server uses Weld 3.0 (JSR-365 aka CDI 2.0) for managed beans. The most common business functions are implemented through a set of beans. This [article](https://jans.io/docs/admin/developer/managed-beans/) presents many ready-to-use beans which can be used to build a script.

______________________________________________________________________

### Operations on custom scripts using jans-cli

Jans-cli supports the following six operations on custom scripts:

1. `get-config-scripts`, gets a list of custom scripts.
1. `post-config-scripts`, adds a new custom script.
1. `put-config-scripts`, updates a custom script.
1. `get-config-scripts-by-type`, requires an argument `--url-suffix TYPE: ______`.\
   You can specify the following types: PERSON_AUTHENTICATION, INTROSPECTION, RESOURCE_OWNER_PASSWORD_CREDENTIALS, APPLICATION_SESSION, CACHE_REFRESH, UPDATE_USER, USER_REGISTRATION, CLIENT_REGISTRATION, ID_GENERATOR, UMA_RPT_POLICY, UMA_RPT_CLAIMS, UMA_CLAIMS_GATHERING, CONSENT_GATHERING, DYNAMIC_SCOPE, SPONTANEOUS_SCOPE, END_SESSION, POST_AUTHN, SCIM, CIBA_END_USER_NOTIFICATION, PERSISTENCE_EXTENSION, IDP, or UPDATE_TOKEN.
1. `get-config-scripts-by-inum`, requires an argument `--url-suffix inum: _____`
1. `delete-config-scripts-by-inum`, requires an argument `--url-suffix inum: _____`

The post-config-scripts and put-config-scripts require various details about the scripts. The following command gives the basic schema of the custom scripts to pass to these operations.

### Basic schema of a custom script

Command:

`/opt/jans/jans-cli/config-cli.py --schema CustomScript`

Output:

```
{
    "dn": "string",
    "inum": "string",
    "name": "string",
    "aliases": [
        "string"
    ],
    "description": "string",
    "script": "string",
    "scriptType": "ciba_end_user_notification",
    "programmingLanguage": "python",
    "level": 45,
    "revision": 156,
    "enabled": true,
    "scriptError": {
        "raisedAt": {
        "type": "string",
        "format": "date-time"
        },
        "stackTrace": {
        "type": "string"
        }
    },
    "modified": true,
    "internal": true,
    "locationType": "db",
    "locationPath": "string",
    "baseDn": "string"
}
```

To add or modify a script first, we need to create the script's python file (e.g. /tmp/sample.py) and then create a JSON file by following the above schema and update the fields as :

/tmp/sample.json

```
{
  "name": "mySampleScript",
  "aliases": null,
  "description": "This is a sample script",
  "script": "_file /tmp/sample.py",
  "scriptType": "PERSON_AUTHENTICATION",
  "programmingLanguage": "PYTHON",
  "moduleProperties": [
    {
      "value1": "mayvalue1",
      "value2": "myvalues2",
      "description": "description for property"
    }
  ],
  "configurationProperties": null,
  "level": 1,
  "revision": 0,
  "enabled": false,
  "scriptError": null,
  "modified": false,
  "internal": false
}
```

______________________________________________________________________

### Add, Modify and Delete a script

The following command will add a new script with details given in /tmp/sample.json file. **The jans-cli will generate a unique inum of this new script if we skip inum in the json file.**

```
/opt/jans/jans-cli/config-cli.py --operation-id post-config-scripts --data /tmp/sampleadd.json
```

The following command will modify/update the existing script with details given in /tmp/samplemodify.json file. **Remember to set inum field in samplemodify.json to the inum of the script to update.**

```
/opt/jans/jans-cli/config-cli.py --operation-id put-config-scripts --data /tmp/samplemodify.json
```

To delete a custom script by its inum, use the following command:

```
/opt/jans/jans-cli/config-cli.py --operation-id delete-config-scripts-by-inum --url-suffix inum:SAMPLE-TEST-INUM
```

### List existing custom scripts

These commands to print the details are important, as using them we can get the inum of these scripts which is required to perform update or delete operation.

1. The following command will display the details of all the existing custom scripts. This will be helpful to get the inum of scripts to perform the update and delete operation.

   ```
   /opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts
   ```

1. Following command displays the details of selected custom script (by inum).

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:_____
```

1. Use the following command to display the details of existing custom scripts of a given type (for example: INTROSPECTION).

   ```
   /opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:INTROSPECTION
   ```

   **Note:** Incase the AS's Access token is bound to the client's MTLS certificate, you need to add the certificate and key files to the above commands. E.g: `/opt/jans/jans-cli/config-cli.py --operation-id post-config-scripts --data /tmp/sampleadd.json -cert-file sampleCert.pem -key-file sampleKey.key`

## Client specific implementations

## Useful links

1. [Custom scripts and jans-cli](https://github.com/JanssenProject/jans-cli/blob/main/docs/cli/cli-custom-scripts.md#find-list-of-custom-scripts)
