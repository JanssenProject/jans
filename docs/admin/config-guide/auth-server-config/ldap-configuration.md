---
tags:
  - administration
  - configuration
  - ldap
---


# Lightweight Directory Access Protocol (LDAP) Configuration

The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)


=== "Use Text-based UI"

    LDAP Configuration is not possible in Text-based UI.


=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

## Using Command Line

In the Janssen Server, you can deploy and customize the LDAP  using the
command Line. To get the details of Janssen command line operations relevant to
`LDAP`, you can check the operations under `DatabaseLdapConfiguration` task using the
command below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info DatabaseLdapConfiguration
```

It comes with the following options:

```text title="Sample Output"
Operation ID: get-config-database-ldap
  Description: Gets list of existing LDAP configurations.
Operation ID: put-config-database-ldap
  Description: Updates LDAP configuration
  Schema: GluuLdapConfiguration
Operation ID: post-config-database-ldap
  Description: Adds a new LDAP configuration
  Schema: GluuLdapConfiguration
Operation ID: get-config-database-ldap-by-name
  Description: Gets an LDAP configuration by name.
  Parameters:
  name: Name of LDAP configuration [string]
Operation ID: delete-config-database-ldap-by-name
  Description: Deletes an LDAP configuration
  Parameters:
  name: No description is provided for this parameter [string]
Operation ID: patch-config-database-ldap-by-name
  Description: Patches a LDAP configuration by name
  Parameters:
  name: Name of LDAP configuration [string]
  Schema: Array of JsonPatch
Operation ID: post-config-database-ldap-test
  Description: Tests an LDAP configuration
  Schema: GluuLdapConfiguration

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schema>, for example /opt/jans/jans-cli/config-cli.py --schema GluuLdapConfiguration
```

### Get Existing LDAP Configurations

To find the existing ldap configurations, let's run the following command:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-config-database-ldap
```

```json title="Sample Output" linenums="1"
[
  {
    "configId": "auth_ldap_server",
    "bindDN": "cn=directory manager",
    "bindPassword": "m+OTwmlCEho=",
    "servers": [
      "localhost:1636"
    ],
    "maxConnections": 1000,
    "useSSL": true,
    "baseDNs": [
      "ou=people,o=jans"
    ],
    "primaryKey": "uid",
    "localPrimaryKey": "uid",
    "useAnonymousBind": false,
    "enabled": false,
    "version": 0,
    "level": 0
  }
]

```


### Adds a new LDAP Configuration


