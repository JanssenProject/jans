---
tags:
  - administration
  - installation
  - helm
  - local
  - minikube
  - microk8s
---

# Local Kubernetes Setup (Minikube/MicroK8s)

This guide covers setting up Janssen on a local Kubernetes cluster for development and testing.

## System Requirements

For local deployments, minimum resources are:

- 8 GB RAM
- 4 CPU cores
- 50 GB hard-disk

## Quick Start Script

Start a fresh Ubuntu VM with ports 443 and 80 open, then run:

```bash
sudo su -
wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/startjanssendemo.sh && chmod u+x startjanssendemo.sh && ./startjanssendemo.sh
```

This installs Docker, MicroK8s, Helm, and Janssen with default settings.

#### Accessing Endpoints

The installer adds a hosts record in the VM. To access from outside the VM, map the VM IP to your FQDN.

| Service     | Endpoint                                    |
|-------------|---------------------------------------------|
| Auth server | `https://FQDN/.well-known/openid-configuration` |
| FIDO2       | `https://FQDN/.well-known/fido2-configuration`  |
| SCIM        | `https://FQDN/.well-known/scim-configuration`   |

## Manual Setup

If you prefer manual setup:

1. Install [Minikube](https://minikube.sigs.k8s.io/docs/start/) or [MicroK8s](https://github.com/canonical/microk8s/tree/master#quickstart)
2. Install [Helm](https://helm.sh/docs/intro/install/)
3. Create the namespace:
   ```bash
   kubectl create namespace jans
   ```

For next step in manual setup, proceed to [Ingress Setup](../ingress-setup.md) to configure traffic routing.
