---
tags:
 - administration
 - configuration
 - custom assets
---

# Custom Assets Configuration

The Janssen Server provides multiple configuration tools to configure custom
assets.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-guide/config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-the-command-line)

=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](../config-guide/config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-guide/config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)

##  Using The Command Line


In the Janssen Server, you can deploy custom assets using the
command line. To get the details of Janssen command line operations relevant to
the custom assets, check the operations under the `JansAssets` task using the
command below.

```bash title="Command"
jans cli --info JansAssets
```

```test title="Sample Output" linenums="1"
Operation ID: get-asset-by-inum
 Description: Gets an asset by inum - unique identifier
 Parameters:
 inum: Asset Inum [string]
Operation ID: delete-asset
 Description: Delete an asset
 Parameters:
 inum: Asset identifier [string]
Operation ID: get-asset-by-name
 Description: Fetch asset by name.
 Parameters:
 name: Asset Name [string]
Operation ID: get-all-assets
 Description: Gets all Jans assets.
 Parameters:
 limit: Search size - max size of the results to return [integer]
 pattern: Search pattern [string]
 status: Status of the attribute [string]
 startIndex: The 1-based index of the first query result [integer]
 sortBy: Attribute whose value will be used to order the returned response [string]
 sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
 fieldValuePair: Field and value pair for searching [string]
Operation ID: get-asset-services
 Description: Gets asset services
Operation ID: get-asset-types
 Description: Get valid asset types
Operation ID: put-asset
 Description: Update existing asset
 Schema: AssetForm
Operation ID: post-new-asset
 Description: Upload new asset
 Schema: AssetForm

To get sample schema type jans cli --schema-sample <schema>, for example jans cli --schema-sample AssetForm
```

### Get All Current Custom Assets

Use the operation ID `get-all-assets` to get all the currently configured 
custom assets on the Janssen Server.

```bash title="Command"
jans cli --operation-id get-all-assets
```
```json title="Sample Output" linenums="1"
{
  "start": 0,
  "totalEntriesCount": 3,
  "entriesCount": 3,
  "entries": [
    {
      "dn": "inum=61edc29d-45f8-4ab9-8c9a-7b39e4cbe440,ou=document,o=jans",
      "inum": "61edc29d-45f8-4ab9-8c9a-7b39e4cbe440",
      "fileName": "p2.properties",
      "description": "Valid text description",
      "document": "cHJvcGVydGllcyBoZXJlLiAK\r\n",
      "creationDate": "2024-07-31T07:36:08",
      "jansService": [
        "jans-auth"
      ],
      "jansLevel": 142,
      "jansEnabled": true,
      "baseDn": "inum=61edc29d-45f8-4ab9-8c9a-7b39e4cbe440,ou=document,o=jans"
    },
    {
      "dn": "inum=835c7a68-57b1-4e39-8d2a-2a3963f2a92f,ou=document,o=jans",
      "inum": "835c7a68-57b1-4e39-8d2a-2a3963f2a92f",
      "fileName": "p1.properties",
      "description": "updated thrice Valid text description",
      "document": "cHJvcGVydGllcwo=\r\n",
      "jansService": [
        "jans-auth"
      ],
      "jansEnabled": false,
      "baseDn": "inum=835c7a68-57b1-4e39-8d2a-2a3963f2a92f,ou=document,o=jans"
    },
    {
      "dn": "inum=fd67d07b-c874-4bc1-a9f0-860fc4f7a091,ou=document,o=jans",
      "inum": "fd67d07b-c874-4bc1-a9f0-860fc4f7a091",
      "fileName": "p.properties",
      "description": "Valid text description",
      "document": "cHJvcGVydGllcyBoZXJlLiAK\r\n",
      "creationDate": "2024-07-31T07:40:03",
      "jansService": [
        "jans-auth"
      ],
      "jansLevel": 142,
      "jansEnabled": true,
      "baseDn": "inum=fd67d07b-c874-4bc1-a9f0-860fc4f7a091,ou=document,o=jans"
    }
  ]
}

```

