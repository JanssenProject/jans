---
tags:
  - administration
  - configuration
  - user
---

# User Management

The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](../config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

## Using Command Line

In the Janssen Server, you can do CRUD operations for user management using its command line tool. To get the details of command line for CRUD operations relevant to User Management, you can find the `operation-id` under the `User` task using the Jans CLI in scim mode. The following command line:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --info User
```

```text title="Sample Output" linenums="1"
Operation ID: get-users
  Description: Query User resources (see section 3.4.2 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  filter: An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644 [string]
  startIndex: The 1-based index of the first query result [integer]
  count: Specifies the desired maximum number of query results per page [integer]
  sortBy: The attribute whose value will be used to order the returned responses [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
Operation ID: create-user
  Description: Allows creating a User resource via POST (see section 3.3 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  Schema: UserResource
  Schema: UserResource
Operation ID: get-user-by-id
  Description: Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
Operation ID: update-user-by-id
  Description: Updates a User resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion; every attribute value found in the payload sent will replace the one in the existing resource representation. Attributes not passed in the payload will be left intact.
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: UserResource
  Schema: UserResource
Operation ID: delete-user-by-id
  Description: Deletes a user resource
  Parameters:
  id: Identifier of the resource to delete [string]
Operation ID: patch-user-by-id
  Description: Updates one or more attributes of a User resource using a sequence of additions, removals, and replacements operations. See section 3.5.2 of RFC 7644
  Parameters:
  attributes: A comma-separated list of attribute names to return in the response [string]
  excludedAttributes: When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list) [string]
  id: No description is provided for this parameter [string]
  Schema: PatchRequest
  Schema: PatchRequest
Operation ID: search-user
  Description: Query User resources (see section 3.4.2 of RFC 7644)
  Schema: SearchRequest
  Schema: SearchRequest

To get sample schema type /opt/jans/jans-cli/config-cli.py -scim --schema-sample <schema>, for example /opt/jans/jans-cli/config-cli.py -scim --schema-sample SearchRequest
```

### Get Users List 
  
This operation is used to get list of the users and its properties. The following command line: 
  
```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --operation-id get-users
```

```json title="Sample Output" linenums="1"
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
        "urn:ietf:params:scim:schemas:core:2.0:User"
      ],
      "id": "f764391d-56de-4b74-b0a2-f32814706dcc",
      "meta": {
        "resourceType": "User",
        "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Users/f764391d-56de-4b74-b0a2-f32814706dcc"
      },
      "userName": "admin",
      "name": {
        "familyName": "User",
        "givenName": "Admin",
        "middleName": "Admin",
        "formatted": "Admin Admin User"
      },
      "displayName": "Default Admin User",
      "nickName": "Admin",
      "active": true,
      "emails": [
        {
          "value": "admin@imshakil-boss-guppy.gluu.info",
          "primary": false
        }
      ],
      "groups": [
        {
          "value": "60B7",
          "display": "Janssen Manager Group",
          "type": "direct",
          "$ref": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Groups/60B7"
        }
      ]
    }
  ]
}

```

As shown in the [output](#using-command-line) for `--info` command, `get-users` operation-id also supports parameters for the advanced search. Those parameters are:

    1. attributes
    2. excludeAttributes
    3. filter
    4. count [define maximum number of query]
    5. sortBy [attribute]
    6. sortOrder ['ascending', 'descending']

This is an example with `endpoint-args`:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --operation-id get-users --endpoint-args attributes:emails
```

```json title="Sample Output" linenums="1"
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
        "urn:ietf:params:scim:schemas:core:2.0:User"
      ],
      "id": "f764391d-56de-4b74-b0a2-f32814706dcc",
      "emails": [
        {
          "value": "admin@imshakil-boss-guppy.gluu.info",
          "primary": false
        }
      ]
    }
  ]
}
```

### Creating a New User

