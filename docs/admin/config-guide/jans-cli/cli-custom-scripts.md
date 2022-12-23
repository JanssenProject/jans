---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

# Custom Scripts

> Prerequisite: Know how to use the Janssen CLI in [command-line mode](cli-index.md)

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

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema CustomScript
```

Let's perform each of this operation.


## Find list of Custom scripts

`get-config-scripts` operation id can be used to get a list of custom scripts of the Janssen Server.

The command line is:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts
```

It returns all the custom scripts of the Janssen Server. You may get nagging here as it display lots of custom scripts together, nearly 40 custom scripts on the terminal. So, I would like to prefer use [IM method](im/im-custom-scripts.md#get-list-of-custom-scripts) in this case. 


## Adds a New Custom Script

If we look at the description of this operation we see it supports schema:

```
Operation ID: post-config-scripts
  Description: Adds a new custom script.
  Schema: /components/schemas/CustomScript
```

So, let's get the schema first:

```
/opt/jans/jans-cli/config-cli.py CustomScript > /tmp/cs.json
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
  "name": "custom_script_client",
  "aliases": null,
  "description": "Testing custom script addition",
  "script": "_file /root/client_registration.py",
  "scriptType": "CLIENT_REGISTRATION",
  "programmingLanguage": "PYTHON",
  "moduleProperties": [{
    "value1": "myvalue1",
    "value2": "myvalue2",
    "description": "description for this property"
  }],
  "configurationProperties": null,
  "level": 100,
  "revision": 0,
  "enabled": false,
  "scriptError": null,
  "modified": false,
  "internal": false
}
```
We can remove `dn`, `inum`. As because these two items are auto generated with random value. Also we see `aliases`, `moduleproperties` and `configurationProperties` are the array type `keys`. So we need to put data into `[]` otherwise it will raise an error. We can also use `null` value if we need to skip any of them. For `scriptType` we can choose only selected type of script from the below list.

### Name of the type of scripts

```
PERSON_AUTHENTICATION, INTROSPECTION, RESOURCE_OWNER_PASSWORD_CREDENTIALS, APPLICATION_SESSION, CACHE_REFRESH, CLIENT_REGISTRATION, ID_GENERATOR, UMA_RPT_POLICY, UMA_RPT_CLAIMS, UMA_CLAIMS_GATHERING, CONSENT_GATHERING, DYNAMIC_SCOPE, SPONTANEOUS_SCOPE, END_SESSION, POST_AUTHN, SCIM, CIBA_END_USER_NOTIFICATION, PERSISTENCE_EXTENSION, IDP, UPDATE_TOKEN
```

### Programming Language

Two types of programming language available there. Those are `Python` and `JavaScript`. We can choose any of them regarding the script we need to add.

Alright, let's add the script using the command line we have:

```
/opt/jans/jans-cli/config-cli.py --operation-id post-config-scripts --data /tmp/cs.json
```

```
Getting access token for scope https://jans.io/oauth/config/scripts.write
Server Response:
{
  "dn": "inum=61aef81b-b22d-42c0-89d5-b098c976a2b7,ou=scripts,o=jans",
  "inum": "61aef81b-b22d-42c0-89d5-b098c976a2b7",
  "name": "custom_script_client",
  "aliases": null,
  "description": "Testing custom script addition",
  "script": "...",
  "scriptType": "CLIENT_REGISTRATION",
  "programmingLanguage": "PYTHON",
  "moduleProperties": [
    {
      "value1": "myvalue1",
      "value2": "myvalue2",
      "description": "description for this property"
    }
  ],
  "configurationProperties": null,
  "level": 100,
  "revision": 0,
  "enabled": false,
  "scriptError": null,
  "modified": false,
  "internal": false
}
```

## Update an existing Custom Script

`put-config-scripts` operation-id can be used to update any existing script on the Janssen server.


```
Operation ID: put-config-scripts
  Description: Updates a custom script.
  Schema: /components/schemas/CustomScript
```
As we created a custom script in the [above](cli-custom-scripts.md#adds-a-new-custom-script), let's update that one. So we know the `dn:inum=61aef81b-b22d-42c0-89d5-b098c976a2b7,ou=scripts,o=jans` and `inum:61aef81b-b22d-42c0-89d5-b098c976a2b7` here. In this case, I have modified as below:

```
{
  "dn": "inum=61aef81b-b22d-42c0-89d5-b098c976a2b7,ou=scripts,o=jans",
  "inum": "61aef81b-b22d-42c0-89d5-b098c976a2b7",
  "name": "custom_script_client",
  "aliases": null,
  "description": "Testing custom script addition",
  "script": "_file /root/client_registrationj.py",
  "scriptType": "CLIENT_REGISTRATION",
  "programmingLanguage": "PYTHON",
  "moduleProperties": [{
    "value1": "myvalue1",
    "value2": "myvalue2",
    "description": "description for this property"
  }],
  "configurationProperties": [{
  	"value1": "testconfigvalue1",
  	"value2": "testconfigvalue2",
  	"description": "description for configuration property",
  	"hide": true
  }],
  "level": 100,
  "revision": 0,
  "enabled": false,
  "scriptError": null,
  "modified": false,
  "internal": false
}
```

You can see, I have added `configurationProperties` for testing purpose only. In case you need to change the script, you can do that by changing the `script` path as well.

```
/opt/jans/jans-cli/config-cli.py --operation-id put-config-scripts --data /tmp/cs.json
```

```
Getting access token for scope https://jans.io/oauth/config/scripts.write
Server Response:
{
  "dn": "inum=61aef81b-b22d-42c0-89d5-b098c976a2b7,ou=scripts,o=jans",
  "inum": "61aef81b-b22d-42c0-89d5-b098c976a2b7",
  "name": "custom_script_client",
  "aliases": null,
  "description": "Testing custom script addition",
  "script": "...",
  "scriptType": "CLIENT_REGISTRATION",
  "programmingLanguage": "PYTHON",
  "moduleProperties": [
    {
      "value1": "myvalue1",
      "value2": "myvalue2",
      "description": "description for this property"
    }
  ],
  "configurationProperties": [
    {
      "value1": "testconfigvalue1",
      "value2": "testconfigvalue2",
      "description": "description for configuration property",
      "hide": true
    }
  ],
  "level": 100,
  "revision": 0,
  "enabled": false,
  "scriptError": null,
  "modified": false,
  "internal": false
}
```

## Get Custom Script by type

With this operation-id, we can find a specific type of scripts. It uses `url-suffix` to get the list of a single type scripts.

```
Operation ID: get-config-scripts-by-type
  Description: Gets list of scripts by type.
  url-suffix: type
```

The command line is:
```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:script-type-name
```

For an example, let's find all the scripts of `CLIENT_REGISTRATION` type. So, the command line is:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-type --url-suffix type:CLIENT_REGISTRATION
```

It returns all the custom scripts that are related to the `CLIENT_REGISTRATION` type available in the Janssen Server. You will find the name of all the types [here](cli-custom-scripts.md#name-of-the-type-of-scripts)

## Get Custom Scripts by it's `inum`

In case we need to find out details configuration of any custom script, we can search by its unique `inum` value. 

```
Operation ID: get-config-scripts-by-inum
  Description: Gets a script by Inum.
  url-suffix: inum
```

command line:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:inum_value
```


For example, we can show details here that we already added in the Janssen Server and we know it's `inum` value is `61aef81b-b22d-42c0-89d5-b098c976a2b7`.

In our case, the command line is:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-scripts-by-inum --url-suffix inum:61aef81b-b22d-42c0-89d5-b098c976a2b7
```

It returns the configuration of the custom script matched with the given `inum` value.

```
Getting access token for scope https://jans.io/oauth/config/scripts.readonly
{
  "dn": "inum=61aef81b-b22d-42c0-89d5-b098c976a2b7,ou=scripts,o=jans",
  "inum": "61aef81b-b22d-42c0-89d5-b098c976a2b7",
  "name": "custom_script_client",
  "aliases": null,
  "description": "Testing custom script addition",
  "script": "...",
  "scriptType": "CLIENT_REGISTRATION",
  "programmingLanguage": "PYTHON",
  "moduleProperties": [
    {
      "value1": "myvalue1",
      "value2": "myvalue2",
      "description": "description for this property"
    }
  ],
  "configurationProperties": [
    {
      "value1": "testconfigvalue1",
      "value2": "testconfigvalue2",
      "description": "description for configuration property",
      "hide": true
    }
  ],
  "level": 100,
  "revision": 0,
  "enabled": false,
  "scriptError": null,
  "modified": false,
  "internal": false
}
```

## How to delete Custom Script?

Well, we can delete any custom script also in deed. In that case, we need to remember the `inum` value of the custom script we want to delete. In the above we [added](cli-custom-scripts.md#adds-a-new-custom-script), [updated](cli-custom-scripts.md#update-an-existing-custom-script) a custom script. We know the `inum` value, so let's delete this one.

Command line: 

```
/opt/jans/jans-cli/config-cli.py --operation-id delete-config-scripts-by-inum --url-suffix inum:inum_value
```

For example, in our case; the command line is:

```
/opt/jans/jans-cli/config-cli.py --operation-id delete-config-scripts-by-inum --url-suffix inum:61aef81b-b22d-42c0-89d5-b098c976a2b7
```

That's all for `Custom Script` management with `CLI` feature. You can check `IM` method from [here](im/im-custom-scripts.md).
