---
tags:
  - administration
  - scim
  - bulk-users
---

## Bulk Users

This functionality enables clients to send a potentially large collection of resource operations in a single request.

!!! Note
Operation of multiple resources can be clubbed in one BulkRequest, like for user and group.
Individual BulkOperation can be of any of HTTP method - POST, PUT, PATCH, etc

## Bulk operation JSON example 
> ```javascript
>{
>  "schemas": [
>    "urn:ietf:params:scim:api:messages:2.0:BulkRequest"
>  ],
>  "Operations": [
>    {
>      "method": "POST",
>      "path": "/Users",
>      "bulkId": "qwerty",
>      "data": {
>        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
>        "userName": "scim_test_bjensen_1"
>      }
>    },
>   {
>     "method": "POST",
>     "path": "/Groups",
>     "bulkId": "ytrewq",
>     "data": {
>       "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
>       "displayName": "Tour Guides",
>       "members": [
>         {
>           "type": "User",
>           "value": "bulkId:qwerty"
>         }
>       ]
>     }
>   },
>    {
>      "method": "PUT",
>      "path": "/Users/bulkId:qwerty",
>      "data": {
>        "active": true,
>        "password": "top-secret",
>        "roles": [{ "value" : "Master of puppets" }]
>      }
>    },
>    {
>      "method": "PATCH",
>      "path": "/Users/bulkId:qwerty",
>      "data": {
>        "schemas": [
>          "urn:ietf:params:scim:api:messages:2.0:PatchOp"
>        ],
>        "Operations": [
>          {
>            "op": "add",
>            "value": {
>              "nickName": "Babas",
>              "userType": "CEO"
>            }
>          },
>          {
>            "op": "replace",
>            "value": {
>              "displayName": "patched Brava"
>            }
>          }
>        ]
>      }
>    },
>    {
>      "method": "PATCH",
>      "path": "/Users/bulkId:qwerty",
>      "data": {
>        "schemas": [
>          "urn:ietf:params:scim:api:messages:2.0:PatchOp"
>        ],
>        "Operations": [
>          {
>            "op": "replace",
>            "path": "name",
>            "value": {
>              "familyName": "re-patched Jensen",
>              "givenName": "re-patched Barbara",
>              "middleName": "re-patched Jane"
>            }
>          },
>          {
>            "op": "replace",
>            "path": "phoneNumbers",
>            "value": [
>              {
>                "value": "re-patch 555 123 4567",
>                "type": "other"
>              },
>              {
>                "value": "re-patch 666 000 1234",
>                "type": "work"
>              }
>            ]
>          },
>          {
>            "op": "remove",
>            "path": "name.middleName"
>          }
>        ]
>      }
>    }
>  ]
>}
>}
> ```


## Required scope

```text
https://jans.io/scim/bulk
```

## Bulk operation configuration
SCIM configuration has various attributes related to bulk operation. Following are related attributes and their default values.

> ```javascript
>    ...
>    "maxCount": 200,
>    "bulkMaxOperations": 30,
>    "bulkMaxPayloadSize": 3072000,
>    "userExtensionSchemaURI": "urn:ietf:params:scim:schemas:extension:gluu:2.0:User",
>    ...
> ```
