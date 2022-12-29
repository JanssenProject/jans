---
tags:
  - administration
  - config-api
  - attributes
---

# Attributes

Attributes are individual pieces of user data, like uid or email, that are required by applications in order to identify a user and grant access to protected resources. In OpenID Connect, these are called user claims.

------------------------------------------------------------------------------------------

## Listing existing attributes

<details>
 <summary><code>GET</code> <code><b>/</b></code> <code>(gets list of attributes based on search parameters)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                                                                     |
> |------------|-------------|----------------|-----------|--------------|---------------------------------------------------------------------------------|
> | limit      |  query      | integer        | optional  |50            |Search size - max size of the results to return                                  |
> | pattern    |  query      | string         | optional  |N/A           |Comma separated search patter. E.g. `pattern=edu`, `pattern=edu,locale,License`  |
> | status     |  query      | string         | optional  |all           |Search size - max size of the results to return                                  |
> | startIndex |  query      | integer        | optional  |1             |Index of the first query result                                                  |
> | sortBy     |  query      | string         | optional  |inum          |Field whose value will be used to order the returned response                |
> | sortOrder  |  query      | string         | optional  |ascending     |Search size - max size of the results to return                                  |


### Responses

> | http code     | content-type                      | response                                                            |
> |---------------|-----------------------------------|---------------------------------------------------------------------|
> | `200`         | `application/json`                | `Paginated result`                                                  |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                           |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                              |

### Example cURL

> ```javascript
>  curl -k -i -H "Accept: application/json" -H "Content-Type: application/json" -H "Authorization:Bearer 697479e0-e6f4-453d-bf7a-ddf31b53efba" -X GET http://my.jans.server/jans-config-api/api/v1/attributes?limit=5&pattern=edu,locale,carLicense&startIndex=1
> ```

### Sample Response
> ```javascript
>  {
>    "start": 0,
>    "totalEntriesCount": 78,
>    "entriesCount": 2,
>    "entries": [
>        {
>            "dn": "inum=08E2,ou=attributes,o=jans",
>            "selected": false,
>            "inum": "08E2",
>            "name": "departmentNumber",
>            "displayName": "Department",
>            "description": "Organizational Department",
>            "origin": "jansCustomPerson",
>            "dataType": "string",
>            "editType": [
>                "admin"
>            ],
>            "viewType": [
>                "user",
>                "admin"
>            ],
>            "claimName": "department_number",
>            "status": "inactive",
>            "saml1Uri": "urn:mace:dir:attribute-def:departmentNumber",
>            "saml2Uri": "urn:oid:2.16.840.1.113730.3.1.2",
>            "urn": "urn:mace:dir:attribute-def:departmentNumber",
>            "oxMultiValuedAttribute": false,
>            "custom": false,
>            "requred": false,
>            "whitePagesCanView": false,
>            "adminCanEdit": true,
>            "userCanView": true,
>            "userCanEdit": false,
>            "adminCanAccess": true,
>            "adminCanView": true,
>            "userCanAccess": true,
>            "baseDn": "inum=08E2,ou=attributes,o=jans"
>        },
>        {
>            "dn": "inum=0C18,ou=attributes,o=jans",
>            "selected": false,
>            "inum": "0C18",
>            "name": "telephoneNumber",
>            "displayName": "Home Telephone Number",
>            "description": "Home Telephone Number",
>            "origin": "jansCustomPerson",
>            "dataType": "string",
>            "editType": [
>                "user",
>                "admin"
>            ],
>            "viewType": [
>                "user",
>                "admin"
>            ],
>            "claimName": "phone_number",
>            "status": "inactive",
>            "saml1Uri": "urn:mace:dir:attribute-def:telephoneNumber",
>            "saml2Uri": "urn:oid:2.5.4.20",
>            "urn": "urn:mace:dir:attribute-def:phone_number",
>            "oxMultiValuedAttribute": false,
>            "custom": false,
>            "requred": false,
>            "whitePagesCanView": false,
>            "adminCanEdit": true,
>            "userCanView": true,
>            "userCanEdit": true,
>            "adminCanAccess": true,
>            "adminCanView": true,
>            "userCanAccess": true,
>            "baseDn": "inum=0C18,ou=attributes,o=jans"
>        }
>  }
> ```

