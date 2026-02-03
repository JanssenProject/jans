---
tags:
  - administration
  - installation
  - helm
  - EKS
  - Amazon Web Services
  - AWS
---

# Amazon EKS Cluster Setup

This guide covers the prerequisites and cluster creation specific to Amazon Elastic Kubernetes Service (EKS).

## Prerequisites

1. Install [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

2. Configure your AWS user account using the [aws configure](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) command. This user account must have permissions to work with Amazon EKS IAM roles, service linked roles, AWS CloudFormation, and VPC resources.

3. Install [kubectl](https://docs.aws.amazon.com/eks/latest/userguide/install-kubectl.html)

4. Install [eksctl](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html) 

5. Install [Helm3](https://helm.sh/docs/intro/install/)

## Create the EKS Cluster

```bash
eksctl create cluster --name janssen-cluster --nodegroup-name jans-nodes --node-type NODE_TYPE --nodes 2 --managed --region REGION_CODE
```

Adjust `node-type` and `nodes` as per your desired cluster size.

## Install the EBS CSI Driver

To attach volumes to your pods, install the Amazon [EBS CSI driver](https://docs.aws.amazon.com/eks/latest/userguide/csi-iam-role.html).

## Create the Janssen Namespace

```bash
kubectl create namespace jans
```

## Next Steps

Proceed to [Ingress Setup](../ingress-setup.md) to configure traffic routing.
