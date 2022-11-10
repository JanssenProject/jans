# Overview

**This image is for testing and development purposes only! Use Janssen [helm charts](../charts) for production setups**

Docker monolith image packaging for Janssen.This image packs janssen services including, the auth-server, config-api, fido2, and scim.

## Versions

See [Releases](https://github.com/JanssenProject/docker-jans-monolith/releases) for stable versions. This image should never be used in production.
For bleeding-edge/unstable version, use `janssenproject/monolith:1.0.4_dev`.

## Environment Variables

The following environment variables are supported by the container:

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


## Pre-requisites

- [Docker](https://docs.docker.com/install). Docker compose should be installed by default with Docker.

## How to run

```bash
docker compose -f jans-mysql-compose.yml up -d
```

## Clean up

Remove setup and volumes

```
docker compose -f jans-mysql-compose.yml down && rm -rf jans-*
```

## Test

```bash
docker exec -ti docker-jans-monolith-jans-1 bash
```

Run 
```bash
/opt/jans/jans-cli/config-cli.py
#or
/opt/jans/jans-cli/scim-cli.py
```

## Access endpoints externally

Add to your `/etc/hosts` file the ip domain record which should be the ip of the instance docker is installed at and the domain used in the env above `CN_HOSTNAME`.

```bash
# For-example
172.22.0.3      demoexample.jans.io
```

After adding the record you can hit endpoints such as https://demoexample.jans.io/.well-known/openid-configuration

## Quick start 

Grab a fresh ubuntu 22.04 lts VM and run:

```bash
wget https://raw.githubusercontent.com/JanssenProject/jans/main/automation/startjanssenmonolithdemo.sh && chmod u+x startjanssenmonolithdemo.sh && sudo bash startjanssenmonolithdemo.sh demoexample.jans.io MYSQL
```

