---
tags:

subtitle: Learn how to manage and change Agama project configuration 
---

# Custom Assrts

The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-guide/config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

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

##  Using Command Line


In the Janssen Server, you can deploy and customize the Assets using the
command line. To get the details of Janssen command line operations relevant to
Asset, you can check the operations under the `JansAssets` task using the
command below.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info JansAssets
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
  fieldValuePair: Field and value pair for seraching [string]
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

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema-sample <schema>, for example /opt/jans/jans-cli/config-cli.py --schema-sample AssetForm
```

### Gets all Jans assets

You can get the all Custom Assets  of your Janssen Server by performing this operation.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-all-assets
```
```json title="Sample Output" linenums="1"
{
  "start": 0,
  "totalEntriesCount": 2,
  "entriesCount": 2,
  "entries": [
    {
      "dn": "inum=36014ca4-0978-4d95-8858-964b815ea770,ou=document,o=jans",
      "inum": "36014ca4-0978-4d95-8858-964b815ea770",
      "displayName": "custom.xhtml",
      "description": "custom page",
      "document": "",
      "creationDate": "2024-07-25T11:40:17",
      "jansService": [
        "jans-auth"
      ],
      "jansLevel": 1,
      "jansEnabled": true,
      "baseDn": "inum=36014ca4-0978-4d95-8858-964b815ea770,ou=document,o=jans"
    },
    {
      "dn": "inum=b5ab08e4-17b2-487d-845a-bdc6b48fd5b4,ou=document,o=jans",
      "inum": "b5ab08e4-17b2-487d-845a-bdc6b48fd5b4",
      "displayName": "a.png",
      "description": "custom image",
      "document": "",
      "creationDate": "2024-07-25T11:44:38",
      "jansService": [
        "jans-auth"
      ],
      "jansLevel": 2,
      "jansEnabled": true,
      "baseDn": "inum=b5ab08e4-17b2-487d-845a-bdc6b48fd5b4,ou=document,o=jans"
    }
  ]
}
```

### Gets an asset by inum

With `get-all-assets` operation-id, we can get any specific asset matched with `inum`.
 If we know the `inum`, we can simply use the below command:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-asset-by-inum \
--url-suffix inum:36014ca4-0978-4d95-8858-964b815ea770
```
It returns the details as below:


```json title="Sample Output" linenums="1"
 {
  "dn": "inum=36014ca4-0978-4d95-8858-964b815ea770,ou=document,o=jans",
  "inum": "36014ca4-0978-4d95-8858-964b815ea770",
  "displayName": "custom.xhtml",
  "description": "custom page",
  "document": "",
  "creationDate": "2024-07-25T11:40:17",
  "jansService": [
    "jans-auth"
  ],
  "jansLevel": 1,
  "jansEnabled": true,
  "baseDn": "inum=36014ca4-0978-4d95-8858-964b815ea770,ou=document,o=jans"
}
```


### Gets an asset by name

With `get-asset-by-name` operation-id, we can get any specific asset matched with `name`.
 If we know the `name`, we can simply use the below command:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-asset-by-name \
--url-suffix name:a.png
```
It returns the details as below:

```json title="Sample Output" linenums="1"
{
  "start": 0,
  "totalEntriesCount": 1,
  "entriesCount": 1,
  "entries": [
    {
      "dn": "inum=b5ab08e4-17b2-487d-845a-bdc6b48fd5b4,ou=document,o=jans",
      "inum": "b5ab08e4-17b2-487d-845a-bdc6b48fd5b4",
      "displayName": "a.png",
      "description": "custom image",
      "document": "",
      "creationDate": "2024-07-25T11:44:38",
      "jansService": [
        "jans-auth"
      ],
      "jansLevel": 2,
      "jansEnabled": true,
      "baseDn": "inum=b5ab08e4-17b2-487d-845a-bdc6b48fd5b4,ou=document,o=jans"
    }
  ]
}
```

### Gets asset services


You can get the asset services  of your Janssen Server by performing `get-asset-services` operation.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-asset-services
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


### Get valid asset types

You can get the  asset types of your Janssen Server by performing `get-asset-types` operation.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-asset-types
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

TO DO

### Add New Asset

To create a new ssset, we can use `post-new-asset` operation id. As shown in
the [output](#using-command-line) for `--info` command, the `post-new-asset` 
operation requires data to be sent according to `AssetForm` schema.


To see the schema, use the command below:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema AssetForm
```

For better understanding, the Janssen Server also provides an sample of data to 
be sent to the server.This sample conforms to the schema above. Use the command 
below to get the sample.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema-sample AssetForm
```

Using the schema and the example above, we have added below data to the 
file `/tmp/add-asset.json`

```json title="Sample Output" linenums="1" 

```

TO DO

### Update Existing Asset

To update the configuration follow the steps below.

1. [Gets the asset by inum](#gets-an-asset-by-inum) and store it into a file for editing.
   The following command will retrieve the existing asset in the schema file.
  ```bash title="Sample Command"
  /opt/jans/jans-cli/config-cli.py -no-color --operation-id put-asset \
  --url-suffix inum:36014ca4-0978-4d95-8858-964b815ea770 > /tmp/update-asset.json
  ```
2. Edit and update the desired configuration values in the file while keeping other
   properties and values unchanged. Updates must adhere to the `AssetForm`
   schema as mentioned [here](#using-command-line).

3. We have changed only the `` to ` ` in existing asset.
   Use the updated file to send the update to the Janssen Server using the command below
   ```bash title="Command"
   /opt/jans/jans-cli/config-cli.py --operation-id put-asset \
   --data /tmp/update-asset.json
   ```
This will updated the existing asset matched with inum value.


### Delete Asset

You can delete any Asset by its `inum` value.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id delete-asset --url-suffix inum:36014ca4-0978-4d95-8858-964b815ea770
```

Just change the `inum` value to your own according to which one you want to delete.



## Using Text-based UI


In Janssen, You can deploy and customize an Asset using
the [Text-Based UI](./config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
sudo /opt/jans/jans-cli/jans_cli_tui.py
```

### Asset Screen

Navigate to `Assest` to open the Asset screen as shown in the image below.

* To get the list of currently added Asset, bring the control to Search box (using the tab key),
and press Enter. Type the search string to search for Asset with matching `Display Name` and `inum`.


![Image](../../assets/tui-asset-screen.png)


* Use the `Add Asset` button to create a new asset. 
* You can add several types of srvices in the screen below
* For example, below is the picture availability Asset data.

![Image](../../assets/tui-asset-data.png)



## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the custom assets. Endpoint details are published in the [Swagger
document](./../reference/openapi.md).