To create a new user using Jans CLI, we can use `create-user` operation-id. As shown in the [output](#using-command-line) for `--info` command, the `create-user` operation requires data to be sent according to `UserResource` schema. To see the schema, use the command as below:

```bash title="Command" linenums="1"
/opt/jans/jans-cli/config-cli.py -scim --schema UserResource
```

The Janssen Server also provides sample data for the above schema. Let's run the following command to get the sample schema:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --schema-sample UserResource
```

From the above example of schema file, we can fill required values in a data file `/tmp/user.json`. As we have seen in the sample schema there are lot of properties, but we are going to fill minimum to create a `test user`:

```json title="user.json" linenums="1"
{
  "userName": "test.user",
  "displayName": "Test User",
  "nickName": "testu",
  "active": true,
  "password": "pass@word",
  "emails": [
	  {
		  "value": "testuser@maildomain.net",
		  "primary": true
	  }
  ]
}
```
Let's run the following command to create user in Janssen Server:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --operation-id create-user --data /tmp/user.json
```

```json title="Output"
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "e24c1479-4a61-4f1f-aa30-2ccc13c0b130",
  "meta": {
    "resourceType": "User",
    "created": "2024-09-04T06:36:00.882Z",
    "lastModified": "2024-09-04T06:36:00.882Z",
    "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Users/e24c1479-4a61-4f1f-aa30-2ccc13c0b130"
  },
  "userName": "test.user",
  "displayName": "Test User",
  "nickName": "testu",
  "active": true,
  "emails": [
    {
      "value": "testuser@maildomain.net",
      "primary": true
    }
  ]
}
```

### Find User by Id

We can retrieve user details using user's `id`. For example in the above created user id is `e24c1479-4a61-4f1f-aa30-2ccc13c0b130`. To get the user details by user id, We can use the `get-user-by-id` operation as below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --operation-id get-user-by-id --url-suffix="id:e24c1479-4a61-4f1f-aa30-2ccc13c0b130"
```

```json title="Output"
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "e24c1479-4a61-4f1f-aa30-2ccc13c0b130",
  "meta": {
    "resourceType": "User",
    "created": "2024-09-04T06:36:00.882Z",
    "lastModified": "2024-09-04T06:36:00.882Z",
    "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Users/e24c1479-4a61-4f1f-aa30-2ccc13c0b130"
  },
  "userName": "test.user",
  "displayName": "Test User",
  "nickName": "testu",
  "active": true,
  "emails": [
    {
      "value": "testuser@maildomain.net",
      "primary": true
    }
  ]
}
```

### Update User by Id

Using Jans CLI, We can update user information. As shown in the [output](#using-command-line) command, the `update-user-by-id` operation requires user data that needs to be changed. You can find details of user properties in [schema](#creating-a-new-user). Let's change the `nickname` for the above `Test user`. First,we need to put the update data into a json file `/tmp/update-user.json`:
```json title='update-user.json
{
  "nickName": "testuser"
}
```
Let's run the following command:
```bash title="Command"
 /opt/jans/jans-cli/config-cli.py -scim --operation-id update-user-by-id --url-suffix="id:e24c1479-4a61-4f1f-aa30-2ccc13c0b130" --data /tmp/update-user.json
```
```json title="Sample Output"
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "e24c1479-4a61-4f1f-aa30-2ccc13c0b130",
  "meta": {
    "resourceType": "User",
    "created": "2024-09-04T06:36:00.882Z",
    "lastModified": "2024-09-05T05:38:31.491Z",
    "location": "https://imshakil-boss-guppy.gluu.info/jans-scim/restv1/v2/Users/e24c1479-4a61-4f1f-aa30-2ccc13c0b130"
  },
  "userName": "test.user",
  "displayName": "Test User",
  "nickName": "testuser",
  "active": true,
  "emails": [
    {
      "value": "testuser@maildomain.net",
      "primary": true
    }
  ]
}
```

### Patch User by Id

Using `patch-user-by-id` operation, We can modify user properties partially. As we have seen in the [Output](#using-command-line) of `--info` command, `patch-user-by-id` operation requires `PatchRequest` [schema](../config-tools/jans-cli/README.md#about-schemas) definition for payload data. To get the sample `PatchRequest` schema, run the followwing command:

```bash titl="Command"
/opt/jans/jans-cli/config-cli.py -scim --schema-sample PatchRequest
```

For example, In the above `test user`, we are going to `add` one more email, `remove` nickName and `replace` displayName. Let's put all the operations in a json file `/tmp/patch-user.json`:

```json title="patch-user.json"
{
    "schemas": [
        "urn:ietf:params:scim:api:messages:2.0:PatchOp"
    ],
    "Operations": [
        {
            "op": "replace",
            "path": "displayName",
            "value": "Test User"
        },
        {
            "op": "add",
            "path": "emails",
            "value": [
                {
                    "value": "test.user@example.jans.io",
                    "primary": true
                }
            ]
        },
        {
            "op": "remove",
            "path": "nickName"
        }
    ]
}
```
The command line to run all of these operations:
```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --operation patch-user-by-id --url-suffix="id:e24c1479-4a61-4f1f-aa30-2ccc13c0b130" --data /tmp/patch-user.json
```

### Delete User by ID

To delete the, run the following command with the specific user ID as `--url-suffix=id:user-id`. For example, let's delete the `test user` we have created earlier:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py -scim --operation-id delete-user-by-id --url-suffix="id:e24c1479-4a61-4f1f-aa30-2ccc13c0b130"
```

## Using Text-based UI
Start TUI using the command below:

```bash title="Command"
/opt/jans/jans-cli/jans_cli_tui.py
```

Navigate to `Users` to open the users tab as shown in the image below:

![](../../../assets/jans-tui-user-mgt.png)

- We can see the list of users from search option
- To get the list of users available in the Janssen Server, bring the control to `Search` box (using `tab` key) and press `Enter` key. 


### Add / Update / Delete User

1. To add user into server, hit `Enter` on `Add Users` button, fill up user details and press `Save` button to save it. Please check image below:

![add-user](../../../assets/jans-tui-create-user.png)

2. To modify any user properties, find the user from search box and hit `Enter` to pop-up user details, update user details and finally hit on `Save` button to update the changes. 

![update-user](../../../assets/jans-tui-update-user.png)

3. To delete user, bring the control on the specific user row and press `delete` or `d` key from keyboard. It will show a pop-up for confirmation as below:

![delete-user](../../../assets/jans-tui-delete-user.png)

## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the OpenID Connect Client. Endpoint details are published in the [Swagger
document](../../reference/openapi.md).
