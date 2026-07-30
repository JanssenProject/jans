This is person authentication module for Jans-Auth which allows Jans/Flex to use multiple backends for user authentication.

## Installation

This list of steps needed to do to enable Basic Multi person authentication module.

1. Prepare authentication configuration file `/etc/certs/multi_auth_conf.json`. There is format description and sample configuration below. Make sure to keep permission "jetty:root" for this file.
1. servers: hostname/IP address with port of LDAP/AD servers.
1. bindDN: bindDN username
1. bindPassword: password for bindDN
   - 'bindPassword' should be the base64 encoded of password text
   - You can take the advantage of 'encode.py' script to encode/decode your password.
   - 'encode.py' is available inside Jans ( location: /opt/jans/bin/ )
1. useSSL: for 1636/636, it will be "true", otherwise "false"
1. maxConnections: total number of concurrent connection to LDAP/AD servers.
1. baseDN: where users are located.
1. loginAttributes: Primary attribute of remote LDAP/AD server or localhost:1636
1. localLoginAttributes: Primary attribute of Jans/Flex.

```
{
  "ldap_configuration":
  [
     {
        "configId":"ad_1",
        "servers":["localhost:1636"],
        "bindDN":"cn=directory manager",
        "bindPassword":"encoded_pass",
        "useSSL":true,
        "maxConnections":3,
        "baseDNs":["ou=people,o=jans"],
        "loginAttributes":["uid"],
        "localLoginAttributes":["uid"]
     },
     {
        "configId":"ad_2",
        "servers":["sample.ad.test:636"],
        "bindDN":"cn=directory manager",
        "bindPassword":"encoded_pass",
        "useSSL":true,
        "maxConnections":3,
        "baseDNs":["ou=people,o=gluu"],
        "loginAttributes":["sAMAccountName"],
        "localLoginAttributes":["uid"]
     }
  ]
}
```

1. Enable 'basic_multi_auth' in Admin-UI:
1. Log into Admin-UI as admin user
1. `Admin` > `Scripts`
1. Click on "+" sign to add new script ( to right corner )
1. Add new script:
1. Name: basic_multi_auth
1. Description: Basic Multi Auth
1. Select SAML ACRS: nothing special to select for testing purpose
1. Script Type: Person Authentication
1. Programming Lanugage: Jython
1. Location Type: Database
1. Interactive: nothing special to select for testing purpose.
1. Level: nothing special to add for testing purpose.
1. Custom Properties:
   - auth_configuration_file: /etc/certs/multi_auth_conf.json
1. Module Propertiest: db
1. Script: get script and paste it here.
1. Enable it and wait for 2 mins.
1. Test

Jans Tarp can be easily used to test new ACR. Here is a howto video: https://www.loom.com/share/6bfe8c5556a94abea05467e3deead8a2?sid=b65c81d9-c1a1-475c-b89b-c105887d31ad
