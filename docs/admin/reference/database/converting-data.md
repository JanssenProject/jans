---
tags:
  - administration
  - reference
  - database
  - migration
---

# Data Conversion

This document lists the steps required to move data from LDAP to various other 
persistence mechanisms supported by Janssen Server.

## LDAP to MySQL Migration Script

This script migrates data from LDAP to MySQL on Ubuntu Linux platform.

1. To use this script, firt install **python3-ldap** module

   ```shell
   apt install python3-ldap
   ```

2. Install MySQL Server, create a database (namely **jansdb**), add a user (namely **jans**) and
   give grant previlages to user **jans** on **jansdb**

3. Download script to `/opt/jans/jans-setup`
  ```shell
  wget https://raw.githubusercontent.com/JanssenProject/jans/jans-linux-setup-ldap2mysql/jans-linux-setup/tools/ldap2mysql/ldap2mysql.py -O /opt/jans/jans-setup/ldap2mysql.py
  ```

4. Execute
  ```shell
  cd /opt/jans/jans-setup`
  python3 ldap2mysql.py -remote-rdbm=mysql -rdbm-user=jans -rdbm-password=<password> -rdbm-db=jansdb -rdbm-host=<rdbm_host>
  ```


!!! Contribute
If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)