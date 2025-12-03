# Cloud SQL Connector

This module provides support for connecting to Google Cloud SQL instances (PostgreSQL and MySQL) from Cloud Run services using Cloud SQL Connectors with Private IP.

## Overview

The Cloud SQL Connectors provide a secure and efficient way to connect to Cloud SQL instances without managing IP allowlists, SSL certificates, or network configurations manually. The connectors handle secure tunnel establishment and automatic SSL/TLS encryption.

**Note**: This implementation uses standard SQL username/password authentication. For IAM database authentication, additional configuration would be required.

## Architecture

The Janssen Project uses two Cloud SQL Connectors to support both Python and Java components:

| Component | Connector | Purpose |
|-----------|-----------|---------|
| Python services (jans-pycloudlib) | Cloud SQL Python Connector | SQLAlchemy database connections |
| Java services (jans-auth-server, etc.) | Cloud SQL JDBC Socket Factory | JDBC database connections |

Both connectors use the same environment variables (`CN_SQL_CLOUDSQL_CONNECTOR_ENABLED`, `CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME`) for consistent configuration.

---

## Python Connector

### Supported Databases

| Database | Driver | SQLAlchemy URL |
|----------|--------|----------------|
| PostgreSQL | pg8000 | `postgresql+pg8000://` |
| MySQL | pymysql | `mysql+pymysql://` |

## Installation

The Cloud SQL Python Connector is included as a base dependency in jans-pycloudlib:

```bash
pip install jans-pycloudlib
```

This automatically includes:
- `cloud-sql-python-connector[pg8000]>=1.0.0`
- `pg8000>=1.30.0`
- `pymysql>=1.0.2` (for MySQL connections)

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `CN_SQL_CLOUDSQL_CONNECTOR_ENABLED` | Enable Cloud SQL Python Connector | Yes | `false` |
| `CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME` | Cloud SQL instance connection name (format: `project:region:instance`) | Yes (when enabled) | - |
| `CN_SQL_DB_USER` | Database username | Yes | `jans` |
| `CN_SQL_DB_NAME` | Database name | Yes | `jans` |
| `CN_SQL_PASSWORD_FILE` | Path to file containing database password | Yes | `/etc/jans/conf/sql_password` |
| `CN_SQL_DB_DIALECT` | Database dialect (`pgsql`, `postgresql`, or `mysql`) | Yes | `mysql` |

### Example Configuration - PostgreSQL

```bash
export CN_SQL_CLOUDSQL_CONNECTOR_ENABLED=true
export CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME=my-project:us-central1:my-postgres-instance
export CN_SQL_DB_USER=jans
export CN_SQL_DB_NAME=jans
export CN_SQL_DB_DIALECT=pgsql
```

### Example Configuration - MySQL

```bash
export CN_SQL_CLOUDSQL_CONNECTOR_ENABLED=true
export CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME=my-project:us-central1:my-mysql-instance
export CN_SQL_DB_USER=jans
export CN_SQL_DB_NAME=jans
export CN_SQL_DB_DIALECT=mysql
```

## Implementation Details

The implementation uses:

- **`creator` argument**: SQLAlchemy's `create_engine` is called with a `creator` function that returns connections from the Cloud SQL Connector.
- **LAZY refresh strategy**: The connector is initialized with `refresh_strategy='LAZY'` to defer token refresh until needed.
- **Private IP**: Connections use `IPTypes.PRIVATE` for connecting to Cloud SQL instances via Private IP.

### Architecture

The Cloud SQL Connector functionality is implemented using a shared mixin pattern:

- **`CloudSqlConnectorMixin`**: Base mixin class that provides:
  - Connector lifecycle management (`_ensure_connector`, `close`)
  - Instance name validation (`_get_instance_connection_name`)
  - Connection creator factory (`get_cloudsql_connection_creator`)
  - `cloudsql_connector_enabled` property

- **`PostgresqlAdapter`**: Inherits from `CloudSqlConnectorMixin`, sets `cloudsql_driver = "pg8000"`
- **`MysqlAdapter`**: Inherits from `CloudSqlConnectorMixin`, sets `cloudsql_driver = "pymysql"`

This design eliminates code duplication while allowing each adapter to specify its driver.

### Creating a Manager Instance

To create a proper `Manager` instance, use the following method:

```python
from jans.pycloudlib.manager import Manager

manager = Manager()
manager.bootstrap()  # handles asset bootstrap, e.g. Vault's RoleID and SecretID, for configs and secrets layers
```

or use the shortcut instead:

```python
from jans.pycloudlib import get_manager

manager = get_manager()
```

### Code Example - PostgreSQL (Context Manager)

```python
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib import get_manager

import os
os.environ["CN_SQL_CLOUDSQL_CONNECTOR_ENABLED"] = "true"
os.environ["CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME"] = "project:region:instance"
os.environ["CN_SQL_DB_DIALECT"] = "pgsql"
os.environ["CN_SQL_DB_USER"] = "jans"
os.environ["CN_SQL_DB_NAME"] = "jans"

manager = get_manager()

with SqlClient(manager) as client:
    if client.connected():
        print("Successfully connected to Cloud SQL PostgreSQL via Private IP")
```

