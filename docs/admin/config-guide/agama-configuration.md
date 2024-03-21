---
tags:
  - administration
  - configuration
  - agama
---
> Prerequisite: Know how to use the Janssen CLI in [command-line mode](config-tools/jans-cli/README.md)

# Agama

In Janssen, You can deploy and customize agama project using commandline. To get the details of Janssen commandline feature of Agama, you can run this command as below:

```
/opt/jans/jans-cli/config-cli.py --info Agama
```

It will show you the details of available operation-id for Agama.

```
Operation ID: get-agama-prj-by-name
  Description: Fetches deployed Agama project based on name.
  Parameters:
  name: Agama project name [string]
Operation ID: post-agama-prj
  Description: Deploy an Agama project.
  Parameters:
  name: Agama project name [string]
Operation ID: delete-agama-prj
  Description: Delete a deployed Agama project.
  Parameters:
  name: Agama project name [string]
Operation ID: get-agama-prj-configs
  Description: Retrieve the list of configs based on name.
  Parameters:
  name: Agama project name [string]
Operation ID: put-agama-prj
  Description: Update an Agama project.
  Parameters:
  name: Agama project name [string]
  Parameters:
    type: Description not found for this property
    additionalProperties: Description not found for this property
Operation ID: get-agama-prj
  Description: Retrieve the list of projects deployed currently.
  Parameters:
  start: No description is provided for this parameter [integer]
  count: No description is provided for this parameter [integer]
```

<!-- Table to be added later

|Operation-id   	| url-suffix  	|  endpoint-args 	|  Description 	| Example  	|
|---	|---	|---	|---	|---	|
| `get-agama-prj`  	| NA  	|  `start`,`count` 	|   	|   	|
|   `post-agama-prj`	|  `name` 	|  NA 	|   	|   	|
|   	|   	|   	|   	|   	|

-->

## List of Deployed Projects

To retrieve the list of deployed agama projects:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-prj
```

To get details of all the agama flows:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-prj
Please wait while retrieving data ...
{
  "start": 0,
  "totalEntriesCount": 1,
  "entriesCount": 1,
  "entries": [
    {
      "dn": "jansId=46546e9a-fed6-34d0-ba63-b615233b2115,ou=deployments,ou=agama,o=jans",
      "id": "46546e9a-fed6-34d0-ba63-b615233b2115",
      "createdAt": "2023-08-13T14:45:27",
      "taskActive": false,
      "finishedAt": "2023-08-13T14:45:43",
      "details": {
        "error": "There were problems processing one or more flows",
        "flowsError": {
          "mmrraju.np.test.me": null,
          "mmrraju.test2.agama": null,
          "mmrraju.u2f.me": "Syntax error: mismatched input '<EOF>' expecting {'|', 'Log', 'Trigger', 'Call', 'RRF', 'When', 'Repeat', 'Iterate over', 'Match', 'Finish', 'RFAC', ALPHANUM, QNAME, DOTEXPR, DOTIDXEXPR, WS}\nSymbol: [@9,43:42='<EOF>',<-1>,2:23]\nLine: 2\nColumn: 24"
        },
        "projectMetadata": {
          "projectName": "user_pass_auth",
          "author": "mmrraju",
          "type": "community",
          "description": "This is password based authentication",
          "version": "1.0.34",
          "configs": null
        }
      },
      "baseDn": "jansId=46546e9a-fed6-34d0-ba63-b615233b2115,ou=deployments,ou=agama,o=jans"
    }
  ]
}
```

It will display the total number of agama flows that are enabled and their list. You can get modified list using supported parameters. 

### Endpoint Arguments

`start`: Should be an integer value. It's an index value of starting point of the list.

`count`: Should be an integer value. Total entries number you want to display. 

**Example:**

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-prj --endpoint-args start:1,count:1

Please wait while retrieving data ...
{
  "start": 1,
  "totalEntriesCount": 3,
  "entriesCount": 1,
  "entries": [
    {
      "dn": "jansId=3a0e91a4-b79b-37c2-9df7-122247e8ed9c,ou=deployments,ou=agama,o=jans",
      "id": "3a0e91a4-b79b-37c2-9df7-122247e8ed9c",
      "createdAt": "2023-08-15T05:56:19",
      "taskActive": false,
      "finishedAt": "2023-08-15T05:56:42",
      "details": {
        "error": "Archive missing web and/or code subdirectories",
        "flowsError": null,
        "projectMetadata": {
          "projectName": "testAuth",
          "configs": null
        }
      },
      "baseDn": "jansId=3a0e91a4-b79b-37c2-9df7-122247e8ed9c,ou=deployments,ou=agama,o=jans"
    }
  ]
}

```

## View Agama Project By Name

You can get the details of an Agama project deployed in Janssen by the project name. Commandline for this operation as below:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-prj-by-name --endpoint-args name:"agama-project-name"
```

