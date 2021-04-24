# Attribute

First thing, let's get the information for `Attribute`:
```commandline
/opt/jans/jans-cli/config-cli.py --info Attribute
```
In return, we get:

```text
Operation ID: get-attributes
  Description: Gets all attributes. Optionally max-size of the result, attribute status and pattern can be provided.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
  status: Status of the attribute [string]
Operation ID: post-attributes
  Description: Adds a new attribute.
  Schema: /components/schemas/GluuAttribute
Operation ID: put-attributes
  Description: Updates an existing attribute.
  Schema: /components/schemas/GluuAttribute
Operation ID: get-attributes-by-inum
  Description: Gets an attribute based on inum.
  url-suffix: inum
Operation ID: delete-attributes-by-inum
  Description: Deletes an attribute based on inum.
  url-suffix: inum
Operation ID: patch-attributes-by-inum
  Description: Partially modify a GluuAttribute.
  url-suffix: inum
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```

- **get-attributes**

```text
Operation ID: get-attributes
  Description: Gets all attributes. Optionally max-size of the result, attribute status and pattern can be provided.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
  status: Status of the attribute [string]
```
Let's do some queries using `get-attributes` operation ID.

To get all the attributes without any arguments, run the following command:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes
```

To get attributes with passing the arguments, let's retrieve randomly limit:5:

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes --endpoint-args limit:5
```

It will return any 5 attributes randomly:
```text
Getting access token for scope https://jans.io/oauth/config/attributes.readonly
Calling with params limit=5
[
  {
    "dn": "inum=B4B0,ou=attributes,o=jans",
    "inum": "B4B0",
    "selected": false,
    "name": "givenName",
    "displayName": "First Name",
    "description": "Given name(s) or first name(s) of the End-User.Note that in some cultures, people can have multiple given names;all can be present, with the names being separated by space characters.",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "USER",
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "given_name",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:givenName",
    "saml2Uri": "urn:oid:2.5.4.42",
    "urn": "urn:mace:dir:attribute-def:givenName",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=29DA,ou=attributes,o=jans",
    "inum": "29DA",
    "selected": false,
    "name": "inum",
    "displayName": "Inum",
    "description": "XRI i-number, persistent non-reassignable identifier",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": null,
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "inum",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:inum",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.117",
    "urn": "urn:jans:dir:attribute-def:inum",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=B52A,ou=attributes,o=jans",
    "inum": "B52A",
    "selected": false,
    "name": "jansPrefUsrName",
    "displayName": "Preferred Username",
    "description": "A domain issued and managed identifier for the person.Subject - Identifier for the End-User at the Issuer.",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "preferred_username",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:preferredUsername",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.320",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/preferred_username",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=64A0,ou=attributes,o=jans",
    "inum": "64A0",
    "selected": false,
    "name": "profile",
    "displayName": "Profile URL",
    "description": "URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "USER",
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "profile",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:profile",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.321",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/profile",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=42E0,ou=attributes,o=jans",
    "inum": "42E0",
    "selected": false,
    "name": "uid",
    "displayName": "Username",
    "description": "A domain issued and managed identifier for the person.Subject - Identifier for the End-User at the Issuer.",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "user_name",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:uid",
    "saml2Uri": "urn:oid:0.9.2342.19200300.100.1.1",
    "urn": "urn:mace:dir:attribute-def:uid",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  }
]
```

To get attributes with `pattern & status`:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-attributes --endpoint-args limit:5,pattern:profile,status:ACTIVE
```
In return, we get:

```properties
Getting access token for scope https://jans.io/oauth/config/attributes.readonly
Calling with params limit=5&pattern=profile&status=ACTIVE
[
  {
    "dn": "inum=EC3A,ou=attributes,o=jans",
    "inum": "EC3A",
    "selected": false,
    "name": "picture",
    "displayName": "Picture URL",
    "description": "URL of the End-User's profile picture",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "USER",
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "picture",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:picture",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.322",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/picture",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  },
  {
    "dn": "inum=64A0,ou=attributes,o=jans",
    "inum": "64A0",
    "selected": false,
    "name": "profile",
    "displayName": "Profile URL",
    "description": "URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.",
    "dataType": "STRING",
    "status": "ACTIVE",
    "lifetime": null,
    "sourceAttribute": null,
    "salt": null,
    "nameIdType": null,
    "origin": "jansPerson",
    "editType": [
      "USER",
      "ADMIN"
    ],
    "viewType": [
      "USER",
      "ADMIN"
    ],
    "usageType": null,
    "claimName": "profile",
    "seeAlso": null,
    "saml1Uri": "urn:mace:dir:attribute-def:profile",
    "saml2Uri": "urn:oid:1.3.6.1.4.1.48710.1.3.321",
    "urn": "http://openid.net/specs/openid-connect-core-1_0.html/StandardClaims/profile",
    "scimCustomAttr": null,
    "oxMultiValuedAttribute": false,
    "custom": false,
    "requred": false,
    "attributeValidation": null,
    "tooltip": null
  }
]
```

- **post-attributes**

```text
Operation ID: post-attributes
  Description: Adds a new attribute.
  Schema: /components/schemas/GluuAttribute
```
Before adding a new attribute, let's get sample `schema`:
```commandline
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/GluuAttribute > /tmp/attribute.json
```  
It will return as below:

```text
{
  "dn": null,
  "inum": null,
  "selected": true,
  "name": "name, displayName, birthdate, email",
  "displayName": "string",
  "description": "string",
  "dataType": "STRING",
  "status": "REGISTER",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": null,
  "editType": [],
  "viewType": "array",
  "usageType": [],
  "claimName": null,
  "seeAlso": null,
  "saml1Uri": null,
  "saml2Uri": null,
  "urn": null,
  "scimCustomAttr": true,
  "oxMultiValuedAttribute": true,
  "custom": true,
  "requred": true,
  "attributeValidation": {
    "regexp": null,
    "minLength": null,
    "maxLength": null
  },
  "tooltip": null
}
```
Modify it to update attribute `name`, `display name`, `view type`:
```text
nano /tmp/attribute.json
```

![post-attribute.png](../img/cl-post-attribute.png)

Now, let's add this attribute using `post-attributes`:
```commandline
/opt/jans/jans-cli/config-cli.py --operation-id post-attributes --data /tmp/attribute.json
```
It will create a new attribute into the Attribute list with updated `inum & dn`:

```text
Getting access token for scope https://jans.io/oauth/config/attributes.write
Server Response:
{
  "dn": "inum=256135af-56eb-43f3-9583-d7e9fc75c672,ou=attributes,o=jans",
  "inum": "256135af-56eb-43f3-9583-d7e9fc75c672",
  "selected": false,
  "name": "testAttribute",
  "displayName": "test Attribute",
  "description": "testing post-attributes",
  "dataType": "CERTIFICATE",
  "status": "REGISTER",
  "lifetime": null,
  "sourceAttribute": null,
  "salt": null,
  "nameIdType": null,
  "origin": null,
  "editType": null,
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
  "scimCustomAttr": false,
  "oxMultiValuedAttribute": false,
  "custom": false,
  "requred": false,
  "attributeValidation": {
    "regexp": null,
    "minLength": null,
    "maxLength": null
  },
  "tooltip": null
}
```


