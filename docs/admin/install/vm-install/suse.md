---
tags:
- administration
- installation
- vm
- SUSE
- SLES
---

# Install Janssen Server using the SUSE Linux Enterprise Server (SLES) Package

Janssen Server can be installed using the Linux package for SLES 

## Supported versions
- SUSE Linux Enterprise Server (SLES) 15

!!! note
[SELinux](https://wiki.ubuntu.com/SELinux) should be disabled

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Prerequisites

- `curl` should be installed. This can be easily installed using the command below

 ```
 zypper install curl
 ```

## Install

- Download and install package from the Janssen Project site using the command below

```
zypper --no-gpg-checks install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*suse15.x86_64.rpm' | head -n 1)
```

- Initiate the setup process using the command below. The setup process will prompt for user for inputs.

```
python3 /opt/jans/jans-setup/setup.py
```

## Verify the Installation

After the successful completion of setup process, [verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

