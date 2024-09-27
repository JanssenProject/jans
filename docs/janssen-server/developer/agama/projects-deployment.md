---
tags:
  - administration
  - developer
  - agama
---

# Agama projects deployment

The Agama framework defines a file [format](../../../agama/gama-format.md) to package and distribute Agama projects. Here we describe the process of deployment in the Jans Agama engine.

## Workflow

Deployment occurs through a REST API. Here is a typical workflow once a `.gama` file is ready:

1. Send (POST) the archive contents to the deployment endpoint. Normally a 202 response should be obtained meaning a task has been queued for processing
1. Poll (via GET) the status of the deployment. When the archive has been effectively deployed a status of 200 should be obtained. It may take up to 30 seconds for the process to complete once the archive is sent. This time may extend if there is another deployment in course
1. Optionally supply configuration properties for flows if needed. This is done via PUT to the `/configs` endpoint. The response of the previous step may contain descriptive/sample configurations that can be used as a guide
1. Test the deployed flows and adjust the archive for any changes required
1. Go to point 1 if needed. If configuration properties were previously set and no changes are required in this regard, step 3 can be skipped 
1. If desired, a request can be sent to undeploy the flows (via DELETE)

The following tables summarize the available endpoints. All URLs are relative to `/jans-config-api/api/v1`. An OpenAPI document is also available [here](https://github.com/JanssenProject/jans/blob/main/jans-config-api/docs/jans-config-api-swagger.yaml#L177).


|Endpoint -> |`/agama-deployment`|
|-|-|
|Purpose|Retrieve a list of deployments in the server. This is not a search, just a paged listing|
|Method|GET|
|Query params|`start` and `count` helps paginating results. `start` is 0-index based. Both params optional|
|Output|Existing deployments (regardless they are thoroughly processed or still being deployed). They are listed and sorted by deployment date|
|Sample output|`{"start":...,"totalEntriesCount":...,"entriesCount":...,"entries":[ ... ]}`|
|Status|200 (OK)|


|Endpoint -> |`/agama-deployment/{name}`|
|-|-|
|Purpose|Retrieve details of a single deployment by name|
|Method|GET|
|Path params|`name`|
|Sample output|The structure of a deployment is explained below|
|Status|200 (deployment task is finished), 204 (task still in course), 404 (project unknown)|


|Deployment structure|Notes|Example|
|-|-|-|
|dn|Distinguished name|`jansId=123,ou=deployments,ou=agama,o=jans`|
|id|Identifier of deployment (generated automatically)|`115276`|
|createdAt|Datetime (in UTC) of the instant the deployment task was created (POSTed)|`2022-12-07T21:47:42`|
|taskActive|Boolean value indicating if the deployment task is being currently processed, ie. the `gama` file is being scanned, flows analysed, etc.|`false`|
|finishedAt|Datetime (in UTC) representing the instant when the deployment task was finished, or `null` if it hasn't ended|`2022-12-07T21:48:42`|
|details|Extra details, see below|


|Deployment details structure|Notes|Example|
|-|-|-|
|projectMetadata|Includes author, type, description, project's name, and example configuration - as supplied when the deployment task was created||
|error|A general description of the error (if presented) when processing the task, otherwise `null`|`Archive missing web and/or code subdirectories`|
|flowsError|A mapping of the errors obtained per flow found in the archive. The keys correspond to qualified names. A `null` value indicates the flow was successfully added|`{ "co.acme.example": "Syntax error on line 4", "io.jans.test": null }`|
|libs|A listing of paths to `java`, `groovy`, or `jar` files included in the project archive||


|Endpoint -> |`/agama-deployment/{name}`|
|-|-|
|Purpose|Add or replace an Agama project to the server|
|Method|POST|
|Path params|`name` (the project's name)|
|Body|The binary contents of a `.gama` file; example [here](../../../agama/gama-format.md#sample-project). Ensure to use header `Content-Type: application/zip`|
|Query params|`autoconfigure` - passing `true` will make this project be configured with the sample configurations found in the provided binary archive (`configs` section of [project.json](../../../agama/gama-format.md#metadata)). This param should rarely be passed: use only in controlled environments where the archive is not shared with third parties|
|Output|Textual explanation, e.g. `A deployment task for project XXX has been queued. Use the GET endpoint to poll status`|
|Status|202 (the task was created and scheduled for deployment), 409 (there is a task already for this project and it hasn't finished yet), 400 (a param is missing)|


|Endpoint -> |`/agama-deployment/configs/{name}`|
|-|-|
|Purpose|Retrieve the configurations associated to flows that belong to the project of interest. The project must have been already processed fully|
|Method|GET|
|Path params|`name` (the project's name)|
|Output|A JSON object whose properties are flow names and values correspond to configuration properties defined (JSON objects too)|
|Status|200 (successful response), 204 (this project is still in course of deployment), 404 (unknown project)|


|Endpoint -> |`/agama-deployment/configs/{name}`|
|-|-|
|Purpose|Set or replace the configurations associated to flows that belong to the project of interest. The project must have been already processed fully|
|Method|PUT|
|Path params|`name` (the project's name)|
|Body|JSON payload|
|Output|A JSON object whose properties are flow names and values correspond to a boolean indicating the success of the update for the given flow|
|Status|200 (successful response), 204 (this project is still in course of deployment), 404 (unknown project), 400 (a param is missing)|


|Endpoint -> |`/agama-deployment/{name}`|
|-|-|
|Purpose|Undeploy an Agama project from the server. Entails removing flows and assets initally supplied|
|Method|DELETE|
|Path params|`name` (the project's name)|
|Status|204 (scheduled for removal), 409 (the project is being deployed currently), 404 (unknown project)|

### Endpoints access

API operations are protected by Oauth2 scopes this way:

- GET: `https://jans.io/oauth/config/agama.readonly`
- POST:  `https://jans.io/oauth/config/agama.write`
- DELETE: `https://jans.io/oauth/config/agama.delete`

## Internals of deployment

This section offers details on how the server deploys a `.gama` file. In summary five steps take place once the deployment task is picked up:

1. Input payload validation
1. Flows transpilation
1. Transfer of libraries
1. Transfer of assets and source files
1. Task finalization

After the initial validation of structure takes place, the `code` directory is scanned for `flow` files and every flow is transpiled and added to the database if its transpilation was successful. 

Next, `jar` files found are transferred to the server's custom libs directory - this only applies for VM-based installations.

Then a transfer of templates and assets from `web` directory followed by copying Groovy/Java sources (from `lib`) takes place. Finally the deployment task is marked as finalized and a status summary saved to database for later retrieval.

**Notes:**

- Developers are required to restart the authentication server for the classes in jar files to be effectively picked up
- Steps 3 and 4 are carried out only if all flows passed transpilation successfully
- In Cloud Native environments only one node takes charge of processing a given deployment thoroughly. Other nodes will automatically sync with regards to the files of step 4.

The API also offers undeployment capabilities. Here is how it works:

1. Directories holding templates and assets are removed
1. Source files and jar files are removed, if any
1. Flows are removed from database

The above applies only for the artifacts originally part of the deployment, of course. Again, nodes in Cloud Native installations will sync accordingly.
