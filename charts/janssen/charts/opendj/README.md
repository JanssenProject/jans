# opendj

![Version: 1.0.7-dev](https://img.shields.io/badge/Version-1.0.7--dev-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.0.7-dev](https://img.shields.io/badge/AppVersion-1.0.7--dev-informational?style=flat-square)

OpenDJ is a directory server which implements a wide range of Lightweight Directory Access Protocol and related standards, including full compliance with LDAPv3 but also support for Directory Service Markup Language (DSMLv2).Written in Java, OpenDJ offers multi-master replication, access control, and many extensions.

**Homepage:** <https://jans.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | <support@jans.io> | <https://github.com/moabu> |

## Source Code

* <https://github.com/GluuFederation/docker-opendj>

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
| hpa | object | `{"behavior":{},"enabled":true,"maxReplicas":10,"metrics":[],"minReplicas":1,"targetCPUUtilizationPercentage":50}` | Configure the HorizontalPodAutoscaler |
| hpa.behavior | object | `{}` | Scaling Policies |
| hpa.metrics | list | `[]` | metrics if targetCPUUtilizationPercentage is not set |
| image.pullPolicy | string | `"IfNotPresent"` | Image pullPolicy to use for deploying. |
| image.pullSecrets | list | `[]` | Image Pull Secrets |
| image.repository | string | `"gluufederation/opendj"` | Image  to use for deploying. |
| image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| livenessProbe | object | `{"exec":{"command":["python3","/app/scripts/healthcheck.py"]},"failureThreshold":20,"initialDelaySeconds":30,"periodSeconds":30,"timeoutSeconds":5}` | Configure the liveness healthcheck for OpenDJ if needed. |
| livenessProbe.exec | object | `{"command":["python3","/app/scripts/healthcheck.py"]}` | Executes the python3 healthcheck. |
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
| readinessProbe | object | `{"failureThreshold":20,"initialDelaySeconds":60,"periodSeconds":25,"tcpSocket":{"port":1636},"timeoutSeconds":5}` | Configure the readiness healthcheck for OpenDJ if needed. |
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
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
