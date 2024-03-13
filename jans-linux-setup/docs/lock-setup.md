# Installing Janssen Lock Client

This document explains how to install Janssen Lock Client on a remote machine.
To be able to install Janssen Lock Client, you have to install a main Janssen
Server with PostgreSQL backend [see docs](https://docs.jans.io/v1.0.22/admin/install/vm-install/)


## Obtain salt
On your main Janssen Server, execute the following command to get salt:
`cat /etc/jans/conf/salt`
Output will look like:
`encodeSalt = Qm0MCPh20QGsE21ZovVnUIb8`
**salt** is 24 character strings used for encoding/decoding secret data

## Enable remote connection to PostgreSQL
Since Lock Client will use PostgreSQL server on main Janssen Server, you need to enable remote connection.
 1. Get location of configuration file (execute as root):
  `su - postgres -c 'psql -U postgres -d postgres -t -c "SHOW config_file;"'`
 2. Set **listen_address** to `*` as:
   `listen_addresses = '*'`
   Note that this will allow PostgreSQL to listen all network interfaces, 
   It is your responsibility to take safety precautions
 3. To allow Janssen DB user to connect remotetly, first get location of **pg_hba.conf**
   `su - postgres -c 'psql -U postgres -d postgres -t -c "SHOW hba_file;"'`
    Add the following line to top of **pg_hba.conf**
    `host	jansdb	jans	0.0.0.0/0	md5`
    Note that we assue user is **jans** and database is **jansdb**
 3. Restart PostgreSQL server:
   `systemctl restart postgresql`

## Install Janssen Lock Client
This installation will be online, so downlod Janssen online installer
`wget https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/install.py`
Execute installer with argument `--lock-setup`:
`python3 install.py --lock-setup`

