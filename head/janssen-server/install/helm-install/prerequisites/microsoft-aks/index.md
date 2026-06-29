# Microsoft AKS Cluster Setup

This guide covers the prerequisites and cluster creation specific to Azure Kubernetes Service (AKS).

## Prerequisites

1. Install [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)
1. Install [Helm](https://helm.sh/docs/intro/install/)

## Create a Resource Group

```
az group create --name janssen-resource-group --location eastus
```

## Create the AKS Cluster

```
az aks create -g janssen-resource-group -n janssen-cluster --enable-managed-identity --node-vm-size NODE_TYPE --node-count 2 --enable-addons monitoring --enable-msi-auth-for-monitoring --generate-ssh-keys
```

Adjust `node-count` and `node-vm-size` as per your desired cluster size.

## Connect to the Cluster

```
az aks install-cli
az aks get-credentials --resource-group janssen-resource-group --name janssen-cluster
```

## Create the Janssen Namespace

```
kubectl create namespace jans
```

## Next Steps

Proceed to [Ingress Setup](https://docs.jans.io/head/janssen-server/install/helm-install/ingress-setup/index.md) to configure traffic routing.
