---
tags:
  - administration
  - configuration
  - cache
---



# Cache Configuration

The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](../config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)

=== "Use Text-based UI"

    Cache configuration is not possible in Text-based UI.

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](../config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)



## Using Command Line

To get the details of Janssen command line operations relevant to the cache
configuration, you can check the operations under `CacheConfiguration`
using the command below:

```bash title="Command"
jans cli --info CacheConfiguration
```

It prints below two operations:
```text title="Output"
Operation ID: get-config-cache
  Description: Returns cache configuration.
Operation ID: patch-config-cache
  Description: Patch cache configuration
  Schema: Array of JsonPatch

To get sample schema type jans cli --schema <schema>, for example jans cli --schema JsonPatch
```

### Get Cache Configuration

You can get the current Cache Configuration of your Janssen Server by
performing this operation.

```bash title="Command"
jans cli --operation-id get-config-cache
```

It will show the Cache configuration details.

```json title="Sample Output" linenums="1" 
{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32768,
    "defaultPutExpiration": 60,
    "connectionFactoryType": "DEFAULT"
  },
  "inMemoryConfiguration": {
    "defaultPutExpiration": 60
  },
  "redisConfiguration": {
    "redisProviderType": "STANDALONE",
    "servers": "localhost:6379",
    "defaultPutExpiration": 60,
    "useSSL": false,
    "maxIdleConnections": 10,
    "maxTotalConnections": 500,
    "connectionTimeout": 3000,
    "soTimeout": 3000,
    "maxRetryAttempts": 5
  },
  "nativePersistenceConfiguration": {
    "defaultPutExpiration": 60,
    "defaultCleanupBatchSize": 10000,
    "deleteExpiredOnGetRequest": false,
    "disableAttemptUpdateBeforeInsert": false
  }
}

```

### Patch Cache Configuration

You may need to update Cache configuration, In that case
`patch-config-cache` can be used to modify the cache configuration.

```text
Operation ID: patch-config-cache
  Description: Patch cache configuration
  Schema: Array of JsonPatch

To get sample schema type jans cli --schema <schema>, for example jans cli --schema JsonPatch
```

The `patch-config-cache` operation uses the
[JSON Patch](https://jsonpatch.com/#the-patch) schema to describe
the configuration change. Refer
[here](../config-tools/jans-cli/README.md#patch-request-schema) to know more about
schema.

For instance, to perform a `replace` operation at
`memcachedConfiguration/bufferSize` and change it from `32768` to `32788`,
the JSON Patch data would look like below:

```json title="Input"
[
  {
  "op": "replace",
  "path": "memcachedConfiguration/bufferSize",
  "value": "32788"
  }
]
```

Store the above JSON Patch data in a file, for instance,
`/tmp/patch-cache.json`

Using the above file, perform the operation as below:

```bash title="Sample Command"
jans cli --operation-id patch-config-cache --data /tmp/patch-cache.json
```

```json title="Sample Output" linenums="1"
{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32788,
    "defaultPutExpiration": 60,
    "connectionFactoryType": "DEFAULT"
  },
  "inMemoryConfiguration": {
    "defaultPutExpiration": 60
  },
  "redisConfiguration": {
    "redisProviderType": "STANDALONE",
    "servers": "localhost:6379",
    "defaultPutExpiration": 60,
    "useSSL": false,
    "maxIdleConnections": 10,
    "maxTotalConnections": 500,
    "connectionTimeout": 3000,
    "soTimeout": 3000,
    "maxRetryAttempts": 5
  },
  "nativePersistenceConfiguration": {
    "defaultPutExpiration": 60,
    "defaultCleanupBatchSize": 10000,
    "deleteExpiredOnGetRequest": false,
    "disableAttemptUpdateBeforeInsert": false
  }
}

```

### Quick Patch Operation

In case you need to do a quick patch operation, you can do that also.
For example, let's say we would like to replace `defaultPutExpiration`
value from `nativePersistenceConfiguration`. We can do that simply by
the following command line:

```bash title="Command"
jans cli --operation-id patch-config-cache \
--patch-replace nativePersistenceConfiguration/defaultPutExpiration:90
```

It will change the value with the given one. There are few options to do
such quick patch operations. Please check them out from [here](../config-tools/jans-cli/README.md#quick-patch-operations).

## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring Cache. Endpoint details are published in the 
[Swagger document](./../../reference/openapi.md).