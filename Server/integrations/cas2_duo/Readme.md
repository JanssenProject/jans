# Description

This script uses CAS2 and DUO scripts. Hence both script properties should be added.

Before deploying it user should do:
1. Put Cas2ExternalAuthenticator.py and DuoExternalAuthenticator.py into `/opt/gluu/python/libs`.
  - `wget -c https://raw.githubusercontent.com/GluuFederation/oxAuth/master/Server/integrations/duo/DuoExternalAuthenticator.py`
  - `wget -c https://raw.githubusercontent.com/GluuFederation/oxAuth/master/Server/integrations/cas2/Cas2ExternalAuthenticator.py`
  
2. chown -R root:gluu /opt/gluu/python/libs

3. Restart oxAuth service with: `service oxauth stop` and `service oxauth start`

## Joining flows

This diagram illustrates how script join CAS2 and DUO scripts internally. Result authentication flow can be 2 or 3 steps.

![Flows](./docs/joinded_flows.jpg)
