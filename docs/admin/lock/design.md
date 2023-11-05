## Jans Lock Design Overview

Jans Lock enables domains to enforce policies based on real time OAuth data.  
The Lock Client pushes token data from Auth Server to
[OPA](https://openpolicyagent.org), enabling authorization based on real time
information from the OAuth infrastructure. In order to use Jans Lock, admins
will have to do a few things:
  * Enable the Lock Token Stream in Auth Server
  * Configure a Lock Client
  * Author Rego policies based on OAuth token data

The Lock Client is an OPA helper demon that calls the OPA API to update it with
the latest data, policies, and public keys. Jans Lock consumes updates from an
Auth Server Token Stream websocket, which contains the reference ids of any new
tokens or revocations. Lock retrieves the data for a given token id directly
from the database service.

This architecture results in the best of three worlds. First, authorization
is fast, because reference tokens are in OPA's memory--no introspection is
needed. Second, admins get the power of Rego to express complex policies
based on any combination of data present in the token or context. Third,  
domains can publish central data for local decision making, for example
information about how the end user authenticated.

The Auth Server Lock Token Stream is highly confidential. Domains should
restrict access to trusted first party clients using a private network.
Each Lock Client uses OAuth dynamic client registration, and registers its
public keys to enable asymmetric client authentication and the use of DPoP
access tokens. The Lock Client must present a valid OAuth access token in
order to receive updates from the Lock Token Stream.

Lock Clients download token data directly from the local persistence service.
This design minimizes the network and compute load on Auth Server. Lock Client
also retrieve the latest policies from Git and the latest keys from a list of
OpenID Providers.

The diagram below illustrates a Jans Lock topology where OPA is used to
control course grain authorization in an API gateway, fine grain authorization
in First Party API code, and the issuance of access token scopes.

![Jans Lock Design Overview Diagram][../../assets/lock-design-diagram-00.png]
