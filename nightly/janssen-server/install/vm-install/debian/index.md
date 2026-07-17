# Debian Janssen Installation

Before you install, check the [VM system requirements](https://docs.jans.io/nightly/janssen-server/install/vm-install/vm-requirements/index.md).

## Install the Package

### Debian 13 (Trixie)

- Download the release package from the GitHub Janssen Project [Releases](https://github.com/JanssenProject/jans/releases/latest)

  Command

  ```
  wget https://github.com/JanssenProject/jans/releases/download/nightly/jans_0.0.0-nightly.debian13_amd64.deb -P /tmp
  ```

- Go to `/tmp` directory:

  Command

  ```
  cd /tmp
  ```

- Verify the cryptographic signature using cosign (primary verification):

  Note

  Install the [cosign CLI](https://docs.sigstore.dev/cosign/system_config/installation/) if not already installed.

  - Download the cosign bundle from the [Releases](https://github.com/JanssenProject/jans/releases/latest) page:

    Command

    ```
    wget https://github.com/JanssenProject/jans/releases/download/nightly/jans-debian13-0.0.0-nightly.bundle -P /tmp
    ```

  - Verify the signature:

    Command

    ```
    cosign verify-blob \
      --bundle jans-debian13-0.0.0-nightly.bundle \
      --certificate-identity-regexp "https://github.com/JanssenProject/jans" \
      --certificate-oidc-issuer https://token.actions.githubusercontent.com \
      jans_0.0.0-nightly.debian13_amd64.deb
    ```

    Output similar to below confirms the package was signed by the Janssen CI pipeline:

    Output

    ```
    Verified OK
    ```

- Optionally, verify integrity using the published checksum file (secondary check):

  Command

  ```
  echo 'paste-release-sha256sum jans_0.0.0-nightly.debian13_amd64.deb' | sed 's/^sha256://' >jans_0.0.0-nightly.debian13_amd64.deb.sha256sum && sha256sum -c jans_0.0.0-nightly.debian13_amd64.deb.sha256sum
  ```

  Output similar to below should confirm the integrity of the downloaded package.

  Output

  ```
  jans_0.0.0-nightly.debian13_amd64.deb: OK
  ```

- Install the package

Command

```
sudo apt install ./jans_0.0.0-nightly.debian13_amd64.deb
```

## Run the setup script

- Run the setup script in interactive mode:

Command

```
sudo python3 /opt/jans/jans-setup/setup.py
```

See more detailed [instructions](https://docs.jans.io/nightly/janssen-server/install/setup/index.md) on the setup script if you're confused how to answer any of the questions, for details about command line arguments, or you would prefer to use a properties file instead of interactive mode.

## Verify the Installation

After the successful completion of setup process, [verify the system health](https://docs.jans.io/nightly/janssen-server/install/install-faq/#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

## Log in to Text User Interface (TUI)

Begin configuration by accessing the TUI with the following command:

Command

```
jans tui
```

Full TUI documentation can be found [here](https://docs.jans.io/nightly/janssen-server/config-guide/config-tools/jans-tui/index.md)

If you have selected casa during installation you can access casa using url `https://<host>/jans-casa`

## Let's Encrypt

To enable communication with Janssen Server over tls (https) in production environment, Janssen Server needs details about CA certificate.

To generate Let's Encrypt CA certificate follow this [let's encrypt](https://github.com/JanssenProject/jans/blob/main/docs/contribute/developer-faq.md#how-to-get-certificate-from-lets-encrypt).

## Uninstall

Uninstall process involves two steps and removes all the Janssen Server components.

Note

For removal of the attached persistence store, please refer to [this note](https://docs.jans.io/nightly/janssen-server/install/install-faq/#does-the-janssen-server-uninstall-process-remove-the-data-store-as-well).

1. Delete files installed by Janssen
1. Remove and purge the `jans` package

Use the command below to uninstall the Janssen server

Command

```
sudo python3 /opt/jans/jans-setup/install.py -uninstall
```

You'll see the following confirmation:

Output

```
This process is irreversible.
You will lose all data related to Janssen Server.



Are you sure to uninstall Janssen Server? [yes/N] yes

Uninstalling Janssen Server...
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

Command

```
apt-get --purge remove jans
```

Which should result in the following:

Output

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
Removing jans (0.0.0-nightly~debian13_amd64) ...
Checking to make sure service is down...
```
