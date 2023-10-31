## Overview

[OPA](https://openpolicyagent.org) is a [CNCF](https://cncf.io) project that
provides a compact Policy Decision Point ("PDP") that runs as a sidecar locally.
OPA is performant because all data and policies are in memory. But to be useful,
you have to figure out how to keep the data and policies updated in OPA's
RAM. And this is a challenge because an organization may have numerous instances  
of OPA running on their network.

To solve this data distribution challenge, Jans Lock is a client that
leverages Pub/Sub features of Jans Auth Server in a fanout topology to get the
latest access token values. This means that a client can send the access token
reference id to OPA for evaluation without requiring either introspection or
JWT validation--OPA will have all active access tokens and their JSON equivalent
values in memory.

Jans Lock clients download the token values directly from the persistence
service. This is to keep minimize the network and compute requirement on Jans
Auth Server.

The Jans Lock client will also retrieve the latest policies from Git and the
latest keys from Auth Server, enabling it to validate JWTs, if necessary.

This diagram shows a sample Jans Lock topology for a mobile application, where
OPA is used to control course grain authorization in the API gateway,
fine grain authorization requests from the API code, and policies to
control which scopes Auth Server issues to a client.

![Sample Topology for Mobile App Security with Jans Lock][../assets/lock-design-diagram-00.png]
