This is person authentication module for Jans-Auth which allows Jans/Flex to use multiple backends for user authentication.

## Installation

This list of steps needed to do to enable Basic Multi person authentication module.

<!--
1. This module depends on python libraries. In order to use it we need to install Jython. Please use next articles to proper Jython installation:
    - Installation notest: http://ox.gluu.org/doku.php?id=oxtauth:customauthscript#jython_installation_optional
    - Jython integration: http://ox.gluu.org/doku.php?id=oxtauth:customauthscript#jython_python_integration

2. Copy shared required python libraries from ../shared_libs folder to $CATALINA_HOME/conf/python folder.
--> 
1. Prepare authentication configuration file `/etc/certs/multi_auth_conf.json`. There is format description and sample configuration below. Make sure to keep permission "jetty:root" for this file. 

  - servers: hostname/IP address with port of LDAP/AD servers.
  - bindDN: bindDN username
  - bindPassword: password for bindDN
    - 'bindPassword' should be the base64 encoded of password text
    - You can take the advantage of 'encode.py' script to encode/decode your password.
    - 'encode.py' is available inside Jans ( location: /opt/jans/bin/ )  
  - useSSL: for 1636/636, it will be "true", otherwise "false"
  - maxConnections: total number of concurrent connection to LDAP/AD servers.
  - baseDN: where users are located.
  - loginAttributes: Primary attribute of remote LDAP/AD server or localhost:1636
  - localLoginAttributes: Primary attribute of Jans/Flex.

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
 

2. Enable 'basic_multi_auth' in Admin-UI: 

 - Log into Admin-UI as admin user
 - `Admin` > `Scripts`
 - Click on "+" sign to add new script ( to right corner )
 - Add new script:
   - Name: basic_multi_auth
   - Description: Basic Multi Auth
   - Select SAML ACRS: nothing special to select for testing purpose
   - Script Type: Person Authentication
   - Programming Lanugage: Jython
   - Location Type: Database
   - Interactive: nothing special to select for testing purpose.
   - Level: nothing special to add for testing purpose.
   - Custom Properties:
      - auth_configuration_file: /etc/certs/multi_auth_conf.json
   - Module Propertiest: db
   - Script: get script and paste it here.
   - Enable it and wait for 2 mins. 

 3. Test

  Jans Tarp can be easily used to test new ACR. Here is a howto video: https://www.loom.com/share/6bfe8c5556a94abea05467e3deead8a2?sid=b65c81d9-c1a1-475c-b89b-c105887d31ad
