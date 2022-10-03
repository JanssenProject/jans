---
tags:
  - administration
  - installation
  - vm
  - ubuntu
---

# Install Janssen Server using Ubuntu Linux Package

Janssen Server can be installed using Linux package for Ubuntu

## Supported Versions
- Ubuntu 20.04
- Ubuntu 22.04

!!! note
    [SELinux](https://wiki.ubuntu.com/SELinux) should be disabled

## System Requirements

--8<--- "snippets/vm-system-requirements-dev.md"

## Prerequisites

- `curl` should be instlled. This can be easily installed using command `apt install wget curl`

## Install

Download the installer from Janssen Project site using command below

```
wget http:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*ubuntu20.04_amd64.deb' | head -n 1) -O /tmp/jans.ubuntu20.04_amd64.deb
```

Unpack/install the installer package

```
apt install -y /tmp/jans.ubuntu20.04_amd64.deb
```

Initiate setup process using command below. Setup process will prompt for user for inputs and whether optional modules should be installed or not as shown in example below.

```
sudo python3 /opt/jans/jans-setup/setup.py
```

Sample user prompts: 

```commandline

```

## Verify the Installation

After the successful completion of setup process, [verify the system health]().


## Uninstall 

```commandline
sudo python3 /opt/jans/jans-setup/install.py -uninstall
```

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