### Get Custom Asset By inum

With `get-asset-by-inum` operation-id, we can get any specific asset matched 
with `inum`. If we know the `inum`, we can simply use the below command:

```bash title="Command"
jans cli --operation-id get-asset-by-inum \
--url-suffix inum:61edc29d-45f8-4ab9-8c9a-7b39e4cbe440
```
It returns the details as below:


```json title="Sample Output" linenums="1"
{
  "dn": "inum=61edc29d-45f8-4ab9-8c9a-7b39e4cbe440,ou=document,o=jans",
  "inum": "61edc29d-45f8-4ab9-8c9a-7b39e4cbe440",
  "fileName": "p2.properties",
  "description": "Valid text description",
  "document": "cHJvcGVydGllcyBoZXJlLiAK\r\n",
  "creationDate": "2024-07-31T07:36:08",
  "jansService": [
    "jans-auth"
  ],
  "jansLevel": 142,
  "jansEnabled": true,
  "baseDn": "inum=61edc29d-45f8-4ab9-8c9a-7b39e4cbe440,ou=document,o=jans"
}
```


### Get Custom Asset By Name

With `get-asset-by-name` operation-id, we can get any specific asset matched with `name`.
 If we know the `name`, we can simply use the below command:

```bash title="Command"
jans cli --operation-id get-asset-by-name \
--url-suffix name:p1.properties
```
It returns the details as below:

```json title="Sample Output" linenums="1"
{
  "start": 0,
  "totalEntriesCount": 1,
  "entriesCount": 1,
  "entries": [
    {
      "dn": "inum=835c7a68-57b1-4e39-8d2a-2a3963f2a92f,ou=document,o=jans",
      "inum": "835c7a68-57b1-4e39-8d2a-2a3963f2a92f",
      "fileName": "p1.properties",
      "description": "updated thrice Valid text description",
      "document": "cHJvcGVydGllcwo=\r\n",
      "jansService": [
        "jans-auth"
      ],
      "jansEnabled": false,
      "baseDn": "inum=835c7a68-57b1-4e39-8d2a-2a3963f2a92f,ou=document,o=jans"
    }
  ]
}
```

### Get Services


Get the list of Janssen Server services that support custom assets 
by performing `get-asset-services` operation.

```bash title="Command"
jans cli --operation-id get-asset-services
```

```text title="Sample Output" linenums="1"
[
 "jans-auth",
 "jans-casa",
 "jans-config-api",
 "jans-fido2",
 "jans-link",
 "jans-lock",
 "jans-scim",
 "jans-keycloak-link"
]
```


### Get Valid Asset Types

Get the asset types of your Janssen Server by performing `get-asset-types` 
operation.

```bash title="Command"
jans cli --operation-id get-asset-types
```

```text title="Sample Output" linenums="1"
[
 "properties",
 "jar",
 "xhtml",
 "js",
 "css",
 "png",
 "gif",
 "jpg",
 "jpeg"
]
```

### Add New Custom Asset

