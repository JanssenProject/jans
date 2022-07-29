# Install Janssen Server

Janssen Server can be installed on various OS platforms and hardware environments.

## Installation map:

### Production Environment

| Hardware config <br/> <br/>OS platform | VM                                    | Cloud (Kubernetes)                              |
|-----------------------------------------|---------------------------------------|-------------------------------------------------|
| Ubuntu 20.04                            | [Linux package](vm-install/ubuntu.md) | [Rancher Marketplace](helm-install/rancher.md)  |
| EL 8 (CentOS 8 and Red Hat 8)           | [Linux package](vm-install/rhel.md)   | [Rancher Marketplace](helm-install/rancher.md) |
| SUSE 15                                 | [Linux package](vm-install/suse.md)   | [Rancher Marketplace](helm-install/rancher.md)  |

### Non-Production Environments

!!! note
    Installation methods for [production environments](#production-environment) can also be used in non-production environments that meet system requirements for respective method.  

| Hardware config <br/> <br/>OS platform | VM                                                 | Local Cluster (Kubernetes)        |
|--------------------------------------------------|----------------------------------------------------|-----------------------------------|
| Ubuntu 20.04                                     | [Dynamic download](vm-install/dynamic-download.md) | [MicroK8s](helm-install/local.md) |
| EL 8 (CentOS 8 and Red Hat 8)                    | [Dynamic download](vm-install/dynamic-download.md) | [MicroK8s](helm-install/local.md) |
| SUSE 15                                          | [Dynamic download](vm-install/dynamic-download.md) | [MicroK8s](helm-install/local.md) |