</details>

<details>
  <summary><code>GET</code> <code><b>/{inum}</b></code> <code>(gets attribute based on inum)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                            |
> |------------|-------------|----------------|-----------|--------------|----------------------------------------|
> | `inum`     |  path       | string         | required  | NA           | Attribute unique idendifier            |

### Responses

> | http code     | content-type                      | response                                                            |
> |---------------|-----------------------------------|---------------------------------------------------------------------|
> | `200`         | `application/json        `        | `attribute details`                                                 |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                           |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                              |

### Example cURL

> ```javascript
>  curl -k -i -H "Accept: application/json" -H "Content-Type: application/json" -H "Authorization:Bearer 697479e0-e6f4-453d-bf7a-ddf31b53efba" -X GET http://my.jans.server/jans-config-api/api/v1/attributes/08E2
> ```

### Sample Response

> ```javascript
>  {
>    "dn": "inum=08E2,ou=attributes,o=jans",
>    "selected": false,
>    "inum": "08E2",
>    "name": "departmentNumber",
>    "displayName": "Department",
>    "description": "Organizational Department",
>    "origin": "jansCustomPerson",
>    "dataType": "string",
>    "editType": [
>        "admin"
>    ],
>    "viewType": [
>        "user",
>        "admin"
>    ],
>    "claimName": "department_number",
>    "status": "inactive",
>    "saml1Uri": "urn:mace:dir:attribute-def:departmentNumber",
>    "saml2Uri": "urn:oid:2.16.840.1.113730.3.1.2",
>    "urn": "urn:mace:dir:attribute-def:departmentNumber",
>    "oxMultiValuedAttribute": false,
>    "custom": false,
>    "requred": false,
>    "whitePagesCanView": false,
>    "adminCanEdit": true,
>    "userCanView": true,
>    "userCanEdit": false,
>    "adminCanAccess": true,
>    "adminCanView": true,
>    "userCanAccess": true,
>    "baseDn": "inum=08E2,ou=attributes,o=jans"
>  }
> ```

</details>

------------------------------------------------------------------------------------------

## Creating new attribute

<details>
  <summary><code>POST</code> <code><b>/{inum}</b></code> <code>(creates a new attribute)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                            |
> |------------|-------------|----------------|-----------|--------------|----------------------------------------|
> | None       |  request    | object (JSON)  | required  | NA           | Attribute json                         |

### Responses

> | http code     | content-type                      | response                                                            |
> |---------------|-----------------------------------|---------------------------------------------------------------------|
> | `201`         | `application/json        `        | `attribute details json`                                                 |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                           |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                              |

### Example cURL

> ```javascript
>  curl -X POST -k -H 'Content-Type: application/json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/attributes' --data @post.json
> ```

### Sample Request

> ```javascript
> {
>    "adminCanAccess": true,
>    "adminCanEdit": true,
>    "adminCanView": true,
>    "custom": false,
>    "dataType": "string",
>    "description": "QAAdded Attribute",
>    "displayName": "QAAdded Attribute",
>    "editType": [
>        "admin",
>        "user"
>    ],
>    "name": "qaattribute",
>    "origin": "jansPerson",
>    "jansMultivaluedAttr": false,
>    "requred": false,
>    "status": "active",
>    "urn": "urn:mace:dir:attribute-def:qaattribute",
>    "userCanAccess": true,
>    "userCanEdit": true,
>    "userCanView": true,
>    "viewType": [
>        "admin",
>        "user"
>    ],
>    "whitePagesCanView": false
> }
> ```


</details>

------------------------------------------------------------------------------------------

## Updating existing attribute

<details>
  <summary><code>PUT</code> <code><b>/{inum}</b></code> <code>(updates an existing attribute)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                            |
