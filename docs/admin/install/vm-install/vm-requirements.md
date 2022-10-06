---
tags:
  - administration
  - installation
  - vm
---

# VM System Requirements

## Hardware Requirements

Janssen Server needs below-mentioned minimal resources on VM when data store is installed separately on a different VM.
Requirements for VM hosting the data store (i.e LDAP, RDBMS, etc.) can vary based on size data and type of data store.

### Development and Test Environments 
- 4 GB RAM
- 2 CPU
- 20 GB Disk

### Production Environments 
- 8 GB RAM
- 4 CPU
- 4 GB swap space
- 50 GB Disk

## VM Setup Guidelines

- Janssen Server must be deployed on a server or VM with static IP address
- Static IP address should resolve to a hostname. `localhost` is not supported. This can be achieved by adding entry 
  to `/etc/hosts` file on an Ubuntu system for example.
- For local testing and development purposes, VM can be setup using VMWare Workstation player

### Ports

Janssen Server requires following ports to be open for incoming connections

| Port | Protocol | Notes           |
|------|----------|-----------------|
| 80   | TCP      | Forwards to 443 |
| 443  | TCP      | apache2/httpd   |
| 22   | TCP      | ssh             |