### Code Example - MySQL (Context Manager)

```python
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib import get_manager

import os
os.environ["CN_SQL_CLOUDSQL_CONNECTOR_ENABLED"] = "true"
os.environ["CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME"] = "project:region:instance"
os.environ["CN_SQL_DB_DIALECT"] = "mysql"
os.environ["CN_SQL_DB_USER"] = "jans"
os.environ["CN_SQL_DB_NAME"] = "jans"

manager = get_manager()

with SqlClient(manager) as client:
    if client.connected():
        print("Successfully connected to Cloud SQL MySQL via Private IP")
```

### Resource Cleanup

The `SqlClient` class implements proper resource cleanup for both the SQLAlchemy engine and the Cloud SQL Connector:

**Using Context Manager (Recommended)**:
```python
with SqlClient(manager) as client:
    # Use the client
    pass
# Resources automatically cleaned up
```

**Manual Cleanup**:
```python
client = SqlClient(manager)
try:
    # Use the client
    pass
finally:
    client.close()
```

**Automatic Cleanup**: An `atexit` handler is registered to clean up any remaining `SqlClient` instances when the Python interpreter shuts down.

---

## Java JDBC Socket Factory

The Janssen Project's Java services (jans-auth-server, jans-config-api, etc.) use the Cloud SQL JDBC Socket Factory for database connections when the Cloud SQL Connector is enabled.

### How It Works

When `CN_SQL_CLOUDSQL_CONNECTOR_ENABLED=true`, the Java services use a different JDBC connection URL format that leverages the Cloud SQL JDBC Socket Factory instead of direct TCP connections:

**Standard Connection (Cloud SQL Connector disabled):**
```
jdbc:mysql://10.13.0.3:3306/jans?enabledTLSProtocols=TLSv1.2
jdbc:postgresql://10.13.0.3:5432/jans
```

**Cloud SQL Connector Connection (Cloud SQL Connector enabled):**
```
jdbc:mysql:///jans?cloudSqlInstance=project:region:instance&socketFactory=com.google.cloud.sql.mysql.SocketFactory&serverTimezone=UTC
jdbc:postgresql:///jans?cloudSqlInstance=project:region:instance&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

### Included JARs

The following Cloud SQL JDBC Socket Factory JARs are included in the Janssen Docker images:

| Database | JAR | Maven Coordinates |
|----------|-----|-------------------|
| MySQL | `mysql-socket-factory-connector-j-8-X.X.X.jar` | `com.google.cloud.sql:mysql-socket-factory-connector-j-8` |
| PostgreSQL | `postgres-socket-factory-X.X.X.jar` | `com.google.cloud.sql:postgres-socket-factory` |

These JARs are downloaded directly to the Java classpath (`/opt/jans/jetty/<component>/custom/libs`) during Docker image build and are always available.

### Configuration

The Java services use the same environment variables as the Python connector:

| Variable | Description | Required |
|----------|-------------|----------|
| `CN_SQL_CLOUDSQL_CONNECTOR_ENABLED` | Enable Cloud SQL JDBC Socket Factory | Yes |
| `CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME` | Cloud SQL instance connection name (format: `project:region:instance`) | Yes (when enabled) |
| `CN_SQL_DB_NAME` | Database name | Yes |
| `CN_SQL_DB_DIALECT` | Database dialect (`mysql` or `pgsql`) | Yes |

### Why Use JDBC Socket Factory?

When connecting from Cloud Run to Cloud SQL via Private IP, standard JDBC connections may fail with SSL certificate verification errors like:

```
java.security.cert.CertificateException: Server identity verification failed.
None of the certificate's DNS or IP Subject Alternative Name entries matched the server hostname/IP '10.13.0.3'.
```

This happens because:
1. Cloud SQL's SSL certificates have SANs for DNS names, not internal IPs
2. The JDBC driver tries to verify the certificate against the IP address
3. Verification fails because the IP isn't in the certificate's SANs

The Cloud SQL JDBC Socket Factory solves this by:
1. Establishing a secure tunnel to Cloud SQL
2. Handling SSL/TLS encryption automatically
3. Using Google's Application Default Credentials (ADC) for authentication to the Cloud SQL Admin API

### Example Deployment

```bash
# Set environment variables for Cloud Run
gcloud run deploy jans-auth-server \
  --image=ghcr.io/janssenproject/jans/auth-server:latest \
  --vpc-connector=my-vpc-connector \
  --set-env-vars="CN_SQL_CLOUDSQL_CONNECTOR_ENABLED=true" \
  --set-env-vars="CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME=my-project:us-central1:my-instance" \
  --set-env-vars="CN_SQL_DB_NAME=jans" \
  --set-env-vars="CN_SQL_DB_DIALECT=mysql"
