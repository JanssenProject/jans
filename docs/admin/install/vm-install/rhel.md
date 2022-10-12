---
tags:
- administration
- installation
- vm
- RHEL
- RedHat
---

# Install Janssen Server using Enterprise Linux Package

## Supported versions
- Red Hat 8
- CentOS 8

!!! note
[SELinux](https://wiki.ubuntu.com/SELinux) should be disabled

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Prerequisites

- System should be registered and attached with Red Hat. This is required to run commands that install packages like `mod_auth_openidc`
- Install Extra Packages for Enterprise Linux (EPEL)

```
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
```

- `curl` should be instlled. This can be easily installed using the command below

 ```
 yum install curl
 ```

## Install

- Enable the `mod_auth_openidc` module

```
yum module enable mod_auth_openidc
```

- Download and Install the Janssen Server Linux package

```
yum install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*el8.x86_64.rpm' | head -n 1)
```

- Initiate the setup process using the command below. The setup process will prompt for user for inputs.

```
sudo python3 /opt/jans/jans-setup/setup.py
```

The installer should confirm successful installation with a message similar to the one shown below:

![](../../../assets/image-jans-install-success.png)

## Verify the Installation

After the successful completion of setup process, [verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

### Uninstall Janssen Server

Use the command below to uninstall the Janssen server

```commandline
python3 /opt/jans/jans-setup/install.py -uninstall
```

Console output like below will confirm the successful uninstallation of the Janssen Server

```text
This process is irreversible.
You will lose all data related to Janssen Server.


 
Are you sure to uninstall Janssen Server? [yes/N] yes

Uninstalling Jannsen Server...
Removing /etc/default/jans-config-api
Stopping jans-config-api
Removing /etc/default/jans-scim
Stopping jans-scim
Removing /etc/default/jans-fido2
Stopping jans-fido2
Removing /etc/default/jans-auth
Stopping jans-auth
Removing /etc/default/jans-client-api
Stopping jans-client-api
Stopping OpenDj Server
sh: 1: /opt/opendj/bin/stop-ds: not found
Executing rm -r -f /etc/certs
Executing rm -r -f /etc/jans
Executing rm -r -f /opt/jans
Executing rm -r -f /opt/amazon-corretto*
Executing rm -r -f /opt/jre
Executing rm -r -f /opt/node*
Executing rm -r -f /opt/jetty*
Executing rm -r -f /opt/jython*
Executing rm -r -f /opt/opendj
Executing rm -r -f /opt/dist
Removing /etc/apache2/sites-enabled/https_jans.conf
Removing /etc/apache2/sites-available/https_jans.conf

```

### Remove the linux package

Use the command below to remove and purge `jans` package

```text
yum remove jans.x86_64
```

Successful removal will remove the Janssen Server package along with the removal of all the unused dependencies.

