---
tags:
- administration
- installation
- quick-start
- docker
---

# Quick Start Installation

The quickest way to get a Janssen Server up and running is to install a Docker container-based fully featured Janssen Server on an Ubuntu 22.04 system.

!!! Note

    This method of installation is suitable only for testing, development, or feature exploration purposes. Not for production deployments.  

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Install

Run the command given below to start the installation.

Installation depends on a [set of environment variables](https://github.com/JanssenProject/jans/tree/main/docker-jans-monolith#environment-variables).
These environment variables can be set to customize installation as per the need. If not set, the installer uses default values.

```bash
wget https://raw.githubusercontent.com/JanssenProject/jans/main/automation/startjanssenmonolithdemo.sh && chmod u+x startjanssenmonolithdemo.sh && sudo bash startjanssenmonolithdemo.sh demoexample.jans.io MYSQL
```

## Verify Installation By Accessing Standard Endpoints

To access Janssen Server standard endpoints from outside of the Docker container, systems `/etc/hosts` file needs to be updated. Open the file and add the IP domain record which should be the IP of the instance docker is installed. And the domain used in the env above `CN_HOSTNAME`.

```bash
# For-example
172.22.0.3      demoexample.jans.io
```

After adding the record, hit the standard endpoints such as 

```
https://demoexample.jans.io/.well-known/openid-configuration
```

## Configure Janssen Server

Access the Docker container shell using:

```bash
docker exec -ti docker-jans-monolith-jans-1 bash
```

And then use CLI tools to configure Janssen Server as needed.

```bash
/opt/jans/jans-cli/config-cli.py
#or
/opt/jans/jans-cli/scim-cli.py
```
