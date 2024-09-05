---
tags:
  - administration
  - user management
  - scim
---

## SCIM User Management

SCIM is a specification designed to reduce the complexity of user management operations by providing a common user schema and the patterns for exchanging such schema using HTTP in a platform-neutral fashion. The aim of SCIM is achieving interoperability, security, and scalability in the context of identity management.

For your reference, the current version of the standard is governed by the following documents: [RFC 7642](https://datatracker.ietf.org/doc/html/rfc7642), [RFC 7643](https://datatracker.ietf.org/doc/html/rfc7643), and [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644).


## Installation 

The API is available as a component of Jans Server. Upon [installation](https://docs.jans.io/v1.0.14/admin/install/vm-install/vm-requirements/) you can select if you want SCIM included in your environment. To add SCIM post-install do the following:

1. Run `python3 /opt/jans/jans-setup/setup.py --install-scim`

## About API endpoints

Throughout this document, you will notice endpoints are prefixed with path: 
`/jans-scim/restv1/v2`

## API Protection

Clearly, this API must not be anonymously accessed. However, the basic SCIM standard does not define a specific mechanism to prevent unauthorized requests to endpoints. There are just a few guidelines in section 2 of [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644) concerned with authentication and authorization.

* OAUTH, This is the default and recommended mechanism
* BYPASS

Depending on the scopes associated to a token, you will be granted (or denied) access to perform certain operations. The following lists the available scopes:

|Scope|Actions allowed|
|-|-|
|`https://jans.io/scim/users.read`|Query user resources|
|`https://jans.io/scim/users.write`|Modify user resources|
|`https://jans.io/scim/groups.read`|Query group resources|
|`https://jans.io/scim/groups.write`|Modify group resources|
|`https://jans.io/scim/fido.read`|Query fido resources|
|`https://jans.io/scim/fido.write`|Modify fido resources|
|`https://jans.io/scim/fido2.read`|Query fido 2 resources|
|`https://jans.io/scim/fido2.write`|Modify fido 2 resources|
|`https://jans.io/scim/all-resources.search`|Access the root .search endpoint| 
|`https://jans.io/scim/bulk`|Send requests to the bulk endpoint|

In order to facilitate the process of getting an access token, your Janssen installation already bundles an OAuth client named "SCIM client" with support for all the scopes above. This client uses the `client_credentials` grant type and `client_secret_basic` mechanism to authenticate to the token endpoint.

## Where to locate SCIM-related logs

Please see [here](https://docs.jans.io/v1.0.14/admin/scim/logs/) besides 

* SCIM log is located at `/opt/jans/jetty/jans-scim/logs/scim.log`
* If you use SCIM custom script aslo see `/opt/jans/jetty/jans-scim/logs/scim_script.log`

## API documentation at a glance

[SCIM API](https://docs.jans.io/v1.0.14/admin/reference/openapi/) doc page describes about our implementation of SCIM. The API has also been documented using OpenAPI (swagger) specification for the interested. Find yaml files [here](https://github.com/JanssenProject/jans/blob/main/jans-scim/server/src/main/resources/jans-scim-openapi.yaml).


## Working in OAuth mode

To know more about OAuth protection mode please visit [here](https://docs.jans.io/v1.0.14/admin/scim/oauth-protection/).
The SCIM API endpoints are by default protected by (Bearer) OAuth 2.0 tokens. Depending on the operation, these tokens must have certain scopes for the operations to be authorized. We need a client to get Bearer token. 
### Get SCIM Client
Let's obtain the credentials of this client first. In TUI, navigate to `Auth Server > Clients`. In the search field type SCIM (uppercase). Highlight the row that matches a client named "SCIM Client" and press Enter. To see in `JSON` formate please press `d`.

From the "Basic" section, grab the "client id" and "client secret". This secret is encrypted, to decrypt it, in a terminal run `/opt/jans/bin/encode.py -D ENCRYPTED-SECRET-HERE`.


### Get Access token

Let's get a token, 
```
curl -k -u 'CLIENT_ID:DECRYPTED_CLIENT_SECRET' -k -d grant_type=client_credentials -d scope='https://jans.io/scim/users.read https://jans.io/scim/users write' https://<jans-server>/jans-auth/restv1/token > /tmp/token.json
```
In response `token.json` we will get `access_token`
```
{
"access_token":"11a76589-7955-4247-9ca5-f3ad7884305...",
"scope":"https://jans.io/scim/users.read",
"token_type":"Bearer",
"expires_in":299
}
```

### Retrive existing User 

To get an existing user 

```
curl -k -G -H 'Authorization: Bearer ACCESS_TOKEN' --data-urlencode 'filter=displayName co "Admin"' https://<jans-server>/jans-scim/restv1/v2/Users > /tmp/user.json
```
In response `user.json` we will get 
```
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
      "id": "5fdbb720-a1fd-477f-af92-b7c054f02c98",
      "meta": {
        "resourceType": "User",
        "created": "2023-06-12T14:54:09.531Z",
        "location": "https://raju.jans13.me/jans-scim/restv1/v2/Users/5fdbb720-a1fd-477f-af92-b7c054f02c98"
      },
      "userName": "admin",
      "name": {
        "familyName": "...",
        "givenName": "...",
        "middleName": "...",
        "formatted": "..."
      },
      "displayName": "Admin",
      "active": true,
      "emails": [
        {
          "value": "example@gluu.org",
          "primary": false
        }
      ],
      "groups": [
        {
          "value": "60B7",
          "display": "Jannsen Manager Group",
          "type": "direct",
          "$ref": "https://raju.jans13.me/jans-scim/restv1/v2/Groups/60B7"
        }
      ]
    }
  ]
}
```


## Creating Resource 
### Create an User
Let's start creating a dummy user. A client sends a POST request containing a "User" to the "/Users" endpoint. 
```
POST /Users  HTTP/1.1
Host: example.com
Accept: application/scim+json
Content-Type: application/scim+json
Authorization: Bearer h480djs93hd8..
Content-Length: ...

{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "userName": "bjensen",
  "externalId": "bjensen",
  "name": {
    "formatted": "Ms. Barbara J Jensen III",
    "familyName": "Jensen",
    "givenName": "Barbara"
  }
}
```
Open a text editor and copy paste the json body, name as `input.json`.
Hit on your terminal with bellow command.
```
curl -k -H 'Authorization: Bearer ACCESS_TOKEN' -H 'Content-Type: application/scim+json' -d @input.json -o output.json https://<jans-server>/jans-scim/restv1/v2/Users
```
response looks like 
```
{
    "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:User"
    ],
    "id": "e3009115-b890-4d8b-bd63-bbfef34aa583",
    "externalId": "bjensen",
    "meta": {
        "resourceType": "User",
        "created": "2023-06-26T19:43:32.945Z",
        "lastModified": "2023-06-26T19:43:32.945Z",
        "location": "https://raju.jans13.me/jans-scim/restv1/v2/Users/e3009115-b890-4d8b-bd63-bbfef34aa583"
    },
    "userName": "bjensen",
    "name": {
        "familyName": "Jensen",
        "givenName": "Barbara",
        "formatted": "Ms. Barbara J Jensen III"
    }
}
```

This new user has been given an `id`. If possible, inspect your `ou=people` branch and find the entry whose `inum` matches the `id` given. An easier option would be to via **Jans TUI** and go to `Users` and search "bjensen" to see the recently created user.

### Updating a User(PUT)

Overwrite your `input.json` with the following. Replace content in angle brackets accordingly:

```
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "e3009115-b890-4d8b-bd63-bbfef34aa583",
  "userName": "bjensen",
  "externalId": "bjensen",
  "name": {
    "formatted": "Ms. Barbara J Jensen III",
    "familyName": "Jensen",
    "givenName": "Barbara"
  },
  "displayName": "Jensen Barbara",
  "emails": [
    {
      "value": "jensen@example.com",
      "type": "work",
      "primary": true
    }
  ]
}
```

PUT with curl:

```
curl -k -X PUT -H 'Authorization: Bearer ACCESS_TOKEN' -H 'Content-Type: application/scim+json' -d @input.json -o output.json https://<jans-server>/jans-scim/restv1/v2/Users/<user-inum>
```

Response `(output.json)` will show the same contents of a full retrieval.

Please verify changes were applied whether by inspecting LDAP or issuing a GET. If you have followed the steps properly, you should notice a new e-mail added and the change in `displayName` attribute


### Updating a User (PATCH)

With patching, you can be very precise about the modifications you want to apply. Patching syntax follows JSON Patch spec (RFC 6902) closely. While it's not a must to read the RFC to learn how patch works, see section 3.5.2 of SCIM protocol (RFC 7644) to get the grasp.

If you prefer reading code, [patch test cases](https://github.com/JanssenProject/jans/tree/main/jans-scim/client/src/test/java/io/jans/scim2/client/patch) found in the Java scim-client project are worth to look at.

The following is a simple example that illustrates the kind of modifications developers can achieve via `PATCH`. Overwrite your `input.json` with the following:

```
{
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:PatchOp"
  ],
  "Operations": [
    {
      "op": "replace",
      "value": {
        "name": {
          "givenName": "Joey"
        }
      }
    },
    {
      "op": "replace",
      "path": "emails[type eq \"work\" or primary eq false].value",
      "value": "jensen@example.com"
    },
    {
      "op": "add",
      "value": {
        "name": {
          "middleName": "Jhon"
        }
      }
    },
    {
      "op": "add",
      "value": {
        "emails": [
          {
            "primary": true,
            "value": "my@own.mail"
          }
        ],
        "phoneNumbers": [
          {
            "type": "home",
            "value": "5 123 8901"
          },
          {
            "value": "5 123 8902"
          }
        ]
      }
    },
    {
      "op": "remove",
      "path": "name.middleName"
    },
    {
      "op": "remove",
      "path": "phoneNumbers[value ew \"01\"].type"
    }
  ]
}
```

A collection of modification are provided under "Operations". They are processed in order of appearance. Also, every operation has a type; patching supports add, remove and replace.

The first operations states the following: replace the value of `givenName` subattribute (that belongs to complex attribute `name`) with the string "Joey".

Operations are easier to understand when using a "path". The second operation replaces the value subattribute inside the complex multi-valued attribute emails. Inside the square brackets, we find a filter expression, so the replacement does not apply to all emails in the list but only to those matching the criterion.

So the second operation can be read as "set the value of value subattribute to string `jensen@example.com` where the type subattribute of the `email` equals to string "work" or if primary attribute is false".

The third operation is similar to the first. It sets the value of a subattribute which was unassigned (null). You could have used "replace" operation in this case and results would have been identical.

The fourth operation is more interesting. It adds to the current list of emails a new one. It supplies a couple of subattributes for the email to include: primary and value. Additionally, we set the value of (previously unassigned) phoneNumbers multi-valued attribute passing a list of elements.

In the fifth operation, we remove the `middleName` attribute that was set in operation three. Note how we make explicit the path of data to nullify: "name.middleName".

The sixth operation allows us to remove a specific subattribute of `phoneNumbers`. The aim is to nullify the "type" of the item whose phone number value ends with "01". The remove operation can also be used to remove a complete item from a list, or empty the whole list by providing a suitable value for "path".

Now let's see it in action:


```
curl -k -X PATCH -H 'Authorization: Bearer ACCESS_TOKEN' -H 'Content-Type: application/scim+json' -d @input.json -o output.json https://<jans-server>/jans-scim/restv1/v2/Users/<user-inum>
```

So far our resource look like this

```
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "e3009115-b890-4d8b-bd63-bbfef34aa583",
  "externalId": "bjensen",
  "meta": {
    "resourceType": "User",
    "created": "2023-06-26T19:43:32.945Z",
    "lastModified": "2023-06-26T22:34:27.465Z",
    "location": "https://raju.jans13.me/jans-scim/restv1/v2/Users/e3009115-b890-4d8b-bd63-bbfef34aa583"
  },
  "userName": "bjensen",
  "name": {
    "familyName": "Jensen",
    "givenName": "Joey",
    "formatted": "Ms. Barbara J Jensen III"
  },
  "displayName": "Jensen Barbara",
  "active": false,
  "emails": [
    {
      "value": "my@own.mail",
      "primary": true
    },
    {
      "value": "jensen@example.com",
      "type": "work",
      "primary": false
    }
  ],
  "phoneNumbers": [
    {
      "value": "5 123 8901"
    },
    {
      "value": "5 123 8902"
    }
  ]
}
```

Note the primary subattribute accompanying `email` "my@own.mail" is false but when inserted we provided `true`. This is because the SCIM specification states that after modifications are applied to resources **(PUT or PATCH)**, there cannot be more than one item in a multi-valued attribute with primary value set as `true`.

To see more sample `JSON` payloads, check the `.json` files used by the scim-client test cases referenced above.

### Deleting Users

For deleting, the `DELETE `method of `HTTP` is used.

No input file is used in this case. A delete request could be the following:

```
curl -k -X DELETE -H 'Authorization: Bearer ACCESS_TOKEN' https://<jans-server>/jans-scim/restv1/v2/Users/<user-inum>
```

Use the inum of our dummy user, **Jensen Barbara**.

Check your LDAP or via Jans TUI to see that **Bjensen** is gone.


## How is SCIM data stored?

SCIM [schema spec](https://datatracker.ietf.org/doc/html/rfc7643) does not use LDAP attribute names but a different naming convention for resource attributes (note this is not the case of custom attributes where the SCIM name used is that of the LDAP attribute).

It is possible to determine if a given LDAP attribute is being mapped to a SCIM attribute. For that you need to check in Jans TUI `Auth-Server >> Attributes` and click on any attributes. Check `Include in SCIM Extension:` is `true` or `false`. Whenever you try to map any LDAP attribute to a SCIM attribute keep it's value `true`.  


## FIDO Devices

A FIDO device represents a user credential stored in the Jans Server database that is compliant with the [FIDO](https://fidoalliance.org/) standard. These devices are used as a second factor in a setting of strong authentication.

FIDO devices were superseded by [FIDO 2](#fido2-devices) devices in Jans Server.

## FIDO 2 devices

FIDO 2 devices are credentials that adhere to the more current Fido 2.0 initiative (WebAuthn + CTAP). Examples of FIDO 2 devices are USB security keys and Super Gluu devices.

The SCIM endpoints for FIDO 2 allow application developers to query, update and delete already existing devices. Addition of devices do not take place through the service since this process requires direct end-user interaction, ie. device enrolling.

The schema attributes for a device of this kind can be found by hitting the URL  `https://<jans-server>/jans-scim/restv1/v2/Schemas/urn:ietf:params:scim:schemas:core:2.0:Fido2Device`

To distinguish between regular FIDO2 and SuperGluu devices, note only SuperGluu entries have the attribute `deviceData` populated (i.e. not null)

### Example: Querying Enrolled Devices

Say we are interested in having a list of Super Gluu devices users have enrolled and whose operating system is iOS. We may issue a query like this:

```
curl -k -G -H 'Authorization: Bearer ACCESS_TOKEN' --data-urlencode 
'filter=deviceData co "ios"' -d count=10 https://<jans-server>/jans-scim/restv1/v2/Fido2Devices
```

The response will be like:

```
{
  "totalResults": ...,
  "itemsPerPage": ...,
  "startIndex": 1,
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:ListResponse"
  ],
  "Resources": [
    {
      "id": "...",
      "meta": {...},
      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Fido2Device"],
      "userId": "...",
      ...
      "deviceData": "{...}",
      "displayName": ...,
    },
    ...
  ]
}
```

## Potential performance issues with Group endpoints

In SCIM a group resource basically consists of an identifier, a display name, and a collection of members associated to it. Also, every member is made up of a user identifier, his display name, and other attributes. As a consequence, retrieving group information requires making a correlation with existing user data. Since Gluu database model does not follow a relational database pattern this may entail a considerable amount of user queries when groups contain thousands of members.

While this could have been workarounded by storing members' display names inside group entries, this brings additional problems to deal with.

Another source of potential overhead stems from creation and modification of groups where many new users are associated to a given group: by default checks are made to guarantee only existing users are attached to groups, thus requiring continuous database queries.

Currently there are two ways to lower the amount of database lookups required for SCIM group operations:

* Explicitly excluding display names from responses
* Pass the overhead bypass flag to skip members validations

The first approach consists of using the query parameter `excludedAttributes` (see [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644)) so that display names are neither retrieved from database nor sent in responses. A value like `members.display` does the job. Note the query parameter attributes can also be used for this purpose, for example with a value like `members.value` that will output only members' identifiers and ignore other non-required attributes.

This approach is particularly useful in search and retrievals when users' display names are not needed.

The second is a stronger approach that turns off validation of incoming members data: if the usage of a POST/PUT/PATCH operation implies adding members, their existence is not verified, they will simply get added. Here, the client application is responsible for sending accurate data. To use this approach add a query or header parameter named `Group-Overhead-Bypass` with any value. Note under this mode of operation:

* Display names are never returned regardless of `attributes` or `excludedAttributes` parameters values
* Remove/replace patch operations that involve display names in path filters are ignored, eg: `"path": "members[value eq \"2819c223\" or display eq \"Joe\"]"`


## User Registration Process with SCIM
SCIM service has many use cases. One interesting and often arising is that of coding your own user registration process. With your SCIM endpoints you can build a custom application to maintain user entries in your database.


### Important Considerations
Here, you have some useful tips before you start:

1. Choose a toolset you feel comfortable to work with. Keep in mind that you have to leverage the capabilities of your language/framework to issue complex HTTPS requests. Be sure that:

      * You will be able to use at least the following verbs: GET, POST, PUT, and DELETE

      * You can send headers in your requests as well as reading them from the service response

2. If not supported natively, choose a library to facilitate JSON content manipulation. As you have already noticed we have been dealing with JSON for requests as well as for responses. Experience shows that being able to map from objects (or data structures) of your language to Json and viceversa helps saving hours of coding.

3. Shape your data model early. List the attributes your application will operate upon and correlate with those found in the SCIM user schema. You can learn about the schema in [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644). At least, take a look at the JSON-formatted schema that your Jans Server shows: visit `https://<host-name>/jans-scim/restv1/v2/Schemas/urn:ietf:params:scim:schemas:core:2.0:User`

4. You will have to manipulate database contents very often as you develop and run tests, thus, find a suitable tool for the task. In the case of LDAP, a TUI client is a good choice.

5. Always check your [logs](#where-to-locate-scim-related-logs).

6. In this user management guide with SCIM, we have already touched upon the fundamentals of SCIM in Jans Server and shown a good amount of sample requests for manipulation of user information. However, keep in mind the SCIM spec documents are definitely the key reference to build working request messages, specially [RFC 7643](https://datatracker.ietf.org/doc/html/rfc7643), and [RFC 7644](https://datatracker.ietf.org/doc/html/rfc7644).

