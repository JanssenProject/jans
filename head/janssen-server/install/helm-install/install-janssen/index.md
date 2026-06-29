# Install Janssen with Helm

After configuring your cluster, ingress, and database, you're ready to install Janssen.

## Complete override.yaml Example

Here's a complete `override.yaml` combining Gateway API and MySQL:

```
global:
  lbIp: ""  # Add your LoadBalancer IP
  fqdn: demoexample.jans.io  # Your domain
  isFqdnRegistered: true
  gateway-api:
    enabled: true
  nginx-ingress:
    enabled: false
gateway-api:
  gateway:
    className: nginx # Match your controller (nginx, istio, etc.)
    name: jans-gateway
    httpPort: 80
    httpsPort: 443
    attachLbIp: false # Set the value to true if loadbalancer didn't assign IP address to the gateway automatically
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

Security

Replace example credentials with secure values for production deployments.

Adjust values based on your choices from the previous steps.

## Add the Janssen Helm Repository

```
helm repo add janssen https://docs.jans.io/charts
helm repo update
```

## Install Janssen

```
helm install janssen janssen/janssen -n jans --create-namespace -f override.yaml
```

## Verify Installation

Check pod status:

```
kubectl get pods -n jans
```

Wait for all pods to reach `Running` or `Completed` status.

## Upgrade an Existing Installation

To apply configuration changes:

```
helm upgrade janssen janssen/janssen -n jans -f override.yaml
```

## Uninstall

To remove Janssen:

```
helm uninstall janssen -n jans
```

## Chart Reference

For all available Helm values, see the [Helm Chart Reference](https://docs.jans.io/head/janssen-server/reference/kubernetes/#helm-chart-references).

## Next Steps

Proceed to [Post-Installation](https://docs.jans.io/head/janssen-server/install/helm-install/post-install/index.md) to configure and verify your deployment.
