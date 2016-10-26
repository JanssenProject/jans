## Pre-requisites
- Ubuntu 14.04
- OpenLDAP Binary as a deb package - Preferably Symas Openldap Gold 

### Procedure for replacing OpenDJ with OpenLDAP in 2.4.4
1. Install the Openldap debian package  `dpkg -i <openldap.deb>`
2. Generate the certificates using OpenSSL

    ```bash
    cd /etc/certs
    openssl genrsa -des3 -out openldap.key 2048
    openssl rsa -in openldap.key -out openldap.key.insecure
    mv openldap.key.insecure openldap.key
    openssl req -new -key openldap.key -out openldap.csr
    openssl x509 -req -days 365 -in openldap.csr -signkey openldap.key -out openldap.crt
    ```

3. Clone the community-edition-setup for the scripts

    ```bash
    cd ~
    git clone https://github.com/GluuFederation/community-edition-setup.git
    cd community-edition-setup/openldap_migration
    ```

4. Export the data from OpenDJ

    ```bash
    python export_opendj.py
    ```

5. Setup OpenLDAP

    ```bash
    python setup_openldap.py
    ```

4. Stop OpenDJ and  Start the OpenLDAP server

    ```
    service opendj stop
    service solserver start
    ```


### Procedure for migrating an existing Gluu Server 2.4.4
1. Export the ldap data using the export\_opendj script

  ```bash
  service gluu-server-2.4.4 login
  wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/openldap_migration/export_opendj.py
  python export_opendj.py
  exit
  ```
  
  This creates a folder called backup\_24 that will contain all the LDAP data in the ldif file format.
2. Install the Gluu Server 3.0.0 alpha version.

  ```bash
  echo "deb https://repo.gluu.org/ubuntu/ trusty-devel main" > /etc/apt/sources.list.d/gluu-repo.list
  curl https://repo.gluu.org/ubuntu/gluu-apt.key | apt-key add -
  apt-get update
  apt-get install gluu-server-3.0.0
  ```
  
3. Stop the old server and copy the files to the new one. Assuming you have `openldap.deb` in the `/root` directory

  ```bash
  service gluu-server-2.4.4 stop
  cp -r /opt/gluu-server-2.4.4/root/backup_24/ /opt/gluu-server-3.0.0/root/
  cp openldap.deb /opt/gluu-server-3.0.0/root/
  ```
  
4. Start the new server and login and do some bootstrapping.

  ```bash
  service gluu-server-3.0.0 start
  service gluu-server-3.0.0 login
  dpkg -i openldap.deb
  cd /install
  rm -rf community-edition-setup
  git clone https://github.com/GluuFederation/community-edition-setup.git
  cd community-edition-setup
  cp /root/backup_24/setup.properties /install/community-edition-setup/
  sed -i 's/ldap_type\ \=\ \"opendj\"/ldap_type\ \=\ \"openldap\"/' setup.py
  ./setup.py
  ```
  
5. Input the values and wait for the installation to finish.
6. Import the old OpenDJ data into OpenLDAP

  ```bash
  wget -c https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/openldap_migration/import_openldap.py
  wget -c https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/ldif.py
  apt-get update
  apt-get install python-pip
  pip install jsonmerge
  python import_openldap.py backup_24
  ```
  
7. Start the Openldap server

  ```bash
  service solserver start
  ```
  
8. Verify connection using username `cn=directory manager,o=gluu` and your ldap password from the old installation on the port 1636.
