---
tags:
  - administration
  - configuration
  - attributes
---

# Attribute

First thing, let's get the information for `Attribute`:
```shell
/opt/jans/jans-cli/config-cli.py --info Attribute
```
In return, we get a list of Operations ID as below:

```text
Operation ID: get-attributes
  Description: Gets a list of Gluu attributes.
  Parameters:
  limit: Search size - max size of the results to return [integer]
  pattern: Search pattern [string]
  status: Status of the attribute [string]
  startIndex: The 1-based index of the first query result [integer]
  sortBy: Attribute whose value will be used to order the returned response [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
  fieldValuePair: Field and value pair for seraching [string]
Operation ID: put-attributes
  Description: Updates an existing attribute
  Schema: JansAttribute
Operation ID: post-attributes
  Description: Adds a new attribute
  Schema: JansAttribute
Operation ID: get-attributes-by-inum
  Description: Gets an attribute based on inum
  Parameters:
  inum: Attribute Id [string]
Operation ID: delete-attributes-by-inum
  Description: Deletes an attribute based on inum
  Parameters:
  inum: Attribute Id [string]
Operation ID: patch-attributes-by-inum
  Description: Partially modify a JansAttribute
  Parameters:
  inum: Attribute Id [string]
  Schema: Array of PatchRequest

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema PatchRequest
```

We have discussed here about each of this operations ID with few examples to understand how these really works.

Table of Contents
=================

