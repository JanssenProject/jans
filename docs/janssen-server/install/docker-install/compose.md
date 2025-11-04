---
title: Docker Compose
---

# Installing and Running Janssen with Docker Compose

This document provides instructions for running Janssen using Docker Compose. It's designed as a flexible deployment tool for development, testing, and light proof-of-concept use cases where a straightforward deployment is needed on a single machine (single node deployment) either on-premises or in the cloud. For production environments or for high availability and scalability, a Kubernetes-based deployment is recommended.

## Prerequisites

- Install [Docker](https://docs.docker.com/engine/install/) and [Docker Compose](https://docs.docker.com/compose/install/).
- Depending on your operating system and the architecture of your CPU, you may need to build local images. See [building local images](#building-custom-images-optional)

## System Requirements

The following system requirements are as per the single mode deployment:

| Component | CPU Unit | RAM  | Disk Space                |
|-----------|----------|------|---------------------------|
| Janssen   | 2.5      | 8 GB | 40 GB                     |
| MySQL     | 1        | 1 GB | Included in above 40 GB   |
| PostgreSQL| 1        | 1 GB | Included in above 40 GB   |

## Supported Persistence Backends

The Janssen Docker Compose setup supports two persistence backends:

1. MySQL
2. PostgreSQL

## Environment Variables

| ENV                        | Description                                                                                                                                                          | Default                                         |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| CN_HOSTNAME                | Hostname to install Janssen with                                                                                                                                    | `demoexample.jans.io`                           |
| CN_ADMIN_PASS              | Admin password to log in to the UI                                                                                                                                  | `Test1234#`                                     |
| CN_ORG_NAME                | Organization name                                                                                                                                                    | `Janssen`                                       |
| CN_EMAIL                   | Email to register Janssen with                                                                                                                                      | `support@jans.io`                               |
| CN_CITY                    | City to register Janssen with                                                                                                                                       | `Austin`                                        |
| CN_STATE                   | State to register Janssen with                                                                                                                                      | `TX`                                            |
| CN_COUNTRY                 | Country to register Janssen with                                                                                                                                    | `US`                                            |
| TEST_CLIENT_ID             | Test client id                                                                                                                                                       | `9876baac-de39-4c23-8a78-674b59df8c09`          |
| TEST_CLIENT_SECRET         | Test client secret                                                                                                                                                   | `1t5Fin3#`                                      |
| TEST_CLIENT_TRUSTED        | Whether client is trusted                                                                                                                                            | `true`                                          |
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
wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/up.sh
wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/down.sh
wget https://raw.githubusercontent.com/JanssenProject/jans/main/docker-jans-monolith/clean.sh
```

Give execute permission to the scripts

`chmod u+x up.sh down.sh clean.sh`

## Create and Start Containers

`up.sh` script invokes the appropriate compose file based on parameter passed. You can pass `mysql` or `pgsql` as an argument to the script. If you don't pass any, it will default to mysql.

It creates two containers, a janssen monolith container and a container for persistence.

!!! Troubleshooting Tip
    Sometimes the command below runs into an error regarding TLS handshake timeout while trying to connect to the backend persistence. Just re-run the command until successful.

```bash
./up.sh #You can pass mysql|pgsql as an argument to the script. If you don't pass any, it will default to mysql.
```

## Shut down Containers

To shut down the deployment, run:

```bash
./down.sh #You can pass mysql|pgsql as an argument to the script. If you don't pass any, it will default to mysql.
```

## Configure Janssen Server

1. Access the Docker container shell using:

    ```bash
    docker compose -f jans-mysql-compose.yml exec jans /bin/bash #This opens a bash terminal in the running container
    ```

1. You can grab `client_id` and `client_pw`(secret) pairs and other values from `setup.properties` or `/opt/jans/jans-setup/setup.properties.last`

1. Use the CLI tools located under `/opt/jans/jans-cli/` to configure Janssen as needed. For example you can run the [TUI](https://docs.jans.io/head/admin/config-guide/config-tools/jans-tui/):

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

## Building custom images (optional)

In environments where the default image is not compatible with the architecture available or doesn't work (for example, M-series Macs), you can build a custom image using:

```bash
docker build -t <image-name>:<tag> .
```

Replace the image line in the compose file to use your newly built image:

```yaml
services:
  jans:
    image: <image-name>:<tag> # Replace with the name and tag specified during build
```
