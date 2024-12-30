---
tags:
- administration
- installation
- vm
- ubuntu
---

# Ubuntu Janssen Installation

## Supported Versions

- Ubuntu 22.04
- Ubuntu 20.04

Before you install, check the [VM system requirements](vm-requirements.md).


- Download the GPG key zip file , unzip and import GPG key

```shell
wget https://github.com/JanssenProject/jans/files/11814522/automation-jans-public-gpg.zip
```

```shell
unzip automation-jans-public-gpg.zip
```

```shell
sudo gpg --import automation-jans-public-gpg.asc;

```

## Install the Package

### Ubuntu 22.04

- Download the release package from the Github Janssen Project
[Releases](https://github.com/JanssenProject/jans/releases)

```
wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans_replace-janssen-version-stable.ubuntu22.04_amd64.deb -P /tmp
```

- Verify integrity of the downloaded package by verifying published `sha256sum`.   

    Download `sha256sum` file for the package

    ```shell
    wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans_replace-janssen-version-stable.ubuntu22.04_amd64.deb.sha256sum -P /tmp
    ```

    Check the hash if it is matching.

    ```shell
    cd /tmp
    sha256sum -c jans_replace-janssen-version.ubuntu22.04_amd64.deb.sha256sum
    ```

    Output similar to below should confirm the integrity of the downloaded package.

    ```text
    jans_replace-janssen-version.ubuntu22.04_amd64.deb.sha256sum: OK
    ```

- Install the package

```
sudo apt install  ./jans_replace-janssen-version.ubuntu22.04_amd64.deb
```

### Ubuntu 20.04

- Download the release package from the Github Janssen Project
[Releases](https://github.com/JanssenProject/jans/releases)

```
wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans_replace-janssen-version-stable.ubuntu20.04_amd64.deb -P /tmp
```

- Verify integrity of the downloaded package by verifying published `sha256sum`.   

    Download `sha256sum` file for the package

    ```shell
    wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans_replace-janssen-version-stable.ubuntu20.04_amd64.deb.sha256sum -P /tmp
    ```

    Check the hash if it is matching.

    ```shell
    cd /tmp
    sha256sum -c jans_replace-janssen-version.ubuntu20.04_amd64.deb.sha256sum
    ```

    Output similar to below should confirm the integrity of the downloaded package.

    ```text
    jans_replace-janssen-version.ubuntu20.04_amd64.deb.sha256sum: OK
    ```

- Install the package

```
sudo apt install  ./jans_replace-janssen-version.ubuntu20.04_amd64.deb
```

## Run the setup script

- Run the setup script in interactive mode:

```
sudo python3 /opt/jans/jans-setup/setup.py
```

See more detailed [instructions](../setup.md) on the setup script if you're
confused how to answer any of the questions, for details about command line
arguments, or you would prefer to use a properties file instead of
interactive mode.

## Verify the Installation

After the successful completion of setup process, [verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

## Log in to Text User Interface (TUI)

Begin configuration by accessing the TUI with the following command:

```
jans tui
```

Full TUI documentation can be found [here](../../config-guide/config-tools/jans-tui/README.md)

If you have selected casa during installation you can access casa using url ```https://<host>/jans-casa```

## Let's Encrypt
To enable communication with Janssen Server over tls (https) in production environment, Janssen Server needs details about CA certificate.

To generate Let’s Encrypt CA certificate follow this [let's encrypt](https://github.com/JanssenProject/jans/blob/main/docs/contribute/developer-faq.md#how-to-get-certificate-from-lets-encrypt).
 
## Uninstall

Uninstall process involves two steps and removes all the Janssen Server components.

!!! Note
For removal of the attached persistence store, please refer to [this note](../install-faq.md#does-the-janssen-server-uninstall-process-remove-the-data-store-as-well).

1. Delete files installed by Janssen
1. Remove and purge the `jans` package

Use the command below to uninstall the Janssen server

```
sudo python3 /opt/jans/jans-setup/install.py -uninstall
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
Executing rm -r -f /etc/certs
Executing rm -r -f /etc/jans
Executing rm -r -f /opt/jans
Executing rm -r -f /opt/amazon-corretto*
Executing rm -r -f /opt/jre
Executing rm -r -f /opt/node*
Executing rm -r -f /opt/jetty*
Executing rm -r -f /opt/jython*
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
Building dependency tree... Done
Reading state information... Done
The following packages were automatically installed and are no longer required:
  apache2 apache2-bin apache2-data apache2-utils libapr1 libaprutil1 libaprutil1-dbd-sqlite3 libaprutil1-ldap liblua5.3-0 postgresql postgresql-contrib python3-pymysql python3-ruamel.yaml
  python3-ruamel.yaml.clib
Use 'apt autoremove' to remove them.
The following packages will be REMOVED:
  jans*
0 upgraded, 0 newly installed, 1 to remove and 2 not upgraded.
After this operation, 1631 MB disk space will be freed.
Do you want to continue? [Y/n] y
(Reading database ... 166839 files and directories currently installed.)
Removing jans (replace-janssen-version~ubuntu20.04) ...
Checking to make sure service is down...
```
