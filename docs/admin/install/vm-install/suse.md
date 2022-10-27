---
tags:
- administration
- installation
- vm
- SUSE
- SLES
- Tumbleweed
---

# SUSE Janssen Installation

Before you install, check the [VM system requirements](vm-requirements.md).


## Supported versions
- SUSE Linux Enterprise Server (SLES) 15
- openSUSE Leap 15.4
- openSUSE Tumbleweed (non-production)

## Install the Package

Download the latest package from the Github Janssen Project
[Releases](https://github.com/JanssenProject/jans/releases)

!!! note
    `curl`  is required to run the command below

```
zypper --no-gpg-checks install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*suse15.x86_64.rpm' | head -n 1)
```

## Run the setup script

- Run the setup script in interactive mode:

```
python3 /opt/jans/jans-setup/setup.py
```

See more detailed [instructions](../setup.md) on the setup script if you're
confused how to answer any of the questions, for details about command line
arguments, or you would prefer to use a properties file instead of
interactive mode.


## Verify the Installation

After the successful completion of setup process, [verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

## Uninstall

Uninstall process involves two steps

1. Uninstall Janssen Server
2. Remove and purge the `jans` package


Use the command below to uninstall the Janssen server

```
python3 /opt/jans/jans-setup/install.py -uninstall
```

You'll see the following confirmation:

```
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
