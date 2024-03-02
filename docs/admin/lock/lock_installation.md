---
tags:
 - administration
 - lock
 - installation
 - opa
---

# Lock Installation

This document details the steps required to set up a main Jans AS running lock 
as a service, and a lock client. 

## Helm Deployments

This section is a work in progress.

## VM Installation

!!! warning
    VM installations are not recommeded for production deployments.

For the purpose of this document, we are using a VM installation 
with PostgreSQL persistence and [OPA](https://www.openpolicyagent.org/) as PDP.

## Install Lock and OPA on Janssen Server

The authorization server acts as the source of token data for all lock clients.

- Follow the Jans AS setup instructions as documented [here](https://docs.jans.io/head/admin/install/)
  - When choosing persistence, select local PostgreSQL
- When prompted for `Jans Lock [No]`, select `Yes`
- Select the option to install `Jans Lock as service`. This will add lock as a 
plugin to the authorization server
- When prompted to install OPA, select `Yes`

## Install Lock Client

The Lock client acts as a medium by which an application can get token 
information from the auth server in real time. 

In this setup, the Lock client 
is installed as a jetty server with OPA running alongside it. This client
can be queried by any application that needs to make authorization decision.
For example, a Flask API 
running on the same server can query OPA for token details coming in from the 
auth server.

To install Janssen Lock Client, you need a main Janssen Server. For the purpose
of this document, let us assume that persistence used is PostgreSQL.

1. Obtain salt by executing the following command on Janssen Server 
   ```shell
   cat /etc/jans/conf/salt
   ``` 
   Output will look like: 
   ```text
   encodeSalt = Qm0MCPh20QGsE21ZovVnUIb8
   ```
    This is a 24 character string used to encode/decode secret data.

1. Obtain the decrypted password for PostgreSQL by executing the following command on Janssen Server
  ```shell
  cat /opt/jans/jans-setup/setup.properties.last | grep rdbm`
  ```
1. Enable remote connections to PostgreSQL on your main server

     1. Run this command as root to find the configuration fileee
     ```shell
     su - postgres -c 'psql -U postgres -d postgres -t -c "SHOW config_file;"'
     ```
   
     2. Edit the configuration file, uncomment the line containing 
     `listen_addresses` and edit it to `listen_addresses = '*'`. 
     This will allow PostgreSQL to listen on all network interfaces. 
     You should take precautions accordingly, such as setting up firewalls.
   
     3. Run this command as root to find the client authentication configuration file: `su - postgres -c 'psql -U postgres -d postgres -t -c "SHOW hba_file;"'`

     4. Add the following line
     ```text
     host jansdb  jans  0.0.0.0/0 md5
     ```

     5. Save the file, and restart PostgreSQL
     ```shell
     systemctl restart postgresql
     ```
   
- Download the lock installer
  ```shell
  wget https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/install.py
  ```
- Run the installer
  ```shell
  python3 install.py --lock-setup
  ```
- When prompted, enter the salt, and the main server's PostgreSQL details. 
- Once setup is complete, verify status of lock client by visiting `http://<hostname>/jans-lock/sys/health-check`
