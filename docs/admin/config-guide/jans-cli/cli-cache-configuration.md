---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

# Cache Configuration

> Prerequisite: Know how to use the Janssen CLI in [command-line mode](cli-index.md)

Cache Configuration supports two types of operation through the Single Line command of Janssen CLI.
Let's get the information for Cache Configuration.

```
/opt/jans/jans-cli/config-cli.py --info CacheConfiguration
```

It prints below two operations:
```text

Operation ID: get-config-cache
  Description: Returns cache configuration.
Operation ID: patch-config-cache
  Description: Partially modifies cache configuration.
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest

```

Table of Contents
=================

- [Cache Configuration](#cache-configuration)
- [Table of Contents](#table-of-contents)
  - [Get Cache Configuration](#get-cache-configuration)
  - [Patch Cache Configuration](#patch-cache-configuration)
  - [Quick Patch Operation](#quick-patch-operation)

## Get Cache Configuration

You can get the current Cache Configuration of your Janssen Server by performing this operation.

```commandline
/opt/jans/jans-cli/config-cli.py --operation-id get-config-cache
```

It will show the Cache configuration with details.

```text
{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32768,
    "defaultPutExpiration": 60,
    "connectionFactoryType": "DEFAULT"
  },
  "redisConfiguration": {
    "redisProviderType": "STANDALONE",
    "servers": "localhost:6379",
    "password": null,
    "defaultPutExpiration": 60,
    "sentinelMasterGroupName": null,
    "useSSL": false,
    "sslTrustStoreFilePath": null,
    "maxIdleConnections": 10,
    "maxTotalConnections": 500,
    "connectionTimeout": 3000,
    "soTimeout": 3000,
    "maxRetryAttempts": 5
  },
  "inMemoryConfiguration": {
    "defaultPutExpiration": 60
  },
  "nativePersistenceConfiguration": {
    "defaultPutExpiration": 60,
    "defaultCleanupBatchSize": 10000,
    "deleteExpiredOnGetRequest": false
  }
}
```

## Patch Cache Configuration

You may need to update Cache configuration, In that case `patch-config-cache` can be used to modify cache configuration.

```text
Operation ID: patch-config-cache
  Description: Partially modifies cache configuration.
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest
```

Let's see the sample schema of cache configuration.

```text
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest > /tmp/patch-cache.json

{
  "op": "add",
  "path": "string",
  "value": {}
}
```

Let, We want to replace `memcachedConfiguration/bufferSize`:

We can edit this json as follows (remember to make it an array):

```commandline
nano /tmp/patch-cache.json

[
  {
  "op": "replace",
  "path": "memcachedConfiguration/bufferSize",
  "value": "32788"
  }
]
```

Now, let's do the operation: 
```text
/opt/jans/jans-cli/config-cli.py --operation-id patch-config-cache --data /tmp/patch-cache.json


Getting access token for scope https://jans.io/oauth/config/cache.write
Server Response:
{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32788,
    "defaultPutExpiration": 60,
    "connectionFactoryType": "DEFAULT"
  },
  "redisConfiguration": {
    "redisProviderType": "STANDALONE",
    "servers": "localhost:6379",
    "password": null,
    "defaultPutExpiration": 60,
    "sentinelMasterGroupName": null,
    "useSSL": false,
    "sslTrustStoreFilePath": null,
    "maxIdleConnections": 10,
    "maxTotalConnections": 500,
    "connectionTimeout": 3000,
    "soTimeout": 3000,
    "maxRetryAttempts": 5
  },
  "inMemoryConfiguration": {
    "defaultPutExpiration": 60
  },
  "nativePersistenceConfiguration": {
    "defaultPutExpiration": 60,
    "defaultCleanupBatchSize": 10000,
    "deleteExpiredOnGetRequest": false
  }
```

You see `bufferSize` has changed. You may want to know more about patching cache configuration. Please, have a look to [this link](cli-index.md#patch-request-schema) to know more about how you can modify the schema file.


## Quick Patch Operation

In case you need to do a quick patch operation, you can do that also. For example, let's say we would like to replace `defaultPutExpiration` value from `nativePersistenceConfiguration`. We can do that simply by the following command line:

```
/opt/jans/jans-cli/config-cli.py --operation-id patch-config-cache --patch-replace nativePersistenceConfiguration/defaultPutExpiration:90
```

It will change the value with given one. There are few option to do such quick patch operations. Please check them out from [here](cli-index.md#quick-patch-operations).

