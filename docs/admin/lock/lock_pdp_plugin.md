---
tags:
 - administration
 - lock
 - opa
---

# Developer Guide: Lock PDP Client 

## Janc core messages API

Jans provides `jans-core-message` library to work with PubSub servers. It has minimum dependencies and can be integrated in other projects. Current implementation supports 2 servers PostgreSQL and Redis.

To work with PostgreSQL/Redis servers it has [MessageInterface](https://github.com/JanssenProject/jans/blob/main/jans-core/message/src/main/java/io/jans/service/message/provider/MessageInterface.java)/[MessageProvider](https://github.com/JanssenProject/jans/blob/main/jans-core/message/src/main/java/io/jans/service/message/provider/MessageProvider.java) implementations. It has methods to subscribe and publish messages defined in:

```
public abstract class MessageProvider<T> implements MessageInterface {

	/*
	 * Delegate internal connection object
	 */
	public abstract T getDelegate();

	public abstract MessageProviderType getProviderType();

	public abstract void shutdown();

}

public interface MessageInterface {

	void subscribe(PubSubInterface pubSubAdapter, String... channels);

	void unsubscribe(PubSubInterface pubSubAdapter);

	boolean publish(String channel, String message);

}

```

All methods defined in this interface `MessageInterface` are asynchronous.
`MessageProvider` provides additional methods needed to determine it type and handle correct shutdown.

[PubSubInterface](https://github.com/JanssenProject/jans/blob/main/jans-core/message/src/main/java/io/jans/service/message/pubsub/PubSubInterface.java) here defines call back class which API should call on messaging events:

To add new PubSub support library should add new `MessageProvider` implementation.

```
public interface PubSubInterface {

	void onMessage(String channel, String message);

	void onSubscribe(String channel, int subscribedChannels);

	void onUnsubscribe(String channel, int subscribedChannels);

}
```

## New PDP support


To add new PDP support jar file should has 2 classes implementation: `MessageConsumer` and `PolicyConsumer`. Lock has 2 implementations of these interfaces [OpaMessageConsumer](https://github.com/JanssenProject/jans/blob/main/jans-lock/service/src/main/java/io/jans/lock/service/consumer/message/opa/OpaMessageConsumer.java) and [OpaPolicyConsumer](https://github.com/JanssenProject/jans/blob/main/jans-lock/service/src/main/java/io/jans/lock/service/consumer/policy/opa/OpaPolicyConsumer.java). After adding jar with new PDP support Lock should automatically find these implementations at startup.

```
public abstract class MessageConsumer implements MessageConsumerInterface {

	public abstract String getMessageConsumerType();

	public abstract void destroy();

}

public interface MessageConsumerInterface extends PubSubInterface {

	public boolean putData(String message, JsonNode messageNode);

}

```

Reference implementation for OPA is [OpaMessageConsumer](https://github.com/JanssenProject/jans/blob/main/jans-lock/service/src/main/java/io/jans/lock/service/consumer/message/opa/OpaMessageConsumer.java). It subscribes to message channels and send data to OPA through Rest API.

```
public abstract class PolicyConsumer implements MessagePolicyInterface {

	public abstract String getPolicyConsumerType();

	public abstract void destroy();

}

public interface MessagePolicyInterface {
	
	public boolean putPolicies(String sourceUri, List<String> policies);

	public boolean removePolicies(String sourceUri);

}
```

Reference implementation for OPA is [`OpaPolicyConsumer`](https://github.com/JanssenProject/jans/blob/main/jans-lock/service/src/main/java/io/jans/lock/service/consumer/policy/opa/OpaPolicyConsumer.java). `PolicyDownloadService` calls this service to send updates to OPA through Rest API.
