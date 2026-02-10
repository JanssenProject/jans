---
tags:
  - administration
  - installation
  - helm
---

# Install Janssen with Helm

After configuring your cluster, ingress, and database, you're ready to install Janssen.

## Complete override.yaml Example

Here's a complete `override.yaml` combining Gateway API and MySQL:

```yaml
global:
  lbIp: ""  # Add your LoadBalancer IP
  fqdn: demoexample.jans.io  # Your domain
  isFqdnRegistered: true
  gatewayApi:
    enabled: true
  nginx-ingress:
    enabled: false
gatewayApi:
  gatewayClassName: nginx
  name: jans-gateway
config:
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 3306
    cnSqlDbDialect: mysql
    cnSqlDbHost: mysql.jans.svc
    cnSqlDbUser: root  # Use a dedicated user in production
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#  # Change for production!
```

!!! warning "Security"
    Replace example credentials with secure values for production deployments.

Adjust values based on your choices from the previous steps.

## Add the Janssen Helm Repository

```bash
helm repo add janssen https://docs.jans.io/charts
helm repo update
```

## Install Janssen

```bash
helm install janssen janssen/janssen -n jans --create-namespace -f override.yaml
```

## Verify Installation

Check pod status:

```bash
kubectl get pods -n jans
```

Wait for all pods to reach `Running` or `Completed` status.

## Upgrade an Existing Installation

To apply configuration changes:

```bash
helm upgrade janssen janssen/janssen -n jans -f override.yaml
```

## Uninstall

To remove Janssen:

```bash
helm uninstall janssen -n jans
```

## Chart Reference

For all available Helm values, see the [Helm Chart Reference](../../reference/kubernetes/README.md#helm-chart-references).

## Next Steps

Proceed to [Post-Installation](post-install.md) to configure and verify your deployment.
