---
tags:
  - administration
  - vm
  - operations
---

# Web server certificate

## Web server certitification installation

We are using LetsEncrypt cert to write this documentation. 

 - Install repository: `apt-get install software-properties-common` 
 - Update system: `apt-get update`
 - Install certbot: `apt-get install python3-certbot-apache` 
 - Issue certificate: `certbot --apache -d fqdn_of_Gluu_server`
 - Cert and key are available in: `/etc/letsencrypt/live/` location
 - Reboot your server

