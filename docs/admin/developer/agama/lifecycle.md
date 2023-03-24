---
tags:
  - administration
  - developer
  - agama
---

# Development lifecycle

In this page an overview of the flow development process is presented. In short, the following are the steps required to build a flow:

- Design and code the flow
- Add the flow to the server
- Upload flow assets (templates, images, Java libs and classes, etc.)
- Trigger an authentication request
- Apply flow updates

As usual, several iterations will take place until you get it right.

!!! Note
    Throughout this document it is assumed you have a standard single-VM Janssen installation

## Design and code

It is up to developers how to design. This will normally require identifying the several "steps" that make up a flow and the conditions upon which "branching" takes place. Also it is important to check already existing flows in the server that may be reused for the purpose.

Agama DSL was made to structure flows only, not for doing general purpose programming. This means developers have to use Java for doing low-level computations. This way, the resulting implementation (in DSL) serves as a depiction of the flow itself, hiding most of the internal details.      

Knowledge of the DSL is a requirement as consequence of the above. Fortunately Agama is small and very easy to learn. Check the DSL basics [here](./dsl.md). Also the ["Hello World"](./quick-start.md#hello-world-sample-flow) sample flow will give you a first impresssion on the language.  

Currently there are no IDE/editor plugins for coding in Agama available. We hope to deliver tools in the future to ease the development experience.

### About crashes

As a flow executes things can go wrong for reasons that developers cannot foresee. A database may have crashed, a connection to an external system may have failed, the flow engine may have some bug, etc. When an abnormal situation is presented, a flow simply crashes.

If a flow crashes, its parent flows (or flow) if they exist, crash as well. Trying to handle crashes involves a lot of planning and work which is too costly and will unlikely account for the so many things that might fail in a real setting. Thus, coding defensively is not recommended. While in Agama is possible to deal with Java exceptions, that feature should be used sparingly.

## Creating a flow

In order to manage flows, developers interact with a small REST API. Click [here](./quick-start.md#getting-an-access-token) to learn about the requisites to access this API and [here](https://github.com/JanssenProject/jans/blob/main/jans-config-api/docs/jans-config-api-swagger.yaml) to check the respective open API definition - locate the endpoints starting with `/jans-config-api/api/v1/agama`.

There are two ways available to add a flow:

|Endpoint|Payload's content type|Method|Description|
|-|-|-|-|
|`/jans-config-api/api/v1/agama/{qname}`|`text/plain`|POST|Creates a flow with the supplied qualified name (*qname*) and source code passed in the payload|
|`/jans-config-api/api/v1/agama`|`application/json`|POST|Creates a flow with the data supplied in the payload|

**Notes:**

- Ensure the tokens used have scope `https://jans.io/oauth/config/agama.write`
- The response of a successful operation returns a 201 status code (i.e. created) and a JSON representation of the created flow - source code not included. If some fields result unfamiliar to you, consult the swagger (open api) document linked above
- A 400 response (i.e. bad request) is generally obtained when the input source code has [syntax problems](#about-syntax-errors)

**Examples:**

- Creates a flow named `com.acme.myflow` using as source the data stored in file `flow.txt`

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -H 'Content-Type: text/plain'
         --data-binary @flow.txt
         https://<your-host>/jans-config-api/api/v1/agama/com.acme.myflow
    ```

- Creates a flow based on the data stored in file `flow.js`

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json'
         -d@flow.js https://<your-host>/jans-config-api/api/v1/agama
    ```

where `flow.js` might look like this:

```
{
  "qname": "com.acme.myflow",
  "source": "Flow com.acme.myflow\n\tBasepath \"\"\n\nin = { name: \"John\" }\nRRF \"index.ftlh\" in\n\nFinish \"john_doe\"",
  "enabled": true
}
```

**Notes:**

- Only `qname` and `source` are required in the JSON payload
- If `enabled` is absent, a `false` value is used by default in the JSON-based endpoint. The text-based version always assumes `true`. This property allows or prevents launching a flow directly from the browser

<!--
,
  "metadata": {
    "displayName": "Biometric authentication",
    "author": "John",
    "description": "This has not been written yet",
    "properties": {
      "api_key": "e2987c51",
      "secret": "change it"
    }
  }
- `properties` in `metadata` refers to the configuration parameters of the flow. See `Configs` keyword [here](./dsl-full.md#header-basics)
-->

## Upload required assets

!!! Note
    For convenience, references to the directory `/opt/jans/jetty/jans-auth/agama` will be replaced by `<AGAMA-DIR>` here onwards.

The `Basepath` directive determines where flow assets reside. Ensure to create the given directory under `<AGAMA-DIR>/ftl`. Probably the same has to be done under `<AGAMA-DIR>/fl`. The difference between `ftl` and `fl` is subtle but important: the former directory must hold Freemarker templates while the latter assets like stylesheets, images and javascript code. This separation avoids Jetty server to expose the raw source code of your templates whose corresponding URL would be quite easy to guess.

As an example, suppose your `Basepath` is `foo` and you have the instructions `RRF index.ftlh` and `RRF bar/index2.ftlh` somewhere in your code. Then your local `<AGAMA-DIR>/ftl` should look like:

```
foo
|- index.ftlh
+- bar
   \- index2.ftlh

```

Say `index.ftlh` has markup like `<img src="bar/me.png">` and `index2.ftlh` has `<link href="my/style.css" rel="stylesheet">` somewhere. This is how `<AGAMA-DIR>/fl` should look like:

```
foo
+- bar
   |- me.png
   \- my
      \- style.css
```
 
### Correspondence to URLs

In practice, assets will map directly under the URL `https://<your-host>/jans-auth/fl`. This means that to access `me.png` per the example above, is a matter of hitting `https://<your-host>/jans-auth/fl/foo/bar/me.png` in your browser. Trying to get access to templates in `<AGAMA-DIR>/ftl` directly is not possible. 

## Running a flow

The quick start guide exemplifies how to [run a flow](./quick-start.md#craft-an-authentication-request) and provides links to [sample applications](./quick-start.md#client-application) that can be used to play around with authentication  in any OpenId Connect-compliant server like Janssen. If necessary, contact your server administrator for the settings required to trigger authentication requests using OpenId Conect (OIDC).

Thinks to keep in mind when testing flows:

- Once you can successfully start your first flow, it is recommended to take a look at the log statements your flow may have produced. Click [here](./logging.md) to learn more. 

- When a flow crashes for some reason, a page is shown summarizing the error details. Sometimes this is enough to fix the problems, however logs tend to offer quite a better insight.

- Authentication flows are usually short-lived. This means the "journey" has to finish within a defined timeframe. If exceeded, users will land at an error page. To learn more about this behavior and how to tweak, visit [Flows lifetime](./flows-lifecycle.md#timeouts).

- The engine often prevents manipulation of URLs so end users cannot mess with the navigation and provoke inconsistent states. This sometimes occurs in web applications when the browser's back button is used. Click [here](./flows-lifecycle.md#flow-advance-and-navigation) to learn more about flow navigation.

## Flow updates

!!! Important
    Ensure you have read about the requisites to access the [REST API](#creating-a-flow)
    
You may like to make modifications and enhancements to your flow. There are two ways to do so:

|Endpoint|Payload's content type|Method|Description|
|-|-|-|-|
|`/jans-config-api/api/v1/agama/source/{qname}`|`text/plain`|PUT|Updates the source code of the flow identified by the given qualified name (*qname*) with the value passed in the payload|
|`/jans-config-api/api/v1/agama/{qname}`|`application/json-patch+json`|PATCH|Modifies the flow identified by the given qualified name (*qname*) using the JSON patch provided in the payload. See [RFC 6902](https://datatracker.ietf.org/doc/html/rfc6902/)|

**Notes:**

- Ensure the tokens used have scope `https://jans.io/oauth/config/agama.write`
- Altering the source code of a flow via PATCH is possible but requires transforming the code into a (one liner) JSON string; this will be a repetitive burden. The PUT version is clearly more straightforward. If you still want to use PATCH, ensure to also modify the integer property `revision` increasing it by one. This will ensure the source changes are effectively picked.
- The response of a successful operation returns a 200 status code and a JSON representation of the updated flow - source code not included
- A 400 response (i.e. bad request) is generally obtained if the supplied source code was has [syntax problems](#about-syntax-errors)

**Examples:**

- Modifies the flow `com.acme.myflow` replacing its source with the data stored in file `flow.txt`

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -H 'Content-Type: text/plain'
         -X PUT --data-binary @flow.txt
         https://<your-host>/jans-config-api/api/v1/agama/source/com.acme.myflow
    ```

- Applies a series of modifications to the flow `com.acme.myflow`: nullifies its description, sets the value of configuration properties, and modifies the creation timestamp to *Aug 8th 2022 23:06:40 UTC*

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json-patch+json'
         -X PATCH -d@patch.js
         https://<your-host>/jans-config-api/api/v1/agama/com.acme.myflow
    ```

where `patch.js` contents are:

```
[
{
  "op": "remove",
  "path": "/metadata/description"
},
{ 
  "op":"replace",
  "path": "/metadata/properties",
  "value": {
    "string_key1": "value_1",
    "number_key2": 10
  }
},
{ 
  "op":"replace",
  "path": "/metadata/timestamp",
  "value": 1660000000000
}
]

```

## Flow retrieval and removal

!!! Important
    Ensure you have read about the requisites to access the [REST API](#creating-a-flow)
    
There are two endpoints for retrieval:

|Endpoint|Method|Description|
|-|-|-|
|`/jans-config-api/api/v1/agama`|GET|Retrieves all flows' data|
|`/jans-config-api/api/v1/agama/{qname}`|GET|Retrieves the data of the flow identified by the given qualified name (*qname*)|

**Notes:**

- Ensure the tokens used have scope `https://jans.io/oauth/config/agama.readonly`
- The response of a successful operation returns a 200 status code with a JSON representation of the flow(s). If some fields result unfamiliar to you, consult the swagger (open api) document linked [above](#creating-a-flow) 
- By default the source code is not included (this may clutter the output considerably). Append `?includeSource=true` to the endpoint URL to have the source in the output

Example:

- Retrieve the data associated to the flow `com.acme.myflow` including its source code

    ```
    curl -k -i -H 'Authorization: Bearer <token>'
         https://<your-host>/jans-config-api/api/v1/agama/com.acme.myflow?includeSource=true
    ```

There is one endpoint to remove a flow:

|Endpoint|Method|Description|
|-|-|-|
|`/jans-config-api/api/v1/agama/{qname}`|DELETE|Removes the flow identified by the given qualified name (*qname*)|

**Notes**:

- Ensure the tokens used have scope `https://jans.io/oauth/config/agama.delete`
- The output of a successful removal is 204 (no content)

Example:

- Remove the the flow `com.acme.myflow`

    ```
    curl -k -i -H 'Authorization: Bearer <token>' -X DELETE
         https://<your-host>/jans-config-api/api/v1/agama/com.acme.myflow
    ```

## About syntax errors

Every time a flow is created or updated, a preliminar syntax check on the flow code is performed. If an error is found, the response will contain details that include the location (line and column) of the problem in the source code.

By design Agama is a [transpiled language](./dsl.md#language-compiler) and transpilation occurs in the background - in the form of a timer task. This task involves processes that go beyond the checks applied upon flow creation or update. When an error is detected at transpilation time, two things can occur:

- If this was the first transpilation attempt, i.e. it's a recently created flow, a message like "Source code has errors" will appear in the browser when launching the flow
- Otherwise, no error is shown and the flow will behave as if no changes had been applied to the flow's code. This helps preserve the last known "healthy" state of your flow so end-users are not impacted

In any case, the cause of the error can be inspected by [retrieving](#flow-retrieval-and-removal) the flow's data and checking the property `codeError`.


## `.gama` files: an alternative for deployment

There is an alternative way to manage flows and is via deployment of `.gama` files. This is a more elaborate technique that allows bundling several flows and their required assets and classes for bulk deployment. Learn more about it [here](#gama-deployment.md).
