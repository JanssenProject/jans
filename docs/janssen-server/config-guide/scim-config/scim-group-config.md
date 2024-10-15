---
tags:
  - administration
  - configuration
  - group
---

# Group Management

The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

## Using Command Line

```bash title="Command"
jans cli -scim --info Group
```

```text title="Command Output"
Operation ID: get-groups
  Description: Query Group resources (see section 3.4.2 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  filter: An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644 [string]
  startIndex: The 1-based index of the first query result [integer]
  count: Specifies the desired maximum number of query results per page [integer]
  sortBy: The attribute whose value will be used to order the returned responses [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
Operation ID: create-group
  Description: Allows creating a Group resource via POST (see section 3.3 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  Schema: GroupResource
  Schema: GroupResource
Operation ID: get-group-by-id
  Description: Retrieves a Group resource by Id (see section 3.4.1 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
Operation ID: update-group-by-id
  Description: Updates a Group resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion&amp;#58; every
attribute value found in the payload sent will replace the one in the existing resource representation. Attributes
not passed in the payload will be left intact.

  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: GroupResource
  Schema: GroupResource
Operation ID: delete-group-by-id
  Description: Deletes a group resource (see section 3.6 of RFC 7644)
  Parameters:
  id: Identifier of the resource to delete [string]
Operation ID: patch-group-by-id
  Description: Updates one or more attributes of a Group resource using a sequence of additions, removals, and replacements operations. See section 3.5.2 of RFC 7644

  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: PatchRequest
  Schema: PatchRequest
Operation ID: search-group
  Description: Query Group resources (see section 3.4.2 of RFC 7644)
  Schema: SearchRequest
  Schema: SearchRequest

To get sample schema type jans cli -scim --schema-sample <schema>, for example jans cli -scim --schema-sample SearchRequest 
```


### Get Groups

This operation can be used to get list of groups. To get the list of groups, run the following command:

```bash title="Command"
jans cli -scim --operation-id get-groups
```

It will show the list of groups with all the members linked with each of these groups. 

```json title="Sample Output"
{
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:ListResponse"
  ],
  "totalResults": 1,
  "startIndex": 1,
  "itemsPerPage": 1,
  "Resources": [
    {
      "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:Group"
      ],
      "id": "60B7",
      "meta": {
        "resourceType": "Group",
        "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Groups/60B7"
      },
      "displayName": "Janssen Manager Group",
      "members": [
        {
          "value": "f764391d-56de-4b74-b0a2-f32814706dcc",
          "type": "User",
          "display": "Default Admin User",
          "$ref": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Users/f764391d-56de-4b74-b0a2-f32814706dcc"
        }
      ]
    }
  ]
}
```

You can filter for the advanced search with some of its properties:

1. attributes
2. excludeAttributes
3. filter
4. count [define maximum number of query]
5. sortBy [attribute]
6. sortOrder ['ascending', 'descending']

### Create Group

