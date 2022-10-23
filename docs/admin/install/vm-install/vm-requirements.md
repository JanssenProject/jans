---
tags:
- requirements
- installation
- vm
---

# VM System Requirements

The Janssen Project currently provides packages for these Linux distros:

- Ubuntu (versions: 20.04)
- SUSE (SLES or LEAP) (version: 15)
- RedHat Enterprise Linux (version: 8)

## Hardware Requirements

A single-VM deployment is where all services are running on one server. Although,
the requirements can vary based on the size of the data and the required
concurrency, the following guidelines can help you plan:

### Development and Test Environments
- 4 GB RAM
- 2 CPU
- 20 GB Disk

### Production Environment Recommendation:
- 8 GB RAM
- 4 CPU
- 4 GB swap space
- 50 GB Disk

## Port Configuration

Janssen Server requires the following ports to be open for incoming connections.

| Port | Protocol | Notes           |
|------|----------|-----------------|
| 443  | TCP      | TLS/HTTP        |

You may want to use a redirect on port 80 to 443, although it is not required.
Of course you will also need some way to login to your server, but that is
out of scope of these docs.

Check your server firewall documentation to configure your firewall to
allow `https`.

## Hostname / IP Address Configuration

It is recommended that you use a static ip address for your Janssen Server.
Your server should also return the hostname for the `hostname` command,
it's recommended that you add the hostname to the `/etc/hosts` file.

## File Descriptor Configuration (FD)

Like most database and Internet servers, you must have at least 65k file
descriptors. If you don't, your server will hang.

First, check the current file descriptor limit using command below. If the
existing FD limit exceeds 65535, then you're good.

```text
# cat /proc/sys/fs/file-max
```

If FD limit is less than 65535 (e.g. 1024), then follow the steps below to
increase the value.

1) Set soft and hard limits by adding the following lines in the
`/etc/security/limits.conf` file

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

4) Use the `ulimit` command to set the FD limit to the hard limit specified
in `/etc/security/limits.conf`. If setting to hard limit doesn't work, then
try to set it to the soft limit.

```text
ulimit -n 262144
```

5) Restart the system
