# Install Janssen Server using Ubuntu Linux Package

## Supported Versions
- Ubuntu 20.04

!!! note
    SELinux should be disabled

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Installation Steps

```
apt install wget curl
```
```
wget http:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*ubuntu20.04_amd64.deb' | head -n 1) -O /tmp/jans.ubuntu20.04_amd64.deb
```
```
apt install -y /tmp/jans.ubuntu20.04_amd64.deb
```
```
sudo python3 /opt/jans/jans-setup/setup.py
```
