# Install Janssen Server using dynamic download

## System Requirements
- 4 GB RAM
- 2 CPU
- 20 GB Disk

## Supported Linux distributions
- Enterprise Linux 8 (CentOS 8 and Red Hat 8)
- Ubuntu 20
- SUSE 15

## Steps for installation

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