```

---

## MANDATORY CHECKLIST: IAM and VPC Requirements

Before deploying your Cloud Run service with Cloud SQL Connector, ensure all the following requirements are met:

### IAM Requirements

- [ ] **Cloud SQL Client Role**: The Cloud Run service account must have the `roles/cloudsql.client` IAM role on the Cloud SQL instance.

  ```bash
  gcloud projects add-iam-policy-binding PROJECT_ID \
    --member="serviceAccount:SERVICE_ACCOUNT_EMAIL" \
    --role="roles/cloudsql.client"
  ```

- [ ] **Cloud SQL Instance User Role** (Optional but recommended): For IAM database authentication, grant `roles/cloudsql.instanceUser`.

  ```bash
  gcloud projects add-iam-policy-binding PROJECT_ID \
    --member="serviceAccount:SERVICE_ACCOUNT_EMAIL" \
    --role="roles/cloudsql.instanceUser"
  ```

- [ ] **Service Account Configured**: The Cloud Run service is configured to use a service account with the above roles (not the default compute service account in production).

### VPC Connector Requirements

- [ ] **VPC Network Created**: A VPC network exists that can reach the Cloud SQL instance's Private IP.

- [ ] **Serverless VPC Access Connector Created**: A VPC Access connector is created in the same region as your Cloud Run service.

  ```bash
  gcloud compute networks vpc-access connectors create CONNECTOR_NAME \
    --region=REGION \
    --network=VPC_NETWORK \
    --range=IP_RANGE  # e.g., 10.8.0.0/28
  ```

- [ ] **Cloud Run Service Configured with VPC Connector**: The Cloud Run service is deployed with the VPC connector.

  ```bash
  gcloud run deploy SERVICE_NAME \
    --image=IMAGE_URL \
    --vpc-connector=CONNECTOR_NAME \
    --vpc-egress=private-ranges-only
  ```

### Cloud SQL Instance Requirements

- [ ] **Private IP Enabled**: The Cloud SQL instance has Private IP enabled.

  ```bash
  gcloud sql instances patch INSTANCE_NAME \
    --network=VPC_NETWORK \
    --no-assign-ip  # Optional: disable public IP
  ```

- [ ] **Private Services Access Configured**: Private services access is configured for the VPC network.

  ```bash
  gcloud compute addresses create google-managed-services-NETWORK \
    --global \
    --purpose=VPC_PEERING \
    --prefix-length=16 \
    --network=VPC_NETWORK

  gcloud services vpc-peerings connect \
    --service=servicenetworking.googleapis.com \
    --ranges=google-managed-services-NETWORK \
    --network=VPC_NETWORK
  ```

- [ ] **Database User Created**: A database user exists with the credentials specified in environment variables.

### Network Firewall Rules

- [ ] **Egress Allowed**: The VPC network allows egress traffic to the Cloud SQL instance's Private IP on the appropriate port:
  - PostgreSQL: Port 5432
  - MySQL: Port 3306

- [ ] **No Conflicting Firewall Rules**: No firewall rules block traffic from the VPC connector's IP range to the Cloud SQL instance.

### Verification Steps

1. **Test Connectivity**: Deploy a test Cloud Run service and verify it can connect to Cloud SQL.

2. **Check Logs**: Review Cloud Run logs for connection errors or IAM permission issues.

3. **Verify IAM Bindings**:
   ```bash
   gcloud projects get-iam-policy PROJECT_ID \
     --flatten="bindings[].members" \
     --filter="bindings.role:roles/cloudsql.client"
   ```

4. **Verify VPC Connector Status**:
   ```bash
   gcloud compute networks vpc-access connectors describe CONNECTOR_NAME \
     --region=REGION
   ```

---

## Backward Compatibility

The Cloud SQL Connector is **opt-in** and disabled by default. Existing deployments using standard connections (psycopg2 for PostgreSQL, pymysql for MySQL) will continue to work without any changes.

To migrate to Cloud SQL Connector:

1. Complete the IAM and VPC checklist above
2. Set `CN_SQL_CLOUDSQL_CONNECTOR_ENABLED=true`
3. Set `CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME` to your instance connection name
4. Deploy your updated Cloud Run service

## Troubleshooting

### Common Issues

1. **"Permission denied" or IAM errors**
   - Verify the service account has `roles/cloudsql.client` role
   - Check that the correct service account is attached to Cloud Run

2. **Connection timeout**
   - Verify the VPC connector is properly configured
   - Check that the Cloud SQL instance has Private IP enabled
   - Ensure firewall rules allow egress to the instance

3. **"Could not connect to Cloud SQL instance"**
   - Verify `CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME` format: `project:region:instance`
   - Check that the instance exists and is running

4. **MySQL strict mode errors**
   - The Cloud SQL Connector still applies MySQL strict mode settings
   - Check your data for compatibility with strict mode
