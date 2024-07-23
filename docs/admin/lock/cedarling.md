---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
---

## Cedarling Authorization

The Cedarling is a local, autonomous Policy Decision Point, or "PDP". It runs as a local 
WebAssembly ("WASM") component--you can call it directly in the browser from a JavaScript 
function. It can also run as a cloud function to provide authorization for server-side apps. 
With each authorization call, the Cedarling has all the policies and data it 
needs to make a fast, local decision. The Cedarling's authorization function is *deterministic*.
The Cedarling always returns either `permit` or `forbid`. You will never get an error 
indicating a network timeout, or a divide by zero error. It is also very fast.

![](../../assets/lock-cedarling-diagram-1.jpg)

In a JavaScript browser framework, the Cedarling loads its Policy Store during initialization, as a 
static JSON file or fetched via REST. Developers may consider the Cedarling Policy Store as part of 
the code. Externalizing the policies makes it easier to audit the security features and 
controls of an application. The Cedarling enables developers to create complex, contextual policies
without cluttering application code with lots of `if` - `then` statements. Importantly, the Cedarling
creates an audit log of all decisions by an application to `permit` or `forbid` actions. In an 
enterprise deployment, this audit log is sent for central archiving.

Where does the Cedarling get the data for policy evaluation? The data is contained in the 
authorization request itself which has the OAuth and OpenID JWTs and details about the resource 
and requested action. Most modern applications rely on a federated identity provider or "IDP". 
Leveraing the JWT tokens to identify the person and software making the request

![](../../assets/lock-cedarling-diagram-2.jpg)

Two JWT tokens in particular are typical: (1) an OpenID Connect id_token and (2) an OAuth access 
token. The id_token represents a user authentication event, and the access token represents a 
client authentication event. The Cedarling can trust the id_token and access token to extract the User, 
Role and Client pricipals. The tokens also contain other interesting contextual data. An OpenID 
Connect id_token JWT, as a record of an authentication event, tells you who authenticated, when 
they authenticated, how they authenticatated, and other claims like the subject's Roles. An OAuth 
Access Token JWT can tell you information about the software that obtained the JWT, its extent 
of access as defined by the OAuth Authorization Server (*i.e.* the values of the `scope` claim), or 
other claims--domains frequently enhance the access token to contain business specific data needed 
for policy evaluation. If an OpenID Userinfo token is sent to the cedarling, it is combined with the
id_token to paint a fuller picture of the User's claims. 

