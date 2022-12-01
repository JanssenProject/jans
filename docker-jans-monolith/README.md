---
tags:
- administration
- installation
- quick-start
- docker compose
---

> **Warning**
> This image is for testing and development purposes only. Use Janssen [helm charts](https://github.com/JanssenProject/jans/tree/main/charts/janssen) for production setups.

## Overview

Docker monolith image packaging for Janssen. This image packs janssen services including the auth-server, config-api, fido2, and scim.

## Pre-requisites

- [Docker](https://docs.docker.com/install)
- [Docker compose](https://docs.docker.com/compose/install/)


## Environment Variables

Installation depends on the set of environment variables shown below. These environment variables can be set to customize installation as per the need. If not set, the installer uses default values.

| ENV                     | Description                                      | Default                                          |
|-------------------------|--------------------------------------------------|--------------------------------------------------|
| `CN_HOSTNAME`           | Hostname to install janssen with.                | `demoexample.jans.io`                            |
| `CN_ADMIN_PASS`         | Password of the admin user.                      | `1t5Fin3#security`                               |
| `CN_ORG_NAME`           | Organization name. Used for ssl cert generation. | `Janssen`                                        |
| `CN_EMAIL`              | Email. Used for ssl cert generation.             | `support@jans.io`                                |
| `CN_CITY`               | City. Used for ssl cert generation.              | `Austin`                                         |
| `CN_STATE`              | State. Used for ssl cert generation              | `TX`                                             |
| `CN_COUNTRY`            | Country. Used for ssl cert generation.           | `US`                                             |
| `CN_INSTALL_LDAP`       | **NOT SUPPORRTED YET**                           | `false`                                          |
| `CN_INSTALL_CONFIG_API` | Installs the Config API service.                 | `true`                                           |
| `CN_INSTALL_SCIM`       | Installs the SCIM  API service.                  | `true`                                           |
| `CN_INSTALL_FIDO2`      | Installs the FIDO2 API service.                  | `true`                                           |
| `MYSQL_DATABASE`        | MySQL jans database.                             | `jans`                                           |
| `MYSQL_USER`            | MySQL database user.                             | `jans`                                           |
| `MYSQL_PASSWORD`        | MySQL database user password.                    | `1t5Fin3#security`                               |
| `MYSQL_HOST`            | MySQL host.                                      | `mysql` which is the docker compose service name |


## How to run

Download the compose file 

```bash

wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/jans-mysql-compose.yml 
```

This docker compose file runs two containers, the janssen monolith container and mysql container.

```bash
docker compose -f jans-mysql-compose.yml up -d
```

To view the containers running

```bash

docker compose -f jans-mysql-compose.yml ps
```

## Configure Janssen Server

```bash

docker compose -f jans-mysql-compose.yml exec jans sh #This opens a bash terminal in the running container

/opt/jans/jans-cli/config-cli.py #configure using the config-cli

/opt/jans/jans-cli/scim-cli.py #configure using the scim-cli
```

## Access endpoints externally

Add to your `/etc/hosts` file the ip domain record which should be the ip of the instance docker is installed at and the domain used in the env above `CN_HOSTNAME`.

```bash
# For-example
172.22.0.3      demoexample.jans.io
```

After adding the record you can hit endpoints such as https://demoexample.jans.io/.well-known/openid-configuration

## Clean up

Remove setup and volumes

```
docker compose -f jans-mysql-compose.yml down
```

