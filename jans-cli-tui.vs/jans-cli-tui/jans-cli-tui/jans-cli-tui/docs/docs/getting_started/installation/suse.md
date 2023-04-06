# Install Janssen Server using SUSE Linux Package

## Supported versions
- SUSE 15

!!! note
    SELinux should be disabled

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Installation Steps

```
zypper install curl
```
```
zypper --no-gpg-checks install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*suse15.x86_64.rpm' | head -n 1)
```
```
sudo python3 /opt/jans/jans-setup/setup.py
```