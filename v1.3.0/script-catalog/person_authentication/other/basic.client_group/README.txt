This person authentication module for oxAuth allows to restrict access to RP for specific user groups. It allows to define configuration for each client.

This module has one mandatory property `configuration_file`. It's path to JSON configuration file
   Example: /etc/certs/client_group.json
   Example content of this file:

{
   "client_1":{
      "client_inum":"client_inum_1",
      "user_group":[
         "group_dn_1",
         "group_dn_2"
      ]
   },
   "client_2":{
      "client_inum":"client_inum_2",
      "user_group":[
         "group_dn_1",
         "group_dn_2"
      ]
   }
}

Also it's possible to define how it should work when there is no configuration for specific client. This is controlled via property:
`allow_default_login`: true/false
