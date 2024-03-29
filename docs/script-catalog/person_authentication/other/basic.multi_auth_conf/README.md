This is person authentication module for oxAuth which allows to specify multiple authentication configurations.

## Installation

This list of steps needed to do to enable Basic Multi person authentication module.

1. This module depends on python libraries. In order to use it we need to install Jython. Please use next articles to proper Jython installation:
    - Installation notest: http://ox.gluu.org/doku.php?id=oxtauth:customauthscript#jython_installation_optional
    - Jython integration: http://ox.gluu.org/doku.php?id=oxtauth:customauthscript#jython_python_integration

2. Copy shared required python libraries from ../shared_libs folder to $CATALINA_HOME/conf/python folder.

3. Prepare authentication configuration file /etc/certs/multi_auth_conf.json. There is format description and sample configuration in README.txt.  

4. Confire new custom module in oxTrust:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Custom Scripts" page.
    - Select "Person Authentication" tab.
    - Click on "Add custom script configuration" link.
    - Enter name = basic_multi_auth_conf
    - Enter level = 0-100 (priority of this method).
    - Select usage type "Interactive".
    - Add custom required and optional properties which specified in README.txt
    - Copy/paste script from BasicMultiAuthConfExternalAuthenticator.py.
    - Activate it via "Enabled" checkbox.
    - Click "Update" button at the bottom of this page.

5. Configure oxAuth to use Basic Multi authentication by default:
    - Log into oxTrust with administrative permissions.
    - Open "Configuration→Manage Authentication" page.
    - Scroll to "Default Authentication Method" panel. Select "basic_multi_auth_conf" authentication mode.
    - Click "Update" button at the bottom of this page.

6. Try to log in using Basic Multi authentication method:
    - Wait 30 seconds and try to log in again. During this time oxAuth reload list of available person authentication modules.
    - Open second browser or second browsing session and try to log in again. It's better to try to do that from another browser session because we can return back to previous authentication method if something will go wrong.

There are log messages in this custom authentication script. In order to debug this module we can use command like this:
tail -f /opt/tomcat/logs/wrapper.log | grep "Basic (multi auth conf)"

## Configuration

This module has only one property:
1) auth_configuration_file - It's path to file which contains AD LDAP authentication connection details and list of attributes which user can use in order to log in.
   Example: /etc/certs/multi_auth_conf.json
   Example content of this file [ 'bindPassword' should be the base64 encoded of password text. You can take the advantage of 'encode.py' script to encode/decode your password. 'encode.py' is available inside Gluu server container ( location: /opt/gluu/bin/ ) ]:


{
  "ldap_configuration":
  [
     {
        "configId":"ad_1",
        "servers":["localhost:1389"],
        "bindDN":"cn=directory manager",
        "bindPassword":"encoded_pass",
        "useSSL":false,
        "maxConnections":3,
        "baseDNs":["ou=people,o=gluu"],
        "loginAttributes":["uid"],
        "localLoginAttributes":["uid"]
     },
     {
        "configId":"ad_2",
        "servers":["localhost:2389"],
        "bindDN":"cn=directory manager",
        "bindPassword":"encoded_pass",
        "useSSL":false,
        "maxConnections":3,
        "baseDNs":["ou=people,o=gluu"],
        "loginAttributes":["mail"],
        "localLoginAttributes":["mail"]
     }
  ]
}


   The names/values of properties are similar to oxAuth/oxTrust ldap configuration files.
