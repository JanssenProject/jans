# Post-Installation

After installing Janssen, use this guide to configure and verify your deployment.

## Verify Pod Status

Ensure all pods are running:

```
kubectl get pods -n jans
```

Expected output shows pods in `Running` or `Completed` state.

## Configure Janssen with TUI

The Terminal User Interface (TUI) provides an interactive way to configure Janssen components. The TUI calls the Config API to perform configuration.

See the [TUI for Kubernetes](https://docs.jans.io/head/janssen-server/kubernetes-ops/tui-k8s/index.md) guide for detailed instructions.

## Verify Endpoints

Test that your Janssen endpoints are accessible:

| Service     | Endpoint                                          |
| ----------- | ------------------------------------------------- |
| Auth Server | `https://<FQDN>/.well-known/openid-configuration` |
| FIDO2       | `https://<FQDN>/.well-known/fido2-configuration`  |
| SCIM        | `https://<FQDN>/.well-known/scim-configuration`   |

## View Logs

Check logs for troubleshooting:

```
kubectl logs -n jans -l app=auth-server
kubectl logs -n jans -l app=config-api
```

## Common Issues

### Pods not starting

Check events for the namespace:

```
kubectl get events -n jans --sort-by='.lastTimestamp'
```

### Database connection errors

Verify database connectivity:

```
kubectl exec -it -n jans <config-pod> -- nc -zv <db-host> <db-port>
```

### Certificate issues

Ensure TLS certificates are properly configured:

```
kubectl get secrets -n jans | grep tls
```

## Next Steps

- [Kubernetes Operations](https://docs.jans.io/head/janssen-server/kubernetes-ops/index.md) - Day-2 operations
- [Config API](https://docs.jans.io/head/janssen-server/config-guide/config-tools/config-api/index.md) - REST API configuration
- [Monitoring](https://docs.jans.io/head/janssen-server/config-guide/config-tools/config-api/monitoring/index.md) - Set up monitoring
