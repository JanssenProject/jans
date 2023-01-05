---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

# UMA Resources

> Prerequisite: Know how to use the Janssen CLI in [command-line mode](cli-index.md)

Let's get the information for OAuthUMAResources:

```
/opt/jans/jans-cli/config-cli.py --info OAuthUMAResources

Operation ID: get-oauth-uma-resources
  Description: Gets list of UMA resources.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
Operation ID: post-oauth-uma-resources
  Description: Creates an UMA resource.
  Schema: /components/schemas/UmaResource
Operation ID: put-oauth-uma-resources
  Description: Updates an UMA resource.
  Schema: /components/schemas/UmaResource
Operation ID: get-oauth-uma-resources-by-id
  Description: Gets an UMA resource by ID.
  url-suffix: id
Operation ID: delete-oauth-uma-resources-by-id
  Description: Deletes an UMA resource.
  url-suffix: id
Operation ID: patch-oauth-uma-resources-by-id
  Description: Partially updates an UMA resource by Inum.
  url-suffix: id
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```

Table of Contents
=================

* [UMA Resources](#uma-resources)
  * [Get List of UMA Resources](#get-list-of-uma-resources)
  * [Get Oauth UMA Resource by ID](#get-oauth-uma-resource-by-id)
  * [Patch OAuth UMA Resource by ID](#patch-oauth-uma-resource-by-id)


## Get List of UMA Resources

This operation is used to search UMA Resources.

```text
Operation ID: get-oauth-uma-resources
  Description: Gets list of UMA resources.
  Parameters:
  limit: Search size - max size of the results to return. [integer]
  pattern: Search pattern. [string]
```

To get a list of UMA resources:
`/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-uma-resources --endpoint-args limit:5`

It will return random 5 UMA resources.
```text
Getting access token for scope https://jans.io/oauth/config/uma/resources.readonly
Calling with params limit=5
[
  {
    "dn": "jansId=1800.1ed09ec8-5918-4cb5-9123-e4b9df36231f,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.1ed09ec8-5918-4cb5-9123-e4b9df36231f",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-0B30,ou=scopes,o=jans",
      "inum=CACA-BFDB,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.78e4c317-4d5a-4f23-b767-e63793364bee,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.78e4c317-4d5a-4f23-b767-e63793364bee",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/acrs",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-D906,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/acrs"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.556448d7-b349-45d8-a3a8-0d163df8753c,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.556448d7-b349-45d8-a3a8-0d163df8753c",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-4525,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/config/cache"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-0B30,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.049d1198-a911-4032-aaf6-59cc94d3f4ef,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.049d1198-a911-4032-aaf6-59cc94d3f4ef",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/acrs",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": [
      "inum=CACA-D906,ou=scopes,o=jans",
      "inum=CACA-698C,ou=scopes,o=jans"
    ],
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/acrs"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  }
]
```

To search using multiple arguments, you can change pattern that you want to find:

```text
 /opt/jans/jans-cli/config-cli.py --operation-id get-oauth-uma-resources --endpoint-args limit:1,pattern:"Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache/native-persistence"

Getting access token for scope https://jans.io/oauth/config/uma/resources.readonly
Calling with params limit=1&pattern=Jans+Cofig+Api+Uma+Resource+%2Fjans-config-api%2Fapi%2Fv1%2Fconfig%2Fcache%2Fnative-persistence
[
  {
    "dn": "jansId=1800.02d24ac8-13d6-464d-af1d-46a6261eaa65,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.02d24ac8-13d6-464d-af1d-46a6261eaa65",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache/native-persistence",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": null,
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/config/cache/native-persistence"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  },
  {
    "dn": "jansId=1800.6cd9bb98-05ac-43d2-bebb-3b1c92b9b409,ou=resources,ou=uma,o=jans",
    "inum": null,
    "id": "1800.6cd9bb98-05ac-43d2-bebb-3b1c92b9b409",
    "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/config/cache/native-persistence",
    "iconUri": "http://www.jans.io/img/scim_logo.png",
    "scopes": null,
    "scopeExpression": null,
    "clients": [
      "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
    ],
    "resources": [
      "https://testjans.gluu.com/jans-config-api/api/v1/config/cache/native-persistence"
    ],
    "rev": "1",
    "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
    "description": null,
    "type": null,
    "creationDate": null,
    "expirationDate": null,
    "deletable": true
  }
]
```

## Get Oauth UMA Resource by ID

```text
Operation ID: get-oauth-uma-resources-by-id
  Description: Gets an UMA resource by ID.
  url-suffix: id
```

To get uma resource by its ID, run the following command:
```text
/opt/jans/jans-cli/config-cli.py --operation-id get-oauth-uma-resources-by-id --url-suffix id:1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da


Getting access token for scope https://jans.io/oauth/config/uma/resources.readonly
{
  "dn": "jansId=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da,ou=resources,ou=uma,o=jans",
  "inum": null,
  "id": "1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da",
  "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
  "iconUri": "http://www.jans.io/img/scim_logo.png",
  "scopes": [
    "inum=CACA-0B30,ou=scopes,o=jans"
  ],
  "scopeExpression": null,
  "clients": [
    "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
  ],
  "resources": [
    "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
  ],
  "rev": "1",
  "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
  "description": null,
  "type": null,
  "creationDate": null,
  "expirationDate": null,
  "deletable": true
}
```
replace the id with accurate one.

## Patch OAuth UMA Resource by ID

```text
Operation ID: patch-oauth-uma-resources-by-id
  Description: Partially updates an UMA resource by Inum.
  url-suffix: id
  Schema: Array of /components/schemas/PatchRequest
```

As you see the description, you can update an existing uma resource partially with this following operation.

Let's get the sample schema:
```text
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > /tmp/patch-uma.json

{
  "op": "move",
  "path": "string",
  "value": {}
}
```
Let's want to update as `deletable:false` to an uma resource whose `id=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da`.
So we are going to operate `replace` where `path` is `deletable` with `value: false`.
let's update the json as below:

```json
nano /tmp/patch-uma.json

[
  {
    "op": "replace",
    "path": "deletable",
    "value": false
  }
]
```

now let's do the operation:

```json
/opt/jans/jans-cli/config-cli.py --operation-id patch-oauth-uma-resources-by-id --url-suffix id:1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da --data /tmp/patch-uma.json

        

Getting access token for scope https://jans.io/oauth/config/uma/resources.write
Server Response:
{
  "dn": "jansId=1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da,ou=resources,ou=uma,o=jans",
  "inum": null,
  "id": "1800.c4e0d1b6-e731-4c8d-a0ab-66784349a4da",
  "name": "Jans Cofig Api Uma Resource /jans-config-api/api/v1/attributes",
  "iconUri": "http://www.jans.io/img/scim_logo.png",
  "scopes": [
    "inum=CACA-0B30,ou=scopes,o=jans"
  ],
  "scopeExpression": null,
  "clients": [
    "inum=1801.6e8351d4-5d3f-4773-b632-6c84bf8207cd,ou=clients,o=jans"
  ],
  "resources": [
    "https://testjans.gluu.com/jans-config-api/api/v1/attributes"
  ],
  "rev": "1",
  "creator": "inum=d206dd87-3a22-465d-bd25-bec9313cd42d,ou=people,o=json",
  "description": null,
  "type": null,
  "creationDate": null,
  "expirationDate": null,
  "deletable": false
}
```
you must see that `deletable` updated to `false`.