To add a new ldap configuration, we can use `post-config-database-ldap`
operation id. As shown in the [output](#using-command-line) for
`--info` command, the `post-config-database-ldap` operation
requires data to be sent according to `GluuLdapConfiguration` schema.

To see the schema, use the command below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema GluuLdapConfiguration
```

The Janssen Server also provides an example of data that adheres
to the above schema. To fetch the example, use the command below.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema-sample GluuLdapConfiguration
```

Using the schema and the example above, we have added below data to the
file `/tmp/ldap.json`.


```json title="Input" linenums="1"
{
  "configId": "test_ldap",
  "bindDN": "cn=directory manager",
  "bindPassword": "password",
  "servers": [
	"localhost:1636"
	],
  "maxConnections": 1000,
  "useSSL": "true",
  "baseDNs": ["ou=people,o=jans"],
  "primaryKey": "uid",
  "localPrimaryKey": "uid",
  "useAnonymousBind": false,
  "enabled": false,
  "version": 0,
  "level": 0
}
```
Now let's post this ldap to the Janssen Server to be added to the existing set:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id post-config-database-ldap\
 --data /tmp/ldap.json
```

Please note that `configId` should be a unique identifier name for each configuration. 
Otherwise you will get error while going to post duplicate configuration into the server. 
In that case, you can go through the next option to replace instead of adding a new one.


### Updating LDAP Database Configurations

To update ldap database configuration, we can use `put-config-database-ldap`
operation id. As shown in the [output](#using-command-line) for `--info` command,
the `put-config-database-ldap` operation requires data to be sent according to
`GluuLdapConfiguration` schema.

For example, let say we are going to change to `maxConnections` for `1000` to `100`
in the above `test_ldap` configuration. So lets modify the `/tmp/ldap.json` file as below:

```bash title="Comaand"
/opt/jans/jans-cli/config-cli.py --operation-id put-config-database-ldap \
--data /tmp/ldap.json
```

```json title="Sample Output" linenums="1"
{
  "configId": "test_ldap",
  "bindDN": "cn=directory manager",
  "bindPassword": "zHDUXV5vAPc=",
  "servers": [
    "localhost:1636"
  ],
  "maxConnections": 100,
  "useSSL": true,
  "baseDNs": [
    "ou=people,o=jans"
  ],
  "primaryKey": "uid",
  "localPrimaryKey": "uid",
  "useAnonymousBind": false,
  "enabled": false,
  "version": 0,
  "level": 0
}
```

### Gets LDAP Database Configuration by its name

In the above operation, we have updated `test_ldap.json`. Let's check the updated result 
with this operation by calling its name id. 

```bash title=""
/opt/jans/jans-cli/config-cli.py --operation-id get-config-database-ldap-by-name\
--url-suffix name:test_ldap
```

Here name is the `configId` of the configuration. If we run this command, it returns the 
configuration details matched with configId.

```json title="Sample Output" linenums="1"
{
  "configId": "test_ldap",
  "bindDN": "cn=directory manager",
  "bindPassword": "3eFs1t1aRPsW4xtxvCiGQQ==",
  "servers": [
    "localhost:1636"
  ],
  "maxConnections": 100,
  "useSSL": true,
  "baseDNs": [
    "ou=people,o=jans"
  ],
  "primaryKey": "uid",
  "localPrimaryKey": "uid",
  "useAnonymousBind": false,
  "enabled": false,
  "version": 0,
  "level": 0
}
```

### Delete LDAP Database Configurations

In case, we need to delete any existing LDAP Database configuration we can do that as well.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id delete-config-database-ldap-by-name \
 --url-suffix name:test_ldap
```

It will delete data ldap database configuration matched with the name.

Now you check with `get-config-database-ldap` operation

### Patch LDAP Database Configurations

If required, We can patch single information of a ldap database configuration
by using its name id. In that case, we have to make an array of operations
in schema file. So, let's get the schema file first.

```text
Operation ID: patch-config-database-ldap-by-name
  Description: Patches a LDAP configuration by name
  Parameters:
  name: Name of LDAP configuration [string]
  Schema: Array of JsonPatch
```

```bash
/opt/jans/jans-cli/config-cli.py --schema JsonPatch > /tmp/patch.json
```
The `patch-config-database-ldap-by-name` uses the [JSON Patch](https://jsonpatch.com/#the-patch) 
schema to describe the configuration change. Refer
[here](../config-tools/jans-cli/README.md#patch-request-schema) 
to know more about schema.

For example, let's say, we want to change the level of the `test_ldap` configuration. So, 
Let's update the patch file as below:

```json title="Input"
[
  {
    "op": "replace",
    "path": "level",
    "value": "100"
  }
]
```

To patch data, the command looks like for this:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id patch-config-database-ldap-by-name\
 --url-suffix name:test_ldap --data /tmp/patch.json
```

It will update the configuration and will show the updated result as below display.

```json title="Sample Output" linenums="1"
{
  "configId": "test_ldap",
  "bindDN": "cn=directory manager",
  "bindPassword": "Kn0cqLRFzk2ASG+kwAuY2Q==",
  "servers": [
    "localhost:1636"
  ],
  "maxConnections": 1000,
  "useSSL": true,
  "baseDNs": [
    "ou=people,o=jans"
  ],
  "primaryKey": "uid",
  "localPrimaryKey": "uid",
  "useAnonymousBind": false,
  "enabled": false,
  "version": 0,
  "level": 100
}
```
## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the Lightweight Directory Access Protocol. Endpoint details are published in the [Swagger
document](./../../reference/openapi.md).