---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# Cache Configuration


!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.
    
> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

In the following Main Menu, Options `3, 4, 5, 6 & 7` are for **Cache Configuration**. 
- Cache Configuration
- Cache Configuration – Memcached
- Cache Configuration – Redis
- Cache Configuration – in-Memory
- Cache Configuration – Native-Persistence

Select option 3 to enter in _Cache Configuration_ menu. 
You will get two options as below:

```text
Cache Configuration
-------------------
1 Returns cache configuration
2 Partially modifies cache configuration
```
If you want to view `cache configuration` then choose option 1
from Cache Configuration Menu. It will return cache configuration in details as below:
```json5
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

To update partially, select option 2. then you will be asked to enter `op`, `path` and `value`.

- **__op__** means operation to be done: [`replace`, `move`, `add`, `remove`, `copy`, `test`]
- **__path__** chose path where operation will be performed: for example `memcachedConfiguration/bufferSize`
- **__value__** value that you want update. It can be integer, boolean or string.

At next it will ask `Patch another param?` you can press `y` if you want to update multiple parameters at a time otherwise `n`. After 
then it will show all the patches that are going to be performed. 

> `Continue?` 

If any mistake happens simply press `n` to abort this operation  otherwise press `y` to go with it.
It will show you the updated result. please, see below example, you will get a clear concept on this.

```text

Selection: 2

«The operation to be performed. Type: string»
op: replace

«A JSON-Pointer. Type: string»
path: memcachedConfiguration/bufferSize

«The value to be used within the operations. Type: object»
value  [{}]: 32777

Patch another param? n
[
  {
    "op": "replace",
    "path": "/memcachedConfiguration/bufferSize",
    "value": "32777"
  }
]

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait patching...

