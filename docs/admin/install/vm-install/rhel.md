# Install Janssen Server using Enterprise Linux Package

## Supported versions
- Red Hat 8
- CentOS 8

!!! note
    SELinux should be disabled

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Installation Steps

```
yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm
```
```
yum module enable mod_auth_openidc
```
```
yum install curl
```
```
yum install -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*el8.x86_64.rpm' | head -n 1)
```
```
sudo python3 /opt/jans/jans-setup/setup.py
```