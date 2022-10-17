# nginx-ingress

![Version: 1.0.3-dev](https://img.shields.io/badge/Version-1.0.3--dev-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.0.3-dev](https://img.shields.io/badge/AppVersion-1.0.3--dev-informational?style=flat-square)

Nginx ingress definitions chart

**Homepage:** <https://jans.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | <support@jans.io> | <https://github.com/moabu> |

## Source Code

* <https://github.com/kubernetes/ingress-nginx>
* <https://kubernetes.io/docs/concepts/services-networking/ingress/>
* <https://github.com/JanssenFederation/flex/tree/main/flex-cn-setup/pyjanssen/kubernetes/templates/helm/janssen/charts/nginx-ingress>

## Requirements

Kubernetes: `>=v1.21.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| fullnameOverride | string | `""` |  |
| ingress | object | `{"additionalAnnotations":{"kubernetes.io/ingress.class":"nginx"},"additionalLabels":{},"authServerAdditionalAnnotations":{},"authServerEnabled":true,"authServerLabels":{},"deviceCodeAdditionalAnnotations":{},"deviceCodeEnabled":true,"deviceCodeLabels":{},"enabled":true,"fido2ConfigAdditionalAnnotations":{},"fido2ConfigEnabled":false,"fido2ConfigLabels":{},"fido2Enabled":false,"fido2Labels":{},"firebaseMessagingAdditionalAnnotations":{},"firebaseMessagingEnabled":true,"firebaseMessagingLabels":{},"hosts":["demoexample.jans.io"],"legacy":false,"openidAdditionalAnnotations":{},"openidConfigEnabled":true,"openidConfigLabels":{},"path":"/","scimAdditionalAnnotations":{},"scimConfigAdditionalAnnotations":{},"scimConfigEnabled":false,"scimConfigLabels":{},"scimEnabled":false,"scimLabels":{},"tls":[{"hosts":["demoexample.jans.io"],"secretName":"tls-certificate"}],"u2fAdditionalAnnotations":{},"u2fConfigEnabled":true,"u2fConfigLabels":{},"uma2AdditionalAnnotations":{},"uma2ConfigEnabled":true,"uma2ConfigLabels":{},"webdiscoveryAdditionalAnnotations":{},"webdiscoveryEnabled":true,"webdiscoveryLabels":{},"webfingerAdditionalAnnotations":{},"webfingerEnabled":true,"webfingerLabels":{}}` | Nginx ingress definitions chart |
| ingress.additionalAnnotations | object | `{"kubernetes.io/ingress.class":"nginx"}` | Additional annotations that will be added across all ingress definitions in the format of {cert-manager.io/issuer: "letsencrypt-prod"}. key app is taken Enable client certificate authentication nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional" Create the secret containing the trusted ca certificates nginx.ingress.kubernetes.io/auth-tls-secret: "janssen/tls-certificate" Specify the verification depth in the client certificates chain nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1" Specify if certificates are passed to upstream server nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true" |
| ingress.additionalAnnotations."kubernetes.io/ingress.class" | string | `"nginx"` | Required annotation below. Use kubernetes.io/ingress.class: "public" for microk8s. |
| ingress.additionalLabels | object | `{}` | Additional labels that will be added across all ingress definitions in the format of {mylabel: "myapp"} |
| ingress.authServerAdditionalAnnotations | object | `{}` | Auth server ingress resource additional annotations. |
| ingress.authServerEnabled | bool | `true` | Enable Auth server endpoints /oxauth |
| ingress.authServerLabels | object | `{}` | Auth server config ingress resource labels. key app is taken |
| ingress.deviceCodeAdditionalAnnotations | object | `{}` | device-code ingress resource additional annotations. |
| ingress.deviceCodeEnabled | bool | `true` | Enable endpoint /device-code |
| ingress.deviceCodeLabels | object | `{}` | device-code ingress resource labels. key app is taken |
| ingress.fido2ConfigAdditionalAnnotations | object | `{}` | fido2 config ingress resource additional annotations. |
| ingress.fido2ConfigEnabled | bool | `false` | Enable endpoint /.well-known/fido2-configuration |
| ingress.fido2ConfigLabels | object | `{}` | fido2 config ingress resource labels. key app is taken |
| ingress.fido2Enabled | bool | `false` | Enable all fido2 endpoints |
| ingress.fido2Labels | object | `{}` | fido2 ingress resource labels. key app is taken |
| ingress.firebaseMessagingAdditionalAnnotations | object | `{}` | Firebase Messaging ingress resource additional annotations. |
| ingress.firebaseMessagingEnabled | bool | `true` | Enable endpoint /firebase-messaging-sw.js |
| ingress.firebaseMessagingLabels | object | `{}` | Firebase Messaging ingress resource labels. key app is taken |
| ingress.legacy | bool | `false` | Enable use of legacy API version networking.k8s.io/v1beta1 to support kubernetes 1.18. This flag should be removed next version release along with nginx-ingress/templates/ingress-legacy.yaml. |
| ingress.openidAdditionalAnnotations | object | `{}` | openid-configuration ingress resource additional annotations. |
| ingress.openidConfigEnabled | bool | `true` | Enable endpoint /.well-known/openid-configuration |
| ingress.openidConfigLabels | object | `{}` | openid-configuration ingress resource labels. key app is taken |
| ingress.scimAdditionalAnnotations | object | `{}` | SCIM ingress resource additional annotations. |
| ingress.scimConfigAdditionalAnnotations | object | `{}` | SCIM config ingress resource additional annotations. |
| ingress.scimConfigEnabled | bool | `false` | Enable endpoint /.well-known/scim-configuration |
| ingress.scimConfigLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| ingress.scimEnabled | bool | `false` | Enable SCIM endpoints /scim |
| ingress.scimLabels | object | `{}` | scim config ingress resource labels. key app is taken |
| ingress.u2fAdditionalAnnotations | object | `{}` | u2f config ingress resource additional annotations. |
| ingress.u2fConfigEnabled | bool | `true` | Enable endpoint /.well-known/fido-configuration |
| ingress.u2fConfigLabels | object | `{}` | u2f config ingress resource labels. key app is taken |
| ingress.uma2AdditionalAnnotations | object | `{}` | uma2 config ingress resource additional annotations. |
| ingress.uma2ConfigEnabled | bool | `true` | Enable endpoint /.well-known/uma2-configuration |
| ingress.uma2ConfigLabels | object | `{}` | uma 2 config ingress resource labels. key app is taken |
| ingress.webdiscoveryAdditionalAnnotations | object | `{}` | webdiscovery ingress resource additional annotations. |
| ingress.webdiscoveryEnabled | bool | `true` | Enable endpoint /.well-known/simple-web-discovery |
| ingress.webdiscoveryLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| ingress.webfingerAdditionalAnnotations | object | `{}` | webfinger ingress resource additional annotations. |
| ingress.webfingerEnabled | bool | `true` | Enable endpoint /.well-known/webfinger |
| ingress.webfingerLabels | object | `{}` | webfinger ingress resource labels. key app is taken |
| nameOverride | string | `""` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
