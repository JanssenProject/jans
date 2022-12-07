---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# User Resources

!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.
    
> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

This option can be used to perform such operations to modfiy user resources. If you select the first option from the SCIM Menu, You will get a list of sub-menu as same as below.

```

user
----
1 Query User resources (see section 3.4.2 of RFC 7644)
2 Allows creating a User resource via POST (see section 3.3 of RFC 7644)
3 Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
4 Updates a User resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion&amp;#58; every
attribute value found in the payload sent will replace the one in the existing resource representation. Attributes 
not passed in the payload will be left intact.

5 Deletes a user resource
6 Updates one or more attributes of a User resource using a sequence of additions, removals, and 
replacements operations. See section 3.5.2 of RFC 7644

7 Query User resources (see section 3.4.2 of RFC 7644)

Selection: 

```

## Query User Resources

Query User Resources presents all the user information and its attributes.  It supports query with filter by a list of attributes:

    1. **attributes**: Use comma (,) for multiple attributes
    2. **excludeAttributes**: Use comma (,) for multiple attributes
    3. **filter**: an attribute with value to return as same type of resources
    4. **startIndex**: an integer value indicate a starting position
    5. **count**: an integer value define the maximum search results
    6. **sortBy**: sort list of search results by an attribute
    7. **sortOrder**: ['ascending', 'descending']

A simple query where everything is skipped for default value. 

```

Query User resources (see section 3.4.2 of RFC 7644)
----------------------------------------------------

«A comma-separated list of attribute names to return in the response. Type: string»
attributes: 

«When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list). Type: string»
excludedAttributes: 

«An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644. Type: string»
filter: 

«The 1-based index of the first query result. Type: integer»
startIndex: 

«Specifies the desired maximum number of query results per page. Type: integer»
count: 1

«The attribute whose value will be used to order the returned responses. Type: string»
sortBy: 

«Order in which the sortBy param is applied. Allowed values are "ascending" and "descending". Type: string»
sortOrder: 
Calling Api with parameters: {'count': 1}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/users.read

  "Resources": [
    {
      "externalId": null,
      "userName": "admin",
      "name": {
        "familyName": "User",
        "givenName": "Admin",
        "middleName": "Admin",
        "honorificPrefix": null,
        "honorificSuffix": null,
        "formatted": "Admin Admin User"
      },
      "displayName": "Default Admin User",
      "nickName": "Admin",
      "profileUrl": null,
      "title": null,
      "userType": null,
      "preferredLanguage": null,
      "locale": null,
      "timezone": null,
      "active": true,
      "password": null,
      "emails": [
        {
          "value": "admin@testjans.gluu.com",
          "display": null,
          "type": null,
          "primary": false
        }
      ],
      "phoneNumbers": null,
      "ims": null,
      "photos": null,
      "addresses": null,
      "groups": [
        {
          "value": "60B7",
          "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7",
          "display": "Jannsen Manager Group",
          "type": "direct"
        }
      ],
      "entitlements": null,
      "roles": null,
      "x509Certificates": null,
      "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
      "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:User"
      ],
      "id": "18ca6089-42fb-410a-a5b5-c2631d75dc7d",
      "meta": {
        "resourceType": "User",
        "created": null,
        "lastModified": null,
        "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d"
      }
    }
  ],
  "schemas": [
    "urn:ietf:params:scim:api:messages:2.0:ListResponse"
  ],
  "totalResults": 1,
  "startIndex": 1,
  "itemsPerPage": 1
}

Selection: 

```

## Creating an User

With this option, An adminstration can create  user resources easily. To create an user, you need to provide user value for its attributes. List of attributes are given below:

    1. familyName
    2. givenName
    3. middleName
    4. honorificPrefix
    5. honorificSuffix
    6. displayName
    7. password
    8. Email

    
  Optional Fields:

      1 schemas
      2 id
      3 meta
      4 externalId
      5 nickName
      6 profileUrl
      7 title
      8 userType
      9 preferredLanguage
      10 locale
      11 timezone
      12 active
      13 phoneNumbers
      14 ims
      15 photos
      16 addresses
      17 groups
      18 entitlements
      19 roles
      20 x509Certificates
      21 urn:ietf:params:scim:schemas:extension:gluu:2.0:User

You can skip less important attributes if you want. Please see below follow-up method to create an user.

