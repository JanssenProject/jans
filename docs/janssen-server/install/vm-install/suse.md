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

## Install the Package

- If the server firewall is running, make sure you allow `https`, which is
needed for OpenID and FIDO.

```shell
sudo firewall-cmd --permanent --zone=public --add-service=https
```

```shell
sudo firewall-cmd --reload
```

- for SLES, we need to enable PackageHub as per OSversion and architecture
```
sudo SUSEConnect -p PackageHub/15.5/x86_64

```

- Download the release package from the GitHub Janssen Project
  [Releases](https://github.com/JanssenProject/jans/releases/latest)

```shell
wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-replace-janssen-version-stable.suse16.x86_64.rpm -P ~/
```

- Verify the cryptographic signature using cosign (primary verification):

    !!! Note
        Install the [cosign CLI](https://docs.sigstore.dev/cosign/system_config/installation/) if not already installed.

    - Download the cosign bundle from the [Releases](https://github.com/JanssenProject/jans/releases/latest) page:

        ```bash title="Command"
        wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-suse16-replace-janssen-version.bundle
        ```

    - Verify the signature:

        ```bash title="Command"
        cosign verify-blob \
          --bundle jans-suse16-replace-janssen-version.bundle \
          --certificate-identity-regexp "https://github.com/JanssenProject/jans" \
          --certificate-oidc-issuer https://token.actions.githubusercontent.com \
          jans-replace-janssen-version-stable.suse16.x86_64.rpm
        ```

        Output similar to below confirms the package was signed by the Janssen CI pipeline:

        ```text title="Output"
        Verified OK
        ```

- Optionally, verify integrity using the published checksum file (secondary check):

    ```bash title="Command"
    wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-replace-janssen-version-stable.suse16.x86_64.rpm.sha256sum
    sha256sum -c jans-replace-janssen-version-stable.suse16.x86_64.rpm.sha256sum
    ```

    Output similar to below should confirm the integrity of the downloaded package.

      ```text
      jans-replace-janssen-version-stable.suse16.x86_64.rpm: OK
      ```

- Install the package

```
sudo zypper install ~/jans-replace-janssen-version-stable.suse16.x86_64.rpm
```

!!! note "Expected zypper prompt during install"
    zypper will display a warning that the package header is not GPG-signed:

    ```text
    jans-...: Package header is not signed!
    Signature verification failed [6-File is unsigned]
    Abort, retry, ignore? [a/r/i] (a):
    ```

    This is expected. Janssen uses keyless cosign signing (verified in the step
    above) rather than embedding a GPG signature in the RPM header. Enter `i`
    to ignore this prompt and proceed with the installation.

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

After the successful completion of setup process,
[verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

## Log in to Text User Interface (TUI)

Begin configuration by accessing the TUI with the following command:

```bash
jans tui
```

Full TUI documentation can be found [here](../../config-guide/config-tools/jans-tui/README.md)

If you have selected casa during installation you can access casa using url ```https://<host>/jans-casa```

## Enabling HTTPS

To enable communication with Janssen Server over TLS (https) in a production
environment, Janssen Server needs details about CA certificate. Update the
HTTPS cofiguration file `https_jans.conf` as shown below:

!!! Note
    Want to use `Let's Encrypt` to get a certificate? Follow [this guide](../../../contribute/developer-faq.md#how-to-get-certificate-from-lets-encrypt).

- Open `https_jans.conf`
  ```bash
  sudo vi /etc/apache2/vhosts.d/_https_jans.conf
  ```

- Update `SSLCertificateFile` and `SSLCertificateKeyFile` parameters values
  ```bash
  SSLCertificateFile location_of_fullchain.pem
  SSLCertificateKeyFile location_of_privkey.pem
  ```

- Restart `httpd` service for changes to take effect
  ```bash
  sudo /usr/sbin/rcapache2 restart
  ```

## Uninstall

Uninstall process involves two steps and removes all the Janssen Server components.

!!! Note
    For removal of the attached persistence store, please refer to [this note](../install-faq.md#does-the-janssen-server-uninstall-process-remove-the-data-store-as-well).

1. Uninstall Janssen Server
2. Remove and purge the `jans` package

If you have not run the Jans setup script, you can skip step 1 and just remove
the package.

First, run command below to uninstall the Janssen server

```
sudo python3 /opt/jans/jans-setup/install.py -uninstall
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
