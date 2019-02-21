# Gluu Server 3.x Migration from OpenLDAP to OpenDJ

## Overview

This guide covers migrating the database for the Gluu Server from OpenLDAP to OpenDJ in Ubuntu or Centos 7 using a [migration script](https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/scripts/openldap2opendj_migration/openldap2opendj.py).

## Update setup.properties

First, copy `setup.properties.last` as `setup.properties` inside the Gluu container with the following command:

```
# cp /install/community-edition-setup/setup.properties.last /install/community-edition-setup/setup.properties
```

If `setup-properties.last` has been deleted, the script will not complete the migration successfully.

## Export data to LDIF

Use the following commands to export data from OpenLDAP to LDIF

``` 
# /opt/opendj/bin/ldapsearch -X -Z -D "cn=directory manager,o=gluu" -w <adminPassword> -h localhost -p 1636 -b "o=gluu" "Objectclass=*" > /root/gluu.ldif
# /opt/opendj/bin/ldapsearch -X -Z -D "cn=directory manager,o=gluu" -w <adminPassword> -h localhost -p 1636 -b "o=site" "Objectclass=*" > /root/site.ldif
```

Replace `<adminPassword>` with your Gluu admin password.

## Stop the servers

```
# /etc/init.d/identity stop
# /etc/init.d/oxauth stop
# /etc/init.d/solserver stop
```

If you have other Gluu Servers, also stop them.

## Obtain the migration script

Get the migration script from the Gluu repo with the following command:

```
# wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/scripts/openldap2opendj_migration/openldap2opendj.py -O /install/community-edition-setup/openldap2opendj.py

```

## Install python-ldap

CentOS7 users:

```
# yum install -y python-ldap
```

Ubuntu Users:

```
# apt-get install -y python-ldap
```

## Run the migration script:

```
# cd /install/community-edition-setup/
# python openldap2opendj.py
```

If you have custom OpenLDAP schema, convert them to OpenDJ schema files with 
https://github.com/GluuFederation/community-edition-setup/blob/master/static/scripts/openldap2opendj.py
and copy the converted schema file(s) to `/opt/opendj/config/schema` directory

## Import data

First, stop the OpenDJ server:

```
# /etc/init.d/opendj stop
```

Then, import the data to OpenDJ:

```
# /opt/opendj/bin/import-ldif  -b "o=gluu" -n userRoot -l /root/gluu.ldif  -R /root/gluu.ldif.rejects
# /opt/opendj/bin/import-ldif  -b "o=site" -n site -l /root/site.ldif  -R /root/site.ldif.rejects
```

Now, start OpenDJ:

```
# /etc/init.d/opendj start
```

Re-run the migration script with the `-p` argument to do post-migration finalization:

```
# python openldap2opendj.py -p
```

## Start the servers

```
# /etc/init.d/oxauth start
# /etc/init.d/identity start
```

Try to log in to Gluu UI. If it's working as expected, remove OpenLDAP:

CentOS7:

```
# yum remove symas-openldap-gluu
```

Ubuntu:

```
# apt-get remove symas-openldap-gluu
```
