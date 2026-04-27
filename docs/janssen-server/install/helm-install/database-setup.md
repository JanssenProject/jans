---
tags:
  - administration
  - installation
  - helm
  - database
  - postgresql
  - mysql
---

# Database Setup

Janssen requires a database for persistence storage. Choose between PostgreSQL (recommended) or MySQL.

## Production Recommendations

For production environments, use a managed database service:

| Cloud Provider | PostgreSQL Service          | MySQL Service               |
|----------------|-----------------------------|-----------------------------|
| AWS            | Amazon RDS for PostgreSQL   | Amazon RDS for MySQL        |
| Google Cloud   | Cloud SQL for PostgreSQL    | Cloud SQL for MySQL         |
| Azure          | Azure Database for PostgreSQL | Azure Database for MySQL  |

## Option 1: PostgreSQL (Recommended)

### Testing/Development Setup

Deploy PostgreSQL on your cluster for testing:

```bash
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/pgsql.yaml
kubectl apply -f pgsql.yaml
```

### PostgreSQL Configuration

Add this to your `override.yaml`:

```yaml
config:
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 5432
    cnSqlDbDialect: pgsql
    cnSqlDbHost: postgresql.jans.svc
    cnSqlDbUser: postgres
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#  # Change for production!
```

## Option 2: MySQL

### Testing/Development Setup

Deploy MySQL on your cluster for testing:

```bash
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/mysql.yaml
kubectl apply -f mysql.yaml
```

### MySQL Configuration

Add this to your `override.yaml`:

```yaml
config:
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 3306
    cnSqlDbDialect: mysql
    cnSqlDbHost: mysql.jans.svc
    cnSqlDbUser: root
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#  # Change for production!
```

## Connecting to Managed Databases

When using a managed database service, update these values:

- `cnSqlDbHost`: Your database endpoint/hostname
- `cnSqlDbUser`: Your database username
- `cnSqldbUserPassword`: Your database password
- `cnSqlDbName`: Your database name (create beforehand)

!!! warning "Security"
    Never use default passwords in production. Store credentials securely using Kubernetes Secrets.

## Next Steps

Proceed to [Install Janssen](install-janssen.md) to deploy the Helm chart.
