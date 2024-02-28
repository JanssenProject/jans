---
tags:
 - administration
 - lock
 - opa
---

# Lock Setup

This document details the steps required to set up a main Jans AS running lock as a service, and a lock client. For this example we are using a VM install method (not recommended for production environments), with PostgreSQL persistence and [OPA](https://www.openpolicyagent.org/) as PDP.

## Authorization Server Setup

The authorization server acts as the source of token data for all lock clients.

- Follow the Jans AS setup instructions as documented [here](https://docs.jans.io/head/admin/install/)
  - When choosing persistence, select local PostgreSQL
- When prompted for `Jans Lock [No]`, select `Yes`
- Select the option to install `Jans Lock as service`. This will add lock as a plugin to the authorization server.
- When prompted to install OPA, select `Yes`.

## Lock Client Setup

The lock client acts as a medium by which an application can get token information from the auth server in real time. In this setup, the lock client is installed as a jetty server with OPA running alongside it, which can be queried by any application running on the server. For example, a Flask API running on the same server can query OPA for token details coming in from the auth server.

To install Janssen Lock Client, you need a main Janssen Server with PostgreSQL persistence.

- Obtain salt from your main Janssen Server. Execute the following command to get salt: `cat /etc/jans/conf/salt` Output will look like: `encodeSalt = Qm0MCPh20QGsE21ZovVnUIb8`. This is a 24 character string used to encode/decode secret data.
- Obtain the decrypted password for PostgreSQL: `cat /opt/jans/jans-setup/setup.properties.last | grep rdbm`
- Enable remote connections to PostgreSQL on your main server. 
  - Run this command as root to find the configuration file: `su - postgres -c 'psql -U postgres -d postgres -t -c "SHOW config_file;"'`
  - Edit the configuration file with your favorite text editor, uncomment the line containing `listen_addresses` and edit it to `listen_addresses = '*'`. This will allow PostgreSQL to listen on all network interfaces. You should take precautions accordingly, such as setting up firewalls.
  - Run this command as root to find the client authentication configuration file: `su - postgres -c 'psql -U postgres -d postgres -t -c "SHOW hba_file;"'`
  - Add the following line: `host jansdb  jans  0.0.0.0/0 md5`
  - Save the file, and restart PostgreSQL: `systemctl restart postgresql`
- Download the lock installer: `wget https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/install.py`
- Run the installer: `python3 install.py --lock-setup`
- When prompted, enter the salt, and the main server's PostgreSQL details. 
- Once setup is complete, verify status of lock client by visiting `http://<hostname>/jans-lock/sys/health-check`