```

Selection: 2

«Identifier for the user, typically used by the user to directly authenticate (id and externalId are opaque identifiers generally not known by users). Type: string»
userName: shakil

Data for object name. See section 4.1.1 of RFC 7643

   «Type: string»
   familyName: shakil

   «Type: string»
   givenName: shakil

   «Type: string»
   middleName: shakil

   «A "title" like "Ms.", "Mrs.". Type: string»
   honorificPrefix: Mr.

   «Name suffix, like "Junior", "The great", "III". Type: string»
   honorificSuffix: Miah

   «Full name, including all middle names, titles, and suffixes as appropriate. Type: string»
   formatted: 

«Name of the user suitable for display to end-users. Type: string»
displayName: shakil

«Type: string»
password: password

«See section 4.1.2 of RFC 7643. »
Add Email? shakil@gluu.org
Please enter one of y, n
Add Email? n

Populate optional fields? y
Optional Fields:
1 schemas
2 id
3 meta
4 externalId
5 nickName
6 profileUrl
7 title
8 userType
9 preferredLanguage
10 locale
11 timezone
12 active
13 phoneNumbers
14 ims
15 photos
16 addresses
17 groups
18 entitlements
19 roles
20 x509Certificates
21 urn:ietf:params:scim:schemas:extension:gluu:2.0:User

«c: continue, #: populate filed. »
Selection: c
Obtained Data:

{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": null
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": null,
  "password": "12345678",
  "emails": [],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": null,
  "id": null,
  "meta": null
}

Continue? y
Getting access token for scope https://jans.io/scim/users.write
Please wait while posting data ...

{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": "Mr. shakil shakil shakil Miah"
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": null,
  "password": null,
  "emails": [],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "7881ed5c-1dad-4265-9b74-ee6c3932c11f",
  "meta": {
    "resourceType": "User",
    "created": "2021-03-29T19:04:52.353Z",
    "lastModified": "2021-03-29T19:04:52.353Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/7881ed5c-1dad-4265-9b74-ee6c3932c11f"
  }
}

Selection: 

```

## Retrieves an User Resources by its ID

You can retrieve an user resources by its ID. Also it supports filter in searching means you can choose list of attributes you want to retrieve and exclude list of attributes that you don't want to retrieve. Here, I have skipped for each property to retrieve all its attributes.

```
Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
---------------------------------------------------------------

«id. Type: string»
id: 7881ed5c-1dad-4265-9b74-ee6c3932c11f

«A comma-separated list of attribute names to return in the response. Type: string»
attributes: 

«When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list). Type: string»
excludedAttributes: 
Calling Api with parameters: {'id': '7881ed5c-1dad-4265-9b74-ee6c3932c11f'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/users.read
{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": "Mr. shakil shakil shakil Miah"
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": false,
  "password": null,
  "emails": null,
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "7881ed5c-1dad-4265-9b74-ee6c3932c11f",
  "meta": {
    "resourceType": "User",
    "created": "2021-03-29T19:04:52.353Z",
    "lastModified": "2021-03-29T19:04:52.353Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/7881ed5c-1dad-4265-9b74-ee6c3932c11f"
  }
}

Selection: 
```

## Update an User resource: 

You can update an user resources by its ID also. If you enter an ID of an user resource, It will show a list of attributes. You can select any of theme one by one to update each value of its property. 

```

Retrieves a User resource by Id (see section 3.4.1 of RFC 7644)
---------------------------------------------------------------

«id. Type: string»
id: 7881ed5c-1dad-4265-9b74-ee6c3932c11f
Calling Api with parameters: {'id': '7881ed5c-1dad-4265-9b74-ee6c3932c11f'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/users.read
Fields:
 1 active
 2 addresses
 3 displayName
 4 emails
 5 entitlements
 6 externalId
 7 groups
 8 id
 9 ims
10 locale
11 meta
12 name
13 nickName
14 password
15 phoneNumbers
16 photos
17 preferredLanguage
18 profileUrl
19 roles
20 schemas
21 timezone
22 title
23 urn:ietf:params:scim:schemas:extension:gluu:2.0:User
24 userName
25 userType
26 x509Certificates

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 

```
Let's say we are going to change the user `active` status, there is a follow-up process: 

```
Selection: 1

«Type: boolean»
active  [false]: true
Please enter a(n) boolean value: _true, _false
active  [false]: _true

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: s
Changes:
active: True

Continue? y
Please wait while posting data ...

Getting access token for scope https://jans.io/scim/users.write
{
  "externalId": null,
  "userName": "shakil",
  "name": {
    "familyName": "shakil",
    "givenName": "shakil",
    "middleName": "shakil",
    "honorificPrefix": "Mr.",
    "honorificSuffix": "Miah",
    "formatted": "Mr. shakil shakil shakil Miah"
  },
  "displayName": "shakil",
  "nickName": null,
  "profileUrl": null,
  "title": null,
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": true,
  "password": null,
  "emails": null,
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": null,
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "7881ed5c-1dad-4265-9b74-ee6c3932c11f",
  "meta": {
    "resourceType": "User",
    "created": "2021-03-29T19:04:52.353Z",
    "lastModified": "2021-04-01T22:45:15.804Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/7881ed5c-1dad-4265-9b74-ee6c3932c11f"
  }
}

Selection: 

```

This is how you can update each of its attributes.


