---
tags:
  - administration
  - reference
  - database
  - additional claims to database
  - jansPerson
  - LDAP to MySQL Migration Script
---

### **Change password for user `jans`** :

*  `ALTER USER 'jans'@'localhost' IDENTIFIED BY 'TopSecret';`
*  `GRANT ALL PRIVILEGES ON jansdb.* TO 'jans'@'localhost';`

### Create new user claims to `jansPerson`: 
* You can add additional attributes to `jansPerson` table and use them. This will be similar to LDAP where DB stores all user attributes in one entry. Additional attributes will not affect the server functionality.
* Ensure you restart services after DB schema modification

### **Modify column size of jansPerson** :
Say we want to increase the size of `mail` field to 144. Do the following:<br>
* a. Modify column size - 
 ```
 ALTER TABLE `jansdb`.`jansPerson` CHANGE COLUMN `mail` `mail` VARCHAR(144) NULL DEFAULT NULL ;
 ```
* b. Drop indexes and re-create - 
 ```
 ALTER TABLE jansdb.jansPerson DROP INDEX `jansPerson_CustomIdx2`;
 ALTER TABLE jansdb.jansPerson ADD INDEX `jansPerson_CustomIdx2` ((lower(`mail`)));
 ```
* c. Ensure you restart services after DB schema modification


### LDAP to MySQL Migration Script

This script migrates data from ldap to MySQL.

1. To use this script, firt install **python3-ldap** module
  `apt install python3-ldap`

2. Install MySQL Server, create a database (namely **jansdb**), add a user (namely **jans**) and
give grant previlages to user **jans** on **jansdb**

3. Download script to `/opt/jans/jans-setup`
  ```
  wget https://raw.githubusercontent.com/JanssenProject/jans/jans-linux-setup-ldap2mysql/jans-linux-setup/tools/ldap2mysql/ldap2mysql.py -O /opt/jans/jans-setup/ldap2mysql.py
  ```

4. Execute
  ```
  cd /opt/jans/jans-setup`
  python3 ldap2mysql.py -remote-rdbm=mysql -rdbm-user=jans -rdbm-password=<password> -rdbm-db=jansdb -rdbm-host=<rdbm_host>
  ```
