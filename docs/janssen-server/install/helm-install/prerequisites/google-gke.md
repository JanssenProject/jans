---
tags:
  - administration
  - installation
  - helm
  - GKE
  - Google Cloud
  - GCP
---

# Google GKE Cluster Setup

This guide covers the prerequisites and cluster creation specific to Google Kubernetes Engine (GKE).

## Prerequisites

1. Enable [GKE API](https://console.cloud.google.com/kubernetes) if not enabled yet.

2. If you are using Cloud Shell, skip to step 5.

3. Install [gcloud CLI](https://cloud.google.com/sdk/docs/quickstarts).

4. Install kubectl:

   ```bash
   gcloud components install kubectl
   ```

5. Install [Helm3](https://helm.sh/docs/intro/install/).

## Create the GKE Cluster

```bash
gcloud container clusters create janssen-cluster --num-nodes 2 --machine-type e2-standard-4 --zone us-west1-a
```

Adjust `num-nodes` and `machine-type` as per your desired cluster size.

## Create the Janssen Namespace

```bash
kubectl create namespace jans
```

## Next Steps

Proceed to [Ingress Setup](../ingress-setup.md) to configure traffic routing.
