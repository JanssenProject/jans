## Overview

Docker Load Test image packaging for Janssen. This image can load test users to a janssen environment and can execute jmeter tests.

## Pre-requisites

- [Docker](https://docs.docker.com/install)

## Environment Variables

Installation depends on the set of environment variables shown below. These environment variables can be set to customize loading users and testing.

## Parameters

### Loading users

| ENV                              | Description                                                                                                   | Default                |
|----------------------------------|---------------------------------------------------------------------------------------------------------------|------------------------|
| `TEST_USERS_PREFIX_STRING`       | The user prefix string attached to the test users loaded                                                      | `test_user`            |
| `USER_NUMBER_STARTING_POINT`     | The user number to start from . This is appended to the username i.e test_user0                               | `0`                    |
| `USER_NUMBER_ENDING_POINT`       | The user number to end at.                                                                                    | `50000000`             |
| `LOAD_USERS_TO_RDBMS`            | Enable loading users to RDBMS persistence. `true` or `false` == ``                                            | `false`                |
| `USER_SPLIT_PARALLEL_THREADS`    | The number of parallel threads to break the total number users across. This number heavily effects CPU usage. | `20`                   |
| `RDBMS_TYPE`                     | RDBMS type if `mysql` or `pgsql` is the persistence to load users in.                                         | `mysql`                |
| `RDBMS_DB`                       | RDBMS Database name if `mysql` or `pgsql` is the persistence to load users in.                                | `jans`                 |
| `RDBMS_USER`                     | RDBMS user if `mysql` or `pgsql` is the persistence to load users in.                                         | `jans`                 |
| `RDBMS_PASSWORD`                 | RDBMS user password if `mysql` or `pgsql` is the persistence to load users in. .                              | ``                     |
| `RDBMS_HOST`                     | RDBMS host if `mysql` or `pgsql` is the persistence to load users in.                                         | `localhost`            |

### Running tests

| ENV                                | Description                                              | Default                       |
|------------------------------------|----------------------------------------------------------|-------------------------------|
| `FQDN`                             | The hostname janssen is using.                           | `https://demoexample.jans.io` |
| `CLIENT_CREDENTIALS_CLIENT_ID`     | Authorization code client id                             | ``                            |
| `CLIENT_CREDENTIALS_CLIENT_SECRET` | Authorization code client id                             | ``                            |
| `AUTHZ_CLIENT_ID`                  | Authorization code client id                             | ``                            |
| `AUTHZ_CLIENT_SECRET`              | Authorization code client secret                         | ``                            |
| `ROPC_CLIENT_ID`                   | Resource grant client id                                 | ``                            |
| `ROPC_CLIENT_SECRET`               | Resource grant client secret                             | ``                            |
| `RUN_CLIENT_CREDENTIALS_TEST`      | Run the client credentials test                          | `false`                       |
| `RUN_AUTHZ_TEST`                   | Run the Authorization code test                          | `false`                       |
| `RUN_ROPC_TEST`                    | Run the ROPC test                                        | `false`                       |
| `TEST_USERS_PREFIX_STRING`         | The user prefix string attached to the test users loaded | `test_user`                   |
| `THREAD_COUNT`                     | Thread count in jmeter tests                             | `200`                         |
| `USER_NUMBER_STARTING_POINT`       | The user number to start from .                          | `0`                           |
| `USER_NUMBER_ENDING_POINT`         | The user number to end at.                               | `50000000`                    |


## How to run

### Kubernetes

#### Loading users

Edit the custom connection values and other envs for the [load_users_rdbms_job.yaml](./yaml/load-users/load_users_rdbms_job.yaml) file and run:

```bash
kubectl apply -f load_users_<persistence_of_choice>_job.yaml
```

Wait for the job to finish before running any tests.

#### Running tests

##### Authorization code flow

1. Create the client needed to run the test by executing the following. Make sure to change the `FQDN`  :

```bash
cat << EOF > auth_code_client.json
{
    "dn": null,
    "inum": null,
    "displayName": "Auth Code Flow Load Test Client",
    "redirectUris": [
      "https://FQDN"
    ],
    "responseTypes": [
      "id_token",
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "implicit",
      "refresh_token"
    ],
    "tokenEndpointAuthMethod": "client_secret_basic",
    "scopes": [
      "openid",
      "profile",
      "email",
      "user_name"
    ],
    "trustedClient": true,
    "includeClaimsInIdToken": false,
    "accessTokenAsJwt": false,
    "disabled": false,
    "deletable": false,
    "description": "Auth Code Flow Load Testing Client"
}
EOF
```

Download or build [config-cli-tui](../../../jans-cli-tui) and run:

```bash
# add -noverify if your fqdn is not registered
./config-cli-tui.pyz --host <FQDN> --client-id <ROLE_BASED_CLIENT_ID> --client-secret <ROLE_BASED_CLIENT_SECRET> --no-tui --operation-id=post-oauth-openid-client --data=auth_code_client.json
```

Save the client id and secret from the response and enter them along with any custom property in the yaml [file](yaml/load-test/load_test_auth_code.yaml) then execute :

```bash
kubectl apply -f load_test_auth_code.yaml
```

##### Resource owner password credentials flow

1. Create the client needed to run the test by executing the following. Make sure to change the `FQDN`  :

```bash
cat << EOF > ropc_client.json
{
    "dn": null,
    "inum": null,
    "displayName": "ROPC Flow Load Test Client",
    "redirectUris": [
      "https://FQDN"
    ],
    "responseTypes": [
      "id_token",
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "implicit",
      "refresh_token",
      "password"
    ],
    "tokenEndpointAuthMethod": "client_secret_basic",
    "scopes": [
      "openid",
      "profile",
      "email",
      "user_name"
    ],
    "trustedClient": true,
    "includeClaimsInIdToken": false,
    "accessTokenAsJwt": false,
    "disabled": false,
    "deletable": false,
    "description": "ROPC Flow Load Testing Client"
}
EOF
```

Run the follwing:

```bash
# add -noverify if your fqdn is not registered
./config-cli-tui.pyz --host <FQDN> --client-id <ROLE_BASED_CLIENT_ID> --client-secret <ROLE_BASED_CLIENT_SECRET> --no-tui --operation-id=post-oauth-openid-client --data=ropc_client.json
```

Save the client id and secret from the response and enter them along with any custom property in the yaml [file](yaml/load-test/load_test_ropc.yaml) then execute :

```bash
kubectl apply -f load_test_ropc.yaml
```
