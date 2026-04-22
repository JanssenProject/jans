# Benchmarks

To compare the performance of regular queries vs. queries filtered via this plugin, benchmarking tests were designed. This is what a test does:

- Removes the index in question entirely (e.g. `student`)
- Loads in bulk a set of JSON documents. Every document is generated with a (short) random name, a random graduation year (uniformly distributed between 2024 and 2027), and a GPA (random floating number between 0 and 5) 
- Runs five queries that return documents with GPAs matching the intervals [0, 1), [1, 2), etc. <!--Before this, a preliminary warmup query is issued for the interval [5, 6) and its result discarded: it was observed that after the bulk is performed, the very first query takes a very long time compared to subsequent queries -->
- The average query response time is computed. This does not include network latency - only server side processing

The test is run twice, one without Cedarling (regular OpenSearch query), and one with the plugin. To avoid bias due to caching or other factors, the database is restarted before executing every test. The average decision time per document is also reported when using the plugin. <!--This allows to discriminate the overhead introduced by Cedarling and the plugin separately.  -->

Details like the server to point to, index name, and the number of random entries to generate, among others, are parameterizable.

## Performance data

The following data were obtained in a setup using a [Digital Ocean](https://slugs.do-api.dev/) Basic VM (s-4vcpu-8gb) with Ubuntu 22, OpenSearch 3.6.0 (single node, package-based installation with default configuration), and (local) Jans Server 1.16.0 (AS and database components only):

|Measurement|Value|
|-|-|
|Average query response time (regular)|114.6 ms|
|Average query response time (plugin)|4837.5 ms|
|Ratio|42.21|
|Average Cedarling authz time per document|2.2ms|

The number of documents (bulk size) per test was 10,000. Given the uniform distribution of documents among GPA values, each of the five queries returned approximately 2,000 hits, which leads to the following average query response times per document:

- Regular: 0.0573 ms
- Plugin: 2.41875 ms

The above shows that most of the processing time is due to Cedarling authorization while the rest of plugin code only accounts for 9% of the overhead (0.21875 ms). 

These tests force the retrieval of all matching documents at once every time, however in practice many apps will process query responses in small pages - the default search endpoint page size in OpenSearch is 10. This means despite the big ratio obtained (42.21), using Cedarling authorization is promising, specifically when retrieving data in small batches of documents is fine. For example 40 documents could be retrieved in approximately 97 milliseconds in the circumstances described in this document.

## How to run

For the interested, the below are the instructions to run a test:

- Ensure to deploy and configure the plugin as explained in the [README](./README.md) <!--. At the top level of the JSON settings, add `"skipHits": true`. This will make the plugin omit the serialization of the results (hits) in the response. This avoids transfering a lot of data through the network and reduces the running time of the Java test considerably without effects in the computations of performance metrics -->
- `cd` to the root directory of this repo
- Edit the file `src/test/resources/testng.properties` accordingly. For `entries`, a value like `100000` (one hundred thousand documents) is OK and will only transfer around 15MB while tests run
- If the plugin will be in use for this particular test, set property `useCedarling` to `true`, and edit the accompanying file `query.json` supplying the tokens - these can be obtained via Tarp as mentioned in the README file
- Restart OpenSearch
- Run `./gradlew test`. Ensure the certificate keystore of Java trusts the certificate that protects the OpenSearch REST API endpoints
- The report in HTML format can be found under `build/reports/tests`. The last lines of the standard output will contain relevant metrics
