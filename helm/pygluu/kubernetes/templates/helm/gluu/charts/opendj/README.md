# opendj

![Version: 5.0.2](https://img.shields.io/badge/Version-5.0.2-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 5.0.0](https://img.shields.io/badge/AppVersion-5.0.0-informational?style=flat-square)

OpenDJ is a directory server which implements a wide range of Lightweight Directory Access Protocol and related standards, including full compliance with LDAPv3 but also support for Directory Service Markup Language (DSMLv2).Written in Java, OpenDJ offers multi-master replication, access control, and many extensions.

**Homepage:** <https://gluu.org/docs/gluu-server>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | support@gluu.org | https://github.com/moabu |

## Source Code

* <https://github.com/GluuFederation/docker-opendj>
* <https://github.com/GluuFederation/cloud-native-edition/tree/master/pygluu/kubernetes/templates/helm/gluu/charts/opendj>

## Requirements

Kubernetes: `>=v1.21.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalAnnotations | object | `{}` | Additional annotations that will be added across all resources  in the format of {cert-manager.io/issuer: "letsencrypt-prod"}. key app is taken |
| additionalLabels | object | `{}` | Additional labels that will be added across all resources definitions in the format of {mylabel: "myapp"} |
| dnsConfig | object | `{}` | Add custom dns config |
| dnsPolicy | string | `""` | Add custom dns policy |
| fullnameOverride | string | `""` |  |
| hpa.behavior | object | `{}` | Scaling Policies |
| hpa.enabled | bool | `true` |  |
| hpa.maxReplicas | int | `10` |  |
| hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| hpa.minReplicas | int | `1` |  |
| hpa.targetCPUUtilizationPercentage | int | `50` |  |
| image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| image.pullSecrets | list | `[]` | Image Pull Secrets |
| image.repository | string | `"gluufederation/opendj"` | Image  to use for deploying. |
| image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| livenessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":20,"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for OpenDJ if needed. https://github.com/GluuFederation/docker-opendj/blob/4.3/scripts/healthcheck.py |
| livenessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | Executes the python3 healthcheck. |
| multiCluster.clusterId | string | `""` | This id needs to be unique to each kubernetes cluster in a multi cluster setup west, east, south, north, region ...etc If left empty it will be randomly generated. |
| multiCluster.enabled | bool | `false` | Enable OpenDJ multiCluster mode. This flag enables loading keys under `opendj.multiCluster` |
| multiCluster.namespaceIntId | int | `0` | Namespace int id. This id needs to be a unique number 0-9 per gluu installation per namespace. Used when gluu is installed in the same kubernetes cluster more than once. |
| multiCluster.replicaCount | int | `1` | The number of opendj non scalabble statefulsets to create. Each pod created must be resolvable as it follows the patterm RELEASE-NAME-opendj-CLUSTERID-regional-{{statefulset pod number}}-{{ $.Values.multiCluster.serfAdvertiseAddrSuffix }} If set to 1, with a release name of gluu,  the address of the pod would be gluu-opendj-regional-0-regional.gluu.org |
| multiCluster.serfAdvertiseAddrSuffix | string | `"regional.gluu.org:30946"` | OpenDJ Serf advertise address suffix that will be added to each opendj replica. i.e RELEASE-NAME-opendj-regional-{{statefulset pod number}}-{{ $.Values.multiCluster.serfAdvertiseAddrSuffix }} |
| multiCluster.serfKey | string | `"Z51b6PgKU1MZ75NCZOTGGoc0LP2OF3qvF6sjxHyQCYk="` | Serf key. This key will automatically sync across clusters. |
| multiCluster.serfPeers | list | `["gluu-opendj-regional-0-regional.gluu.org:30946","gluu-opendj-regional-0-regional.gluu.org:31946"]` | Serf peer addresses. One per cluster. |
| nameOverride | string | `""` |  |
| openDjVolumeMounts.config.mountPath | string | `"/opt/opendj/config"` |  |
| openDjVolumeMounts.config.name | string | `"opendj-volume"` |  |
| openDjVolumeMounts.db.mountPath | string | `"/opt/opendj/db"` |  |
| openDjVolumeMounts.db.name | string | `"opendj-volume"` |  |
| openDjVolumeMounts.flag.mountPath | string | `"/flag"` |  |
| openDjVolumeMounts.flag.name | string | `"opendj-volume"` |  |
| openDjVolumeMounts.ldif.mountPath | string | `"/opt/opendj/ldif"` |  |
| openDjVolumeMounts.ldif.name | string | `"opendj-volume"` |  |
| openDjVolumeMounts.logs.mountPath | string | `"/opt/opendj/logs"` |  |
| openDjVolumeMounts.logs.name | string | `"opendj-volume"` |  |
| persistence.accessModes | string | `"ReadWriteOnce"` |  |
| persistence.size | string | `"5Gi"` | OpenDJ volume size |
| persistence.type | string | `"DirectoryOrCreate"` |  |
| ports | object | `{"tcp-admin":{"nodePort":"","port":4444,"protocol":"TCP","targetPort":4444},"tcp-ldap":{"nodePort":"","port":1389,"protocol":"TCP","targetPort":1389},"tcp-ldaps":{"nodePort":"","port":1636,"protocol":"TCP","targetPort":1636},"tcp-repl":{"nodePort":"","port":8989,"protocol":"TCP","targetPort":8989},"tcp-serf":{"nodePort":"","port":7946,"protocol":"TCP","targetPort":7946},"udp-serf":{"nodePort":"","port":7946,"protocol":"UDP","targetPort":7946}}` | servicePorts values used in StatefulSet container |
| readinessProbe | object | `{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5}` | Configure the readiness healthcheck for OpenDJ if needed. https://github.com/GluuFederation/docker-opendj/blob/4.3/scripts/healthcheck.py |
| replicas | int | `1` | Service replica number. |
| resources | object | `{"limits":{"cpu":"1500m","memory":"2000Mi"},"requests":{"cpu":"1500m","memory":"2000Mi"}}` | Resource specs. |
| resources.limits.cpu | string | `"1500m"` | CPU limit. |
| resources.limits.memory | string | `"2000Mi"` | Memory limit. |
| resources.requests.cpu | string | `"1500m"` | CPU request. |
| resources.requests.memory | string | `"2000Mi"` | Memory request. |
| usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.5.0](https://github.com/norwoodj/helm-docs/releases/v1.5.0)