* [Attribute](#attribute)
  * [Get Attributes](#get-attributes)
  * [Creating an Attribute](#creating-an-attribute)
  * [Updating an Attribute](#updating-an-attribute)
  * [Get Attribute by inum](#get-attribute-by-inum)
  * [Delete Attributes](#delete-attributes)
  * [Patch Attributes](#patch-attributes)

## Get Attributes

> Prerequisite: Know how to use the Janssen CLI in [command-line mode](config-tools/jans-cli/README.md)

As we know, Attributes are individual pieces of user data, like `uid` or `email`, that are required by applications in order to identify a user and grant access to protect resources. The user attributes that are available in your Janssen Server can be found by using this operation-ID. If we look at the description below:

```text
Operation ID: get-attributes
  Description: Gets a list of Gluu attributes.
  Parameters:
  limit: Search size - max size of the results to return [integer]
  pattern: Search pattern [string]
  status: Status of the attribute [string]
  startIndex: The 1-based index of the first query result [integer]
  sortBy: Attribute whose value will be used to order the returned response [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
  fieldValuePair: Field and value pair for seraching [string]
```

To get all the attributes without any arguments, run the following command:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes
```

To get attributes with passing the arguments, let's retrieve randomly limit:5:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes --endpoint-args limit:1
```

It will return only one attribute details randomly:
```text
Getting access token for scope https://jans.io/oauth/config/attributes.readonly
Calling with params limit=1
{
  "start": 0,
  "totalEntriesCount": 71,
  "entriesCount": 1,
  "entries": [
    {
      "dn": "inum=29DA,ou=attributes,o=jans",
      "selected": false,
      "inum": "29DA",
      "name": "inum",
      "displayName": "Inum",
      "description": "XRI i-number, persistent non-reassignable identifier",
      "origin": "jansPerson",
      "dataType": "string",
      "editType": [
        "admin"
      ],
      "viewType": [
        "user",
        "admin"
      ],
      "claimName": "inum",
      "status": "active",
      "saml1Uri": "urn:mace:dir:attribute-def:inum",
      "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.117",
      "urn": "urn:jans:dir:attribute-def:inum",
      "oxMultiValuedAttribute": false,
      "custom": false,
      "adminCanAccess": true,
      "adminCanView": true,
      "adminCanEdit": true,
      "userCanAccess": true,
      "userCanView": true,
      "userCanEdit": false,
      "whitePagesCanView": false,
      "baseDn": "inum=29DA,ou=attributes,o=jans"
    }
  ]
}

```

To get attributes with `pattern & status`:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes --endpoint-args limit:3,pattern:profile,status:ACTIVE
```
In return, we get a list of attribute that are matched with the given `pattern` and `status`:

```text
Please wait while retrieving data ...
{
  "start": 0,
  "totalEntriesCount": 2,
  "entriesCount": 2,
  "entries": [
    {
      "dn": "inum=64A0,ou=attributes,o=jans",
      "selected": false,
      "inum": "64A0",
      "name": "profile",
      "displayName": "Profile URL",
      "description": "URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.",
      "origin": "jansPerson",
      "dataType": "string",
      "editType": [
        "user",
        "admin"
      ],
      "viewType": [
        "user",
        "admin"
      ],
      "claimName": "profile",
      "status": "active",
      "saml1Uri": "urn:mace:dir:attribute-def:profile",
      "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.321",
      "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/profile",
      "oxMultiValuedAttribute": false,
      "custom": false,
      "adminCanAccess": true,
      "adminCanView": true,
      "adminCanEdit": true,
      "userCanAccess": true,
      "userCanView": true,
      "userCanEdit": true,
      "whitePagesCanView": false,
      "baseDn": "inum=64A0,ou=attributes,o=jans"
    },
    {
      "dn": "inum=EC3A,ou=attributes,o=jans",
      "selected": false,
      "inum": "EC3A",
      "name": "picture",
      "displayName": "Picture URL",
      "description": "URL of the End-User's profile picture",
      "origin": "jansPerson",
      "dataType": "string",
      "editType": [
        "user",
        "admin"
      ],
      "viewType": [
        "user",
        "admin"
      ],
      "claimName": "picture",
      "status": "active",
      "saml1Uri": "urn:mace:dir:attribute-def:picture",
      "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.322",
      "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/picture",
      "oxMultiValuedAttribute": false,
      "custom": false,
      "adminCanAccess": true,
      "adminCanView": true,
      "adminCanEdit": true,
      "userCanAccess": true,
      "userCanView": true,
      "userCanEdit": true,
      "whitePagesCanView": false,
      "baseDn": "inum=EC3A,ou=attributes,o=jans"
    }
  ]
}
```

## Creating an Attribute

To create SSO for certain applications, you may need to add custom attributes to your Janssen Server. Custom attributes can be added by using this operation-ID. It has a schema file where it's defined: the properties it needs to be filled to create a new custom attribute.

```text
Operation ID: post-attributes
  Description: Adds a new attribute
  Schema: JansAttribute
```
Before adding a new attribute, let's get sample `schema`:
```commandline
/opt/jans/jans-cli/config-cli.py --schema JansAttribute > /tmp/attribute.json
```  
It will return as below:

```json
{
  "dn": "string",
  "selected": false,
  "inum": "string",
  "sourceAttribute": "string",
  "nameIdType": "string",
  "name": "string",
  "displayName": "string",
  "description": "string",
  "origin": "string",
  "dataType": "certificate",
  "editType": [
    "manager"
  ],
  "viewType": [
    "user"
  ],
  "usageType": [
    "openid"
  ],
  "claimName": "string",
  "seeAlso": "string",
  "status": "inactive",
  "saml1Uri": "string",
  "saml2Uri": "string",
  "urn": "string",
  "scimCustomAttr": true,
  "oxMultiValuedAttribute": true,
  "jansHideOnDiscovery": true,
  "custom": false,
  "attributeValidation": {
    "minLength": {
      "type": "integer",
      "format": "int32"
    },
    "maxLength": {
      "type": "integer",
      "format": "int32"
    },
    "regexp": {
      "type": "string"
    }
  },
  "tooltip": "string",
  "whitePagesCanView": false,
  "adminCanView": true,
  "userCanAccess": false,
  "userCanView": true,
  "adminCanAccess": false,
  "adminCanEdit": false,
  "userCanEdit": true,
  "baseDn": "string"
}
```
Modify it to update attribute `name`, `display name`, `view type`:
```text
nano /tmp/attribute.json
```

![post-attribute.png](../../assets/image-cl-post-attribute-03042021.png)

Now, let's add this attribute using `post-attributes`:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id post-attributes --data /tmp/attribute.json
```
It will create a new attribute into the Attribute list with updated `inum & dn`:

```json
{
  "dn": "inum=0272b98e-0ead-43e9-94eb-4af9548af97d,ou=attributes,o=jans",
  "selected": false,
  "inum": "0272b98e-0ead-43e9-94eb-4af9548af97d",
  "nameIdType": "string",
  "name": "testAttribute",
  "displayName": "testAttribute",
  "description": "testAttribute",
  "dataType": "certificate",
  "editType": [
    "manager"
  ],
  "viewType": [
    "user"
  ],
  "usageType": [
    "openid"
  ],
  "status": "inactive",
  "scimCustomAttr": true,
  "oxMultiValuedAttribute": true,
  "jansHideOnDiscovery": true,
  "custom": false,
  "tooltip": "string",
  "adminCanAccess": false,
  "adminCanView": false,
  "adminCanEdit": false,
  "userCanAccess": true,
  "userCanView": true,
  "userCanEdit": false,
  "whitePagesCanView": false,
  "baseDn": "inum=0272b98e-0ead-43e9-94eb-4af9548af97d,ou=attributes,o=jans"
}
```

## Updating an Attribute

This operation-id can be used to update an existing attribute information. The Janssen Server administrator can make changes to attributes, such as changing their status to `active/inactive` by using this operation-ID. Let's look at the schema:

```
/opt/jans/jans-cli/config-cli.py --schema JansAttribute > /tmp/attrib.json
```

You must see the similar schema while performed in `post-attributes` operation.

To update an existing attribute, we have to ensure following properties in the schema file.

In our case, I have modified the schema file as below:

```
{
  "dn": "inum=b691f2ab-a7db-4725-b85b-9961575b441f,ou=attributes,o=jans",
  "inum": "b691f2ab-a7db-4725-b85b-9961575b441f",
  "selected": true,
  "name": "testAttribute",
  "display_name": "testAttribute",
  "description": "testing put-attribute",
  "data_type": "STRING",
  "status": "ACTIVE",
  "edit_type": ["ADMIN", "OWNER"],
  "view_type": ["ADMIN", "OWNER", "USER"]
}
```

Now if we run the below command line:

```
/opt/jans/jans-cli/config-cli.py --operation-id put-attributes --data /tmp/attrb.json
```

```
Getting access token for scope https://jans.io/oauth/config/attributes.write
Server Response:
{
  "dn": "inum=b691f2ab-a7db-4725-b85b-9961575b441f,ou=attributes,o=jans",
  "inum": "b691f2ab-a7db-4725-b85b-9961575b441f",
  "selected": false,
  "name": "testAttribute",
  "displayName": "testAttribute",
  "description": "testing put-attribute",
  "dataType": "STRING",
  "status": "ACTIVE",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": null,
  "editType": [
    "ADMIN",
    "OWNER"
  ],
  "viewType": [
    "ADMIN",
    "OWNER",
    "USER"
  ],
  "usageType": null,
  "claimName": null,
  "seeAlso": null,
  "saml1Uri": null,
  "saml2Uri": null,
  "urn": null,
  "scimCustomAttr": null,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "attributeValidation": null,
  "tooltip": null,
  "jansHideOnDiscovery": null
}
```

It just replace the previous value with new one. 

## Get Attribute by `inum`

As we know, There are a lot of attributes available in the Janssen Server including custom attributes as well. You may want to know details information for a single attribute uniquely identified by `inum`.
Getting an attribute information by using its `inum` is pretty simple.

```
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes-by-inum --url-suffix inum:attribute-iunm-value
```

It will show all details information of the selected Attribute as below example.

```
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes-by-inum --url-suffix inum:b691f2ab-a7db-4725-b85b-9961575b441f

Getting access token for scope https://jans.io/oauth/config/attributes.readonly
{
  "dn": "inum=b691f2ab-a7db-4725-b85b-9961575b441f,ou=attributes,o=jans",
  "inum": "b691f2ab-a7db-4725-b85b-9961575b441f",
  "selected": false,
  "name": "testAttribute",
  "displayName": "testAttribute",
  "description": "testing put-attribute",
  "dataType": "STRING",
  "status": "ACTIVE",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": null,
  "editType": [
    "ADMIN",
    "OWNER"
  ],
  "viewType": [
    "ADMIN",
    "OWNER",
    "USER"
  ],
  "usageType": null,
  "claimName": null,
  "seeAlso": null,
  "saml1Uri": null,
  "saml2Uri": null,
  "urn": null,
  "scimCustomAttr": null,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "attributeValidation": null,
  "tooltip": null,
  "jansHideOnDiscovery": null
}
```

## Delete Attributes

For any reason, If it needs to delete any attribute, you can do that simply using its `inum` value. See below example, just change the `inum` value with one that you want to delete.

```
/opt/jans/jans-cli/config-cli.py --operation-id delete-attributes-by-inum --url-suffix inum:b691f2ab-a7db-4725-b85b-9961575b441f
```

## Patch Attributes

This operation can also used for updating an existing attribute by using its `inum` value.

```
Operation ID: patch-attributes-by-inum
  Description: Partially modify a JansAttribute.
  url-suffix: inum
  Schema: Array of PatchRequest
```

If we look at the description, we see that there is a schema file. Let's get the schema file with below command:

```
/opt/jans/jans-cli/config-cli.py --schema PatchRequest > /tmp/patch.json
```

```
# cat /tmp/patch.json

{
  "op": "add",
  "path": "string",
  "value": {}
}
```

Let's modify this schema file to change the status of an attribute as below:

![](../../assets/image-cl-attribute-patch-03042021.png)

In the above image, added two tasks. To know more about how we can modify this schema file to perform a specific task, follow this link: [patch-request-schema](config-tools/jans-cli/README.md#patch-request-schema)

Let's update an attribute by its `inum` value. In our case, `inum`: 6EEB. Before patching the selected attribute, you can check its properties using [get-attributes-by-inum](#get-attribute-by-inum) operation.

Before patching the attribute, its properties are:

```
{
  "dn": "inum=6EEB,ou=attributes,o=jans",
  "inum": "6EEB",
  "selected": false,
  "name": "l",
  "displayName": "City",
  "description": "City",
  "dataType": "STRING",
  "status": "INACTIVE",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": "jansCustomPerson",
  "editType": [
    "USER",
    "ADMIN"
  ],
  "viewType": [
    "USER",
    "ADMIN"
  ],
  "usageType": null,
  "claimName": "locality",
  "seeAlso": null,
  "saml1Uri": "urn:mace:dir:attribute-def:l",
  "saml2Uri": "urn:oid:2.5.4.7",
  "urn": "urn:mace:dir:attribute-def:l",
  "scimCustomAttr": null,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "attributeValidation": null,
  "tooltip": null,
  "jansHideOnDiscovery": null
}
```
According to the schema file, There should be two changes, `status` and `jansHideOnDiscovery`. Let's perform the operation:

```
/opt/jans/jans-cli/config-cli.py --operation-id patch-attributes-by-inum --url-suffix inum:6EEB --data /tmp/patch.json
```

The updated attribute looks like:

```
Getting access token for scope https://jans.io/oauth/config/attributes.write
Server Response:
{
  "dn": "inum=6EEB,ou=attributes,o=jans",
  "inum": "6EEB",
  "selected": false,
  "name": "l",
  "displayName": "City",
  "description": "City",
  "dataType": "STRING",
  "status": "ACTIVE",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": "jansCustomPerson",
  "editType": [
    "USER",
    "ADMIN"
  ],
  "viewType": [
    "USER",
    "ADMIN"
  ],
  "usageType": null,
  "claimName": "locality",
  "seeAlso": null,
  "saml1Uri": "urn:mace:dir:attribute-def:l",
  "saml2Uri": "urn:oid:2.5.4.7",
  "urn": "urn:mace:dir:attribute-def:l",
  "scimCustomAttr": null,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "attributeValidation": null,
  "tooltip": null,
  "jansHideOnDiscovery": true
}
```

As you see, there are two changes.
