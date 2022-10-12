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


## Installation Steps

```

```
```
zypper --no-gpg-checks install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*suse15.x86_64.rpm' | head -n 1)
```
```
sudo python3 /opt/jans/jans-setup/setup.py
```