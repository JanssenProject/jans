# nginx-ingress

![Version: 1.0.8-dev](https://img.shields.io/badge/Version-1.0.8--dev-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.0.8-dev](https://img.shields.io/badge/AppVersion-1.0.8--dev-informational?style=flat-square)

Nginx ingress definitions chart

**Homepage:** <https://jans.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | <support@jans.io> | <https://github.com/moabu> |

## Source Code

* <https://github.com/kubernetes/ingress-nginx>
* <https://kubernetes.io/docs/concepts/services-networking/ingress/>

## Requirements

Kubernetes: `>=v1.21.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| fullnameOverride | string | `""` |  |
| ingress | object | `{"additionalAnnotations":{"kubernetes.io/ingress.class":"nginx"},"additionalLabels":{},"authServerAdditionalAnnotations":{},"authServerLabels":{},"deviceCodeAdditionalAnnotations":{},"deviceCodeLabels":{},"enabled":true,"fido2ConfigAdditionalAnnotations":{},"fido2ConfigLabels":{},"fido2Enabled":false,"fido2Labels":{},"firebaseMessagingAdditionalAnnotations":{},"firebaseMessagingLabels":{},"hosts":["demoexample.jans.io"],"legacy":false,"openidAdditionalAnnotations":{},"openidConfigLabels":{},"path":"/","scimAdditionalAnnotations":{},"scimConfigAdditionalAnnotations":{},"scimConfigLabels":{},"scimLabels":{},"tls":[{"hosts":["demoexample.jans.io"],"secretName":"tls-certificate"}],"u2fAdditionalAnnotations":{},"u2fConfigLabels":{},"uma2AdditionalAnnotations":{},"uma2ConfigLabels":{},"webdiscoveryAdditionalAnnotations":{},"webdiscoveryLabels":{},"webfingerAdditionalAnnotations":{},"webfingerLabels":{}}` | Nginx ingress definitions chart |
| ingress.additionalAnnotations | object | `{"kubernetes.io/ingress.class":"nginx"}` | Additional annotations that will be added across all ingress definitions in the format of {cert-manager.io/issuer: "letsencrypt-prod"}. key app is taken Enable client certificate authentication nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional" Create the secret containing the trusted ca certificates nginx.ingress.kubernetes.io/auth-tls-secret: "janssen/tls-certificate" Specify the verification depth in the client certificates chain nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1" Specify if certificates are passed to upstream server nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true" |
| ingress.additionalAnnotations."kubernetes.io/ingress.class" | string | `"nginx"` | Required annotation below. Use kubernetes.io/ingress.class: "public" for microk8s. |
| ingress.additionalLabels | object | `{}` | Additional labels that will be added across all ingress definitions in the format of {mylabel: "myapp"} |
| ingress.authServerAdditionalAnnotations | object | `{}` | Auth server ingress resource additional annotations. |
| ingress.authServerLabels | object | `{}` | Auth server config ingress resource labels. key app is taken |
| ingress.deviceCodeAdditionalAnnotations | object | `{}` | device-code ingress resource additional annotations. |
| ingress.deviceCodeLabels | object | `{}` | device-code ingress resource labels. key app is taken |
| ingress.fido2ConfigAdditionalAnnotations | object | `{}` | fido2 config ingress resource additional annotations. |
| ingress.fido2ConfigLabels | object | `{}` | fido2 config ingress resource labels. key app is taken |
| ingress.fido2Enabled | bool | `false` | Enable all fido2 endpoints |
| ingress.fido2Labels | object | `{}` | fido2 ingress resource labels. key app is taken |
| ingress.firebaseMessagingAdditionalAnnotations | object | `{}` | Firebase Messaging ingress resource additional annotations. |
| ingress.firebaseMessagingLabels | object | `{}` | Firebase Messaging ingress resource labels. key app is taken |
| ingress.legacy | bool | `false` | Enable use of legacy API version networking.k8s.io/v1beta1 to support kubernetes 1.18. This flag should be removed next version release along with nginx-ingress/templates/ingress-legacy.yaml. |
| ingress.openidAdditionalAnnotations | object | `{}` | openid-configuration ingress resource additional annotations. |
| ingress.openidConfigLabels | object | `{}` | openid-configuration ingress resource labels. key app is taken |
| ingress.scimAdditionalAnnotations | object | `{}` | SCIM ingress resource additional annotations. |
| ingress.scimConfigAdditionalAnnotations | object | `{}` | SCIM config ingress resource additional annotations. |
| ingress.scimConfigLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| ingress.scimLabels | object | `{}` | scim config ingress resource labels. key app is taken |
| ingress.u2fAdditionalAnnotations | object | `{}` | u2f config ingress resource additional annotations. |
| ingress.u2fConfigLabels | object | `{}` | u2f config ingress resource labels. key app is taken |
| ingress.uma2AdditionalAnnotations | object | `{}` | uma2 config ingress resource additional annotations. |
| ingress.uma2ConfigLabels | object | `{}` | uma 2 config ingress resource labels. key app is taken |
| ingress.webdiscoveryAdditionalAnnotations | object | `{}` | webdiscovery ingress resource additional annotations. |
| ingress.webdiscoveryLabels | object | `{}` | webdiscovery ingress resource labels. key app is taken |
| ingress.webfingerAdditionalAnnotations | object | `{}` | webfinger ingress resource additional annotations. |
| ingress.webfingerLabels | object | `{}` | webfinger ingress resource labels. key app is taken |
| nameOverride | string | `""` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
