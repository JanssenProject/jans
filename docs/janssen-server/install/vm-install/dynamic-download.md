---
tags:
- administration
- installation
- vm
- dynamic
---

# Install Janssen Server using Dynamic Download

The dynamic download installs the latest development version of the Janssen Server.

!!! Note

    This method of installation is suitable only for testing, development, or feature exploration purposes. Not for production deployments.

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Prerequisites

- `curl` should be installed

## Install

1. Download the installer
```
curl https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-linux-setup/jans_setup/install.py > install.py
```

1. Execute Installer

   To execute installer, run the command below:

   ```text
   python3 install.py
   ```

   The installer can be invoked using various options to tailor the 
   installation process. Invoke the commands below to know about all the 
   available options.

   ```
   python3 install.py --help
   ```

   One particularly useful option for development environments is the one 
   below which installs the Janssen Server with test data loaded.

   ```    
   python3 install.py --args="-t"
   ```

   Installer can be run with all the arguments provided via command-line
   at the time of invocking the install script. This enables prompt-less
   installation. For example, see the command below:

   ```bash
   python3 install.py --args="-n -ip-address 10.229.38.28 -host-name jans-opensuse -city ah -state gj -country in -org-name gluu -email dhaval@gluu.org -jans-max-mem 26349 -admin-password admin --with-casa --install-jans-keycloak-link -t"
   ```



## Uninstall

Use the command below to uninstall the Janssen Server

!!! Note
For removal of the attached persistence store, please refer to [this note](../install-faq.md#does-the-janssen-server-uninstall-process-remove-the-data-store-as-well).

```
python3 install.py -uninstall
```
