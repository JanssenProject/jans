---
tags:
  - administration
  - upgrade
  - vm
---

# Overview
If jans server is already installed and if we need to upgrade jans 
use below to upgrade jans.

Please use the left navigation menu to browse the content of this section while we are still working on developing content for `Overview` page.

**jans upgrade on  Ubuntu  **

1) if you want to upgrade to latest, download latest package file
```
wget http:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*ubuntu20.04_amd64.deb' | head -n -O /tmp/jans.ubuntu20.04_amd64.deb
```
2) run below upgrade command
```
sudo apt upgrade -y /tmp/jans.ubuntu20.04_amd64.deb
```
3) verify upgraded version
```
apt-cache policy jans
```

**jans upgrade on RHEL/CentOS8**

1) if you want to upgrade to latest,download and upgrade latest package file
```
yum upgrade -y https:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*el8.x86_64.rpm' | head -n 
```
2) verify upgraded version
```
rpm -qa |grep jans
```

**jans upgrade on Suse**
1) if you want to upgrade to latest,download and upgrade latest package file
```
wget http:$(curl -s -L https://api.github.com/repos/JanssenProject/jans/releases/latest | egrep -o '/.*suse15.x86_64.rpm' | head -n -O ./jans-1.0.13.nightly-suse15.x86_64.rpm

```
2) upgrade package
```
sudo rpm -U jans-1.0.13.nightly-suse15.x86_64.rpm
```
3) verify upgraded version
```
rpm -qa |grep jans
```


!!! Contribute
    If you’d like to contribute to this document, get started with the [Contribution Guide](https://docs.jans.io/head/CONTRIBUTING/#contributing-to-the-documentation)
