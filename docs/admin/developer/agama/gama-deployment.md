---
tags:
  - administration
  - developer
  - agama
---

# `.gama` files deployment

Flows management often incurs several tasks such as manual upload of UI templates and web assets plus java source files, and interaction with a REST API for the CRUD of flows themselves. Here, an alternative mechanism for deployment is described where several flows can be supplied alongside their assets and metadata in a single bundle for bulk processing at the server.

This mechanism may facilitate developers' work when multiple flows need to be manipulated. Also it can be used in the  development of tools that may serve as IDEs for Agama.

## What's in an `.gama` file?

A `.gama` file is an atomic deployment unit. It is an archive containing the several flows to deploy on the server plus related files and some metadata. This archive has to be "presented" to an [API endpoint](#api-endpoints) which takes charge of its processing.

The below shows the structure of an extracted `.gama` file (recall it has to originally follow the ZIP format):

```
├── code/
├── lib/           
├── web/
├── project.json   
├── LICENSE        
└── README.md      
```

- `code` directory holds Agama code. Every flow has to reside in a separate file with extension `flow` and with file name matching the qualified name of the flow in question   
- `lib` can contain Java/Groovy source files and `jar` files. These are used to agument the classpath as explained [here](./java-classpath.md)
- `web` is expected to hold all UI templates plus required web assets (stylesheets, images, etc.) of all flows in this bundle
- `project.json` file contains metadata about this bundle, like display name, description, etc. More on this later
- `README.md` file may contain extra documentation in markdown format

**Notes**:

- Except for `code` and `web` directories, all elements in the archive structure are optional
- A `flow` file in `code` may reside at the top level or at any level in a directory structure  
- `jar` files in `lib` are expected to be at top level only. Note these files are ignored in Cloud Native installations
- `java` and `groovy` files in `lib` are expected to be placed in a directory hierarchy according to the Java packages they belong to
- files in `web` must follow a directory structure that is consistent with respect to `Basepath` and `RRF` directives found in the included flows. Note except for templates, all files placed here will be publicly accessible via HTTP once the archive is deployed
- The file extension `gama` is not mandatory, just a mere convention

## Metadata

`project.json` file is expected to contain metadata about the contents of the archive in JSON format. While these details don't have direct influence in the deployment process, data is saved for reference and can be later retrieved by the API for deployment listing purposes. This is an example:

```
{
  "projectName": "A name that will be associated to this deployment",
  "author": "A user handle that identifies you",
  "description": "Other relevant data can go here"
}
```

## Sample file

As an example assume you want to deploy these two flows:

```
Flow test
    Basepath "hello"

in = { name: "John" }
RRF "templates/index.ftlh" in

Log "Done!"
Finish "john_doe"
```

```
Flow com.foods.sweet
    Basepath "recipes/desserts"

...

choice = RRF "selection.ftl"
list = Call com.foods.RecipeUtils#retrieveIngredients choice.meal
...
```

Here is how the archive might look like:

```
├── code/
│   └── test.flow
│   └── com.foods.sweet.flow
├── web/
│   ├── hello/
|   |   └── templates/
│   |       └── index.ftlh
│   |           └── js/
│   |               └── font-awesome.js
|   └── recipes/
│       └── desserts/
│           └── selection.ftl
│           └── logo.png
├── lib/
│   └── com/
│       └── foods/
│           └── RecipeUtils.java
└── project.json
```

## API endpoints

Once a `.gama` file is built the deployment process follows. Here is a typical workflow:

1. Send (POST) the archive contents to the deployment endpoint. Normally a 202 response should be obtained meaning a task has been queued for processing
1. Poll (via GET) the status of the deployment. When the archive has been effectively deployed a status of 200 should be obtained. It may take up to 30 seconds for the process to complete once the archive is sent. This time may extend if there is another deployment in course
1. Test the deployed flows and adjust the archive for any changes required
1. Go to point 1 if needed
1. If desired, a request can be sent to undeploy the flows (via DELETE)

The following tables summarize the available endpoints. All URLs are relative to `/jans-config-api/api/v1`. An OpenAPI document is also available [here](https://github.com/JanssenProject/jans/blob/main/jans-config-api/docs/jans-config-api-swagger.yaml#L110-L254).


|Endpoint -> |`/agama-deployment/list`|
|-|-|
|Purpose|Retrieve a list of deployments in the server. This is not a search, just a paged listing|
|Method|GET|
|Query params|`start` and `count` helps paginating results. `start` is 0-index based. Both params optional|
|Output|Existing deployments (regardless they are thoroughly processed or still being deployed). They are listed and sorted by deployment date|
|Sample output|`{"start":...,"totalEntriesCount":...,"entriesCount":...,"entries":[ ... ]}`|
|Status|200 (OK)|


|Endpoint -> |`/agama-deployment`|
|-|-|
|Purpose|Retrieve details of a single deployment by name|
|Method|GET|
|Query params|`name` - mandatory|
|Sample output|The structure of a deployment is explained below|
|Status|200 (deployment task is finished), 204 (task still in course), 404 (project unknown), 400 (a param is missing)|


|Deployment structure|Notes|Example|
|-|-|-|
|dn|Distinguished name|`jansId=123,ou=deployments,ou=agama,o=jans`|
|id|Identifier of deployment (generated automatically)|`115276`|
|createdAt|Datetime (in UTC) of the instant the deployment task was created (POSTed)|`2022-12-07T21:47:42`|
|taskActive|Boolean value indicating if the deployment task is being currently processed, ie. the .agama file is being scanned, flows analysed, etc.|`false`|
|finishedAt|Datetime (in UTC) representing the instant when the deployment task was finished, or `null` if it hasn't ended|`2022-12-07T21:48:42`|
|details|Extra details, see below|


|Deployment details structure|Notes|Example|
|-|-|-|
|projectMetadata|Includes author, type, description, and project's name - as supplied when the deployment task was created||
|error|A general description of the error (if presented) when processing the task, otherwise `null`|`Archive missing web and/or code subdirectories`|
|flowsError|A mapping of the errors obtained per flow found in the archive. The keys correspond to qualified names. A `null` value indicates the flow was successfully added|`{ "co.acme.example": "Syntax error on line 4", "io.jans.test": null }`|


|Endpoint -> |`/agama-deployment`|
|-|-|
|Purpose|Add or replace an ADS project to the server|
|Method|POST|
|Query params|`name` (the project's name) - mandatory|
|Body|The binary contents of a `.gama` file; example [here](#sample-file). Ensure to use header `Content-Type: application/zip`|
|Output|Textual explanation, e.g. `A deployment task for project XXX  has been queued. Use the GET endpoint to poll status`|
|Status|202 (the task was created and scheduled for deployment), 409 (there is a task already for this project and it hasn't finished yet), 400 (a param is missing)|


|Endpoint -> |`/agama-deployment`|
|-|-|
|Purpose|Undeploy an ADS project from the server. Entails removing flows and assets initally supplied|
|Method|DELETE|
|Query params|`name` (the project's name) - mandatory|
|Status|204 (scheduled for removal), 409 (the project is being deployed currently), 404 (unknown project), 400 (a param is missing)|

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

- Developers are required to restart the authentication server and possibly edit the server's XML descriptor for the classes in the jar files to be effectively picked up
- Steps 3 and 4 are carried out only if all flows passed transpilation successfully
- In Cloud Native environments only one node takes charge of processing a given deployment thoroughly. Other nodes will automatically sync with regards to the files of step 4.

The API also offers undeployment capabilities. Here is how it works:

1. Directories holding templates and assets are removed
1. Source files and jar files are removed, if any
1. Flows are removed from database

The above applies only for the artifacts originally part of the deployment, of course. Again, nodes in Cloud Native installations will sync accordingly.
