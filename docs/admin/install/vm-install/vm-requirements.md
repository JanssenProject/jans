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

- Janssen Server must be deployed on a server or VM with static IP address. For instance, on an Ubuntu based desktop 
  or server, [these instructions](https://linuxize.com/post/how-to-configure-static-ip-address-on-ubuntu-20-04/#configuring-static-ip-address-on-ubuntu-server) can help setup static ip.   
- Static IP address should resolve to a hostname. `localhost` is not supported. This can be achieved by adding entry 
  to `/etc/hosts` file on an Ubuntu system for example.
- For local testing and development purposes, VM can be setup using VMWare Workstation player
- Janssen Server requires setting the `file descriptors` to 65k. Take guidance from steps listed [here](https://gluu.org/docs/gluu-server/4.4/installation-guide/#file-descriptors-fd)

## Ports

Janssen Server requires following ports to be open for incoming connections

| Port | Protocol | Notes           |
|------|----------|-----------------|
| 80   | TCP      | Forwards to 443 |
| 443  | TCP      | apache2/httpd   |
| 22   | TCP      | ssh             |

Ensuring above ports are open on an Ubuntu system can be done by following below mentioned steps:

1. To check the status of these ports in Ubuntu, use the following commands (other OS have similar commands)

  ```
  ufw status verbose
  ```

2. If the status is found to be inactive, enable it using 

  ```
  ufw enable
  ```

3. The default for ufw is to deny incoming and allow outgoing. To reset your setting to default :

```
ufw default deny incoming
ufw default allow outgoing
```

4. Reset ufw

```
ufw reset
```

5. If for any reason the ports are closed, allow connections by:

```
ufw allow <port>
```

Ports 443, 80, and 22 must be accessible.