Using `create-group` operation, we can create groups into Janssen Server. As we have seen in the [output](#using-command-line) of `--info` command, this operation requires `GroupResource` schema. To know the details of schema, run the following command:

```bash title="Command"
jans cli -scim --schema GroupResource
```

The Janssen server also provides sample schema. To get the sample schema of `GroupResource`:

```bash title="Command"
jans cli -scim --schema-sample GroupResource
```

According to schema, let's put all the details into a json file `/tmp/create-group.json` to create a group.

```json title="sample"
{
  "displayName": "New Group"
}
```

Now let's run the following command to add group into the server:

```bash title="Command"
 jans cli -scim --operation-id create-group --data /tmp/create-group.json
``` 

```json title="Command Output"
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:Group"
  ],
  "id": "7a20464c-3651-48a0-9c9c-6b59373df60c",
  "meta": {
    "resourceType": "Group",
    "created": "2024-09-06T03:46:31.224Z",
    "lastModified": "2024-09-06T03:46:31.224Z",
    "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Groups/7a20464c-3651-48a0-9c9c-6b59373df60c"
  },
  "displayName": "New Group"
}
```

### Get Group by ID

We can view the specific group details through its `id` using `get-group-by-id` operation. 
For example, We can put the above created group `id:7a20464c-3651-48a0-9c9c-6b59373df60c` with `--url-suffix` to get the groupe details. The following command as below:

```bash title="Command"
jans cli -scim --operation-id get-group-by-id \
--url-suffix="id:7a20464c-3651-48a0-9c9c-6b59373df60c"
```
```json title="Command Output"
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:Group"
  ],
  "id": "7a20464c-3651-48a0-9c9c-6b59373df60c",
  "meta": {
    "resourceType": "Group",
    "created": "Fri Sep 06 03:46:31 UTC 2024",
    "lastModified": "Fri Sep 06 03:46:31 UTC 2024",
    "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Groups/7a20464c-3651-48a0-9c9c-6b59373df60c"
  },
  "displayName": "New Group",
  "members": []
}
```

We see `members` is empty since we did not associate any user with this group yet. We will add members into this gorup in the next operation. 

### Update Group by ID

The `update-group-by-id` operation can be used to update group name and adding members into the group. Let's create a json file `/tmp/update-group.json` according to the [`GroupResource`](#create-group) schema:

```json title="Sample"
{
  "members": [
    {
      "value": "4ed288be-4d1c-4e05-a3af-7a8935fc7f4c",
      "type": "user"
    }
  ]
}
```

We can get the `value` which is actually the `id` of specific users from [user management](./user-config.md#get-users-list) section. Let's run the following command to update empty members properties with a member into the group we created [above](#create-group).

```bash title="Command"
jans cli -scim --operation-id update-group-by-id \
--url-suffix="id:7a20464c-3651-48a0-9c9c-6b59373df60c" --data /tmp/update-group.json
```

```json title="Output"
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:Group"
  ],
  "id": "7a20464c-3651-48a0-9c9c-6b59373df60c",
  "meta": {
    "resourceType": "Group",
    "created": "Fri Sep 06 03:46:31 UTC 2024",
    "lastModified": "2024-09-06T05:15:53.227Z",
    "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Groups/7a20464c-3651-48a0-9c9c-6b59373df60c"
  },
  "displayName": "New Group",
  "members": [
    {
      "value": "4ed288be-4d1c-4e05-a3af-7a8935fc7f4c",
      "type": "User",
      "display": "Test User",
      "$ref": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Users/4ed288be-4d1c-4e05-a3af-7a8935fc7f4c"
    }
  ]
}
```

> **Please remember one thing, this update method just replace the data. If you want to add members instead of replacing then you must try [patch-group-by-id](#patch-group).**

### Delete Group by ID

You can delete a group by its ID. The command line looks like:

```bash title="Command"
jans cli -scim --operation-id delete-group-by-id \
--url-suffix="id:7a20464c-3651-48a0-9c9c-6b59373df60c"
```

It will delete the group and all of its associated data if match with the unique group ID.

### Patch Group

This is also an option to update any existing group resources. The only difference between [update-group-by-id](#update-group-by-id) and [patch-group](#patch-group) is that the first one just replace new data with previous one. It won't add any new data into the group. With `patch-group-by-id` operation, we can `add`, `remove`, and `replace` properties of group.

According to the [output](#using-command-line) of `--info` command, we can see `patch-group-by-id` requires `PatchRequest` schema. 

```
[
	{
		"op": "add",
		"path": "members",
		"value": {
			"value": "f764391d-56de-4b74-b0a2-f32814706dcc",
			"type": "user"
		}
	}
]
```

Let's run the following command:

```bash title="Command"
jans cli -scim --operation-id patch-group-by-id \
--url-suffix="id:7a20464c-3651-48a0-9c9c-6b59373df60c" --data /tmp/patch-user.json
```

## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the OpenID Connect Client. Endpoint details are published in the [Swagger
document](../../reference/openapi.md).