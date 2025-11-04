---
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
