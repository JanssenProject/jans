# nginx-ingress

![Version: 1.2.0](https://img.shields.io/badge/Version-1.2.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.2.0](https://img.shields.io/badge/AppVersion-1.2.0-informational?style=flat-square)

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

Kubernetes: `>=v1.22.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| fullnameOverride | string | `""` |  |
| ingress | object | `{"additionalAnnotations":{},"additionalLabels":{},"enabled":true,"hosts":["demoexample.jans.io"],"ingressClassName":"nginx","legacy":false,"path":"/","tls":[{"hosts":["demoexample.jans.io"],"secretName":"tls-certificate"}]}` | Nginx ingress definitions chart |
| ingress.additionalAnnotations | object | `{}` | Additional annotations that will be added across all ingress definitions in the format of {cert-manager.io/issuer: "letsencrypt-prod"}. key app is taken Enable client certificate authentication nginx.ingress.kubernetes.io/auth-tls-verify-client: "optional" Create the secret containing the trusted ca certificates nginx.ingress.kubernetes.io/auth-tls-secret: "janssen/tls-certificate" Specify the verification depth in the client certificates chain nginx.ingress.kubernetes.io/auth-tls-verify-depth: "1" Specify if certificates are passed to upstream server nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true" |
| ingress.additionalLabels | object | `{}` | Additional labels that will be added across all ingress definitions in the format of {mylabel: "myapp"} |
| ingress.legacy | bool | `false` | Enable use of legacy API version networking.k8s.io/v1beta1 to support kubernetes 1.18. This flag should be removed next version release along with nginx-ingress/templates/ingress-legacy.yaml. |
| nameOverride | string | `""` |  |
