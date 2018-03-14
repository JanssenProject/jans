# Description

This script uses CAS2 and DUO scripts. Hence both script properties should be added.

Before deploying it user should do:
1. Put Cas2ExternalAuthenticator.py and DuoExternalAuthenticator.py into `/opt/gluu/python/libs`.
2. chown -R root:gluu /opt/gluu/python/libs

## Joining flows

This diagram illustrates how script join CAS2 and DUO scripts internally. Result authentication flow can be 2 or 3 steps.

![Flows](./docs/joinded_flows.jpg)
