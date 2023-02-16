---
tags:
- administration
- installation
- quick-start
- docker
---

!!! Warning 
    **This image is for testing and development purposes only. Use Janssen [helm charts](https://github.com/JanssenProject/jans/tree/main/charts/janssen) for production setups.**

## Overview

The quickest way to get a Janssen Server up and running is to install a Docker container-based fully featured Janssen Server.

## System Requirements

System should meet [minimum VM system requirements](vm-requirements.md)

## Install

Installation depends on a [set of environment variables](https://github.com/JanssenProject/jans/tree/main/docker-jans-monolith#environment-variables).
These environment variables can be set to customize installation as per the need. If not set, the installer uses default values.

Run this command to start the installation:

```bash
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/startjanssenmonolithdemo.sh && chmod u+x startjanssenmonolithdemo.sh && sudo bash startjanssenmonolithdemo.sh demoexample.jans.io MYSQL
```

Console messages like below confirms the successful installation:

```
[+] Running 3/3
 ⠿ Network docker-jans-monolith_cloud_bridge  Created                      0.0s
 ⠿ Container docker-jans-monolith-mysql-1     Started                      0.6s
 ⠿ Container docker-jans-monolith-jans-1      Started                      0.9s
 
Waiting for the Janssen server to come up. Depending on the resources it may take 3-5 mins for the services to be up.
Testing openid-configuration endpoint.. 
```

As can be seen, the install script also accesses the well-known endpoints to varify that Janssen Server is responsive.

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

## Uninstall / Remove the Janssen Server

This docker based installation uses `docker compose` under the hood to create containers. Hence uninstalling Janssen server involves invoking `docker compose` with appropriate yml file. Run command below to stop and remove containers.

```
docker compose -f /tmp/jans/docker-jans-monolith/jans-mysql-compose.yml down && rm -rf jans-*
```

Console messages like below confirms the successful removal:

```
[+] Running 3/3
 ⠿ Container docker-jans-monolith-jans-1      Removed                   10.5s
 ⠿ Container docker-jans-monolith-mysql-1     Removed                    0.9s
 ⠿ Network docker-jans-monolith_cloud_bridge  Removed                    0.1s
```
