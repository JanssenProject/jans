---
tags:
- administration
- installation
- vm
- RHEL
- RedHat
---

# Install Janssen Server using Enterprise Linux Package

## Supported versions
- Red Hat 8
- CentOS 8

!!! note
[SELinux](https://wiki.ubuntu.com/SELinux) should be disabled

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Prerequisites

- `curl` should be instlled. This can be easily installed using command below

 ```
 apt install curl
 ```

## Install

- Install Extra Packages for Enterprise Linux (EPEL)

```
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
```

- Enable the `mod_auth_openidc` module

```
yum module enable mod_auth_openidc
```

- Download and Install Janssen Server Linux package

```
yum install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*el8.x86_64.rpm' | head -n 1)
```

- Initiate setup process using command below. Setup process will prompt for user for inputs.

```
sudo python3 /opt/jans/jans-setup/setup.py
```

## Verify the Installation

After the successful completion of setup process, [verify the system health](../install-faq.md#after-installation-how-do-i-verify-that-the-janssen-server-is-up-and-running).

