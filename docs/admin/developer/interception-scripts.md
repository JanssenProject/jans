---
tags:
  - administration
  - developer
---

# Interception Scripts (or custom scripts)

Interception scripts (or custom scripts)  allow you to define custom business
logic for various features offered by the OpenID Provider (Jans-auth server).
Some examples of features which can be customized are - implementing a 2FA
authentication method, consent gathering, client registration, adding business
specific claims to ID token or Access token etc.
Scripts can easily be upgraded and doesn't require forking the Jans Server code
or re-building it.

## Types of Interception scripts in Jans server
Listed below, are custom scripts classified into various types, each of which
represents a feature of the Jans server that can be extended as per the business
need. Each script type is described by a java interface whose methods should be
overridden to implement your business case.

1. [Person Authentication](./scripts/person-authentication.md): Allows the
definition of multi-step authentication workflows, including adaptive
authentication - where the number of steps varies depending on the context.
1. [Consent Gathering](./scripts/consent-gathering.md): Allows exact
customization of the authorization (or consent) process. By default, the OP will
request authorization for each scope, and display the respective scope description.
1. Update User
1. [Client Registration](./scripts/client-registration.md): Allows implementing custom business logic during dynamic client registration, including validating SSA's and granting scopes.
1. [Dynamic Scopes](./scripts/dynamic-scope.md) : Enables admin to generate scopes on the fly, for example by
calling external APIs
1. ID Generator
1. [Update Token](./scripts/update-token.md)
1. Session Management
1. SCIM
1. [Introspection](./scripts/introspection.md)
1. [Post Authentication](./scripts/post-authentication.md)
1. Resource Owner Password Credentials
1. UMA 2 RPT Authorization Policies
1. UMA 2 Claims-Gathering

## Implementation languages - Jython or pure Java

