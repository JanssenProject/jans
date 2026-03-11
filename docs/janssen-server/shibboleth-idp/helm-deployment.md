---
tags:
  - administration
  - shibboleth
  - kubernetes
  - helm
---

# Shibboleth IDP Helm Deployment

This guide covers deploying the Janssen Shibboleth IDP on Kubernetes using Helm.

## Prerequisites

- Kubernetes 1.25+
- Helm 3.x
- Janssen Helm repository configured
- PersistentVolume provisioner (for configuration storage)
- Ingress controller (nginx, traefik, etc.)

> **Note**: The ingress-nginx project (kubernetes/ingress-nginx) is in maintenance mode and will not receive new releases after March 2026. For new deployments, consider using Gateway API implementations or maintained controllers such as Traefik, Contour, or service meshes (e.g., Istio).

## Add Janssen Helm Repository

```bash
helm repo add janssen https://docs.jans.io/charts
helm repo update
```

## Basic Deployment

### Enable Shibboleth IDP in Janssen Chart

```bash
helm install janssen janssen/janssen \
  --namespace janssen \
  --create-namespace \
  --set global.fqdn=auth.example.com \
  --set global.shibboleth-idp.enabled=true
```

### Custom Values File

Create a `values.yaml` file:

```yaml
global:
  fqdn: auth.example.com
  isFqdnRegistered: true
  
  persistence:
    enabled: true
    storageClass: standard
    
config:
  countryCode: US
  email: admin@example.com
  orgName: Example Org
  city: Austin
  state: TX

shibboleth-idp:
  enabled: true
  
  replicaCount: 2
  
  image:
    repository: ghcr.io/janssenproject/jans/shibboleth
    tag: 5.1.6_dev
    pullPolicy: IfNotPresent
    
  resources:
    limits:
      cpu: 2000m
      memory: 1024Mi
    requests:
      cpu: 500m
      memory: 512Mi
      
  hpa:
    enabled: true
    minReplicas: 2
    maxReplicas: 10
    targetCPUUtilizationPercentage: 80
    
  shibboleth:
    entityId: "https://auth.example.com/idp/shibboleth"
    scope: "example.com"
    jansAuth:
      enabled: true
      clientId: "shibboleth-client-id"
      scopes: "openid,profile,email"
```

Deploy with custom values:

```bash
helm install janssen janssen/janssen \
  --namespace janssen \
  --create-namespace \
  -f values.yaml
```

## Configuration Options

### Shibboleth IDP Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `shibboleth-idp.enabled` | Enable Shibboleth IDP | `false` |
| `shibboleth-idp.replicaCount` | Number of replicas | `1` |
| `shibboleth-idp.image.repository` | Image repository | `ghcr.io/janssenproject/jans/shibboleth` |
| `shibboleth-idp.image.tag` | Image tag | `5.1.6_dev` |
| `shibboleth-idp.resources.limits.cpu` | CPU limit | `2000m` |
| `shibboleth-idp.resources.limits.memory` | Memory limit | `1024Mi` |
| `shibboleth-idp.resources.requests.cpu` | CPU request | `500m` |
| `shibboleth-idp.resources.requests.memory` | Memory request | `512Mi` |

### Autoscaling

| Parameter | Description | Default |
|-----------|-------------|---------|
| `shibboleth-idp.hpa.enabled` | Enable HPA | `false` |
| `shibboleth-idp.hpa.minReplicas` | Minimum replicas | `1` |
| `shibboleth-idp.hpa.maxReplicas` | Maximum replicas | `10` |
| `shibboleth-idp.hpa.targetCPUUtilizationPercentage` | Target CPU | `80` |

### Shibboleth Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `shibboleth-idp.shibboleth.entityId` | IDP Entity ID | Auto-generated |
| `shibboleth-idp.shibboleth.scope` | IDP scope | Domain from FQDN |
| `shibboleth-idp.shibboleth.signingKeyAlias` | Signing key alias | `idp-signing` |
| `shibboleth-idp.shibboleth.encryptionKeyAlias` | Encryption key alias | `idp-encryption` |
| `shibboleth-idp.shibboleth.jansAuth.enabled` | Enable Janssen auth | `true` |
| `shibboleth-idp.shibboleth.jansAuth.clientId` | OAuth client ID | Required |
| `shibboleth-idp.shibboleth.jansAuth.scopes` | OAuth scopes | `openid,profile,email` |

### Service Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `shibboleth-idp.service.type` | Service type | `ClusterIP` |
| `shibboleth-idp.service.port` | Service port | `8080` |

### Ingress Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `shibboleth-idp.ingress.enabled` | Enable ingress | `true` |
| `shibboleth-idp.ingress.hosts[0].paths[0].path` | Ingress path | `/idp` |

## High Availability Configuration

For production deployments, configure high availability:

```yaml
shibboleth-idp:
  enabled: true
  replicaCount: 3
  
  hpa:
    enabled: true
    minReplicas: 3
    maxReplicas: 20
    targetCPUUtilizationPercentage: 70
    behavior:
      scaleDown:
        stabilizationWindowSeconds: 300
        policies:
          - type: Percent
            value: 10
            periodSeconds: 60
      scaleUp:
        stabilizationWindowSeconds: 0
        policies:
          - type: Percent
            value: 100
            periodSeconds: 15
            
  resources:
    limits:
      cpu: 4000m
      memory: 2048Mi
    requests:
      cpu: 1000m
      memory: 1024Mi
      
  affinity:
    podAntiAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            labelSelector:
              matchLabels:
                app: shibboleth-idp
            topologyKey: kubernetes.io/hostname
            
  topologySpreadConstraints:
    - maxSkew: 1
      topologyKey: topology.kubernetes.io/zone
      whenUnsatisfiable: ScheduleAnyway
      labelSelector:
        matchLabels:
          app: shibboleth-idp
```

## Verify Deployment

Check deployment status:

```bash
# Check pods
kubectl get pods -n janssen -l app=shibboleth-idp

# Check service
kubectl get svc -n janssen -l app=shibboleth-idp

# Check ingress
kubectl get ingress -n janssen

# View logs
kubectl logs -n janssen -l app=shibboleth-idp -f
```

Test IDP status:

```bash
# Port forward for testing
kubectl port-forward -n janssen svc/shibboleth-idp 8080:8080

# Check status
curl http://localhost:8080/idp/status
```

## Upgrade

Upgrade the deployment:

```bash
helm upgrade janssen janssen/janssen \
  --namespace janssen \
  -f values.yaml
```

## Uninstall

Remove the deployment:

```bash
helm uninstall janssen --namespace janssen
```

## Troubleshooting

### Pod Not Starting

Check pod events:

```bash
kubectl describe pod -n janssen -l app.kubernetes.io/name=shibboleth-idp
```

### Configuration Issues

Check configuration:

```bash
kubectl exec -n janssen -it deployment/shibboleth-idp -- cat /opt/shibboleth-idp/conf/idp.properties
```

### Authentication Failures

Check logs for authentication errors:

```bash
kubectl logs -n janssen -l app.kubernetes.io/name=shibboleth-idp | grep -i "authn\|error"
```
