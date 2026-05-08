---
tags:
  - administration
  - installation
  - helm
  - rancher
---

This guide covers installing Janssen through the Rancher Marketplace.


## Install Rancher

For a stable, production-ready environment that natively supports Persistent Volumes (PVs), install Rancher on a dedicated Kubernetes cluster using Helm. Follow the official [Rancher Helm Installation Guide](https://ranchermanager.docs.rancher.com/pages-for-subheaders/install-upgrade-on-a-kubernetes-cluster).

!!! tip "Testing/Dev Alternative"

    If you are only testing and do not need PVs, you can use a single-node Docker installation. The Linux single-node are **4 CPU cores**, **16 GB RAM**, **50 GB SSD**, and ports **80** and **443** open

    ```bash
    docker run -d \
    --restart=unless-stopped \
    -p 80:80 \
    -p 443:443 \
    --privileged rancher/rancher:v2.14.1
    ```

    Retrieve the bootstrap password with
    
    ```bash
    docker logs <container-id> 2>&1 | grep "Bootstrap Password:"
    ```

    Open: `https://<RANCHER-SERVER-IP>`

    Log in with the default `admin` credentials or your bootstrap password.

    Set a new password when prompted.



## Install Janssen

### 1. Install Database

You have two options for setting up your backend database depending on your infrastructure's capabilities. 

#### Option A: Install via Kubernetes (Requires PV Support)

!!! Note
    For this setup to work, a PV provisioner must be present and configured in your underlying Kubernetes infrastructure.

Open a kubectl shell from the top right navigation menu `>_` and run:

=== "MySQL"

    ```bash
    wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/mysql.yaml
    kubectl apply -f mysql.yaml # adjust values as preferred
    ```

=== "PostgreSQL"

    ```bash 
    wget https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/automation/pgsql.yaml
    kubectl apply -f pgsql.yaml # adjust values as preferred
    ```

#### Option B: Install on VM (Docker/No PV Support)


Use this option if you are running a single-node Docker test environment or lack PV support. You can install the database package(MySQL/PostgreSQL) directly on your Linux VM.

### 2. Configure Ingress and Traffic Management

Follow this [guide](../ingress-setup.md) to choose between the modern Gateway API (recommended) or the legacy Kubernetes Ingress.


### 3. Install Janssen

- Navigate to **Apps** > **Repositories** > **Create**
- Add repository with Name: `Janssen` and Index URL: `https://docs.jans.io/charts`
- Click **Create**
- Go to **Apps** > **Charts** > Search for "Janssen" and start the installation
- In Step 1, select **Customize Helm options before install**
- In Step 2, customize your settings
- In Step 3, unselect **Wait** and start the installation. Leaving "Wait" selected makes Helm wait for all resources to reach a ready state, which can cause the install to hang or time out in UI-driven installs. Unselecting it allows the release to be created asynchronously. Note that you'll need to check resource readiness separately (e.g., via `kubectl get pods -n jans`).

## Next Steps

!!! note "Rancher Marketplace Flow"
    When installing via Rancher Marketplace, the Ingress, Database, and Helm install steps are handled through the Rancher UI during chart installation. Skip directly to post-installation.

After installation, proceed to [Post-Installation](../post-install.md) for configuration.
