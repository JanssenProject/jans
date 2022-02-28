
## LDAP to MySQL Migration Script

This script migrates data from ldap to MySQL.

1. To use this script, firt install **python3-ldap** module

  `apt install python3-ldap`

2. Install MySQL Server, create a database (namely **jansdb**), add a user (namely **jans**) and
give grant previlages to user **jans** on **jansdb**

3. Copy this script to `/opt/jans/jans-setup`

4. Execute

  `cd /opt/jans/jans-setup`

  `python3 ldap2mysql.py -remote-rdbm=mysql -rdbm-user=jans -rdbm-password=<password> -rdbm-db=jansdb -rdbm-host=<rdbm_host>`
