
Sector identifiers provide a way to group clients from the same adminstrative domain using pairwise subject identifiers. In this case, each client needs to be given the same pairwise ID for the person to maintain continuity across all the related websites

With `jans-cli` you can do such operations:

```
OAuth - OpenID Connect - Sector Identifiers
-------------------------------------------
1 Gets list of OpenID Connect Sectors
2 Create new OpenID Connect Sector
3 Update OpenId Connect Sector
4 Get OpenID Connect Sector by Inum
5 Delete OpenID Connect Sector
6 Partially update OpenId Connect Sector by Inum
```

**Get list of OpenID Connect Sectors**

To get list of OpenID Connect sectors, go with the first option. It will display all the connect sectors available in your janssen server.

```
Gets list of OpenID Connect Sectors
-----------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.readonly
[
  {
    "id": "26fba854-7adf-4a81-99d2-b772aa053b93",
    "description": "Testing OpenID Connect Sector",
    "redirectUris": [
      "https://test.gluu.org"
    ],
    "clientIds": [
      "1801.86c324cc-621f-477f-836d-09fcd720353e"
    ]
  },
  {
    "id": "53337668-f721-4bc0-9d04-a249f38d0def",
    "description": "Testing sector identifier",
    "redirectUris": [
      "https://test.gluu.com"
    ],
    "clientIds": null
  }
]


```

**Create new OpenID Sector Identifier**

There are some specific follow-up method to create an OpenID sector identifier.

1. It will ask to enter an Unique ID (ex; 'test')
2. It will ask to update optional fields. `enter` 'y' to confirm or 'n' to skip.
3. If you enter 'y', It will show some optional fields as below:

    - 1 description
    - 2 redirectUris
    - 3 clientIds

4. After completing 3rd steps enter `c` to continue. It will show all the data you provided.
5. At the end it will ask for the confirmation to save it. enter `y` to continue. 


> description: Add some information related to the sector identifier

> redirectUris: Add redirect url here

> clientIds: add list of the client ID linked with sector identifier

Please, see below results to better understand.

```
Selection: 2

«XRI i-number. Sector Identifier to uniquely identify the sector. Type: string»
id: test

Populate optional fields? y
Optiaonal Fields:
1 description
2 redirectUris
3 clientIds

«c: continue, #: populate filed. »
Selection: 1

«A human-readable string describing the sector. Type: string»
description: Testing sector identifier

«c: continue, #: populate filed. »
Selection: 2

«Redirection URI values used by the Client. One of these registered Redirection URI values must exactly match the redirect_uri parameter value used in each Authorization Request. Type: array of string separated by _,»
Example: https://client.example.org/cb
redirectUris: https://test.gluu.com/back

«c: continue, #: populate filed. »
Selection: 3

«List of OAuth 2.0 Client Identifier valid at the Authorization Server. Type: array of string separated by _,»
clientIds: 

«c: continue, #: populate filed. »
Selection: c
Obtained Data:

{
  "id": "test",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com/back"
  ],
  "clientIds": []
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.write
Please wait while posting data ...

{
  "id": "1102af41-6b2e-4d65-b2fd-620675e1efe3",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com/back"
  ],
  "clientIds": null
}
```

**Update OpenID Connect Sector**

To update an OpenID Connect sector identifier, you need an `inum` of a sector identifier which one you want to update. Let's say, We are going to update the 2nd identifier from the above list of OpenID connect sector identifer, its id: `53337668-f721-4bc0-9d04-a249f38d0def`.

Choosing the 3rd option from the Sector Identifier Menu:
- It will ask to enter the id of a Sector Identifier
- After then, It comes with some fields

    Fields:
    
      1. clientIds
      2. description
      3. id
      4. redirectUris

- select each of the field to update it. Here, We are going to update `clientIds` with this value: `1801.86c324cc-621f-477f-836d-09fcd720353e`

- If update is done, then enter `s` to save the changes.
- Finally, It will ask to enter `y` for the confirmation. 
- and at the end, it will show all updated result as below:

```
Continue? y
Please wait while posting data ...

Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.write
{
  "id": "53337668-f721-4bc0-9d04-a249f38d0def",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com"
  ],
  "clientIds": [
    "1801.86c324cc-621f-477f-836d-09fcd720353e"
  ]
}

Selection: 

```

**Get OpenID Connect Sector Identifier by inum**

Simply enter an `id` of a Sector Identifier, It will retrieve data and display on the monitor.


```
Get OpenID Connect Sector by Inum
---------------------------------

«id. Type: string»
id: 53337668-f721-4bc0-9d04-a249f38d0def
Calling Api with parameters: {'id': '53337668-f721-4bc0-9d04-a249f38d0def'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/sectoridentifiers.readonly
{
  "id": "53337668-f721-4bc0-9d04-a249f38d0def",
  "description": "Testing sector identifier",
  "redirectUris": [
    "https://test.gluu.com"
  ],
  "clientIds": [
    "1801.86c324cc-621f-477f-836d-09fcd720353e"
  ]
}

Selection: 
```

**Delete OpenID Connect Sector**

To delete an OpenID Connect Sector by its id, choose option 5 from the Sector Identifier Menu. Then enter `id` which one you are going to delete. Here, We are going to delete a Sector Identifier with `id:53337668-f721-4bc0-9d04-a249f38d0def`. press `y` for the confirmation. It will delete entry from the server.

-->