**Example:**
```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-prj-by-name --endpoint-args name:testAuth
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

## Post Agama Project in Janssen 

Also, You can deploy agama project in Janssen through commandline.

```
/opt/jans/jans-cli/config-cli.py --operation-id post-agama-prj --endpoint-args name:agama-project-name
```

**Example:**

Let's upload [a test project](../../../assets/agama/journey.zip)

```
/opt/jans/jans-cli/config-cli.py --operation-id=post-agama-prj --url-suffix="name:Agama Lab Journey" --data /tmp/journey.zip
Server Response:
{
  "message": "A deployment task for project Agama Lab Journey has been queued. Use the GET endpoint to poll status"
}
```

To get uploaded projects:

```
Please wait while retrieving data ...
{
  "start": 0,
  "totalEntriesCount": 1,
  "entriesCount": 1,
  "entries": [
    {
      "dn": "jansId=a17e9a67-d44f-3b83-9e4d-4bebab375913,ou=deployments,ou=agama,o=jans",
      "id": "a17e9a67-d44f-3b83-9e4d-4bebab375913",
      "createdAt": "2024-03-12T18:15:29",
      "taskActive": false,
      "finishedAt": "2024-03-12T18:15:33",
      "details": {
        "error": "There were problems processing one or more flows",
        "autoconfigure": false,
        "flowsError": {
          "io.jans.agamalab.credsEnrollment.fido2": "Qualified name mismatch: io.jans.agamalab.credsEnrollment.fido2 vs. io.jans.agamaLab.credsEnrollment.fido2",
          "io.jans.agamaLab.registration": null,
          "io.jans.agamaLab.credsEnrollment.otp": null,
          "io.jans.agamaLab.authenticator.otp": null,
          "io.jans.agamaLab.authenticator.super_gluu": null,
          "io.jans.agamaLab.credsEnrollment.super_gluu": null,
          "io.jans.inbound.oauth2.AuthzCode": null,
          "io.jans.agamaLab.emailVerification": null,
          "io.jans.agamaLab.authenticator": null,
          "io.jans.agamaLab.authenticator.fido2": null,
          "io.jans.inbound.oauth2.AuthzCodeWithUserInfo": null,
          "io.jans.agamaLab.main": null,
          "io.jans.agamaLab.githubAuthn": null,
          "io.jans.agamaLab.credsEnrollment": null
        },
        "projectMetadata": {
          "projectName": "Agama Lab Journey",
          "author": "jgomer2001",
          "type": "Community",
          "configs": {
            "io.jans.agamaLab.credsEnrollment.super_gluu": {
              "timeout": 80
            },
            "io.jans.agamaLab.authenticator.super_gluu": {
              "timeout": 80
            },
            "io.jans.agamaLab.credsEnrollment.otp": {
              "timeout": 80,
              "maxAttempts": 4
            },
            "io.jans.agamaLab.githubAuthn": {
              "authzEndpoint": "https://github.com/login/oauth/authorize",
              "tokenEndpoint": "https://github.com/login/oauth/access_token",
              "userInfoEndpoint": "https://api.github.com/user",
              "clientId": "YOUR CLIENT ID HERE",
              "clientSecret": "YOUR CLIENT SECRET",
              "scopes": [
                "user"
              ]
            },
            "io.jans.agamaLab.main": {
              "minCredsRequired": 2,
              "supportedMethods": [
                "otp",
                "fido2"
              ]
            },
            "io.jans.agamaLab.registration": {
              "recaptcha": {
                "enabled": false,
                "site_key": "SITE KEY (if enabled was set to true), see deployment instructions"
              },
              "zohoCRM": {
                "clientId": "see deployment instructions",
                "clientSecret": "",
                "refreshToken": "",
                "accountsUrl": "https://accounts.zoho.com (domain-specific Zoho Accounts URL - the domain hosting the token endpoint)"
              }
            }
          }
        }
      },
      "baseDn": "jansId=a17e9a67-d44f-3b83-9e4d-4bebab375913,ou=deployments,ou=agama,o=jans"
    }
  ]
}
```

Let's update configuration for this project with [sample project configuration](../../../assets/agama/journey-configs.json):

```
/opt/jans/jans-cli/config-cli.py --operation-id=put-agama-prj --url-suffix "name:Agama Lab Journey" --data /tmp/journey-configs.json 
Server Response:
{
  "io.jans.agamaLab.registration": true,
  "io.jans.agamaLab.main": true,
  "io.jans.agamaLab.credsEnrollment.otp": true,
  "io.jans.agamaLab.authenticator.super_gluu": true,
  "io.jans.agamaLab.credsEnrollment.super_gluu": true,
  "io.jans.agamaLab.githubAuthn": true
}
```


## Retrieve Agama Project Configuration

To retrieve agama project configuration:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-agama-prj-configs --endpoint-args name:agama-project-name
```

## Update Agama Project

To update existing agama project:
```
/optjans/jans-cli/config-cli.py --operation-id put-agama-prj --endpoint-args name:agama-project-name
```

## Delete Agama Project

To delete agama project by its name:
```
/opt/jans/jans-cli/config-cli.py --operation-id delete-agama-prj --endpoint-args name:agama-project-name
```


# Agama Flow Configuration

If you already deployed agama projects successfully in your janssen server through [above](#agama) operations, you can check those agama flow status with these below operations:

```
/opt/jans/jans-cli/config-cli.py --info AgamaConfiguration

Operation ID: agama-syntax-check
  Description: Determine if the text passed is valid Agama code
  Parameters:
  qname: Agama Flow name [string]
```

## Agama Flow Syntax

```
/opt/jans/jans-cli/config-cli.py --operation-id agama-syntax-check --url-suffix qname:
```

You can do some syntax check with this operation-id. It will help to find out syntax error in agama low code projects. 

***Example***:
```
/opt/jans/jans-cli/config-cli.py --operation-id agama-syntax-check --url-suffix qname:"imShakil.co.test"

Server Response:
{
  "error": "mismatched input 'newline' expecting 'Flow'",
  "symbol": "[@0,0:-1='newline',<9>,1:0]",
  "line": 1,
  "column": 0,
  "message": "Syntax error: mismatched input 'newline' expecting 'Flow'\nSymbol: [@0,0:-1='newline',<9>,1:0]\nLine: 1\nColumn: 1"
}
```
