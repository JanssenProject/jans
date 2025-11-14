---
tags:
  - administration
  - cedarling
  - intro
  - terms
---

# Cedarling Terms

## Policy Store

A policy store is a JSON encoded file that contains information for the Cedarling's functions. [Agama Lab](https://cloud.gluu.org/agama-lab) can be used to easily create policy stores. Further information on policy stores can be found [here](../cedarling-policy-store.md)

## Schema

A schema is a declaration of the structure of the entity types that you want to support in your application and for which you want Cedar to provide authorization services. For example, an application acting as a gateway to filter HTTP requests may use a schema with structural definitions for requests, tokens, and any other information required to make decisions. A schema may be human-readable or JSON formatted. Schemas created by [Agama Lab](https://cloud.gluu.org/agama-lab) are JSON formatted and base64-encoded by default. The schemas provided in this document are human-readable for convenience.

Further reading: [Cedar Schema Reference](https://docs.cedarpolicy.com/schema/schema.html)

Cedarling comes with a [default schema](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/schema/cedarling_core.cedarschema) which contains a namespace called `Jans` and includes many entities representing common OpenID connect concepts.

## Entity

An entity in Cedar is a stored object that serves as the representation for principals, actions, and resources that are part of your application. In OpenID terminology, an entity can represent a user, a token, an application, or an HTTP Request. Entities are represented as JSON objects in Cedarling. Each entity in Cedarling has at least the following properties:

- `type`: The name of this entity type, as specified in the schema.
- `id`: A unique identifier for the entity.
- Any additional attributes for this entity type as defined in the schema

Continuing the gateway example, the schema may have a definition for an HTTP Request:

```
entity HTTP_Request = {
    "header": {
    "Accept"?: String
    }, 
};
```

And Cedarling would create an `HTTP_Request` entity like so:

```json
{
    "entity_type": "HTTP_Request",
    "id": "1",
    "header": {
        "Accept": "application/json"
    }
}
```

Cedarling creates a number of entities based on the input provided, which are described below.

### Principal

The principal element in a Cedar policy represents a user, service, or other identity that can make a request to perform an action on a resource in your application. The principal entity in Cedarling is an object that is evaluated against the principal element in a cedar policy. 
As Cedarling is primarily concerned with OpenID Connect flows, the principal entity is generally created from the three standard OpenID tokens: Access, ID, and Userinfo. Based on these tokens, Cedarling creates a principal entity that falls into one of these categories:

- Workload: When only the access token is provided. This represents an OAuth client.
- User: When at least two tokens are provided. This represents an end user attempting to be authorized.
- Role: As per [cedar best practices](https://docs.cedarpolicy.com/bestpractices/bp-implementing-roles.html#what-is-a-role), a role in Cedarling is defined as a group of user or workload entities with no additional attributes. This allows us to create role based access control (RBAC) policies which are more performant than attribute based access control. The default schema provides definition for `Role` and Cedarling automatically creates the `Role` entity if this claim is present in one or more tokens.

In case no token is provided or the use case is not based on OpenID, Cedarling supports unsigned authorization, where the principal entity is manually provided by the user in the form of a list of JSON objects. 

For example, given a schema defining a user:

```
entity HTTP_Request = {
    "sub": String,
    "role": Set<String>,
};
```
We can create a User entity:
```json
[
  {
    "type": "User",
    "id": "some_id",
    "sub": "some_sub",
    "role": ["admin"]
  }
]
```

### Action

The action element in a Cedar policy is a list of the operations in your application for which this policy statement controls access. Action is different from other entities in that only the name of the action and the namespace where it is located is sufficient to create this entity, as the rest are handled by Cedarling. Therefore:

Given a schema with the namespace `Jans` and an action representing a GET request:

```
namespace Jans {
    action GET appliesTo {
      principal: [Workload],
    };
}
```

The input to create the Action entity is this string: `Jans::Action::"GET"`

### Resource

The resource element in a Cedar policy is a resource defined by your application that can be accessed or modified by the specified action. For Cedarling, the Resource entity is the object on which the Principal performs the Action. For example, a Workload entity (Principal) performing a GET request (Action) on an HTTP endpoint (Resource). The Resource entity must be created by providing JSON input as defined in the schema.

Given the schema for an `HTTP_Request` resource:

```
entity HTTP_Request = {
    "header": {
    "Accept"?: String
    }, 
};
```

We create the resource entity:

```json
{
    "entity_type": "HTTP_Request",
    "id": "1",
    "header": {
        "Accept": "application/json"
    }
}
```

### Context

The context input parameter is used to provide details specific to a request, such as the date and time the request was sent, the IP address the request originated from, or whether the user was authenticated using a multi-factor authentication device. For Cedarling, the Context is optional and `null` or its equivalents may be passed in case there is no such value. Similar to Action, the `type` and `id` are not needed to construct Context and the rest is passed as JSON according to the schema.

Given the schema definition:

```
type Context = {
    network?: String,
    network_type?: String,
    user_agent?: String,
    operating_system?: String,
    device_health?: Set<String>,
    current_time?: Long,
    geolocation?: Set<String>,
    fraud_indicators?: Set<String>,
};
```

The Context is constructed:

```json
{
    "current_time": int(time.time()),
    "device_health": ["Healthy"],
    "fraud_indicators": ["Allowed"],
    "geolocation": ["America"],
    "network": "127.0.0.1",
    "network_type": "Local",
    "operating_system": "Linux",
    "user_agent": "Linux"
}
```

### JWT entities

When performing signed authorization (one or more OpenID tokens are provided) and when token definitions are available in the schema, Cedarling creates an entity corresponding to each type of token and adds them to the User or Workload entities. These entities can then be referred to during policy creation.

For example, given an access token, the workload will have the access token entity. Thus we can refer to it in a policy:

```
permit(
  principal is Jans::Workload,
  action,
  resource
)
when {
    principal has access_token.scope &&
    principal.access_token.scope.contains("profile")
};
```