Interception scripts are written in **[Jython](http://www.jython.org/)** or in
**pure Java**, enabling Java or Python libraries to be imported.

***

## Implementation in Pure Java

A script in Java refers to a java source file (e.g. `Discovery.java`) which is
compiled by AS and executed at runtime.

Some rules:

* The java class file containing the script should not have a package set.
* The name of the class must match to the name set in [CustomScriptType source code](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/CustomScriptType.java) (e.g. for discovery script it is "Discovery")
* Scripts must implement predefined interface which can be found against the [CustomScriptType](https://github.com/JanssenProject/jans/tree/main/jans-core/script/src/main/java/io/jans/model/custom/script/type). For e.g. if you are writing a Person authentication script then your class should implement the [following interface](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/auth/PersonAuthenticationType.java)
* All libraries available at runtime to server are available also to pure java script
* To log to `jans-auth_script.log` use `scriptLogger`
* Normal log will log in to `jans-auth.log`

**Example pure java script**
```java
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
<br> **Steps:**
1. Add library jars to `/opt/jans/jetty/jans-auth/custom/libs/`
2. Edit /opt/jans/jetty/jans-auth/webapps/jans-auth.xml and add the following line replacing the word `library-name` with the actual name of the library:
```
<Set name="extraClasspath">/opt/jans/jetty/jans-auth/custom/libs/library-name.jar</Set>
```
3. Restart jans-auth service
`systemctl restart jans-auth`

***

## Implementation in Jython
The example below is only meant to convey the concept, we will cover the details in later parts of the documentation.
Suppose, we are implementing an Openbanking Identity platform and we have to add business specific claims say `openbanking_intent_id` to the ID token. The custom script which will help us accomplish our goal is of the type `UpdateTokenType` where the `modifyIdToken` method has to be implemented. A sample custom script with this business logic will be as stated below:
```python
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
<br> **Steps:**
1. Add library jars to `/opt/jans/jetty/jans-auth/custom/libs/`
2. Edit /opt/jans/jetty/jans-auth/webapps/jans-auth.xml and add the following line replacing the word `library-name` with the actual name of the library:
```
<Set name="extraClasspath">/opt/jans/jetty/jans-auth/custom/libs/library-name.jar</Set>
```
3. Restart jans-auth service
`systemctl restart jans-auth`

### Using Python libraries in a script:

1. You can only use libraries (packages and modules) that are written in
**Pure Python**. Importing a Python class which is a wrapper around a library
written in C is not supported by the Jans server. As an example, the psycopg2
library used to connect to PostgreSQL from Python. Since it is a C wrapper
around libpq, it won't work with Jython.  

1. Python 3 packages / modules are not supported.

<br> **Steps:**
1. Pure Python libraries should be added to `/opt/jans/python/libs`

2. Using pip to install additional Python packages:

* Find out about your Jython version first. cd into the /opt directory in your
Jans Server container and run ls. A directory named jython-<version> should be
listed too where <version> will correspond to the Jython version. Note the
version.
* Open the file `/etc/jans/conf/jans.properties` and look for the line starting
with `pythonModulesDir=`. Append the value `/opt/jython-<version>/Lib/site-packages`
to any existing value. Each value is separater by a colon (:). It should look
something like this ` pythonModulesDir=/opt/jans/python/libs:/opt/jython-2.7.2a/Lib/site-packages`
Run the following command ` /opt/jython-<version>/bin/jython -m ensurepip `
Install your library with `/opt/jython-<version>/bin/pip install <library_name> `
where <library_name> is the name of the library to install.
* Restart the jans-auth service : `systemctl restart jans-auth`

### Debugging a Jython script

1. This [article](../interception-scripts-debug-ce) covers the details for debugging a script in a developer environment (CE).

2. This [article](../interception-scripts-debug) covers the details for debugging a script in a CN environment.

***

### Mandatory methods to be overridden
This is the [base class of all custom script types](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/BaseExternalType.java) and all custom
scripts should implement the following methods.
* `init(self, customScript, configurationAttributes)` :	This method is only
called once during the script initialization (or jans-auth service restarts). It
can be used for global script initialization, initiate objects etc

* `destroy(self, configurationAttributes)`:	This method is called when a custom
script fails to initialize or upon jans-auth service restarts. It can be used to
free resource and objects created in the init() method

* `getApiVersion(self, configurationAttributes, customScript)` : The
getApiVersion method allows API changes in order to do transparent migration
from an old script to a new API. Only include the customScript variable if the
value for getApiVersion is greater than 10

***

## Configurable properties of a custom script

| Property | Description |
| -------- | ----------- |
| Name | unique identifier(name) for the custom script e.g. person_authentication_google |
| Description | Description Text |
| Programming Languages | Python/Java |
| Level | Used in Person Authentication script type, the strength of the credential is a numerical value assigned to the custom script that is tied to the authentication method. The higher the value, the stronger it is considered. Thus, if a user has several credentials enrolled, he will be asked to present the one of them having the highest strength associated. |
| Location type | <ul> <li> Database - Stored in persistence (LDAP, MYSQL or PLSQL whichever applicable ) </li><li>File - stored as a file </ul> |
| Interactive | <ul><li>Web - web application</li><li>native - mobile application</li><li>both</li></ul> |
| Custom Properties | Key - value pairs for configurable parameters like Third Party API keys, location of configuration files etc |


***
### Building business logic in a custom script

Jans-auth server uses Weld 3.0 (JSR-365 aka CDI 2.0) for managed beans. The most
important aspects of business logic are implemented through a set of beans. This
[article](https://jans.io/docs/admin/developer/managed-beans/) presents many
ready-to-use beans which can be used to build a script.

***

###  Operations on custom scripts using jans-cli

Jans-cli supports the following six operations on custom scripts:

1. `get-config-scripts`, gets a list of custom scripts.
2. `post-config-scripts`, adds a new custom script.
3. `put-config-scripts`, updates a custom script.
4. `get-config-scripts-by-type`, requires an argument `--url-suffix TYPE: ______`.  
    You can specify the following types: PERSON_AUTHENTICATION, INTROSPECTION, RESOURCE_OWNER_PASSWORD_CREDENTIALS, APPLICATION_SESSION, CACHE_REFRESH, UPDATE_USER, USER_REGISTRATION, CLIENT_REGISTRATION, ID_GENERATOR, UMA_RPT_POLICY, UMA_RPT_CLAIMS, UMA_CLAIMS_GATHERING, CONSENT_GATHERING, DYNAMIC_SCOPE, SPONTANEOUS_SCOPE, END_SESSION, POST_AUTHN, SCIM, CIBA_END_USER_NOTIFICATION, PERSISTENCE_EXTENSION, IDP, or UPDATE_TOKEN.
5. `get-config-scripts-by-inum`, requires an argument `--url-suffix inum: _____`
6. `delete-config-scripts-by-inum`, requires an argument `--url-suffix inum: _____`

The post-config-scripts and put-config-scripts require various details about the scripts. The following command gives the basic schema of the custom scripts to pass to these operations.
### Basic schema of a custom script
Command:

`/opt/jans/jans-cli/config-cli.py --schema /components/schemas/CustomScript `

Output:
```json
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
    "locationType": "file",
    "locationPath": "string",
    "baseDn": "string"
}
```
To add or modify a script first, we need to create the script's python file (e.g. /tmp/sample.py) and then create a JSON file by following the above schema and update the fields as :

/tmp/sample.json
```json
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

***

### Add, Modify and Delete a script

The following command will add a new script with details given in /tmp/sample.json file. __The jans-cli will generate a unique inum of this new script if we skip inum in the json file.__
```
/opt/jans/jans-cli/config-cli.py --operation-id post-config-scripts --data /tmp/sampleadd.json
```
The following command will modify/update the existing script with details given in /tmp/samplemodify.json file. __Remember to set inum field in samplemodify.json to the inum of the script to update.__

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

2. Following command displays the details of selected custom script (by inum).

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:_____  
```

3. Use the following command to display the details of existing custom scripts of a given type (for example: INTROSPECTION).
```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:INTROSPECTION
```
:memo: **Note:** Incase the AS's Access token is bound to the client's MTLS certificate, you need to add the certificate and key files to the above commands.
E.g:
```
/opt/jans/jans-cli/config-cli.py --operation-id post-config-scripts --data /tmp/sampleadd.json -cert-file sampleCert.pem -key-file sampleKey.key
 ```


## Client specific implementations

## Useful links
1. [Custom scripts and jans-cli](https://github.com/JanssenProject/jans-cli/blob/main/docs/cli/cli-custom-scripts.md#find-list-of-custom-scripts)
