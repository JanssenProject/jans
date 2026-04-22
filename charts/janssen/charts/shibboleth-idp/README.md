# shibboleth-idp

![Version: 0.0.0-nightly](https://img.shields.io/badge/Version-0.0.0--nightly-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 5.1.6](https://img.shields.io/badge/AppVersion-5.1.6-informational?style=flat-square)

Shibboleth Identity Provider 5.1.6 for SAML SSO, integrated with Janssen Auth Server for authentication.

**Homepage:** <https://jans.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Janssen Project | <support@jans.io> | <https://github.com/JanssenProject> |

## Source Code

* <https://github.com/JanssenProject/jans>
* <https://shibboleth.net/products/identity-provider.html>

## Requirements

Kubernetes: `>=v1.23.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalAnnotations | object | `{}` |  |
| additionalLabels | object | `{}` |  |
| affinity | object | `{}` |  |
| configmaps.jansProperties.enabled | bool | `true` |  |
| fullnameOverride | string | `""` |  |
| hpa.behavior | object | `{}` |  |
| hpa.enabled | bool | `false` |  |
| hpa.maxReplicas | int | `10` |  |
| hpa.metrics | list | `[]` |  |
| hpa.minReplicas | int | `1` |  |
| hpa.targetCPUUtilizationPercentage | int | `80` |  |
| image.pullPolicy | string | `"IfNotPresent"` |  |
| image.repository | string | `"ghcr.io/janssenproject/jans/shibboleth"` |  |
| image.tag | string | `"0.0.0-nightly"` |  |
| imagePullSecrets | list | `[]` |  |
| ingress.annotations | object | `{}` |  |
| ingress.enabled | bool | `true` |  |
| ingress.hosts[0].host | string | `""` |  |
| ingress.hosts[0].paths[0].path | string | `"/idp"` |  |
| ingress.hosts[0].paths[0].pathType | string | `"Prefix"` |  |
| ingress.tls | list | `[]` |  |
| lifecycle.preStop.exec.command[0] | string | `"/bin/sh"` |  |
| lifecycle.preStop.exec.command[1] | string | `"-c"` |  |
| lifecycle.preStop.exec.command[2] | string | `"sleep 20"` |  |
| livenessProbe.failureThreshold | int | `3` |  |
| livenessProbe.httpGet.path | string | `"/idp/status"` |  |
| livenessProbe.httpGet.port | int | `8080` |  |
| livenessProbe.initialDelaySeconds | int | `60` |  |
| livenessProbe.periodSeconds | int | `10` |  |
| livenessProbe.timeoutSeconds | int | `5` |  |
| nameOverride | string | `""` |  |
| nodeSelector | object | `{}` |  |
| podAnnotations | object | `{}` |  |
| podSecurityContext.fsGroup | int | `1000` |  |
| readinessProbe.failureThreshold | int | `3` |  |
| readinessProbe.httpGet.path | string | `"/idp/status"` |  |
| readinessProbe.httpGet.port | int | `8080` |  |
| readinessProbe.initialDelaySeconds | int | `30` |  |
| readinessProbe.periodSeconds | int | `5` |  |
| readinessProbe.timeoutSeconds | int | `5` |  |
| replicaCount | int | `1` |  |
| resources.limits.cpu | string | `"2000m"` |  |
| resources.limits.memory | string | `"1024Mi"` |  |
| resources.requests.cpu | string | `"500m"` |  |
| resources.requests.memory | string | `"512Mi"` |  |
| securityContext.runAsNonRoot | bool | `true` |  |
| securityContext.runAsUser | int | `1000` |  |
| service.name | string | `"http-shibboleth"` |  |
| service.port | int | `8080` |  |
| service.sessionAffinity | string | `"None"` |  |
| service.sessionAffinityConfig.clientIP.timeoutSeconds | int | `10800` |  |
| service.type | string | `"ClusterIP"` |  |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.create | bool | `true` |  |
| serviceAccount.name | string | `""` |  |
| shibboleth.encryptionKeyAlias | string | `"idp-encryption"` |  |
| shibboleth.entityId | string | `""` |  |
| shibboleth.jansAuth.clientId | string | `""` |  |
| shibboleth.jansAuth.enabled | bool | `true` |  |
| shibboleth.jansAuth.redirectUri | string | `""` |  |
| shibboleth.jansAuth.scopes | string | `"openid,profile,email"` |  |
| shibboleth.scope | string | `""` |  |
| shibboleth.signingKeyAlias | string | `"idp-signing"` |  |
| tolerations | list | `[]` |  |
| usrEnvs.normal | object | `{}` |  |
| usrEnvs.secret | object | `{}` |  |
| volumeMounts | list | `[]` |  |
| volumes | list | `[]` |  |
