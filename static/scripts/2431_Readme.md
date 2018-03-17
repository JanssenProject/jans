# Upgrading Gluu Server CE
## Overview
The Gluu Server can **not** be upgraded with a simple `apt-get upgrade`. 
The admin needs to explicitly install the new version of the Gluu Server and 
export and import the required data using scripts. 

!!! Note
    This guide assumes that you are upgrading from version 2.x.x to 3.1.2 and are 
    **OK with changing persistence from OpenDJ to OpenLDAP**. If you prefer to 
    keep OpenDJ in Gluu Server 3.x, follow the separate documentation for 
    [upgrading with OpenDJ](../upgrade/manual-update.md/).

!!! Warning
    Before proceeding with an upgrade, make sure to [backup](../operation/backup.md) 
    the Gluu container or LDAP Ldif before proceeding with the upgrade. 

Upgrading generally involves the following steps:   

* Install new version
* Export the data from your current version
* Stop the current Gluu Server
* Start the new version of Gluu Server
* Import data into the new server

Gluu provides the necessary 
[scripts](https://github.com/GluuFederation/community-edition-setup/tree/master/static/scripts) 
to import and export data in and out of the servers.

## Export the data from the current installation

```
# service gluu-server-2.x.x login

# wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/scripts/export2431.py

# wget -c https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/ldif.py

```

Install the `python-pip` package:

```
# curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
# python get-pip.py
```

Install the `json-merge` Python package and run the import script.

```
# pip install jsonmerge

# chmod +x export2431.py

# ./export2431.py
```

The export script will generate a directory called `backup_2431` which will have all the data from 
the current installation. Check the log file generated in the directory for any errors.

## Install the latest version of the Gluu server

Stop the current version of the gluu-server.

```
# service gluu-server-2.4.x stop
```

Review the [installation docs](../installation-guide/install.md) to install the Gluu Server 
using the package manager. Once the package manager has installed version `3.1.2`, 
then execute the following commands:

```
# cp -r /opt/gluu-server-2.4.x/root/backup_2431/ /opt/gluu-server-3.1.2/root/

# service gluu-server-3.1.2 start

# service gluu-server-3.1.2 login

# cp backup_2431/setup.properties /install/community-edition-setup/

# cd /install/community-edition-setup/

# ./setup.py
```

Enter the required information to complete the installation.

## Import your old data

Navigate to where you have the `backup_2431` folder (if the above commands were followed, it is in `/root/`) 
and execute the following commands to get the necessary scripts.

```

# wget -c https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/static/scripts/import2431.py

# wget -c https://raw.githubusercontent.com/GluuFederation/cluster-mgr/master/testing/ldifschema_utils.py
```

Install the `python-pip` package using your package manager.

```
# curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"

# python get-pip.py
```
Install the `python-ldap` package:
  * on Ubuntu:
```
apt-get update
apt-get install -y python-ldap
```
  * on CentOS/RHEL:

```
# yum install epel-release
# yum clean all
# yum install python-ldap
```

Install the `json-merge` Python package and run the export script.

```
# pip install jsonmerge

# cd /root

# chmod +x import2431.py

# ./import2431.py backup_2431
```

!!! Note
    After completion of import, stop/start gluu-server container one final time

Any errors or warnings will be displayed in the terminal and can be reviewed in the import log. 
Now you should be able to log into the oxTrust web UI using the old admin credentials and you 
should see all previous data in place. 


