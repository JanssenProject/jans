---
tags:
  - administration
  - scim
  - bulk-users
---

# Bulk Adding Users via SCIM

## Adding user in bulk

SCIM supports a bulk operation allowing consumers of the service to send multiple resource operations in a single request. This means the body of a bulk operation may contain one or more operations belonging to any of the supported HTTP verbs, e.g., POST, PUT, DELETE, etc.

This feature is useful, for instance, to create several users at once as we'll see below.

## Endpoint

The bulk endpoint is accessible at `https://<your-server-mame>/jans-scim/restv1/v2/Bulk`. Requests must be sent with a bearer token having scope `https://jans.io/scim/bulk`

## Bulk payload

A bulk is structured as a JSON document with two required properties:

- _schemas_: A list with the single string value `urn:ietf:params:scim:api:messages:2.0:BulkRequest`
- _Operations_: A JSON array describing every operation to perform in the order provided

An operation is a JSON document with the following:

- _method_: The HTTP verb of the operation to perform
- _path_: The path of the actual endpoint to hit relative to the SCIM root. For instance, to create a user, it will be `/Users`
- _data_: The payload of the actual operation. This is required only for POST, PUT, or PATCH

### Payload example for adding users

```
{
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:BulkRequest"
  ],
  "Operations": [
    {
      "method": "POST",
      "path": "/Users",
      "data": {
        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
        "userName": "alanis"
      }
    },
    {
      "method": "POST",
      "path": "/Users",
      "data": {
        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
        "userName": "sheryl",
        "active": true,
        "password": "top-secret",
        "roles": [{ "value" : "Master of puppets" }]
      }
    },
    {
      "method": "POST",
      "path": "/Users",
      "data": {
        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
        "userName": "becca",
        "externalId":"becca_ponx_1234",
        "name":{
          "familyName": "Cadalzo",
          "givenName": "Rebecca"
        }
      }
    }
  ]
}
```

The above payload illustrates how to insert three users with different details each.

## Bulk response

The result of a bulk operation is likewise retrieved in JSON object format:

- _schemas_: A list with the single string value `urn:ietf:params:scim:api:messages:2.0:BulkResponse`
- _Operations_: A JSON array describing the response of every operation performed whether failed or successful

An operation is a JSON document with the following:

- _method_: The HTTP verb of the operation performed
- _location_: The resource endpoint URL. For example, in the case of user creation, this will be the URL where the user's details can be retrieved
- _status_: A string value reporting the HTTP response code obtained for the given operation
- _response_: A JSON object only present when the status does not belong to the 2XX (success) family. It contains details of the error that occurred

### Response example

The following contains a potential response obtained for the bulk request mentioned earlier:

```
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:BulkResponse"],
  "Operations": [
     {
        "method": "POST",
        "location": "https://acme.co/jans-scim/restv1/v2/Users/92b725cd",
        "status": "201"
     },
     {
        "method": "POST",
        "location": "https://acme.co/jans-scim/restv1/v2/Users/a27725cf",
        "status": "201"
     },
     {
        "method": "POST",
        "status": "409",        
        "response":{
           "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
           "scimType": "uniqueness"
           "detail": "User 'becca' already exists"
        }
     }       
  ]
}
```

## Bulk operation configuration

SCIM Jans server has a couple of configuration properties related to bulk operations:

- _bulkMaxOperations_: The maximum number of operations per bulk request. Default is 30
- _bulkMaxPayloadSize_:  The maximum payload size in bytes. Default value is 3072000

