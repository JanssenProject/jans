---
tags:
  - administration
  - planning
  - kubernetes
---

# Kubernetes

## Overview

This planning guide helps you learn about when to use Jans with Kubernetes, why to use it, an overview of best practices around it, platforms supported and important tools that can help ease the operations.

## The When and Why?

Whether to use Kubernetes , VM or a cluster of VMs depends on answering several key questions some of which are:

#### What's the size of my organization/customer serving base?
Answering this question gives the load of authentications per second expected which determines if you are in the threshold of operating a VM for Jans. Anything past 50 auths per second points to a cluster setup which is curerntly Kuberentes. We do plan to support more cloud-native setups soon.

#### Can my organization handle  min-hrs of downtime?
This ties to question 1 but even with the best operational automation VM setups restoring/patching a VM will result in downtime.

#### Does my organization function in different regions?
If your organization works in several regions a VM setup is almost immediately ruled out.

#### Does my organization expect to double in growth each year? or possibly faster?
If your organization or your customer base is growing fast rule out a VM setup.

#### Is this an on-premise vs hybrid vs cloud deployment?
This question weighs on the current infrastructure of your organization. Operating on-premise Kubernetes setups needs generally more technical resources than operating a cloud setup. Comparing that to a VM setup, operating a VM setup is much easier in on-premise setups. The more the momentum of your org pulls to cloud vs onpremise the more it pulls to Kuberetnes vs a VM setup respectively.

#### What's my organizations technical expertise level on Kubernetes vs VMs?
This determines the amount of investment needed to overcome the learning curve to operate a Kubernetes cluster. If your Kubernetes technical resources are low and the above questions point to a VM setup you should think about going with a vm setup. If you got conflicting answers above, for example if the customer size dedicated that you should operate a Kubernetes cluster you need to invest in your technical staff. If you are on the borderline invest in your Kubernetes technical staff while operating a vm setup and prepare to move in the near future.

#### Does the deployment need to be highly available(HA)?
We recommend using Kubernetes-based CN deployments for situations where high availability is a critical factor. Though
[VM cluster](./vm-cluster.md) can provide high availability, we do not recommend the HA setup with VMs. Primarily 
because effort required to upgrade multiple servers and configuring them is much more as compared to a cloud native 
deployment and it is error-prone. We recommend you follow the CN setup for HA as you will get automatic upgrades, 
patches with a single helm upgrade command.

In general the Kubernetes deployment offers an easy deployment strategy with a helm chart and an easy upgrade path but taking the above into account is very important. 

## The Supported Clouds

We support several clouds managed Kubernetes solutions but that doesn't mean our setup will not work with other managed Kubernetes solutions. In fact, our Kubernetes setup is expected to work across almost all managed solutions however those not listed below were not tested.

- AWS 
- GCP
- Azure
- DOKS

## Local/Hybrid
### Rancher
Rancher UI can be deployed to several clouds / multi-clouds and also be used to manage local setups.

## Important tools and tech to consider
- Helm
- Terraform
- Prometheus and Grafana
- Pipeline tech such as Jenkins, Ansible, Cloud Build ..etc