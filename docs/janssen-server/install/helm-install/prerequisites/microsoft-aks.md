---
tags:
  - administration
  - installation
  - helm
  - AKS
  - Microsoft
  - Azure
---

# Microsoft AKS Cluster Setup

This guide covers the prerequisites and cluster creation specific to Azure Kubernetes Service (AKS).

## Prerequisites

1. Install [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)

2. Install [Helm3](https://helm.sh/docs/intro/install/)

## Create a Resource Group

```bash
az group create --name janssen-resource-group --location eastus
```

## Create the AKS Cluster

```bash
az aks create -g janssen-resource-group -n janssen-cluster --enable-managed-identity --node-vm-size NODE_TYPE --node-count 2 --enable-addons monitoring --enable-msi-auth-for-monitoring --generate-ssh-keys
```

Adjust `node-count` and `node-vm-size` as per your desired cluster size.

## Connect to the Cluster

```bash
az aks install-cli
az aks get-credentials --resource-group janssen-resource-group --name janssen-cluster
```

## Create the Janssen Namespace

```bash
kubectl create namespace jans
```

## Next Steps

Proceed to [Ingress Setup](../ingress-setup.md) to configure traffic routing.