The Cedarling, as its name suggests, enables you to define the security rules for your application 
in [Cedar](https://www.cedarpolicy.com/en) policy syntax. Cedar was invented by Amazon for their 
[Verified Permission](https://aws.amazon.com/verified-permissions/) service. It uses the **PARC** 
syntax: **P**rincipal, **A**ction, **R**esource, **C**ontext.  Principal-Action-Resource is typical
for most authorization solutions. For example, you may have a policy that says *Admins* can *write*
to the *config* folder. In this example, the *Admin* Role is the Principal, *write* is the Action,
and the *config* folder is the Resource. The Context is used to specify information about the
enivironment, like the time of day or network address.

The Cedarling authorizes a person using a certain piece of software to do something. From 
a logical perspective, `person_allowed AND client_allowed` must be `True`. While this seems pretty
simple, a person may be either explicitly allowed, or have a role that enables access. For example, 
`person_allowed` may be equal to `True` if `user=mike OR role=SuperUser`. 

![](../../assets/lock-cedarling-diagram-3.jpg)

The JWT's, Action, Resource and Context is sent by the application in the authorization request. For 
example, this is a sample request from a hypothetical JS application:

```
input = { 
           "access_token": "eyJhbGc....", 
           "id_token": "eyJjbGc...", 
           "userinfo_token": "eyJjbGc...",
           "resource": {"Ticket": {"id": "12345", "creator": "foo@bar.com", "organization": "Acme"}},
           "action": "View",
           "context": {
                       "ip_address": "54.9.21.201",
                       "network_type": "VPN",
                       "user_agent": "Chrome 125.0.6422.77 (Official Build) (arm64)",
                       "time": "1719266610.98636",
                      }
         }

decision_result = authz(input)
```

## Cedarling Token Validation

The Cedarling can validate the signatures of the JWTs for developers, by setting the 
`CEDARLING_JWT_VALIDATION` environment variable to `True`. For testing, developers can set this
property to `False` and submit an unsigned JWT, for example one you generate with 
[JWT.io](https://jwt.io). Or developers may prefer to validate the signatures in code--that's ok.

On initiatilization, the Cedarling downloads the public keys of the Trusted IDPs specified in the 
Cedarling policy store. Because all JWT's have an `iss` claim, this is used to determine which keys 
to use for token signature validation. 

In an enterprise deployment, the Cedarling can also check if a JWT has been revoked. The Cedarling
uses a mechanism described in the 
[OAuth Status Lists](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/)
draft. This might be handy for use cases where a token revocation needs to be communicated 
immediately, such as an account takeover situation, or an implementation of a one-time transactions
in a cluster of web servers. Jans Auth Server supports the [Global Token Revocation](https://datatracker.ietf.org/doc/draft-parecki-oauth-global-token-revocation/) OAuth draft. This is how a client can inform the OAuth Server that a given token should be revoked. 

Here is a summary of the ways the Cedarling may validate a JWT, depending on your bootstrap properties:
* Validate signature from Trusted Issuer 
* Discard id_token if `aud` does not match access_token `client_id` 
* Discard Userinfo token not associated with a `sub` from the id_token
* If Cedarling is Locked, check token status
* Check access token and id_token `exp` and `nbf` claims if time sent in Context

![](../../assets/lock-cedarling-diagram-4.jpg)

## Policy Authoring 

The eaisest way to author your policy store is to use the Policy Designer in [Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and trusted IDPs and
to publish a policy store to any Github repository to which you have access.

## Testing the Cedarling

You can perform end to end testing by running the Cedarling in your JS browser application. If
you are using a server side application (e.g. a Wordpress application), you'll need to deploy
the Cedarling as a cloud function. Remember to run the Cedarling init function when your application 
starts. This is necessary to load the policy store and to download the current public keys from 
an IDP if you are using the Cedarling for token validation. If you want to use roles, make sure to 
populate the User.role claim in your id_token or Userinfo token. Call the authz function when 
you want the Cederling to opine on a security RBAC decision.

## Cedarling Policy Store

The Cedarling Policy Store is a JSON file that contains all the data the Cedarling needs to verify JWT tokens and evaluate policies:

1. Cedar Schema - JSON formatted Schema file
2. Cedar Policies - JSON formatted Policy Set file 
3. Trusted Issuers - JSON file with below syntax

By convention, the filename is `cedarling_store.json`. The JSON schema looks like this:

```
{
    "policies": {...},
    "schema": {...},
    "trusted_idps": []
}
```

### Trusted Issuer Schema

This is a hypothetical example.

```
[
{"name": "Acme", 
 "Description": "Acme IDP", 
 "openid_configuration_endpoint": "https://acme.com/.well-known/openid-configuration",
 "access_tokens": {"trusted": True}, 
 "id_tokens": {"trusted":True, "principal_identifier": "email"},
 "userinfo_tokens": {"trusted": True, "role_mapping": "role"}
},
...
]
```

## Cedarling Bootstrap Properties

* **`CEDARLING_APPLICATION_NAME`** : Human friendly identifier for this application

* **`CEDARLING_POLICY_STORE_URI`** : Location of policy store JSON, used if policy store is not local, or retreived from Lock Master.

* **`CEDARLING_JWT_VALIDATION`** : Enabled | Disabled 

* **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`** : ....

* **`CEDARLING_REQUIRE_AUD_VALIDATION`** : Enabled | Disabled. Controls if Cedarling will discard id_token without an access token with the corresponding client_id.

* **`CEDARLING_LOG_LEVEL`** : Controls the verbosity of Cedar logging.

The following bootstrap properties are only needed for enterprise deployments.

* **`CEDARLING_LOCK`** : Enabled | Disabled. If Enabled, the Cedarling will connect to the Lock Master for policies, and subscribe for SSE events. 

* **`CEDARLING_LOCK_MASTER_CONFIGURATION_URI`** : Required if `LOCK` == `Enabled`. URI where Cedarling can get JSON file with all required metadata about Lock Master, i.e. `.well-known/lock-master-configuration`.

* **`CEDARLING_LOCK_SSA_JWT`** : SSA for DCR in a Lock Master deployment. The Cedarling will validate this SSA JWT prior to DCR.

* **`CEDARLING_POLICY_STORE_ID`** : The identifier of the policy stored needed only for Lock Master deployments.

* **`CEDARLING_AUDIT_LOG_INTERVAL`** : How often to send log messages to Lock Master (0 to turn off trasmission)

* **`CEDARLING_AUDIT_HEALTH_INTERVAL`** : How often to send health messages to Lock Master (0 to turn off transmission)

* **`CEDARLING_AUDIT_TELEMETRY_INTERVAL`** : How often to send telemetry messages to Lock Master (0 to turn off transmission)

* **`CEDARLING_DYNAMIC_CONFIGURATION`** : Enabled | Disabled, controls whether Cedarling should listen for SSE config updates

* **`CEDARLING_GET_TOKEN_STATUS_LIST_UPDATES`** :  Enabled | Disabled, controls whether Cedarling should listen for SSE OAuth Status List updates



