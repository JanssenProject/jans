---
tags:
  - administration
  - kubernetes
  - operations
  - scaling
---

## Overview 
Scaling is the ability of handling the increase of usage by expanding the existing resources(nodes/pods).

## Scaling types
Scaling in Kubernetes can be done `automatically` and `manually`.

### Automatic Scaling
Kubernetes has the capability to provision resources `automatically` in order to match the needed demand.

#### Horizontal Pod Autoscaler (HPA)

[HPA](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) automatically resizes the number of `pods` to match demand.

  In order for `hpa` to work, you have to:

  1.  Install metrics server

      ```yaml
      kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
      ```

  2.  Define `requests`for the metric used

It is configured and enabled by default in the deployed `jans` components. 

The default configuration scales in and out based on the CPU utilization of the pods.

```yaml
<component-name>:
    hpa:
      enabled: true
      minReplicas: 1
      maxReplicas: 10
      targetCPUUtilizationPercentage: 50
      # -- metrics if targetCPUUtilizationPercentage is not set
      metrics: []
      # -- Scaling Policies
      behavior: {}
```

#### Cluster Autoscaler
Cluster Autoscaler automatically resizes the number of `nodes` in a given node pool, based on the demands of your workloads. 

Cluster Autoscaler is available in [AWS](https://docs.aws.amazon.com/eks/latest/userguide/autoscaling.html), [GCP](https://cloud.google.com/kubernetes-engine/docs/concepts/cluster-autoscaler) and [Azure](https://learn.microsoft.com/en-us/azure/aks/cluster-autoscaler).


### Manual Scaling
Kubernetes also offers the option to manually scale your resources.

For example you can increase `manually` the pod replicas of auth-server deployment using the following command: 

```bash
kubectl scale --replicas=3 deployment/auth-server -n <namespace>
```