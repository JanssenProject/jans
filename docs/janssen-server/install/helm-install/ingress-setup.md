---
tags:
  - administration
  - installation
  - helm
  - ingress
  - gateway-api
---

# Ingress & Traffic Management

This guide covers configuring ingress for your Janssen deployment. Choose between the modern Gateway API (recommended) or the legacy Kubernetes Ingress.

## Option 1: Gateway API (Recommended)

The Kubernetes Gateway API provides a more expressive and extensible way to manage traffic.

### Install Gateway API CRDs

If your cluster does not have the Gateway API Custom Resource Definitions:

```bash
kubectl apply --server-side -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.4.1/standard-install.yaml
```

### Install a Gateway Controller

You must have a [conformant Gateway Controller](https://gateway-api.sigs.k8s.io/implementations/#conformant) installed.

Example using Nginx Gateway Fabric:

```bash
helm install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric --create-namespace -n nginx-gateway
```

### Configure Gateway IP

**Option A: Static IP (Recommended)**

Reserve a static public IP with your cloud provider before installation. Add this IP to your `override.yaml`.

**Option B: Dynamic IP**

1. Run initial Helm install without `global.lbIp`
2. Wait for the cloud provider to assign an IP:
   ```bash
   kubectl get gateway -n jans
   ```
3. Add the IP to `global.lbIp` in `override.yaml`
4. Run `helm upgrade` to apply

### Gateway API Configuration

Add this to your `override.yaml`:

```yaml
global:
  lbIp: ""  # Add your static IP here
  fqdn: demoexample.jans.io  # Your domain
  isFqdnRegistered: true  # Set to false if no registered domain
  gatewayApi:
    enabled: true
  nginx-ingress:
    enabled: false
gatewayApi:
  gatewayClassName: nginx  # Match your controller (nginx, istio, etc.)
  name: jans-gateway
  httpPort: 80
  httpsPort: 443
```

## Option 2: Kubernetes Ingress (Legacy)

Use this if you prefer the traditional Ingress resource.

### Install Nginx Ingress Controller

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo add stable https://charts.helm.sh/stable
helm repo update
helm install nginx ingress-nginx/ingress-nginx
```

### Get the Load Balancer Address

**For GKE/AKS (IP address):**
```bash
kubectl get svc nginx-ingress-nginx-controller --output jsonpath='{.status.loadBalancer.ingress[0].ip}'
```

**For EKS (hostname):**
```bash
kubectl get svc nginx-ingress-nginx-controller --output jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

### Ingress Configuration

Choose the configuration that matches your setup:

#### With a Registered Domain (FQDN)

For GKE/AKS with a registered domain:

```yaml
global:
  lbIp: ""  # Add LoadBalancer IP from previous command
  fqdn: demoexample.jans.io  # Your registered domain
  isFqdnRegistered: true
nginx-ingress:
  ingress:
    path: /
    hosts:
      - demoexample.jans.io  # Your domain
    tls:
      - secretName: tls-certificate
        hosts:
          - demoexample.jans.io  # Your domain
```

For EKS with a registered domain (uses hostname instead of IP):

```yaml
global:
  fqdn: demoexample.jans.io  # Your registered domain
  isFqdnRegistered: true
config:
  configmap:
    lbAddr: http://YOUR-EKS-HOSTNAME.elb.amazonaws.com  # Add EKS hostname here
nginx-ingress:
  ingress:
    path: /
    hosts:
      - demoexample.jans.io  # Your domain
    tls:
      - secretName: tls-certificate
        hosts:
          - demoexample.jans.io  # Your domain
```

#### Without a Registered Domain

If you don't have a registered domain, use the LoadBalancer address directly:

For GKE/AKS:

```yaml
global:
  lbIp: ""  # Add LoadBalancer IP from previous command
  isFqdnRegistered: false
```

For EKS:

```yaml
config:
  configmap:
    lbAddr: http://YOUR-EKS-HOSTNAME.elb.amazonaws.com  # Add EKS hostname here
global:
  isFqdnRegistered: false
```

## Next Steps

Proceed to [Database Setup](database-setup.md) to configure persistence storage.
