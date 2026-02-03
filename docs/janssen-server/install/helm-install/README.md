---
tags:
  - administration
  - installation
  - helm
---

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

| Platform | Guide |
|----------|-------|
| Amazon EKS | [Amazon EKS Setup](prerequisites/amazon-eks.md) |
| Google GKE | [Google GKE Setup](prerequisites/google-gke.md) |
| Microsoft AKS | [Microsoft AKS Setup](prerequisites/microsoft-aks.md) |
| Local (Minikube/MicroK8s) | [Local Setup](prerequisites/local.md) |
| Rancher Marketplace | [Rancher Setup](prerequisites/rancher.md) (includes all steps) |

## Step 2: Configure Ingress

After your cluster is ready, configure how traffic reaches Janssen:

[Ingress Setup Guide](ingress-setup.md) - Gateway API or Nginx Ingress

## Step 3: Set Up Database

Configure persistence storage:

[Database Setup Guide](database-setup.md) - PostgreSQL or MySQL

## Step 4: Install Janssen

Deploy the Helm chart:

[Install Janssen Guide](install-janssen.md)

## Step 5: Post-Installation

Verify and configure your deployment:

[Post-Installation Guide](post-install.md)

## System Requirements

{% include "includes/cn-system-requirements.md" %}

## Helm Chart Reference

For all configuration options, see the [Helm Chart Reference](../../reference/kubernetes/README.md#helm-chart-references).

## Looking for Older Charts?

We maintain the last 5 versions. For older charts, build from the repository:

```bash
git clone --filter blob:none --no-checkout https://github.com/JanssenProject/jans.git /tmp/jans \
    && cd /tmp/jans \
    && git sparse-checkout init --cone \
    && git checkout v1.0.0 \
    && git sparse-checkout add charts/janssen \
    && cd charts/janssen \
    && helm dependency update \
    && helm package .
```
