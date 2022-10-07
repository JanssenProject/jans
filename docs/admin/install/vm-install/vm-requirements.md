---
tags:
  - administration
  - installation
  - vm
---

# VM System Requirements

Janssen Server can be installed on any of the supported operating systems mentioned below:

- Ubuntu (versions: 20.04)
- SUSE Linux Enterprise Server (versions: 15)
- RedHat Enterprise Linux (versions: 7,8)

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

- Required ports should be open. Refer to [Port Setup](#port-setup) section for OS specific steps.
- Janssen Server must be deployed on a server or VM with static IP address. Refer to [Configure Static IP Address](#configure-static-ip-address) section for OS specific steps.
- Static IP address should resolve to a hostname. `localhost` is not supported. Refer to [Hostname Setup](#port-setup) section for OS specific steps.
- For local testing and development purposes, VM can be setup using VMWare Workstation player
- Janssen Server requires setting the `file descriptors` to 65k. Take guidance from steps listed [here](https://gluu.org/docs/gluu-server/4.4/installation-guide/#file-descriptors-fd)

## Port Configuration

Janssen Server requires following ports to be open for incoming connections

| Port | Protocol | Notes           |
|------|----------|-----------------|
| 80   | TCP      | Forwards to 443 |
| 443  | TCP      | apache2/httpd   |
| 22   | TCP      | ssh             |


### Ubuntu

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

## Static IP Address Configuration

Janssen Server must be deployed on a server or VM with static IP address. 

### Ubuntu 

Steps listed below show how to set up static IP address on an Ubuntu Server.

#### Select the network interface

```text
ip link
```

Above command shows lists all the existing network interfaces in format below:

```text
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp4s0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP mode DEFAULT group default qlen 1000
    link/ether 8c:8c:aa:6a:bf:b5 brd ff:ff:ff:ff:ff:ff
```

here we are going to configure the `enp4s0` network interface.

#### Locate the configuration file

Network interface configuration can be changed using `YAML` configuration files located under

```text
/etc/netplan
```

Above directory will contain one or more `YAML` files. Open the file that has configuration for `enp4s0` network 
interface. Create one if it doesn't exist.

#### Update the configuration

Set the yaml file configuration as shown in example below. Values for gateway, nameservers should be set 
appropriately. `addresses` should be set to desired static IP.

```text
network:
  version: 2
  renderer: networkd
  ethernets:
    enp4s0:
      dhcp4: no
      addresses:
        - 192.168.123.212/24
      gateway4: 192.168.123.1
      nameservers:
          addresses: [8.8.8.8, 1.1.1.1]
```

#### Apply the change

```text
sudo netplan apply
```

#### Verify the new configuration

```text
ip addr show dev enp4s0
```
Newly assigned IP address can be seen in the output

## Hostname Configuration

### Ubuntu

In Ubuntu systems, IP can be mapped to a hostname(FQDN) using entries into `/etc/hosts` file. Run command below to configure a hostname for IP address.

```text
vi /etc/hosts
```

Make an entry similar to one below in this file. IP could be a static IP assigned to the server or VM.

```text
192.168.0.1 jans.op.io
```

## File Descriptor Configuration