---
tags:
- administration
- installation
- vm
- ubuntu
---

# Ubuntu Janssen Installation

Before you install, check the [VM system requirements](vm-requirements.md).

## Supported Versions
- Ubuntu 20.04

## Install the Package

- Download the release package from the Github Janssen Project
[Releases](https://github.com/JanssenProject/jans/releases)

```
wget https://github.com/JanssenProject/jans/releases/download/v1.0.4/jans_1.0.4.ubuntu20.04_amd64.deb -P /tmp
```

- Verify integrity of the downloaded package by verifying published `sha256sum`.   

    Download `sha256sum` file for the package

    ```shell
    wget https://github.com/JanssenProject/jans/releases/download/v1.0.4/jans_1.0.4.ubuntu20.04_amd64.deb.sha256sum -P /tmp
    ```

    Check the hash if it is matching. 

    ```shell
    sha256sum -c /tmp/jans_1.0.4.ubuntu20.04_amd64.deb.sha256sum
    ```

    Output similar to below should confirm the integrity of the downloaded package.
    
    ```text
    <package-name>: OK
    ```

- Install the package

```
apt install -y /tmp/jans_1.0.4.ubuntu20.04_amd64.deb
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

## Ubuntu Janssen Un-Installation

Removing Janssen is a two step process:

1. Delete files installed by Janssen
1. Remove and purge the `jans` package

Use the command below to uninstall the Janssen server

```
python3 /opt/jans/jans-setup/install.py -uninstall
```

You'll see the following confirmation:

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

The command below removes and purges the `jans` package

```
apt-get --purge remove jans
```

Which should result in the following:

```
Reading package lists... Done
Building dependency tree       
Reading state information... Done
The following packages were automatically installed and are no longer required:
  apache2 apache2-bin apache2-data apache2-utils libapr1 libaprutil1 libaprutil1-dbd-sqlite3 libaprutil1-ldap liblua5.2-0 python3-distutils python3-ldap3 python3-lib2to3 python3-prompt-toolkit
  python3-pyasn1 python3-pygments python3-pymysql python3-ruamel.yaml python3-wcwidth
Use 'sudo apt autoremove' to remove them.
The following packages will be REMOVED:
  jans*
0 upgraded, 0 newly installed, 1 to remove and 124 not upgraded.
After this operation, 1,257 MB disk space will be freed.
Do you want to continue? [Y/n] y
(Reading database ... 166839 files and directories currently installed.)
Removing jans (1.0.2~ubuntu20.04) ...
Checking to make sure service is down...

```
