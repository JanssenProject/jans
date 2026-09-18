---
tags:
  - administration
  - user management
  - cli
  - tui
---

# Jans CLI/TUI User Management

Janssen Server provides command-line, text-based, and REST API interfaces for user management.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-guide/config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](../config-guide/config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-guide/config-tools/config-api/README.md) or jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)


## Using Command Line

The Jans CLI provides command-line operations for creating, updating, and deleting users and managing user attributes. 

### Create a User

The `post-user` operation creates a new user in Janssen Server. User information can be provided either in a JSON file or directly as JSON data.

#### Using a JSON File

1. Create a JSON file containing the user attributes.
   ```json
   {
     "userId": "testcliuser",
     "mail": "user@example.com",
     "userPassword": "your-password",
     "status": "active",
     "displayName": "Test User",
     "givenName": "Test"
   }
   ```

2. Run the `post-user` operation and provide the path to the JSON file:
   ```bash
   jans cli --operation-id post-user --data /path/to/user.json 
   ```

3. A successful request returns the details of the newly created user, including its assigned `inum`.


#### Using JSON Data

You can also provide the user attributes directly with the `--data` option:

```bash
jans cli --operation-id post-user \
  --data '{
    "userId": "testcliuser",
    "givenName": "Test",
    "displayName": "Test User",
    "mail": "user@example.com",
    "userPassword": "your-password",
    "status": "active"
  }'
```

A successful request returns the details of the newly created user.

!!! note

    If a required attribute is missing, the server returns an error `USER_CREATION_ERROR` identifying the missing attributes.

    For example:

    ```text
    {
        "code": "400",
        "message": "USER_CREATION_ERROR",
        "description": "Attributes Missing in request are {displayName,givenName}."
    }
    ```

### Update a User

The `put-user` operation updates an existing user in Janssen Server. User information can be provided either in a JSON file or directly as JSON data.

To update a user, include the user's `inum` and `baseDn` along with the user attributes.

#### Using a JSON File

1. Create a JSON file containing the user's information:
   ```json
   {
     "userId": "testcliuser",
     "givenName": "Updated",
     "displayName": "Updated Test User",
     "mail": "user@example.com",
     "status": "active",
     "inum": "<user-inum>",
     "baseDn": "<user-base-dn>"
   }
   ```

2. Run the `put-user` operation and provide the path to the JSON file:
   ```bash
   jans cli --operation-id put-user --data /path/to/user.json
   ```

3. A successful request returns the updated user information, including the `updatedAt` timestamp.

#### Using JSON Data

You can also provide the user information directly with the `--data` option:

```bash
jans cli --operation-id put-user \
  --data '{
    "userId": "testcliuser",
    "givenName": "Updated",
    "displayName": "Updated Test User",
    "mail": "user@example.com",
    "status": "active",
    "inum": "<user-inum>",
    "baseDn": "<user-base-dn>"
  }'
```

A successful request returns the updated user information.

!!! note

    The `inum` and `baseDn` values must correspond to the existing user you want to update.


### Add a Claim

The `put-user` operation adds a claim to an existing user in Janssen Server. The claim is specified through the `customAttributes` field. The user's information, including `inum` and `baseDn`, must also be provided.

#### Using a JSON File

1. Create a JSON file containing the user's information and the claim:
   ```json
   {
     "userId": "testcliuser",
     "givenName": "Updated",
     "displayName": "Updated Test User",
     "mail": "cli-test-user@example.com",
     "status": "active",
     "inum": "<user-inum>",
     "baseDn": "<user-base-dn>",
     "customAttributes": [
       {
         "name": "phoneNumberVerified",
         "multiValued": false,
         "values": [true]
       }
     ]
   }
   ```
   The `customAttributes` field specifies the user attributes to add or update. The `name` specifies the claim name, `multiValued` specifies whether the claim can have multiple values, and `values` contains the claim value.

2. Run the `put-user` operation and provide the path to the JSON file:
   ```bash
   jans cli --operation-id put-user --data /path/to/user.json
   ```

3. A successful request returns the updated user information.


#### Using JSON Data

You can also provide the user information and claim directly with the `--data` option:

```bash
jans cli --operation-id put-user \
  --data '{
    "userId": "updatedtestuser",
    "givenName": "Update given name",
    "displayName": "Updated display name",
    "mail": "cli-test-user@example.com",
    "status": "active",
    "inum": "<user-inum>",
    "baseDn": "<user-base-dn>",
    "customAttributes": [
      {
        "name": "phoneNumberVerified",
        "multiValued": false,
        "values": [true]
      }
    ]
  }'
```

A successful request returns the updated user information with the added claim.

!!! note

    The `inum` and `baseDn` values must correspond to the existing user. The `customAttributes` field specifies the claim to add.


### Change Password

The `patch-user-by-inum` operation changes the password of an existing user in Janssen Server. The user is identified by `inum`, and the new password is provided through the `customAttributes` field.

The password can be provided either in a JSON file or directly as JSON data.

#### Using a JSON File

1. Create a JSON file containing the new password:
   ```json
   {
     "jsonPatchString": "",
     "customAttributes": [
       {
         "name": "userPassword",
         "multiValued": false,
         "value": "your-new-password"
       }
     ]
   }
   ```

2. Run the `patch-user-by-inum` operation and provide the user's `inum` and the path to the JSON file:
   ```bash
   jans cli --operation-id patch-user-by-inum --url-suffix "inum:<user-inum>" --data /path/to/user.json
   ```

3. A successful request returns the updated user information, including the `updatedAt` timestamp.


#### Using JSON Data

You can also provide the new password directly with the `--data` option:

```bash
jans cli --operation-id patch-user-by-inum \
  --url-suffix "inum:<user-inum>" \
  --data '{
    "jsonPatchString": "",
    "customAttributes": [
      {
        "name": "userPassword",
        "multiValued": false,
        "value": "your-new-password"
      }
    ]
  }'
```

A successful request returns the updated user information.

!!! note

    The `<user-inum>` value must correspond to the existing user whose password you want to change.


### Delete User

The `delete-user` operation deletes an existing user from Janssen Server. The user is identified by their `inum`.

1. Run the `delete-user` operation and provide the user's `inum`:
   ```bash
   jans cli --operation-id delete-user --url-suffix "inum:<user-inum>"
   ```  

2. A successful request returns:

   ```text
   Object was successfully deleted.
   ```



## Using Text-based UI

Jans TUI provides user-management operations from the terminal. For instructions on adding, updating, deleting, and managing users with Jans TUI, see [Using Text-based UI](../config-guide/scim-config/user-config.md#using-text-based-ui).


## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing and configuring the user. Endpoint details are published in the [Swagger
document](./../reference/openapi.md).
