---
tags:
  - administration
  - planning
  - kubernetes
---

# Overview

This planning guide helps you learn about when to use jans with kubernetes, why to use it, an overview of best practices around it, platforms supported and important tools that can help ease the operations.

## The When and Why?

Whether to use kubernetes , vm or a cluster of vms depends on answering several key questions some of which are:

#### What's the size of my organization/customer serving base?
Answering this question gives an expected load of authentications per second expected which determines if you are in the threshold of operating a VM for jans. Anything past 50 auths per second points to a cluster setup which is curerntly kuberentes. We do plan to support more cloud native setups soon.

#### Can my organization handle  min-hrs of downtime?
This ties to question 1 but even with the best operational automation vm setups restoring/patching a vm will result in downtime.

#### Does my organization function in different regions?
If your organization works in several regions a vm setup is almost immediately ruled out.

#### Does my organization expect to double in growth each year ? or possibly faster?
If your organization or your customer base is growing fast rule out a VM setup.

#### What's the persistence planned to be used in my jans deployment?
If you plan to use LDAP you are almost immediately locked to a VM setup. Our kuberentes deployment supports an LDAP zonal setup only and we generally do not recommend it for a kubernetes setup.

#### Is this an on-premise vs hybrid vs cloud deployment?
This question wieghs on the current infrastructure of your organization. Operating on-premis kubernetes setups needs generally more technical resources than operating a cloud setup. Comparing that to a vm setup, operating a vm setup is much easier in on-premise setups. The more the momentum of your org pulls to cloud vs onpremise the more it pulls to kuberetnes vs a vm setup respectively.

#### What's my organizations technical expertise level on kubernetes vs vms?
This determines the amount of investment needed to overcome the learning curve to operate a kubernetes cluster. If your kubernetes technical resources are low and the above questions point to a vm setup you should think about going with a vm setup. If you got conflicting answers above , forexample if the customer size dedicated that you should operate a kubernetes cluster you need to invest in your technical staff. If you are on the borderline invest in your kubernetes technical staff while operating a vm setup and prepare to move in the near future.


In general the kubernetes deployment offers an easy deployment strategy with a helm chart and an easy upgrade path but taking the above into account is very important. 

## The Supported Clouds

We support several clouds managed kubernetes solutions but that doesn't mean our setup will not work with other managed kubernetes solutions. In fact, our kubernetes setup is expected to work across almost all managed solutions however those not listed below were not tested.
### AWS
Below are example diagrams for popular jans setups with AWS.
### Google
Below are example diagrams for popular jans setups with AWS.
### Azure
Below are example diagrams for popular jans setups with Azure.
### Digital Ocean
Below are example diagrams for popular jans setups with DOKS.

## Local/Hybrid
### Rancher
Rancher UI can be deployed to several clouds / multi clouds and also be used to manage local setups.

## Important tools and tech to consider
- Terraform
- Grafana and Prometheus
- Helm
- Pipeline tech such as Jenkins, ansible, cloud build ..etc