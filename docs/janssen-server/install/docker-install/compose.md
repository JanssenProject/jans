---
tags:
- administration
- installation
- quick-start
- docker compose
- docker image
---
# Docker compose
> **Warning**
> This image is for testing and development purposes only. Use Janssen [helm charts](https://github.com/JanssenProject/jans/tree/main/charts/janssen) for production setups.

Docker monolith image packaging for Janssen. This image packs janssen services including the auth-server, config-api, fido2, and scim.

## Pre-requisites

- [Docker](https://docs.docker.com/install)
- [Docker compose](https://docs.docker.com/compose/install/)

## Environment Variables

Installation depends on the set of environment variables shown below. These environment variables can be set to customize installation as per the need. If not set, the installer uses default values.

| ENV                        | Description                                                                                                                                                          | Default                                          |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| CN_HOSTNAME                | Hostname for the deployment (FQDN)                                                                                                                                   | `demoexample.jans.io`                           |
| CN_ADMIN_PASS              | Default admin password                                                                                                                                               | `Test1234#`                                     |
| CN_ORG_NAME                | Name of organization                                                                                                                                                 | `Janssen`                                       |
| CN_EMAIL                   | Email address of administrator                                                                                                                                       | `support@jans.io`                               |
| CN_CITY                    | City                                                                                                                                                                 | `Austin`                                        |
| CN_STATE                   | State                                                                                                                                                                | `TX`                                            |
| CN_COUNTRY                 | Two-letter country code                                                                                                                                              | `US`                                            |
| CN_INSTALL_LDAP            | Enable LDAP                                                                                                                                                          | `true`                                          |
| CN_INSTALL_CONFIG_API      | Enable installation of config-api                                                                                                                                    | `true`                                          |
| CN_INSTALL_SCIM            | Enable installation of SCIM                                                                                                                                          | `true`                                          |
| CN_INSTALL_FIDO2           | Enable installation of FIDO2                                                                                                                                         | `true`                                          |
| CN_INSTALL_CASA            | Enable installation of Casa                                                                                                                                          | `false`                                         |
| CN_INSTALL_SAML            | Enable installation of SAML                                                                                                                                          | `false`                                         |
| CN_INSTALL_LOCK            | Enable installation of Lock                                                                                                                                          | `false`                                         |
| CN_INSTALL_KC_LINK         | Enable installation of KC Link                                                                                                                                       | `false`                                         |
| CN_INSTALL_MYSQL           | Install MySQL                                                                                                                                                        | `false`                                         |
| CN_INSTALL_PGSQL           | Install PostgreSQL                                                                                                                                                   | `false`                                         |
| TEST_USERS_PREFIX_STRING   | Prefix used for test users created during setup                                                                                                                      | `test_user`                                     |
| CN_INSTALL_NO_PROMPT       | Run setup without prompting for values                                                                                                                               | `true`                                          |
| CN_OCI_BUILD_ARGS          | Additional build arguments passed to OCI builder                                                                                                                     | ``                                              |

## Download compose and supporting files

Download the compose file for the persistence you want.

=== "MySQL"
    ```bash
    wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/jans-mysql-compose.yml 
    ```
=== "PostgreSQL"
    ```bash 
    wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/jans-postgres-compose.yml 
    ```

Download the script files

```bash
wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/up.sh \
wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/down.sh \
wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/clean.sh
```

Give execute permission to the scripts

`chmod u+x up.sh down.sh clean.sh`

## Create and Start Containers

`up.sh` script invokes the appropriate compose file based on parameter passed. You can pass `mysql` or `pgsql` as an argument to the script. If you don't pass any, it will default to mysql.

It creates two containers, a janssen monolith container and a container for persistence.

!!! Troubleshooting Tip
    Sometimes the command below runs into an error regarding TLS handshake timeout while trying to connect
    to docker registry. Try restarting docker services. Root-cause of this error is not known.
    ```bash
    service docker restart
    ```

=== "MySQL"
    ```bash
    ./up.sh mysql 
    ```
=== "PostgreSQL"
    ```bash
    ./up.sh pgsql
    ```

To view the containers running

=== "MySQL"
    ```bash
    docker compose -f jans-mysql-compose.yml ps
    ```
=== "PostgreSQL"
    ```bash
    docker compose -f jans-postgres-compose.yml ps
    ```

To stop the containers.

```bash
./down.sh #You can pass mysql|pgsql as an argument to the script. If you don't pass any, it will default to mysql.
```

## Configure Janssen Server

1. Access the Docker container shell using:
    ```bash
    docker compose -f jans-mysql-compose.yml exec jans /bin/bash #This opens a bash terminal in the running container
    ```

2. You can grab `client_id` and `client_pw`(secret) pairs and other values from `setup.properties` or `/opt/jans/jans-setup/setup.properties.last`

3. Use the CLI tools located under `/opt/jans/jans-cli/` to configure Janssen as needed. For example you can run the [TUI](https://docs.jans.io/head/admin/config-guide/config-tools/jans-tui/):
    ```bash
    python3 /opt/jans/jans-cli/config-cli-tui.py
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

```bash
./clean.sh #You can pass mysql|pgsql as an argument to the script. If you don't pass any, it will default to mysql.
```
