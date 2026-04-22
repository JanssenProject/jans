# OpenSearch Cedarling demo plugin

This is a demo plugin aimed at integrating token-based access control into [OpenSearch](https://opensearch.org). Specifically it is focused on filtering search results obtained in response to search queries sent to any of the endpoints listed [here](https://docs.opensearch.org/docs/latest/api-reference/search-apis/search/). Filtering takes place based on the Cedarling policy provided in the plugin settings.

## Requisites

- OpenSearch 3.6.0
- [Jans Server](https://github.com/JanssenProject/jans/releases) 1.16.0
- A browser with tarp extension installed
- Basic Cedar and OpenSearch knowledge
- Java 21 and `git` for development

**Notes:**

- All commands given in this document were tested using Ubuntu 22. Accommodate to your specific OS
- The OpenSearch installation used to test here was single node and packaged-based. Installers can be found [here](https://docs.opensearch.org/docs/latest/install-and-configure/) for several OSes. Keep the admin password at hand

### Create an .netrc file

To avoid typing the OpenSearch password in `curl` commands over and over, create a `~/.netrc` [file](https://everything.curl.dev/usingcurl/netrc.html). Here is how it might look:

```
machine localhost login admin password secret
```

### Run the health check

With `curl`, issue a request to the cluster health [endpoint](https://docs.opensearch.org/docs/latest/api-reference/cluster-api/cluster-health/) to check status. By default all API requests are served through port `9200`, e.g. `https://localhost:9200`. It may take some administrative work to properly issue requests from the development machine, however sending `curl` requests directly from the server where OpenSearch resides is OK for testing purposes.

In this document, occurrences of `https://oshost` will refer to the root URL where OpenSearch HTTP API can be reached.

## Create and test a cedar policy

Here, the intention is to create a policy that looks like:


```
@id("alumni_restricted_access")
permit(
  principal,
  action in Jans::Action::"Search",
  resource is Jans::student
)
when {
  resource.grad_year < 2026 ||
  (
    context has tokens.jans_userinfo_token &&
    context.tokens.jans_userinfo_token.hasTag("role") &&
    context.tokens.jans_userinfo_token.getTag("role").contains("AdmissionsCounselor")
  )
};
```

with the `student` entity type in the schema:

```
{
    "shape": {
        "type": "Record",
        "attributes": {
            "name": {
                "type": "String"
            },
            "grad_year": {
                "type": "Long"
            }
        }
    }
}
```
<!--
More attributes can be added if desired.
-->

For this, follow the steps found [here](https://docs.jans.io/head/cedarling/quick-start/cedarling-quick-start/#implement-rbac-using-signed-tokens-tbac) as a guide. Note it is highly recommended to use Agama lab's policy designer in this case as well as Tarp for quickly testing the policy. Ensure `student` is added to the `Search` action.

<!--
The `User` resource should be already there if you used . Ensure the `role` attribute is not mandatory.

The easiest way to test the policy is using Tarp. Continue with the steps in the doc [page](https://docs.jans.io/head/cedarling/cedarling-quick-start-tbac/) for this purpose now using the previously setup Jans Server and the policy just created. To get the policy URI in Agama Lab, go to "Policy Stores", click on "Manage" on the corresponding policy row, and then on "Copy link".
-->

For the short of time, there is a readily available policy store [here](https://github.com/jgomer2001/CedarlingQuickstart/releases/download/v0.0.2/tarpDemo.cjar).

## Plugin deployment

In the development machine, clone this repository. `cd` to the repo directory and run `./gradlew assemble -Dopensearch.version=3.6.0`. Ensure `JAVA_HOME` environment points to a Java 21 installation, e.g. `export JAVA_HOME=/path/to/corretto-21`.

`cd` to `build/distributions`. And run:

- `unzip cedarling.zip 'cedarling-java*'`
- `zip -q -d cedarling-java-0.0.0-nightly.jar 'com/sun/jna/*'`
- `zip -u cedarling.zip cedarling-java-0.0.0-nightly.jar`

Transfer the file `cedarling.zip` to the OpenSearch server. In a terminal run the below:

- `/usr/share/opensearch/bin/opensearch-plugin remove cedarling` (not required for first time deployment)
- `/usr/share/opensearch/bin/opensearch-plugin install file:///path/to/cedarling.zip`
- `systemctl restart opensearch.service`

Verify the plugin was effectively deployed by running `curl -n https://oshost/_cat/plugins`. There should be an entry for `cedarling` in the table.

## Plugin configuration

OpenSearch provides endpoints for handling configuration settings of plugins as well as Java classes for reading those. However when it comes to large, complex, and nested JSON hierarchies, like the settings this plugin may require, OpenSearch facilities are not convenient. For this purpose, an additional endpoint was implemented in order to retrieve and supply configuration settings. This allows to pass complex JSON objects without hassle. Under the hood everything is converted into a big string and stored as a single setting in Opensearch.

Check the file [settings.json](https://raw.githubusercontent.com/jgomer2001/pipelines-plugin/refs/heads/main/settings.json) and fill the value corresponding to the policy store URI.

With a complete settings file, transfer it to the server and call the endpoint `/_plugins/cedarling/settings`:

```
curl -n -H 'Content-Type: application/json' -d @settings.json -X PUT https://oshost/_plugins/cedarling/settings
```

The response will contain a boolean value indicating the operation success. The current settings can be retrieved issuing a `GET` to the same endpoint. To check the actual settings OpenSearch stores, use a request like:

```
curl -n https://oshost/_cluster/settings?pretty
```

This plugin settings are dynamic and persistent which means they can be altered any time and survive server restarts. Check this [page](https://docs.opensearch.org/docs/latest/install-and-configure/configuring-opensearch/index/) for more information in these concepts.

## Setup testing data

In the example policy, the `student` entity type is part of the schema. It is expected all resources referenced by policies exist as OpenSearch indices in equivalence. For this, some "students" should be added:

```
curl -n -H 'Content-Type: application/json' --data-binary @records.txt https://oshost/student/_bulk
```

[Here](https://github.com/jgomer2001/pipelines-plugin/raw/refs/heads/main/records.txt) is a sample `records.txt` file. Insert more similar documents varying the `grad_year`. Learn more about Opensearch bulk requests [here](https://docs.opensearch.org/latest/api-reference/document-apis/bulk/).

Issue a request to retrieve the documents added so far:

```
curl -n -H 'Content-Type: application/json' -d @query.json https://oshost/student/_search?pretty
```

[query.json](https://github.com/jgomer2001/pipelines-plugin/raw/refs/heads/main/query.json) contains a [search request](https://docs.opensearch.org/docs/latest/query-dsl/) that matches all documents in the index. 

Note that in real world scenarios, indices already exist and policies are built in conformance afterwards. Every resource to add in the schema should resemble existing indices structures. More specifically, resources should at least contain the attributes which are needed for policy evaluation.

## Setup a search pipeline

This plugins implements a search response processor which must be "attached" to a [search pipeline](https://docs.opensearch.org/docs/latest/search-plugins/search-pipelines/index/). Transfer the file [pipeline.json](https://github.com/jgomer2001/pipelines-plugin/raw/refs/heads/main/pipeline.json) to the OpenSearch server and run:

```
curl -n -H 'Content-Type: application/json' -d @pipeline.json -X PUT https://oshost/_search/pipeline/cedarling_search?pretty
```

This action needs to be performed only **once** regardless of how many plugin redeployments take place.

With this pipeline, search results may now be filtered as per defined Cedarling policies. See the next section.

## Test

Open Tarp. If this is the first time you use it, add a client there beforehand. Then, in the "Authentication Flow" tab, trigger an authentication flow with the following:

- Acr: `basic`
- Scope: `openid` and `profile`
- Check "Display tokens"

After logging in, copy the UserInfo token and paste it in the corresponding section inside file [query_ext.json](https://github.com/jgomer2001/pipelines-plugin/raw/refs/heads/main/query_ext.json). This is an "extended" search query the plugin will have access to so the Cedarling engine can be supplied with tokens and contextual data to make decisions.

Then, run:

```
curl -n -H 'Content-Type: application/json' -d @query_ext.json 'https://oshost/student/_search?pretty&search_pipeline=cedarling_search'
```

The `search_pipeline` is required so the response to the query is intercepted and processed by the plugin which in turn will invoke Cedarling. The response will probably contain less hits than the query issued [earlier](#setup-testing-data) and will come with an `ext` section that reports:

- The amount of hits that passed authorization
- The average decision time per hit

## About development

Once the work to get all of the pieces running is done, making changes to the plugin is rather straightforward: the Java code is in `src` directory and compilation is a matter of issuing `./gradlew compileJava` at the root of the repo hierarchy.

In package-based installations, OpenSearch log is found at `/var/log/opensearch/opensearch.log`. To be able to see the logging statements produced by this plugin, add a line like the below to `/etc/opensearch/opensearch.yml` and restart opensearch (`systemctl restart opensearch.service`): 

```
logger.io.jans.cedarling: trace
```

## Benchmarking

See this [page](./benchmark.md).

## TODO:

- Check linting and javadoc warnings