## Delete an user resource
 
 If you want to delete an entry from user resources, you can do that thing easily using the Interatice Mode of Janssen CLI. To delete an user entry, you need to provide its `inum`. In our case: It's `id=7881ed5c-1dad-4265-9b74-ee6c3932c11f` which one are going to be deleted. After then, it will ask for the confirmation, just enter 'y' to delete. Please see below result to better understand.

```
Selection: 5

«Entry to be deleted. »
id: 7881ed5c-1dad-4265-9b74-ee6c3932c11f

Are you sure want to delete 7881ed5c-1dad-4265-9b74-ee6c3932c11f ? y
Getting access token for scope https://jans.io/scim/users.write
Please wait while deleting 7881ed5c-1dad-4265-9b74-ee6c3932c11f ...


Entry 7881ed5c-1dad-4265-9b74-ee6c3932c11f was deleted successfully


Selection: 

```

## Updates user resources using operation mode

This is an alternative option to update user resources. To use this option, you need to consider the following things: 

    - **id**: a unique id of user resources
    - **op**: one operation to be done from [add, remove, replace] 
    - **path**: an attribute path where this operation to be done.
    - **value**: any string type value to `add` or `replace`.

This is an example to add `title` which `id: 18ca6089-42fb-410a-a5b5-c2631d75dc7d` 

```
Selection: 6

«Entry to be patched. »
id: 18ca6089-42fb-410a-a5b5-c2631d75dc7d

«The kind of operation to perform. Type: string»
op: add

«Required when op is remove, optional otherwise. Type: string»
path: title

«Only required when op is add or replace. Type: string»
value: Admin

Patch another param? n
[
  {
    "op": "add",
    "path": "title",
    "value": "Admin"
  }
]

Continue? y
Getting access token for scope https://jans.io/scim/users.write
Please wait patching...

{
  "externalId": null,
  "userName": "admin",
  "name": {
    "familyName": "User",
    "givenName": "Admin",
    "middleName": "Admin",
    "honorificPrefix": null,
    "honorificSuffix": null,
    "formatted": "Admin Admin User"
  },
  "displayName": "Default Admin User",
  "nickName": "Admin",
  "profileUrl": null,
  "title": "Admin",
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": true,
  "password": null,
  "emails": [
    {
      "value": "admin@testjans.gluu.com",
      "display": null,
      "type": null,
      "primary": false
    }
  ],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": [
    {
      "value": "60B7",
      "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7",
      "display": "Jannsen Manager Group",
      "type": "direct"
    }
  ],
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "18ca6089-42fb-410a-a5b5-c2631d75dc7d",
  "meta": {
    "resourceType": "User",
    "created": null,
    "lastModified": "2021-04-05T17:56:40.502Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d"
  }
}

Selection: 

```

There is another example to update user resource on a sub-path:

```
Selection: 6

«Entry to be patch
id: 18ca6089-42fb-410a-a5b5-c2631d75dc7d

«The kind of operation to perform. Type: string»
op: replace

«Required when op is remove, optional otherwise. Type: string»
path: name/familyName

«Only required when op is add or replace. Type: string»
value: MH Shakil

Patch another param? n
[
  {
    "op": "replace",
    "path": "name.familyName",
    "value": "MH Shakil"
  }
]

Continue? y
Getting access token for scope https://jans.io/scim/users.write
Please wait patching...

{
  "externalId": null,
  "userName": "admin",
  "name": {
    "familyName": "MH Shakil",
    "givenName": "Admin",
    "middleName": "Admin",
    "honorificPrefix": null,
    "honorificSuffix": null,
    "formatted": "Admin Admin User"
  },
  "displayName": "Default Admin User",
  "nickName": "Admin",
  "profileUrl": null,
  "title": "MH Shakil",
  "userType": null,
  "preferredLanguage": null,
  "locale": null,
  "timezone": null,
  "active": true,
  "password": null,
  "emails": [
    {
      "value": "admin@testjans.gluu.com",
      "display": null,
      "type": null,
      "primary": false
    }
  ],
  "phoneNumbers": null,
  "ims": null,
  "photos": null,
  "addresses": null,
  "groups": [
    {
      "value": "60B7",
      "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7",
      "display": "Jannsen Manager Group",
      "type": "direct"
    }
  ],
  "entitlements": null,
  "roles": null,
  "x509Certificates": null,
  "urn:ietf:params:scim:schemas:extension:gluu:2.0:User": null,
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User"
  ],
  "id": "18ca6089-42fb-410a-a5b5-c2631d75dc7d",
  "meta": {
    "resourceType": "User",
    "created": null,
    "lastModified": "2021-04-07T17:57:11.250Z",
    "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d"
  }
}

Selection: 

```

**_Please note_**: you can use any of them between dot (.) and slash (/) to add a sub-path in the operation.


