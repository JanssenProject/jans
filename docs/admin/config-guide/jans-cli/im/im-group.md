---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# Group Resources

!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.

> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

Group resources are used to organize user resources. These are the following options:

```
group
-----
1 Query Group resources (see section 3.4.2 of RFC 7644)
2 Allows creating a Group resource via POST (see section 3.3 of RFC 7644)
3 Retrieves a Group resource by Id (see section 3.4.1 of RFC 7644)
4 Updates a Group resource (see section 3.5.1 of RFC 7644). Update works in a replacement fashion&amp;#58; every
attribute value found in the payload sent will replace the one in the existing resource representation. Attributes 
not passed in the payload will be left intact.

5 Deletes a group resource (see section 3.6 of RFC 7644)
6 Updates one or more attributes of a Group resource using a sequence of additions, removals, and 
replacements operations. See section 3.5.2 of RFC 7644

7 Query Group resources (see section 3.4.2 of RFC 7644)

```

## Query Group Resources

It shows all the group resources and its perspective user resources. To find list of resources with custom filter, it supports advanced search with few properties:

    1. attributes
    2. excludeattributes
    3. filter
    4. startindex
    5. count

This is an demo example where each of this properties skipped for default value:

```
Query Group resources (see section 3.4.2 of RFC 7644)
-----------------------------------------------------

«A comma-separated list of attribute names to return in the response. Type: string»
attributes: 

«When specified, the response will contain a default set of attributes minus those listed here (as a comma-separated list). Type: string»
excludedAttributes: 

«An expression specifying the search criteria. See section 3.4.2.2 of RFC 7644. Type: string»
filter: 

«The 1-based index of the first query result. Type: integer»
startIndex: 

«Specifies the desired maximum number of query results per page. Type: integer»
count: 

«The attribute whose value will be used to order the returned responses. Type: string»
sortBy: 

«Order in which the sortBy param is applied. Allowed values are "ascending" and "descending". Type: string»
sortOrder: 
Please wait while retreiving data ...

Getting access token for scope https://jans.io/scim/groups.read
{
  "Resources": [
    {
      "displayName": "Jannsen Manager Group",
      "members": [
        {
          "$ref": "https://testjans.gluu.com/jans-scim/restv1/v2/Users/18ca6089-42fb-410a-a5b5-c2631d75dc7d",
          "type": "User",
          "display": "Default Admin User",
          "value": "18ca6089-42fb-410a-a5b5-c2631d75dc7d"
        }
      ],
      "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:Group"
      ],
      "id": "60B7",
      "meta": {
        "resourceType": "Group",
        "created": null,
        "lastModified": null,
        "location": "https://testjans.gluu.com/jans-scim/restv1/v2/Groups/60B7"
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
## Update a group Resource: 
  
Updating a group resource works in a replacement fashion and every attribute value found in the payload will replace the one in the existing resource. Attributes those are not passed in the payload will be left as same as before.

If you select option 4 it will be asked to enter the id of a group that you may want to update. After then You will get a list of Fields:

  ```
  Fields:
    1 displayName
    2 id
    3 members
    4 meta
    5 schemas
  ```

You can select each of these fields to update one by one. Let's select 3rd field to add memebers in the group. It will ask to enter some follow-up questions, like `Add Member? [y, n]`. Then enter each value of the user attributes:
  
  - ref: User referral url
  - type: type as a User
  - display: User display Name
  - value: inum of the user

  As you see below, If you choose `y` for `Add another Member?` then similarly you can add resource for another user. But if you choose `n` then you can select few options: 

    - q: to quit from operations
    - v: to view changes
    - l: to display the current list of fields
    - s: to save changes
  
  Please see below result to better understand about how this option really works.

  ```
    «q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 3

«Represents a member of a Group resource. »
Add Member? y

   «URI of the SCIM resource. Type: string»
   ref: https://testjans.gluu.org/jans-scim/restv1/v2/Users/e0b8a6a5-1955-49d7-acba-55a75b2373df

   «The type of member. Only "User" is allowed. Type: string»
   type: User

   «A human readable name, primarily used for display purposes. Type: string»
   display: Default Admin User

   «Identifier (ID) of the resource. Type: string»
   value: e0b8a6a5-1955-49d7-acba-55a75b2373df

Add another Member? n

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: s
Changes:
members: [{'display': 'Default Admin User',
 'ref': 'https://testjans.gluu.org/jans-scim/restv1/v2/Users/e0b8a6a5-1955-49d7-acba-55a75b2373df',
 'type': 'User',
 'value': 'e0b8a6a5-1955-49d7-acba-55a75b2373df'}]
  ```

`continue?` as `y` to perform the operation:

 ```
  Continue? y
  Please wait while posting data ...

  Getting access token for scope https://jans.io/scim/groups.write
  {
    "displayName": "Jannsen Test Group",
    "members": [
      {
        "$ref": "https://testjans.gluu.org/jans-scim/restv1/v2/Users/e0b8a6a5-1955-49d7-acba-55a75b2373df",
        "type": "User",
        "display": "Default Admin User",
        "value": "e0b8a6a5-1955-49d7-acba-55a75b2373df"
      }
    ],
    "schemas": [
      "urn:ietf:params:scim:schemas:core:2.0:Group"
    ],
    "id": "766ffd8c-88a8-4aa8-a430-a5b3ae809c21",
    "meta": {
      "resourceType": "Group",
      "created": "2021-04-14T19:54:03.091Z",
      "lastModified": "2021-04-15T14:21:10.715Z",
      "location": "https://testjans.gluu.org/jans-scim/restv1/v2/Groups/766ffd8c-88a8-4aa8-a430-a5b3ae809c21"
    }
  }
 ```
Finally it will make changes in the group resource.
