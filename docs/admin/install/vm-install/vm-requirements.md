---
tags:
- administration
- installation
- vm
---

# VM System Requirements

Janssen Server can be installed on a VM with any of the supported operating systems mentioned below:

- Ubuntu (versions: 20.04)
- SUSE Linux Enterprise Server (versions: 15)
- RedHat Enterprise Linux (versions: 7,8)

## Hardware Requirements

Janssen Server needs the below-mentioned minimum resources on VM when the data store is installed separately on a different VM.

### Development and Test Environments
- 4 GB RAM
- 2 CPU
- 20 GB Disk

### Production Environments
- 8 GB RAM
- 4 CPU
- 4 GB swap space
- 50 GB Disk

Requirements for VM hosting the data store (i.e LDAP, RDBMS, etc.) can vary based on the size of the data and type of the data store.

## VM Setup Guidelines

Ensure that VM hosting the Janssen Server is configured based on the following guidelines:

- Required ports should be open. Refer to [Port Configuration](#port-configuration) section for OS specific steps.
- Janssen Server must be deployed on a server or VM with a static IP address. Refer to [Static IP Address Configuration](#static-ip-address-configuration) section for OS specific instructions.
- Static IP address should resolve to a hostname. `localhost` is not supported. Refer to [Hostname Configuration](#hostname-configuration) section for OS specific instructions.
- For local testing and development purposes, VM can be set up using VMWare Workstation player. Often, VM tools keep the IP address of the VM consistent across restarts. And most of the VM ports are also accessible. Hence some of the configuration described in this document may not be required for local testing and development.
- Janssen Server requires setting the `file descriptors` to 65k. Refer to [File Descriptor Configuration](#file-descriptor-configuration-fd) section for OS specific instructions.

## Port Configuration

Janssen Server requires the following ports to be open for incoming connections.

| Port | Protocol | Notes           |
|------|----------|-----------------|
| 80   | TCP      | Forwards to 443 |
| 443  | TCP      | apache2/httpd   |
| 22   | TCP      | ssh             |


### Ubuntu

Ensuring the above ports are open on an Ubuntu system can be done by below mentioned steps:

1) Check the status of these ports

  ```
  ufw status verbose
  ```

2) If the ufw status is `inactive`, enable it using

  ```
  ufw enable
  ```


3) If the ports are closed, allow connections by:

```
ufw allow <port>
```

### SLES and RHEL

SUSE Linux Enterprise Server and Red Hat Enterprise Linux use `Firewalld` to control the network access. Use the commands below to configure the ports.

1) Get zone for the network interface

```text
firewall-cmd --get-zone-of-interface=<your-network-interface>
```

2) Add port

```text
firewall-cmd --zone <zone-name> --permanent --add-port 443/tcp
```

3) Restart firewalld

```text
systemctl stop firewalld
systemctl start firewalld
```

## Static IP Address Configuration

Janssen Server must be deployed on a server or VM with a static IP address.

This section describes the steps required to set static IP for supported OS platforms. Commands and steps mentioned here might have changed at the time of use, if so please refer to respective OS documentation for the most up-to-date steps to set up static IP.

First, Select the network interface for which static IP needs to be set. On any Linux based OS platform, run the command below:

```text
ip link
```

The above command lists all the existing network interfaces in format below:

```text
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN mode DEFAULT group default qlen 1000
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
2: enp4s0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UP mode DEFAULT group default qlen 1000
    link/ether 8c:8c:aa:6a:bf:b5 brd ff:ff:ff:ff:ff:ff
```

here we are going to configure the `enp4s0` network interface.

### Ubuntu

The steps listed below show how to set up a static IP address on an Ubuntu Server.

1) Locate the configuration file

Network interface configuration can be changed using `YAML` configuration files located under

```text
/etc/netplan
```

Above directory will contain one or more `YAML` files. Open the file that has the configuration for `enp4s0` network
interface. Create one if it doesn't exist.

2) Update the configuration

Set the yaml file configuration as shown in the example below. Values for the gateway and the nameservers should be set
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

3) Apply the change

```text
sudo netplan apply
```

4) Verify the new configuration

```text
ip addr show dev enp4s0
```
The newly assigned IP address can be seen in the output

### SUSE Linux Enterprise Server

SLES provides [YaST](https://yast.opensuse.org/) tool to manage the system configuration.

1) Open YaST

```text
yast
```

2) Navigate to `System` -> `Network Settings` and select the network interface that needs to be set to static IP.

![](../../../assets/suse-staticIP-yast-select-network.png)

3) Hit `F4` to enter edit mode as shown in the image below. Select `Statically Assiged IP Address` instead of `DHCP`. Also provide the required details for `IP address`, `Subnet Mask`, and `Hostname`.

![](../../../assets/suse-staticIP-yast-edit-network.png)

### RedHat Enterprise Linux

RHEL provides `Nmcli` tool to configure and manage the VM network.

1) Set IP address for selected network interface

```text
nmcli con mod enp4s0 ipv4.addresses <static-IP>/24
```

2) Set the appropriate gateway

```text
nmcli con mod enp4s0 ipv4.gateway <gateway-IP>
nmcli con mod enp4s0 ipv4.method manual
```

3) Configure DNS

```text
nmcli con mod enp4s0 ipv4.dns "<dns-IP>"
```

4) Reload configuration

```text
nmcli con up enp4s0 
```

5) See the configuration being reflected at

```text
cat /etc/sysconfig/network-scripts/ifcfg-enp4s0
```

## Hostname Configuration

IP can be mapped to a hostname(FQDN) using entries into `/etc/hosts` file. Run the command below to configure a hostname for the IP address.

```text
vi /etc/hosts
```

Make an entry similar to one below in this file. IP should be a static IP assigned to the server or VM.

```text
<static-IP> jans.op.io
```

## File Descriptor Configuration (FD)

Janssen Server requires setting the `file descriptors` to 65k. Follow the steps below to set the value for file descriptors. These steps apply to SUSE Linux Enterprise, Ubuntu Server, and RedHat Enterprise Linux.

First, check the current file descriptor limit using command below. If the existing FD limit exceeds 65535, then continue with the same.

```text
# cat /proc/sys/fs/file-max
```

In case the existing FD limit is less than 65535, then follow the steps below to set the value.

1) Set soft and hard limits by adding the following lines in the `/etc/security/limits.conf` file

```text
* soft nofile 65535
* hard nofile 262144
```

2) Add the following lines to `/etc/pam.d/login` if not already present

```text
session required pam_limits.so
```

3) Increase the FD limit in `/proc/sys/fs/file-max`

```text
echo 65535 > /proc/sys/fs/file-max**
```

4) Use the `ulimit` command to set the FD limit to the hard limit specified in `/etc/security/limits.conf`. If setting to hard limit doesn't work, then try to set it to the soft limit.

```text
ulimit -n 262144
```

5) Restart the system
