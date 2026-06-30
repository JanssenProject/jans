# Helm Deployments

Deploy Janssen on Kubernetes using Helm charts. This guide walks you through the installation process step by step.

## Installation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Choose Your Platform                                        │
│     └─> Set up your Kubernetes cluster                          │
├─────────────────────────────────────────────────────────────────┤
│  2. Configure Ingress                                           │
│     └─> Gateway API (recommended) or Nginx Ingress              │
├─────────────────────────────────────────────────────────────────┤
│  3. Set Up Database                                             │
│     └─> PostgreSQL (recommended) or MySQL                       │
├─────────────────────────────────────────────────────────────────┤
│  4. Install Janssen                                             │
│     └─> Run helm install with your override.yaml                │
├─────────────────────────────────────────────────────────────────┤
│  5. Post-Installation                                           │
│     └─> Verify and configure using TUI                          │
└─────────────────────────────────────────────────────────────────┘
```

## Step 1: Choose Your Platform

Select your Kubernetes platform to get started:

| Platform                  | Guide                                                                                                                                 |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| Amazon EKS                | [Amazon EKS Setup](https://docs.jans.io/nightly/janssen-server/install/helm-install/prerequisites/amazon-eks/index.md)                |
| Google GKE                | [Google GKE Setup](https://docs.jans.io/nightly/janssen-server/install/helm-install/prerequisites/google-gke/index.md)                |
| Microsoft AKS             | [Microsoft AKS Setup](https://docs.jans.io/nightly/janssen-server/install/helm-install/prerequisites/microsoft-aks/index.md)          |
| Local (Minikube/MicroK8s) | [Local Setup](https://docs.jans.io/nightly/janssen-server/install/helm-install/prerequisites/local/index.md)                          |
| Rancher Marketplace       | [Rancher Setup](https://docs.jans.io/nightly/janssen-server/install/helm-install/prerequisites/rancher/index.md) (includes all steps) |

## Step 2: Configure Ingress

After your cluster is ready, configure how traffic reaches Janssen:

[Ingress Setup Guide](https://docs.jans.io/nightly/janssen-server/install/helm-install/ingress-setup/index.md) - Gateway API or Nginx Ingress

## Step 3: Set Up Database

Configure persistence storage:

[Database Setup Guide](https://docs.jans.io/nightly/janssen-server/install/helm-install/database-setup/index.md) - PostgreSQL or MySQL

## Step 4: Install Janssen

Deploy the Helm chart:

[Install Janssen Guide](https://docs.jans.io/nightly/janssen-server/install/helm-install/install-janssen/index.md)

## Step 5: Post-Installation

Verify and configure your deployment:

[Post-Installation Guide](https://docs.jans.io/nightly/janssen-server/install/helm-install/post-install/index.md)

## System Requirements

The resources may be set minimally to the below:

- 8-12 GB RAM based on the services deployed
- 8-10 CPU cores based on the services deployed
- 50GB hard-disk

Use the listing below for a detailed estimation of minimum required resources. The table contains the default resources recommendation per service. Depending on the use of each service the resources need may be increased or decreased.

| Service           | CPU Unit | RAM   | Disk Space | Processor Type | Required                  |
| ----------------- | -------- | ----- | ---------- | -------------- | ------------------------- |
| Auth server       | 2.5      | 2.5GB | N/A        | 64 Bit         | Yes                       |
| config - job      | 0.3      | 0.3GB | N/A        | 64 Bit         | Yes on fresh installs     |
| persistence - job | 0.3      | 0.3GB | N/A        | 64 Bit         | Yes on fresh installs     |
| auth-key-rotation | 0.3      | 0.3GB | N/A        | 64 Bit         | No [Strongly recommended] |
| cleanup - job     | 0.3      | 0.3GB | N/A        | 64 Bit         | No [Strongly recommended] |
| fido2             | 0.5      | 0.5GB | N/A        | 64 Bit         | No                        |
| scim              | 1        | 1GB   | N/A        | 64 Bit         | No                        |
| nginx             | 1        | 1GB   | N/A        | 64 Bit         | No                        |
| config-api        | 1        | 1GB   | N/A        | 64 Bit         | No                        |
| casa              | 0.5      | 0.5GB | N/A        | 64 Bit         | No                        |
| link              | 0.5      | 1GB   | N/A        | 64 Bit         | No                        |
| saml              | 0.5      | 1GB   | N/A        | 64 Bit         | No                        |

Releases of images are in style 0.0.0-nightly or x.y-z-1

## Helm Chart Reference

For all configuration options, see the [Helm Chart Reference](https://docs.jans.io/nightly/janssen-server/reference/kubernetes/#helm-chart-references).

## Looking for Older Charts?

We maintain the last 5 versions. For older charts, build from the repository:

```
git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans.git /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout v1.0.0 \
    && git sparse-checkout add charts/janssen \
    && cd charts/janssen \
    && helm dependency update \
    && helm package .
```
