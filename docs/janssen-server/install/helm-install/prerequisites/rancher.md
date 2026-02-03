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

2. Install [Docker](https://docs.docker.com/engine/install/)

## Start Rancher

```bash
docker run -d --restart=unless-stopped -p 80:80 -p 443:443 --privileged rancher/rancher:v2.13.2
```

!!! tip "Version Pinning"
    Use a specific version tag (e.g., `v2.13.2`) instead of `latest` for production deployments to ensure reproducibility.

!!! note
    If deploying an Ingress controller that uses ports 80 and 443, adjust the host ports. See [Rancher documentation](https://ranchermanager.docs.rancher.com/reference-guides/single-node-rancher-in-docker/advanced-options#running-rancherrancher-and-rancherrancher-agent-on-the-same-node) for details.

## Get Bootstrap Password

The final line of the docker run output is the container-id. Retrieve the password:

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
9. In Step 3, unselect **Wait** and start the installation

## Next Steps

!!! note "Rancher Marketplace Flow"
    When installing via Rancher Marketplace, the Ingress, Database, and Helm install steps are handled through the Rancher UI during chart installation. Skip directly to post-installation.

After installation, proceed to [Post-Installation](../post-install.md) for configuration.
