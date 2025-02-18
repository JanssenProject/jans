---
tags:
- administration
- installation
- vm
- RHEL
---

# Red Hat EL Janssen Installation

Before you install, check the [VM system requirements](vm-requirements.md).

## Supported versions

- Red Hat Enterprise Linux 8 (RHEL 8)

## Install the Package

- Install EPEL and mod-auth-openidc as dependencies

```
sudo yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
sudo yum -y module enable mod_auth_openidc 
```

- Download the GPG key zip file , unzip and import GPG key

```shell
wget https://github.com/JanssenProject/jans/files/11814522/automation-jans-public-gpg.zip
```

```shell
unzip automation-jans-public-gpg.zip
```

```shell
sudo rpm -import automation-jans-public-gpg.asc
```

- Download the release package from the Github Janssen Project
  [Releases](https://github.com/JanssenProject/jans/releases)

```
wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-replace-janssen-version-stable.el8.x86_64.rpm -P ~/
```

- Verify integrity of the downloaded package using published `sha256sum`.

    Download `sha256sum` file for the package

    ```shell
    wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-replace-janssen-version-stable.el8.x86_64.rpm.sha256sum -P ~/
    ```

    Check the hash if it is matching.

    ```shell
    sha256sum -c jans-replace-janssen-version-stable-el8.x86_64.rpm.sha256sum
    ```

    Output similar to below should confirm the integrity of the downloaded package.

    ```text
    jans-replace-janssen-version-stable-el8.x86_64.rpm: OK
    ```
  
- Install the package

```
sudo yum install ~/jans-replace-janssen-version-stable.el8.x86_64.rpm
```

## Run the setup script

- Run the setup script in interactive mode:

```
sudo python3 /opt/jans/jans-setup/setup.py
```

The installer should confirm successful installation with a message similar
to the one shown below:

![](../../../assets/image-jans-install-success.png)

See more detailed [instructions](../setup.md) on the setup script if you're
confused how to answer any of the questions, for details about command line
arguments, or you would prefer to use a properties file instead of
interactive mode.

## Verify the Installation

After the successful completion of setup process, [verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

## Log in to Text User Interface (TUI)

Begin configuration by accessing the TUI with the following command:

```bash
jans tui
```

Full TUI documentation can be found [here](../../config-guide/config-tools/jans-tui/README.md)

If you have selected casa during installation you can access casa using url``` https://<host>/jans-casa ```

## Enabling HTTPS

To enable communication with Janssen Server over TLS (https) in a production
environment, Janssen Server needs details about CA certificate. Update the
HTTPS cofiguration file `https_jans.conf` as shown below:

!!! Note
    Want to use `Let's Encrypt` to get a certificate? Follow [this guide](../../../contribute/developer-faq.md#how-to-get-certificate-from-lets-encrypt).

- Open `https_jans.conf`

  ```bash
  sudo vi /etc/httpd/conf.d/https_jans.conf
  ```

- Update `SSLCertificateFile` and `SSLCertificateKeyFile` parameters values

  ```bash
  SSLCertificateFile location_of_fullchain.pem
  SSLCertificateKeyFile location_of_privkey.pem
  ```

- Restart `httpd` service for changes to take effect

  ```bash
  sudo service httpd restart
  ```
  
## Uninstall

Uninstall process involves two steps and removes all the Janssen Server components.

!!! Note
For removal of the attached persistence store, please refer to [this note](../install-faq.md#does-the-janssen-server-uninstall-process-remove-the-data-store-as-well).

1. Delete files installed by Janssen
1. Remove and purge the `jans` package

- Use the command below to uninstall the Janssen server

```
sudo python3 /opt/jans/jans-setup/install.py -uninstall
```

Console output like below will confirm the successful uninstallation of the Janssen Server

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

- Remove the linux package

Use the command below to remove and purge `jans` package

```
yum remove jans.x86_64
```

Successful removal will remove the Janssen Server package along with
the removal of all the unused dependencies.
