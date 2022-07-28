# Install Janssen Server using Ubuntu Linux package

## Supported versions
- Ubuntu 20.04

!!! note
    SELinux should be disabled

## Steps for installation

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
