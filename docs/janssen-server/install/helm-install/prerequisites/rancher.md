---
tags:
  - administration
  - installation
  - helm
  - rancher
---

# Rancher Marketplace Setup

This guide covers installing Janssen through the Rancher Marketplace.

## Prerequisites

1. Provision a Linux VM with:
   - 4 CPU cores
   - 16 GB RAM
   - 50 GB SSD
   - Ports 443 and 80 open

    These are minimum specs for testing or single-node/demo installations. Production environments require higher resources and HA configurations. See [Rancher production requirements](https://ranchermanager.docs.rancher.com/getting-started/installation-and-upgrade/installation-requirements) for guidance.

2. Install [Docker](https://docs.docker.com/engine/install/)

## Start Rancher

!!! warning "Not for Production"
    Single-node Docker installations are intended only for testing and development. For production deployments, use a high-availability Kubernetes-based Helm installation. See [Rancher HA installation docs](https://ranchermanager.docs.rancher.com/getting-started/installation-and-upgrade/install-upgrade-on-a-kubernetes-cluster) for guidance.

```bash
docker run -d --restart=unless-stopped -p 80:80 -p 443:443 --privileged rancher/rancher:v2.13.2
```

!!! tip "Version Pinning"
    Use a specific version tag (e.g., `v2.13.2`) instead of `latest` for production deployments to ensure reproducibility.

!!! note
    If deploying an Ingress controller that uses ports 80 and 443, adjust the host ports. See [Rancher documentation](https://ranchermanager.docs.rancher.com/reference-guides/single-node-rancher-in-docker/advanced-options#running-rancherrancher-and-rancherrancher-agent-on-the-same-node) for details.

## Get Bootstrap Password

When you run `docker run -d`, it outputs only the container ID. Save this value. If you missed it, you can retrieve the container ID later using `docker ps`.

Retrieve the bootstrap password:

```bash
docker logs <container-id> 2>&1 | grep "Bootstrap Password:"
```

## Access Rancher UI

1. Navigate to `https://<VM-IP-ADDRESS>`
2. Log in with username `admin` and the bootstrap password
3. Set a new password when prompted

## Install Janssen via Marketplace

1. From the Rancher home page, click on the `local` cluster
2. Navigate to **Apps** > **Repositories** > **Create**
3. Add repository:
   - Name: `janssen`
   - Index URL: `https://docs.jans.io/charts`
4. Click **Create**
5. Go to **Apps** > **Charts**
6. Search for "Janssen" and begin installation
7. In Step 1, select **Customize Helm options before install**
8. In Step 2, customize your settings
9. In Step 3, unselect **Wait** and start the installation. Leaving "Wait" selected makes Helm wait for all resources to reach a ready state, which can cause the install to hang or time out in UI-driven installs. Unselecting it allows the release to be created asynchronously. Note that you'll need to check resource readiness separately (e.g., via `kubectl get pods -n jans`).

## Next Steps

!!! note "Rancher Marketplace Flow"
    When installing via Rancher Marketplace, the Ingress, Database, and Helm install steps are handled through the Rancher UI during chart installation. Skip directly to post-installation.

After installation, proceed to [Post-Installation](../post-install.md) for configuration.
