---
tags:
  - administrationR
  - configuration
  - cli
  - commandline
  - agama
---
> Prerequisite: Know how to use the Janssen CLI in [command-line mode](jans-cli/README.md)

# Agama Developer Studio

In Janssen, You can deploy and customize agama project using commandline. To get the details of Janssen commandline feature of Agama Developer Studio, you can run this command as below:

```
/opt/jans/jans-cli/config-cli.py --info AgamaDeveloperStudio
```

It will show you the details of available operation-id for Agama developer studio.

## View Agama Developer Studio Project

You can get the details of an Agama project deployed in Janssen by the project name. Commandline for this operation as below:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-dev-studio-prj-by-name --url-suffix name:"agama-project-name"
```

**Example:**
```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-dev-studio-prj-by-name --url-suffix name:testAuth
Please wait while retrieving data ...
{
  "dn": "jansId=3a0e91a4-b79b-37c2-9df7-122247e8ed9c,ou=deployments,ou=agama,o=jans",
  "id": "3a0e91a4-b79b-37c2-9df7-122247e8ed9c",
  "createdAt": 1687247278717,
  "taskActive": false,
  "finishedAt": 1687247288646,
  "assets": null,
  "details": {
    "folders": null,
    "libs": [],
    "flowsError": {
      "imShakil.co.basicAuth": null
    },
    "error": null,
    "projectMetadata": {
      "projectName": "testAuth",
      "author": "imShakil",
      "type": "community",
      "description": "testing authentication with janssen server",
      "version": "1.0.0",
      "configs": null
    }
  },
  "baseDn": "jansId=3a0e91a4-b79b-37c2-9df7-122247e8ed9c,ou=deployments,ou=agama,o=jans"
}
```

## Post Agama Developer Studio Project in Janssen 

Also, You can deploy agama project in Janssen through commandline.

```
/opt/jans/jans-cli/config-cli.py --operation-id post-agama-dev-studio-prj --url-suffix name:"agama-project-name"
```


# Agama Flow Configuration

If you already deployed agama projects successfully in your janssen server through [above](#agama-developer-studio) operations, you can check those agama flow status with these below operations:

```
/opt/jans/jans-cli/config-cli.py --info AgamaConfiguration

Operation ID: get-agama-flows
  Description: Fetches all agama flow.
  Parameters:
  pattern: Search pattern [string]
  limit: Search size - max size of the results to return [integer]
  startIndex: The 1-based index of the first query result [integer]
  sortBy: Attribute whose value will be used to order the returned response [string]
  sortOrder: Order in which the sortBy param is applied. Allowed values are "ascending" and "descending" [string]
  includeSource: Boolean flag to indicate agama source is to be included [boolean]
Operation ID: post-agama-flow
  Description: Create a new agama flow
  Schema: Flow
Operation ID: get-agama-flow
  Description: Gets an agama flow based on Qname.
  Parameters:
  qname: Agama Flow name [string]
  includeSource: Boolean flag to indicate agama source is to be included [boolean]
Operation ID: post-agama-flow-from-source
  Description: Create a new agama flow from source.
  Parameters:
  qname: Agama Flow name [string]

```

## Get Agama Flow

To get details of all the agama flows:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-flows
Please wait while retrieving data ...
{
  "start": 0,
  "totalEntriesCount": 2,
  "entriesCount": 2,
  "entries": [
    {
      "dn": "agFlowQname=imShakil.co.basicAuth,ou=flows,ou=agama,o=jans",
      "qname": "imShakil.co.basicAuth",
      "revision": 0,
      "enabled": true,
      "metadata": {
        "funcName": "_imShakil_co_basicAuth",
        "author": "imShakil",
        "timestamp": 1688453467147
      },
      "baseDn": "agFlowQname=imShakil.co.basicAuth,ou=flows,ou=agama,o=jans"
    },
    {
      "dn": "agFlowQname=mmrraju.test2.agama,ou=flows,ou=agama,o=jans",
      "qname": "mmrraju.test2.agama",
      "revision": 0,
      "enabled": true,
      "metadata": {
        "funcName": "_mmrraju_test2_agama",
        "author": "mmrraju",
        "timestamp": 1688451217241
      },
      "baseDn": "agFlowQname=mmrraju.test2.agama,ou=flows,ou=agama,o=jans"
    }
  ]
}
```

It will display the total number of agama flows that are enabled and their list. You can get detail of an enabled agama flow by its `qname`.

To get details through the `qname` of a agama flow:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-flow --url-suffix qname:"name"
```
`qname` stands for Qualified Name. You can grab that from above operation. In my case, qname for the first one of the above list is: `imShakil.co.basicAuth`

**Example**:
```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-flow --url-suffix qname:"imShakil.co.basicAuth"

Please wait while retrieving data ...
{
  "dn": "agFlowQname=imShakil.co.basicAuth,ou=flows,ou=agama,o=jans",
  "qname": "imShakil.co.basicAuth",
  "revision": 0,
  "enabled": true,
  "metadata": {
    "funcName": "_imShakil_co_basicAuth",
    "author": "imShakil",
    "timestamp": 1688453467147
  },
  "baseDn": "agFlowQname=imShakil.co.basicAuth,ou=flows,ou=agama,o=jans"
}
```

## Create Agama Flow

To create an agama flow in janssen server, let's get the schema first as below:

```
/opt/jans/jans-cli/config-cli.py --schema Flow > /tmp/flow.json
```

Schema should be look like this:

```
{
  "dn": "string",
  "qname": "string",
  "transHash": "string",
  "revision": 133,
  "enabled": true,
  "metadata": {
    "funcName": {
      "type": "string"
    },
    "inputs": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "timeout": {
      "type": "integer",
      "format": "int32"
    },
    "displayName": {
      "type": "string"
    },
    "author": {
      "type": "string"
    },
    "timestamp": {
      "type": "integer",
      "format": "int64"
    },
    "description": {
      "type": "string"
    },
    "properties": {
      "type": "object",
      "additionalProperties": {
        "type": "object"
      }
    }
  },
  "source": "string",
  "transpiled": "string",
  "codeError": "string",
  "baseDn": "string"
}
```

To post agama flow after modification, run this below commandline:

```
/opt/jans/jans-cli/config-cli.py --operation-id post-agama-flow --data /tmp/flow.json 
```

It will display the server response.

