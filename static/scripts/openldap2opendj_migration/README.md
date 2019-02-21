# Gluu Server 3.1.x Migration from OpenLDAP to OpenDj

Inorder to make migration, you need `setup.properties.last`, If you don't have this
file don't try this manual. Login to gluu container and copy `setup.properties.last` as `setup.properties`:

```
# cp /install/community-edition-setup/setup.properties.last /install/community-edition-setup/setup.properties
```

## Export data to ldif

Use the following commands to export data from openldap to ldif

``` 
# /opt/opendj/bin/ldapsearch -X -Z -D "cn=directory manager,o=gluu" -w <admiPassword> -h localhost -p 1636 -b "o=gluu" "Objectclass=*" > /root/gluu.ldif
# /opt/opendj/bin/ldapsearch -X -Z -D "cn=directory manager,o=gluu" -w <admiPassword> -h localhost -p 1636 -b "o=site" "Objectclass=*" > /root/site.ldif
```

replace `<admiPassword>` with your Gluu admin password.

## Stop servers

```
# /etc/init.d/identity stop
# /etc/init.d/oxauth stop
# /etc/init.d/solserver stop
```

If you have other Gluu servers, also stop them.

## Obtain migration script

Get migration script from repo:

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

## Run migration script:

```
# cd /install/community-edition-setup/
# python openldap2opendj.py
```

If you have custom OpenLDAP schema, convert them to OpenDJ schema files with 
https://github.com/GluuFederation/community-edition-setup/blob/master/static/scripts/openldap2opendj.py
and copy converted schema file(s) to `/opt/opendj/config/schema` directory

## Import data

First stop opendj server:

```
# /etc/init.d/opendj stop
```

Then import data to OpenDJ

```
# /opt/opendj/bin/import-ldif  -b "o=gluu" -n userRoot -l /root/gluu.ldif  -R /root/gluu.ldif.rejects
# /opt/opendj/bin/import-ldif  -b "o=site" -n site -l /root/site.ldif  -R /root/site.ldif.rejects
```

Now start OpenDJ:

```
# /etc/init.d/opendj start
```

Re-run migration script with `-p` argument to do post migrations

```
# python openldap2opendj.py -p
```

## Start Servers
```
# /etc/init.d/oxauth start
# /etc/init.d/identity start
```

Try login to Gluu UI, if everything is well remove OpenLDAP:

CentOS7:

```
# yum remove symas-openldap-gluu
```

Ubuntu:

```
# apt-get remove symas-openldap-gluu
```
