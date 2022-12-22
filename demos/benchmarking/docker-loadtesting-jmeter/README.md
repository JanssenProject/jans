## Overview

Docker Load Test image packaging for Janssen. This image can load test users to a janssen environment and can execute jmeter tests.

## Pre-requisites

- [Docker](https://docs.docker.com/install)

## Environment Variables

Installation depends on the set of environment variables shown below. These environment variables can be set to customize installation as per the need. If not set, the installer uses default values.

| ENV                        | Description                                      | Default                                          |
|----------------------------|--------------------------------------------------|--------------------------------------------------|
| `FQDN`                     | Hostname to install janssen with.                | `https://demoexample.jans.io`                    |
| `AUTHZ_CLIENT_ID`          | Password of the admin user.                      | `1t5Fin3#security`                               |
| `AUTHZ_CLIENT_SECRET`      | Organization name. Used for ssl cert generation. | `Janssen`                                        |
| `ROPC_CLIENT_ID`           | Email. Used for ssl cert generation.             | `support@jans.io`                                |
| `ROPC_CLIENT_SECRET`       | City. Used for ssl cert generation.              | `Austin`                                         |
| `FIRST_BATCH_MIN`          | State. Used for ssl cert generation              | `TX`                                             |
| `FIRST_BATCH_MAX`          | Country. Used for ssl cert generation.           | `US`                                             |
| `SECOND_BATCH_MIN`         | **NOT SUPPORRTED YET**                           | `false`                                          |
| `SECOND_BATCH_MAX`         | Installs the Config API service.                 | `true`                                           |
| `RUN_AUTHZ_TEST`           | Installs the SCIM  API service.                  | `true`                                           |
| `RUN_ROPC_TEST`            | Installs the FIDO2 API service.                  | `true`                                           |
| `TEST_USERS_PREFIX_STRING` | MySQL jans database.                             | `jans`                                           |
| `THREADCOUNT`              | MySQL database user.                             | `jans`                                           |
| `COUCHBASE_URL`            | MySQL database user password.                    | `1t5Fin3#security`                               |
| `COUCHBASE_PW`             | MySQL host.                                      | `mysql` which is the docker compose service name |


## How to run

