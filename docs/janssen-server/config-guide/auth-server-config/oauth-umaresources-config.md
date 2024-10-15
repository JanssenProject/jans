---
tags:
  - administration
  - configuration
  - uma
---

# UMA Resources

The Janssen Server provides multiple configuration tools to perform these
tasks.


=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)


=== "Use Text-based UI"

    UMA Resource is not possible in Text-based UI.


=== "Use REST API"

    The UMA Resource does not have a REST API.


##  Using Command Line


In the Janssen Server, you can deploy and customize the UMA Resources using the
command line. To get the details of Janssen command line operations relevant to
UMA Resource, you can check the operations under the `OauthUmaResources` task using the
command below.

```bash title="Command"
jans cli --info OauthUmaResources
```

```text title="Sample Output"
Operation ID: get-oauth-uma-resources
  Description: Gets list of UMA resources
  Parameters:
  limit: Search size - max size of the results to return [integer]
  pattern: Search pattern [string]
  startIndex: The 1-based index of the first query result [integer]
  sortBy: Attribute whose value will be used to order the returned response [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
  fieldValuePair: Field and value pair for seraching [string]
Operation ID: put-oauth-uma-resources
  Description: Updates an UMA resource
  Schema: UmaResource
Operation ID: post-oauth-uma-resources
  Description: Creates an UMA resource
  Schema: UmaResource
Operation ID: get-oauth-uma-resources-by-id
  Description: Gets an UMA resource by ID
  Parameters:
  id: Resource description ID [string]
Operation ID: delete-oauth-uma-resources-by-id
  Description: Deletes an UMA resource
  Parameters:
  id: Resource description ID [string]
Operation ID: patch-oauth-uma-resources-by-id
  Description: Patch UMA resource
  Parameters:
  id: Resource description ID [string]
  Schema: Array of JsonPatch
Operation ID: get-oauth-uma-resources-by-clientid
  Description: Fetch uma resources by client id
  Parameters:
  clientId: Client ID [string]

To get sample schema type jans cli --schema-sample <schema>, for example jans cli --schema-sample JsonPatch
```

## Get List of UMA Resources

To find the existing UMA Resources, let's run the following command:


`get-oauth-uma-resources` operation is used to search UMA Resources.


To get a list of UMA resources:
```bash title="Command"
jans cli --operation-id get-oauth-uma-resources
```
```json title="Sample Output"
{
  "start": 0,
  "totalEntriesCount": 2,
  "entriesCount": 2,
  "entries": [
    {
      "dn": "jansId=361e1db0-19b4-4d83-9cb8-616dda8292b7,ou=resources,ou=uma,o=jans",
      "id": "361e1db0-19b4-4d83-9cb8-616dda8292b7",
      "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
      "iconUri": "http://www.jans.io/img/scim_logo.png",
      "scopes": [
        "[\"inum=CACA-0B30,ou=scopes,o=jans\"]"
      ],
      "clients": [
        "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
      ],
      "resources": [
        "[\"https://testjans.gluu.com/jans-config-api/api/v1/attributes\"]"
      ],
      "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
      "description": "uma",
      "deletable": true
    },
    {
      "dn": "jansId=c0204b2a-4047-4c2b-86a8-a088e2ee54de,ou=resources,ou=uma,o=jans",
      "id": "c0204b2a-4047-4c2b-86a8-a088e2ee54de",
      "name": "Jans Cofig Api Uma Resource",
      "iconUri": "http://www.jans.io/img/scim_logo.png",
      "clients": [
        "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
      ],
      "resources": [
        "[\"https://testjans.gluu.com/jans-config-api/api/v1/config/cache/native-persistence\"]"
      ],
      "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
      "description": "uma resource",
      "deletable": true
    }
  ]
}
```

To search using multiple arguments, you can change pattern that you want to find:

```bash 
jans cli --operation-id get-oauth-uma-resources \
--endpoint-args limit:1,pattern:"Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes"
```


```json
{
  "start": 0,
  "totalEntriesCount": 1,
  "entriesCount": 1,
  "entries": [
    {
      "dn": "jansId=361e1db0-19b4-4d83-9cb8-616dda8292b7,ou=resources,ou=uma,o=jans",
      "id": "361e1db0-19b4-4d83-9cb8-616dda8292b7",
      "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
      "iconUri": "http://www.jans.io/img/scim_logo.png",
      "scopes": [
        "[\"inum=CACA-0B30,ou=scopes,o=jans\"]"
      ],
      "clients": [
        "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
      ],
      "resources": [
        "[\"https://testjans.gluu.com/jans-config-api/api/v1/attributes\"]"
      ],
      "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
      "description": "uma",
      "deletable": true
    }
  ]
}

```

## Get Oauth UMA Resource by ID

With `get-oauth-uma-resources-by-id` operation-id, we can get any specific
uma Resource matched with `ID`. If we know the `ID`, we can simply use the below command:

```bash title="Command"
jans cli --operation-id get-oauth-uma-resources-by-id \
--url-suffix id:361e1db0-19b4-4d83-9cb8-616dda8292b7
```

```json title="Sample Output"
{
  "dn": "jansId=361e1db0-19b4-4d83-9cb8-616dda8292b7,ou=resources,ou=uma,o=jans",
  "id": "361e1db0-19b4-4d83-9cb8-616dda8292b7",
  "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
  "iconUri": "http://www.jans.io/img/scim_logo.png",
  "scopes": [
    "[\"inum=CACA-0B30,ou=scopes,o=jans\"]"
  ],
  "clients": [
    "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
  ],
  "resources": [
    "[\"https://testjans.gluu.com/jans-config-api/api/v1/attributes\"]"
  ],
  "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
  "description": "uma",
  "deletable": true
}
```


