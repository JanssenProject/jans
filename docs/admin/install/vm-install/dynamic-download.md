---
tags:
- administration
- installation
- vm
- Dynamic
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
curl https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/install.py > install.py
```

1. Execute Installer

    The installer can be invoked using various options to tailor the installation process. Invoke the commands below to know about all the available options.

    ```
    install.py --help
    setup.py --help
    ```

    One particularly useful option for development environments is the one below which installs the Janssen Server with test data loaded.

     ```    
     python3 install.py --args="-t"
     ```

    To install without test data loaded, run the command below:

     ```text
     python3 install.py
     ```

## Uninstall

Use the command below to uninstall the Janssen Server

```
python3 install.py -uninstall
```
