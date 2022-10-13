---
tags:
- administration
- installation
- vm
- SUSE
- SLES
---

# Install Janssen Server using the SUSE Linux Enterprise Server (SLES) Package

Janssen Server can be installed using the Linux package for SLES 

## Supported versions
- SUSE Linux Enterprise Server (SLES) 15

!!! note
    [SELinux](https://wiki.ubuntu.com/SELinux) should be disabled

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Prerequisites

- `curl` should be installed. This can be easily installed using the command below

 ```
 zypper install curl
 ```

## Install

- Download and install package from the Janssen Project site using the command below

```
zypper --no-gpg-checks install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*suse15.x86_64.rpm' | head -n 1)
```

- Initiate the setup process using the command below. The setup process will prompt for user for inputs.

```
python3 /opt/jans/jans-setup/setup.py
```

## Verify the Installation

After the successful completion of setup process, [verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

## Uninstall

Uninstall process involves two steps

1. Uninstall Janssen Server
2. Remove and purge the `jans` package

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