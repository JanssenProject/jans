## Jans Lock Token Stream Service

To enable Jans Auth PubSub messages 2 configuration should be configured:
  * Lock messaging connection details
  * Jans Auth messaginbg configuration
  
It's possible to modify them with jans-config-api.

First configuration Jans stores in `ou=configuration,o=jans` entry. This is reusable configuration. Other application which uses `jans-core-message.jar` library also should use it.
Default configuration after CE install when messaging API is not enabled:

```
jansMessageConf:
{
  "messageProviderType": "DISABLED",
  "postgresConfiguration": {
    "connectionUri": "jdbc:postgresql://localhost:5432/postgres",
    "dbSchemaName": "public",
    "authUserName": "postgres",
    "authUserPassword": "",
    "messageWaitMillis": 100,
    "messageSleepThreadTime": 200
  },
  "redisConfiguration": {
    "servers": "localhost:6379"
  }
}
```

It has 2 section for PostgreSQL (`postgresConfiguration`) and Redis (`redisConfiguration`) connection details.

`messageProviderType` can have 3 values: DISABLED, REDIS, POSTGRES

Full list of properties defined in jans-config-api-swagger.yaml.


Second configuration is needed for Jans Auth. In current version Jans Auth calls message API after id_token persistence/removal from/to DB from `GarntService`. Jans Messages API library provides generic API to hide actual PubSub messages publishing/receiving implementation.

This is default configuration after server install:

```
    "lockMessageConfig": {
        "enableIdTokenMessages" : false,
        "idTokenMessagesChannel": "id_token"
    }
```
First property control sending messages to PubSub server. Second one `idTokenMessagesChannel` specify channel name to send id_token messages.

The format of messages which Jans Auth issue is JSON. And it has minimum data for quick processing. Here is message pattern:

```
{"tknTyp" : "id_token", "tknCde" : "id_token identifier", "tknOp" : "add/del"}
```

Also In order to enable messages it's mandatory to enable id_token persistence because Lock server loads tokens from DB by their identifiers.


Note: For demo purposes in Jans CE with PostgreSQL and Lock server selected to install messages enabled by default. In order to start using it we only need to enable id_token persistence in Jans Auth.

