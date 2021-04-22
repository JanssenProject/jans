
Cache Configuration supports two types of operation through the Single Line command of Janssen CLI.
Let's get the information for Cache Configuration.

`/opt/jans/jans-cli/config-cli.py`

It prints below two operations:
```text

Operation ID: get-config-cache
  Description: Returns cache configuration.
Operation ID: patch-config-cache
  Description: Partially modifies cache configuration.
  Schema: Array of /components/schemas/PatchRequest

To get sample shema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest

```

- **get-config-cache**

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

- **patch-config-cache**

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

When you examine this sample schema, you will see three properties in an object: op, path, and value.
* __op__ operation to be done, one of `add`, `remove`, `replace`, `move`, `copy`, `test`
* __path__ Path of property to be changed. use path separator `/` to change a property inside an object.
* __value__ New value to be assigned for each property defined in `path`

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
root@testjans:/mnt/Academic/Gluu Support/jans-cli# /opt/jans/jans-cli/config-cli.py --operation-id patch-config-cache --data /tmp/patch-cache.json

Getting access token for scope https://jans.io/oauth/config/cache.writee.json
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
}
```

You see `bufferSize` has changed.
That's all for Cache Configuration.

