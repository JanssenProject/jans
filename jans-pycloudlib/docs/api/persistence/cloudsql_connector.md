# Cloud SQL Python Connector

This module provides support for connecting to Google Cloud SQL instances (PostgreSQL and MySQL) from Cloud Run services using the Cloud SQL Python Connector with Private IP.

## Overview

The Cloud SQL Python Connector provides a secure and efficient way to connect to Cloud SQL instances without managing IP allowlists, SSL certificates, or network configurations manually. The connector handles secure tunnel establishment and automatic SSL/TLS encryption.

**Note**: This implementation uses standard SQL username/password authentication. For IAM database authentication, additional configuration would be required (setting `enable_iam_auth=True` in the connector).

## Supported Databases

| Database | Driver | SQLAlchemy URL |
|----------|--------|----------------|
| PostgreSQL | pg8000 | `postgresql+pg8000://` |
| MySQL | pymysql | `mysql+pymysql://` |

## Installation

Install the optional Cloud SQL dependencies:

```bash
pip install 'jans-pycloudlib[cloudsql]'
```

This installs:
- `cloud-sql-python-connector[pg8000]>=1.0.0`
- `pg8000>=1.30.0`

**Note**: For MySQL connections, `pymysql` is already included as a base dependency.

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

### Code Example - PostgreSQL

```python
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.manager import Manager

import os
os.environ["CN_SQL_CLOUDSQL_CONNECTOR_ENABLED"] = "true"
os.environ["CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME"] = "project:region:instance"
os.environ["CN_SQL_DB_DIALECT"] = "pgsql"
os.environ["CN_SQL_DB_USER"] = "jans"
os.environ["CN_SQL_DB_NAME"] = "jans"

manager = Manager()
client = SqlClient(manager)

if client.connected():
    print("Successfully connected to Cloud SQL PostgreSQL via Private IP")
```

### Code Example - MySQL

```python
from jans.pycloudlib.persistence.sql import SqlClient
from jans.pycloudlib.manager import Manager

import os
os.environ["CN_SQL_CLOUDSQL_CONNECTOR_ENABLED"] = "true"
os.environ["CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME"] = "project:region:instance"
os.environ["CN_SQL_DB_DIALECT"] = "mysql"
os.environ["CN_SQL_DB_USER"] = "jans"
os.environ["CN_SQL_DB_NAME"] = "jans"

manager = Manager()
client = SqlClient(manager)

if client.connected():
    print("Successfully connected to Cloud SQL MySQL via Private IP")
```

---

## MANDATORY CHECKLIST: IAM and VPC Requirements

Before deploying your Cloud Run service with Cloud SQL Python Connector, ensure all the following requirements are met:

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

1. Install the optional dependencies: `pip install 'jans-pycloudlib[cloudsql]'`
2. Complete the IAM and VPC checklist above
3. Set `CN_SQL_CLOUDSQL_CONNECTOR_ENABLED=true`
4. Set `CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME` to your instance connection name
5. Deploy your updated Cloud Run service

## Troubleshooting

### Common Issues

1. **"Cloud SQL Python Connector is not installed"**
   - Install with: `pip install 'jans-pycloudlib[cloudsql]'`

2. **"Permission denied" or IAM errors**
   - Verify the service account has `roles/cloudsql.client` role
   - Check that the correct service account is attached to Cloud Run

3. **Connection timeout**
   - Verify the VPC connector is properly configured
   - Check that the Cloud SQL instance has Private IP enabled
   - Ensure firewall rules allow egress to the instance

4. **"Could not connect to Cloud SQL instance"**
   - Verify `CN_SQL_CLOUDSQL_INSTANCE_CONNECTION_NAME` format: `project:region:instance`
   - Check that the instance exists and is running

5. **MySQL strict mode errors**
   - The Cloud SQL Connector still applies MySQL strict mode settings
   - Check your data for compatibility with strict mode