To create a new asset, we can use `post-new-asset` operation id. As shown in
the [output](#using-the-command-line) for `--info` command, the `post-new-asset` 
operation requires data to be sent according to `AssetForm` schema.


To see the schema, use the command below:

```bash title="Command"
jans cli --schema AssetForm
```

For better understanding, the Janssen Server also provides a sample of data to 
be sent to the server. This sample conforms to the schema above. Use the command 
below to get the sample.

```bash title="Command"
jans cli --schema-sample AssetForm
```

Using the schema and the example above, we have added below data to the 
file `/tmp/add-asset.json`. Example below will load `p3.properties` file as
a custom asset to the `jans-auth` service.

```json title="Input" linenums="1" 
{
  "document": {
    "fileName": "p3.properties",
    "description": "text description",
    "jansService": [
      "jans-auth"
    ],
    "jansLevel": 144,
    "jansEnabled": true
  },
  "assetFile": "/tmp/p3.properties"
}
```
Now let's post this Assert to the Janssen Server to be added to the existing set:

```bash title="Command"
 jans cli --operation-id post-new-asset \
 --data /tmp/add-asset.json
```

### Update Existing Custom Assets

Use the `put-asset` operation to update an existing asset. This operation uses
same schema as [add new asset](#add-new-custom-asset) operation. For example,
assuming that there is an existing asset as show below:

```json title="Existing Asset" linenums="1"
{
  "dn": "inum=fd67d07b-c874-4bc1-a9f0-860fc4f7a091,ou=document,o=jans",
  "inum": "fd67d07b-c874-4bc1-a9f0-860fc4f7a091",
  "fileName": "p.properties",
  "description": "Valid text description",
  "document": "cHJvcGVydGllcyBoZXJlLiAK\r\n",
  "creationDate": "2024-07-31T07:40:03",
  "jansService": [
    "jans-auth"
  ],
  "jansLevel": 142,
  "jansEnabled": true,
  "baseDn": "inum=fd67d07b-c874-4bc1-a9f0-860fc4f7a091,ou=document,o=jans"
}
```

!!! Note

    `assetFile` attribute is optional for update operation as there may be scenario where only metadata of an asset is to be updated.
    

Now to update level of this asset to 6, create a text file with following
content in it. Let's name this text file as `/tmp/update-asset.json`

```json title="Input" linenums="1"
{
  "document": {
      "dn": "inum=fd67d07b-c874-4bc1-a9f0-860fc4f7a091,ou=document,o=jans",
      "inum": "fd67d07b-c874-4bc1-a9f0-860fc4f7a091",
      "fileName": "p.properties",
      "description": "Valid text description",
      "document": "cHJvcGVydGllcyBoZXJlLiAK\r\n",
      "creationDate": "2024-07-31T07:40:03",
      "jansService": [
        "jans-auth"
      ],
      "jansLevel": 6,
      "jansEnabled": true,
   },
  "assetFile": "/tmp/p.properties"
}
```

Now use the command below to update the asset with new value for level.

```bash title="Sample Command"
jans cli --operation-id put-asset \
--data /tmp/update-asset.json
```

Upon successful execution, this command will return with updated asset values.

```json title="Return values" linenums="1"
{
  "dn": "inum=fd67d07b-c874-4bc1-a9f0-860fc4f7a091,ou=document,o=jans",
  "inum": "fd67d07b-c874-4bc1-a9f0-860fc4f7a091",
  "fileName": "p.properties",
  "description": "Valid text description",
  "document": "cHJvcGVydGllcyBoZXJlLiAK\r\n",
  "creationDate": "2024-07-31T07:40:03",
  "jansService": [
    "jans-auth"
  ],
  "jansLevel": 6,
  "jansEnabled": true,
  "baseDn": "inum=fd67d07b-c874-4bc1-a9f0-860fc4f7a091,ou=document,o=jans"
}
```

### Delete Custom Asset

You can delete any custom asset by its `inum` value.

```bash title="Command"
jans cli --operation-id delete-asset \
--url-suffix inum:61edc29d-45f8-4ab9-8c9a-7b39e4cbe440
```

## Using Text-based UI


In Janssen, You can deploy custom asset using
the [Text-Based UI](./config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
jans tui
```

### Asset Screen

Navigate to `Assets` tab to open the Assets screen as shown in the image below.

* To get the list of currently added Assets, bring the control to the Search 
box (using the tab key), and press Enter. Type the search string to search 
for Asset with matching `inum`, or `File Name` or `Description`


![Image](../../assets/tui-asset-screen.png)


* Use the `Add Asset` button to create a new asset. 
* From the screen below, select the custom asset that needs to be uploaded
and select the Janssen Server service to which the asset will be uploaded.

![Image](../../assets/tui-asset-data.png)



## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the custom assets. Endpoint details are published in the [Swagger
document](./../reference/openapi.md).
