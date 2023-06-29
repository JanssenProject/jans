---
tags:
- administration
- installation
- vm
- SUSE
- SLES
- Leap
---

# SUSE Janssen Installation

Before you install, check the [VM system requirements](vm-requirements.md).

## Supported versions

- SUSE Linux Enterprise Server (SLES) 15
- openSUSE Leap 15.4

## Install the Package

- If the server firewall is running, make sure you allow `https`, which is
needed for OpenID and FIDO.

```shell
firewall-cmd --permanent --zone=public --add-service=https
```

```shell
firewall-cmd --reload
```

- Download the GPG key zip file , unzip and import GPG key

```shell
wget https://github.com/JanssenProject/jans/files/11814522/automation-jans-public-gpg.zip
```

```shell
unzip automation-jans-public-gpg.zip
```

```shell
rpm -import automation-jans-public-gpg.asc
```

- Download the release package from the GitHub Janssen Project
  [Releases](https://github.com/JanssenProject/jans/releases)

```shell
wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-replace-janssen-version-suse15.x86_64.rpm
```

- Verify integrity of the downloaded package using published `sha256sum`.

Download `sha256sum` file for the package

```shell
wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-replace-janssen-version-suse15.x86_64.rpm.sha256sum
```

Check the hash if it is matching. You may need to change your working directory
to where both the rpm and sha256sum file are located.

```shell
sha256sum -c jans-replace-janssen-version-suse15.x86_64.rpm.sha256sum
```

Output similar to below should confirm the integrity of the downloaded package.

```text
<package-name>: OK
```

- Install the package

```
zypper install ~/jans-replace-janssen-version-suse15.x86_64.rpm
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

After the successful completion of setup process,
[verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

## Log in to Text User Interface (TUI)

Begin configuration by accessing the TUI with the following command:

```
/opt/jans/jans-cli/jans_cli_tui.py
```

Full TUI documentation can be found [here](../../config-guide/jans-tui/README.md)

## Uninstall

Uninstall process involves two steps

1. Uninstall Janssen Server
2. Remove and purge the `jans` package

If you have not run the Jans setup script, you can skip step 1 and just remove
the package.

First, run command below to uninstall the Janssen server

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

Second uninstall the package:

```
rpm -qa | grep jans
```

And then use `zypper remove <package>`
