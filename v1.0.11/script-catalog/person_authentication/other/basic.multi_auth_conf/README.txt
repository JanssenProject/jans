This is person authentication module for oxAuth which allows to specify multiple authentication configurations.

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