{
  "cacheProviderType": "NATIVE_PERSISTENCE",
  "memcachedConfiguration": {
    "servers": "localhost:11211",
    "maxOperationQueueLength": 100000,
    "bufferSize": 32777,
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

## Cache Configuration - Memcached

Do You want to update _Memcached_ only? you can go with this option. _Memcached_ have two options:

```text
Cache Configuration – Memcached
-------------------------------
1 Returns Memcached cache configuration
2 Updates Memcached cache configuration
```

__Option 1__ to get _memcached_ configuration. select 1, and you will get the details:

```text
Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
  "servers": "localhost:11211",
  "maxOperationQueueLength": 100000,
  "bufferSize": 32777,
  "defaultPutExpiration": 60,
  "connectionFactoryType": "DEFAULT"
}
```

__Option 2__ to update _memcached_ configuration. It will ask for each parameter, enter a value or skip to set default.

```text
Selection: 2

Returns Memcached cache configuration
-------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«Server details separated by spaces. Type: string»
servers  [localhost:11211]: 

«Maximum operation Queue Length. Type: integer»
maxOperationQueueLength  [100000]: 

«Buffer Size. Type: integer»
bufferSize  [32777]: 

«Expiration timeout value. Type: integer»
defaultPutExpiration  [60]: 

«The MemcachedConnectionFactoryType Type. Type: string»
connectionFactoryType  [DEFAULT]: 
Obtained Data:

{
  "servers": "localhost:11211",
  "maxOperationQueueLength": 100000,
  "bufferSize": 32777,
  "defaultPutExpiration": 60,
  "connectionFactoryType": "DEFAULT"
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait while posting data ...

{
  "servers": "localhost:11211",
  "maxOperationQueueLength": 100000,
  "bufferSize": 32777,
  "defaultPutExpiration": 60,
  "connectionFactoryType": "DEFAULT"
}
```

## Cache Configuration - Redis

To `get/update` **redis** configuration, select option 5 to enter the menu.
```text
Cache Configuration – Redis
---------------------------
1 Returns Redis cache configuration
2 Updates Redis cache configuration
```

__Option 1__ to get redis cache configuration.

```text
Selection: 1

Returns Redis cache configuration
---------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
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
}
```

__Option 2__ to update the Redis cache configuration. You can fill each property or keep as empty to set default. 
> `Continue?` press `y` to update the Redis configuration.

```text
Selection: 2

Returns Redis cache configuration
---------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«Type of connection. Type: string»
redisProviderType  [STANDALONE]: 

«server details separated by comma e.g. 'server1:8080server2:8081'. Type: string»
servers  [localhost:6379]: 

«Redis password. Type: string»
password: 

«defaultPutExpiration timeout value. Type: integer»
defaultPutExpiration  [60]: 

«Sentinel Master Group Name (required if SENTINEL type of connection is selected). Type: string»
sentinelMasterGroupName: 

«Enable SSL communication between Gluu Server and Redis cache. Type: boolean»
useSSL  [false]: 

«Directory Path to Trust Store. Type: string»
sslTrustStoreFilePath: 

«The cap on the number of \idle\ instances in the pool. If max idle is set too low on heavily loaded systems it is possible you will see objects being destroyed and almost immediately new objects being created. This is a result of the active threads momentarily returning objects faster than they are requesting them causing the number of idle objects to rise above max idle. The best value for max idle for heavily loaded system will vary but the default is a good starting point. Type: integer»
maxIdleConnections  [10]: 

«The number of maximum connection instances in the pool. Type: integer»
maxTotalConnections  [500]: 

«Connection time out. Type: integer»
connectionTimeout  [3000]: 

«With this option set to a non-zero timeout a read() call on the InputStream associated with this Socket will block for only this amount of time. If the timeout expires a java.net.SocketTimeoutException is raised though the Socket is still valid. The option must be enabled prior to entering the blocking operation to have effect. The timeout must be > 0. A timeout of zero is interpreted as an infinite timeout. Type: integer»
soTimeout  [3000]: 

«Maximum retry attempts in case of failure. Type: integer»
maxRetryAttempts  [5]: 
Obtained Data:

{
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
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait while posting data ...

{
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
}
```

## Cache Configuration - In-Memory

To enter `In-Memory` menu select option 6, you will get two options as below:
```text
Cache Configuration – in-Memory
-------------------------------
1 Returns in-Memory cache configuration
2 Updates in-Memory cache configuration
```

__Option 1__ to get the information of In-Memory cache configuration:

```text
Selection: 1

Returns in-Memory cache configuration
-------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
  "defaultPutExpiration": 60
}
```

__Option 2__ to update the information of In-Memory cache configuration:

```text
Selection: 2

Returns in-Memory cache configuration
-------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«defaultPutExpiration timeout value. Type: integer»
defaultPutExpiration  [60]: 
Obtained Data:

{
  "defaultPutExpiration": 60
}
```

## Cache Configuration - Native-Persistence

```text
Cache Configuration – Native-Persistence
----------------------------------------
1 Returns native persistence cache configuration
2 Updates native persistence cache configuration
```

__Option 1__ to get the information of native persistence cache configuration.
```text
Selection: 1

Returns native persistence cache configuration
----------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

{
  "defaultPutExpiration": 60,
  "defaultCleanupBatchSize": 10000,
  "deleteExpiredOnGetRequest": false
}
```

__Option 2__ to update the information of native persistence cache configuration.

```text
Selection: 2

Returns native persistence cache configuration
----------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/cache.readonly

«defaultPutExpiration timeout value. Type: integer»
defaultPutExpiration  [60]: 

«defaultCleanupBatchSize page size. Type: integer»
defaultCleanupBatchSize  [10000]: 

«Type: boolean»
deleteExpiredOnGetRequest  [false]: 
Obtained Data:

{
  "defaultPutExpiration": 60,
  "defaultCleanupBatchSize": 10000,
  "deleteExpiredOnGetRequest": false
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/cache.write
Please wait while posting data ...

{
  "defaultPutExpiration": 60,
  "defaultCleanupBatchSize": 10000,
  "deleteExpiredOnGetRequest": false
}
```


