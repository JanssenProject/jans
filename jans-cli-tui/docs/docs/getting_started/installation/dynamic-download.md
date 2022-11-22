# Install Janssen Server using Dynamic Download

Dynamic download installs the latest development version of Janssen Server. This installation method is suitable for setting up development environments.

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Supported Linux Distributions
- Enterprise Linux 8 (CentOS 8 and Red Hat 8)
- Ubuntu 20
- SUSE 15

## Installation Steps

1. Download installer
```
curl https://raw.githubusercontent.com/JanssenProject/jans/main/jans-linux-setup/jans_setup/install.py > install.py
```

2. Execute installer
```    
python3 install.py
```

3. Uninstalling Janssen Server
```
python3 install.py -uninstall
```