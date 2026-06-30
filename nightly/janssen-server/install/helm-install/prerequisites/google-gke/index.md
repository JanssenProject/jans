# Google GKE Cluster Setup

This guide covers the prerequisites and cluster creation specific to Google Kubernetes Engine (GKE).

## Prerequisites

1. Enable [GKE API](https://console.cloud.google.com/kubernetes) if not enabled yet.

1. If you are using Cloud Shell, skip to step 5.

1. Install [gcloud CLI](https://cloud.google.com/sdk/docs/quickstarts).

1. Install kubectl:

   ```
   gcloud components install kubectl
   ```

1. Install [Helm](https://helm.sh/docs/intro/install/).

## Create the GKE Cluster

```
gcloud container clusters create janssen-cluster --num-nodes 2 --machine-type e2-standard-4 --zone us-west1-a
```

Adjust `num-nodes` and `machine-type` as per your desired cluster size.

## Create the Janssen Namespace

```
kubectl create namespace jans
```

## Next Steps

Proceed to [Ingress Setup](https://docs.jans.io/nightly/janssen-server/install/helm-install/ingress-setup/index.md) to configure traffic routing.
