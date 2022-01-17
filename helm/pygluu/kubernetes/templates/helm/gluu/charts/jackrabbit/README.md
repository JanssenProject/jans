# jackrabbit

![Version: 5.0.2](https://img.shields.io/badge/Version-5.0.2-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 5.0.0](https://img.shields.io/badge/AppVersion-5.0.0-informational?style=flat-square)

Jackrabbit Oak is a complementary implementation of the JCR specification. It is an effort to implement a scalable and performant hierarchical content repository for use as the foundation of modern world-class web sites and other demanding content applications.

**Homepage:** <https://gluu.org/docs/gluu-server/installation-guide/install-kubernetes/#working-with-jackrabbit>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | support@gluu.org | https://github.com/moabu |

## Source Code

* <https://gluu.org/docs/gluu-server/installation-guide/install-kubernetes/#working-with-jackrabbit>
* <https://github.com/GluuFederation/docker-jackrabbit>
* <https://jackrabbit.apache.org/jcr/index.html>
* <https://github.com/GluuFederation/cloud-native-edition/tree/master/pygluu/kubernetes/templates/helm/gluu/charts/jackrabbit>

## Requirements

Kubernetes: `>=v1.21.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalAnnotations | object | `{}` | Additional annotations that will be added across the gateway in the format of {cert-manager.io/issuer: "letsencrypt-prod"} |
| additionalLabels | object | `{}` | Additional labels that will be added across the gateway in the format of {mylabel: "myapp"} |
| clusterId | string | `""` | This id needs to be unique to each kubernetes cluster in a multi cluster setup west, east, south, north, region ...etc If left empty it will be randomly generated. |
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
| image.repository | string | `"gluufederation/jackrabbit"` | Image  to use for deploying. |
| image.tag | string | `"5.0.0_dev"` | Image  tag to use for deploying. |
| jackrabbitVolumeMounts.repository.mountPath | string | `"/opt/jackrabbit/repository"` |  |
| jackrabbitVolumeMounts.repository.name | string | `"jackrabbit-volume"` |  |
| jackrabbitVolumeMounts.version.mountPath | string | `"/opt/jackrabbit/version"` |  |
| jackrabbitVolumeMounts.version.name | string | `"jackrabbit-volume"` |  |
| jackrabbitVolumeMounts.workspaces.mountPath | string | `"opt/jackrabbit/workspaces"` |  |
| jackrabbitVolumeMounts.workspaces.name | string | `"jackrabbit-volume"` |  |
| livenessProbe | object | `{"initialDelaySeconds":25,"periodSeconds":25,"tcpSocket":{"port":"http-jackrabbit"},"timeoutSeconds":5}` | Configure the liveness healthcheck for the Jackrabbit if needed. |
| livenessProbe.tcpSocket | object | `{"port":"http-jackrabbit"}` | Executes tcp healthcheck. |
| nameOverride | string | `""` |  |
| readinessProbe | object | `{"initialDelaySeconds":30,"periodSeconds":30,"tcpSocket":{"port":"http-jackrabbit"},"timeoutSeconds":5}` | Configure the readiness healthcheck for the Jackrabbit if needed. |
| readinessProbe.tcpSocket | object | `{"port":"http-jackrabbit"}` | Executes tcp healthcheck. |
| replicas | int | `1` | Service replica number. |
| resources | object | `{"limits":{"cpu":"1500m","memory":"1000Mi"},"requests":{"cpu":"1500m","memory":"1000Mi"}}` | Resource specs. |
| resources.limits.cpu | string | `"1500m"` | CPU limit. |
| resources.limits.memory | string | `"1000Mi"` | Memory limit. |
| resources.requests.cpu | string | `"1500m"` | CPU request. |
| resources.requests.memory | string | `"1000Mi"` | Memory request. |
| secrets.cnJackrabbitAdminPassword | string | `"admin"` | Jackrabbit admin uid password |
| secrets.cnJackrabbitPostgresPassword | string | `"P@ssw0rd"` | Jackrabbit Postgres uid password |
| service.name | string | `"http-jackrabbit"` | The name of the jackrabbit port within the jackrabbit service. Please keep it as default. |
| service.port | int | `8080` | Port of the jackrabbit service. Please keep it as default. |
| service.sessionAffinity | string | `"None"` | Default set to None If you want to make sure that connections from a particular client are passed to the same Pod each time, you can select the session affinity based on the client's IP addresses by setting this to ClientIP |
| service.sessionAffinityConfig | object | `{"clientIP":{"timeoutSeconds":10800}}` | the maximum session sticky time if sessionAffinity is ClientIP |
| storage.accessModes | string | `"ReadWriteOnce"` |  |
| storage.size | string | `"5Gi"` | Jackrabbit volume size |
| storage.type | string | `"DirectoryOrCreate"` |  |
| usrEnvs | object | `{"normal":{},"secret":{}}` | Add custom normal and secret envs to the service |
| usrEnvs.normal | object | `{}` | Add custom normal envs to the service variable1: value1 |
| usrEnvs.secret | object | `{}` | Add custom secret envs to the service variable1: value1 |
| volumeMounts | list | `[]` | Configure any additional volumesMounts that need to be attached to the containers |
| volumes | list | `[]` | Configure any additional volumes that need to be attached to the pod |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.5.0](https://github.com/norwoodj/helm-docs/releases/v1.5.0)
