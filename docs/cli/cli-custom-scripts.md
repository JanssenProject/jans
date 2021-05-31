# Custom Scripts

Interception scripts can be used to implement custom business logic for authentication, authorization, and more in a way that is upgrade-proof and doesn't require forking the Gluu Server code. Using Janssen CLI, we can manage custom scripts as well.

Let's get the task information using below command:

```
/opt/jans/jans-cli/config-cli.py --info CustomScripts
```

In return we gets each of the sub-task details:

```
Operation ID: get-config-scripts
  Description: Gets a list of custom scripts.
Operation ID: post-config-scripts
  Description: Adds a new custom script.
  Schema: /components/schemas/CustomScript
Operation ID: put-config-scripts
  Description: Updates a custom script.
  Schema: /components/schemas/CustomScript
Operation ID: get-config-scripts-by-type
  Description: Gets list of scripts by type.
  url-suffix: type
Operation ID: get-config-scripts-by-inum
  Description: Gets a script by Inum.
  url-suffix: inum
Operation ID: delete-config-scripts-by-inum
  Description: Deletes a custom script.
  url-suffix: inum

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/CustomScript
```

Let's perform each of this operation.


## Find list of Custom scripts

`get-config-scripts` operation id can be used to get a list of custom scripts of the Janssen Server.

The command line is:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts
```

It return all the custom scripts of the Janssen Server. You may get nagging here as it display lots of custom scripts together, nearly 40 custom scripts on the terminal. So, I would like to prefer use [IM method](im/../../im/im-custom-scripts.md#get-list-of-custom-scripts) in this case. 

<!-- 
## Add a New Custom Script

If we look at the description of this operation we see it supports schema:

```
Operation ID: post-config-scripts
  Description: Adds a new custom script.
  Schema: /components/schemas/CustomScript
```

So, let's get the schema first:

```
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/CustomScript > /tmp/cs.json
```

```
{
  "dn": null,
  "inum": null,
  "name": "string",
  "aliases": [],
  "description": null,
  "script": "_file /root/script.py",
  "scriptType": "INTROSPECTION",
  "programmingLanguage": "PYTHON",
  "moduleProperties": {
    "value1": null,
    "value2": null,
    "description": null
  },
  "configurationProperties": {
    "value1": null,
    "value2": null,
    "description": null,
    "hide": true
  },
  "level": "integer",
  "revision": 0,
  "enabled": false,
  "scriptError": {
    "raisedAt": null,
    "stackTrace": null
  },
  "modified": false,
  "internal": false
}
```

We need to fill some of these properties with valid data to add this new script. 

```
{
  "dn": null,
  "inum": null,
  "name": "custom_script_update_user",
  "aliases": [],
  "description": "Testing Custom Script for Update User",
  "script": "_file /root/update_user.py",
  "scriptType": "UPDATE_USER",
  "programmingLanguage": "PYTHON",
  "moduleProperties": {
    "value1": null,
    "value2": null,
    "description": null
  },
  "configurationProperties": {
    "value1": null,
    "value2": null,
    "description": null,
    "hide": true
  },
  "level": "integer",
  "revision": 0,
  "enabled": false,
  "scriptError": {
    "raisedAt": null,
    "stackTrace": null
  },
  "modified": false,
  "internal": false
}
```

```
PERSON_AUTHENTICATION, INTROSPECTION, RESOURCE_OWNER_PASSWORD_CREDENTIALS, APPLICATION_SESSION, CACHE_REFRESH, UPDATE_USER, USER_REGISTRATION, CLIENT_REGISTRATION, ID_GENERATOR, UMA_RPT_POLICY, UMA_RPT_CLAIMS, UMA_CLAIMS_GATHERING, CONSENT_GATHERING, DYNAMIC_SCOPE, SPONTANEOUS_SCOPE, END_SESSION, POST_AUTHN, SCIM, CIBA_END_USER_NOTIFICATION, PERSISTENCE_EXTENSION, IDP, UPDATE_TOKEN
```
-->