## Patch OAuth UMA Resource by ID

Using `patch-oauth-uma-resources-by-id` operation, 
we can modify `UMA Resource` partially for its properties.



```text
Operation ID: patch-oauth-uma-resources-by-id
  Description: Patch UMA resource
  Parameters:
  id: Resource description ID [string]
  Schema: Array of JsonPatch
```



To use this operation, specify the id of the Uma that needs to be updated using the `--url-suffix`
and the property and the new value using the [JSON Patch ](https://jsonpatch.com/#the-patch).
Refer [here](https://docs.jans.io/vreplace-janssen-version/admin/config-guide/config-tools/jans-cli/#patch-request-schema) to know more about schema.

In this example; We will change the value of the property `name` from `uma resource` to `UMA`.

```json title="Input"
[
{
  "op": "replace",
  "path": "name",
  "value": "UMA"
}
]
```

Now let's do the operation with the command line.


```bash title="Command"
jans cli --operation-id patch-oauth-uma-resources-by-id \
--url-suffix id:c0204b2a-4047-4c2b-86a8-a088e2ee54de --data /tmp/patch-uma.json
```
```json title="Sample Output"
{
  "dn": "jansId=c0204b2a-4047-4c2b-86a8-a088e2ee54de,ou=resources,ou=uma,o=jans",
  "id": "c0204b2a-4047-4c2b-86a8-a088e2ee54de",
  "name": "UMA",
  "iconUri": "http://www.jans.io/img/scim_logo.png",
  "clients": [
    "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
  ],
  "resources": [
    "[\"https://testjans.gluu.com/jans-config-api/api/v1/config/cache/native-persistence\"]"
  ],
  "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
  "description": "uma resource",
  "deletable": true
}
```

We see it has replaced the value of the `name` property from `Jans Cofig Api Uma Resource` to `UMA`.

Please read about [patch method](../config-tools/jans-cli/README.md#quick-patch-operations),
You can get some idea of how this patch method works to modify particular properties of any task.




### Adds new Uma Resource

To add a new Uma Resource, we can use `post-oauth-uma-resources` operation id.
As shown in the [output](#using-command-line) for `--info` command, the
`post-oauth-uma-resources` operation requires data to be sent
according to `UmaResource` schema.

To see the schema, use the command below:

```bash title="Command"
jans cli --schema UmaResource
```

The Janssen Server also provides an example of data that adheres to
the above schema. To fetch the example, use the command below.

```bash title="Command"
jans cli --schema-sample UmaResource
```

Using the schema and the example above, we have added below data to the file `/tmp/uma.json`.

```json title="Input"
{
    "name": "Jans Cofig Api Uma",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": null,
    "scopeExpression": null,
    "clients": [
      "inum=b294adfd-b825-4e7f-9815-55d744002315,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/config/cache/native-persistence"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": "uma resource",
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
}
```
Now let's post this uma to the Janssen Server to be added to the existing set:

```bash title="Command"
jans cli --operation-id post-oauth-uma-resources --data /tmp/uma.json
```

## Updates an UMA resource

To update the uma resource follow the steps below.

1. [Get Oauth UMA Resource by ID](#get-oauth-uma-resource-by-id) and store it into a file for editing.
   The following command will retrieve the existing Uma resource in the schema file.
  ```bash title="Sample Command"
  jans cli -no-color --operation-id get-oauth-uma-resources-by-id \
  --url-suffix id:c70c3b5c-d543-4dec-923c-4035bdce52bb > /tmp/update-uma.json
  ```
2. Edit and update the desired configuration values in the file while keeping other
   properties and values unchanged. Updates must adhere to the `UmaResource`
   schema as mentioned [here](#using-command-line).
3. We have changed only the `true` to `false` for `deletable` in existing uma resource.
   Use the updated file to send the update to the Janssen Server using the command below
   ```bash title="Command"
   jans cli --operation-id put-oauth-uma-resources --data /tmp/update-uma.json
   ```
This will updated the existing uma resource matched with id.




## Get Oauth UMA Resource by Client id

With `get-oauth-uma-resources-by-clientid` operation-id, we can get any specific
uma Resource matched with `clientid`. If we know the `clientid`, we can simply use the below command:

```bash title="Command"
jans cli --operation-id get-oauth-uma-resources-by-clientid \
--url-suffix client id:361e1db0-19b4-4d83-9cb8-616dda8292b7
```

```json title="Sample Output"
{
  "dn": "jansId=361e1db0-19b4-4d83-9cb8-616dda8292b7,ou=resources,ou=uma,o=jans",
  "id": "361e1db0-19b4-4d83-9cb8-616dda8292b7",
  "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
  "iconUri": "http://www.jans.io/img/scim_logo.png",
  "scopes": [
    "[\"inum=CACA-0B30,ou=scopes,o=jans\"]"
  ],
  "clients": [
    "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
  ],
  "resources": [
    "[\"https://testjans.gluu.com/jans-config-api/api/v1/attributes\"]"
  ],
  "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
  "description": "uma",
  "deletable": true
}
```




### Delete Uma Resource by `id`

You can delete any Uma Resource by its `id` value.

```bash title="Command"
jans cli --operation-id delete-oauth-uma-resources-by-id \
--url-suffix id:c0204b2a-4047-4c2b-86a8-a088e2ee54de
```

Just change the `id` to your own according to which one you want to delete.