> |------------|-------------|----------------|-----------|--------------|----------------------------------------|
> | None       |  request    | object (JSON)  | required  | NA           | Attribute json                         |

### Responses

> | http code     | content-type                      | response                                                               |
> |---------------|-----------------------------------|------------------------------------------------------------------------|
> | `200`         | `application/json        `        | `attribute details`                                                    |
> | `404`         | `application/json`                | `{"code":"404","message":"The requested <inum> doesn't exist"}`        |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                              |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                                 |

### Example cURL

> ```javascript
>  curl -X PUT -k -H 'Content-Type: application/json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/attributes' --data @put.json
> ```

### Sample Request

> ```javascript
> {
>    "dn": "inum=08E2,ou=attributes,o=jans",
>    "selected": false,
>    "inum": "08E2",
>    "name": "departmentNumber",
>    "displayName": "Department",
>    "description": "Organizational Department",
>    "origin": "jansCustomPerson",
>    "dataType": "string",
>    "editType": [
>        "admin"
>    ],
>    "viewType": [
>        "user",
>        "admin"
>    ],
>    "claimName": "department_number",
>    "status": "inactive",
>    "saml1Uri": "urn:mace:dir:attribute-def:departmentNumber",
>    "saml2Uri": "urn:oid:2.16.840.1.113730.3.1.2",
>    "urn": "urn:mace:dir:attribute-def:departmentNumber",
>    "oxMultiValuedAttribute": false,
>    "custom": false,
>    "requred": false,
>    "whitePagesCanView": false,
>    "adminCanEdit": true,
>    "userCanView": true,
>    "userCanEdit": false,
>    "adminCanAccess": true,
>    "adminCanView": true,
>    "userCanAccess": true,
>    "baseDn": "inum=08E2,ou=attributes,o=jans"
> }
> ```

</details>

------------------------------------------------------------------------------------------

## Patching existing attribute

<details>
  <summary><code>PATCH</code> <code><b>/{inum}</b></code> <code>(patches an existing attribute)</code></summary>

### Parameters

> | name       |  param type | data type          | type      |default value | description                            |
> |------------|-------------|--------------------|-----------|--------------|----------------------------------------|
> | inum       |  path       | string             | required  | NA           | Attribute unique idendifier            |
> | None       |  request    | json-patch object  | required  | NA           | json-patch request                     |


### Responses

> | http code     | content-type                      | response                                                               |
> |---------------|-----------------------------------|------------------------------------------------------------------------|
> | `200`         | `application/json        `        | `attribute details`                                                    |
> | `404`         | `application/json`                | `{"code":"404","message":"The requested <inum> doesn't exist"}`        |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                              |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                                 |

### Example cURL

> ```javascript
>  curl -X PATCH -k -H 'Content-Type: application/json-patch+json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/attributes/08E2' --data @patch.json
> ```

### Sample Request

> ```javascript
> [ {"op":"replace", "path":"/displayName", "value": "PatchCustomAttribute123" } ]
> ```

</details>

------------------------------------------------------------------------------------------

## Deleting existing attribute

<details>
  <summary><code>DELETE</code> <code><b>/{inum}</b></code> <code>(deletes an existing attribute)</code></summary>

### Parameters

> | name       |  param type | data type          | type      |default value | description                            |
> |------------|-------------|--------------------|-----------|--------------|----------------------------------------|
> | inum       |  path       | string             | required  | NA           | Attribute unique idendifier            |


### Responses

> | http code     | content-type                      | response                                                               |
> |---------------|-----------------------------------|------------------------------------------------------------------------|
> | `204`         | `application/json        `        | `No Content`                                                    |
> | `404`         | `application/json`                | `{"code":"404","message":"The requested <inum> doesn't exist"}`        |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                              |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                                 |

### Example cURL

> ```javascript
>  curl -X DELETE -k -H 'Content-Type: application/json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/attributes/08E2'
> ```

### Sample Request
> None

</details>
------------------------------------------------------------------------------------------
