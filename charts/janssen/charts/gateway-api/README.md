# gateway-api

![Version: 0.0.0-nightly](https://img.shields.io/badge/Version-0.0.0--nightly-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.0.0-nightly](https://img.shields.io/badge/AppVersion-0.0.0--nightly-informational?style=flat-square)

Gateway API definitions chart

**Homepage:** <https://jans.io>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Mohammad Abudayyeh | <support@jans.io> | <https://github.com/moabu> |

## Source Code

* <https://github.com/JanssenProject/jans/tree/main/charts/janssen/charts/gateway-api>

## Requirements

Kubernetes: `>=v1.23.0-0`

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalConfig | object | `{"airlock":{"createLbService":false},"cilium":{"ipPoolBlocks":[]},"envoy":{"createGatewayClass":false},"istio":{},"kgateway":{},"nginx":{"enableAuditGrpcRewriteSnippets":false},"traefik":{}}` | Additional configuration for Specific Gateway API implementation |
| additionalConfig.airlock | object | `{"createLbService":false}` | Configuration for Airlock Microgateway |
| additionalConfig.airlock.createLbService | bool | `false` | Create LoadBalancer service using GatewayParameters (by default airlock-microgateway doesn't create the service). See https://docs.airlock.com/microgateway/latest/index/api/crds/gateway-parameters/v1alpha1/ for details. The GatewayParameters will be attached to gateway.infrastructure.parametersRef only if it's empty. |
| additionalConfig.cilium | object | `{"ipPoolBlocks":[]}` | Configuration for Cilium. |
| additionalConfig.cilium.ipPoolBlocks | list | `[]` | Create Cilium IP pool with the specified blocks. See https://docs.cilium.io/en/stable/network/lb-ipam/ for details. |
| additionalConfig.envoy | object | `{"createGatewayClass":false}` | Configuration for Envoy. |
| additionalConfig.envoy.createGatewayClass | bool | `false` | Create GatewayClass named `envoy` (by default Envoy doesn't create gatewayclass). The `envoy` name can be set as value of `gateway.className` attribute. |
| additionalConfig.istio | object | `{}` | Configuration for Istio. |
| additionalConfig.kgateway | object | `{}` | Configuration for kgateway. |
| additionalConfig.nginx | object | `{"enableAuditGrpcRewriteSnippets":false}` | Configuration for NGINX Fabric. |
| additionalConfig.nginx.enableAuditGrpcRewriteSnippets | bool | `false` | Enable URL rewrite to forward audit gRPC requests `/io.jans.lock.audit.AuditService` to `/jans-auth/io.jans.lock.audit.AuditService`. Snippet support must be enabled during NGINX installation (otherwise endpoints will return HTTP status code 500). See https://docs.nginx.com/nginx-gateway-fabric/traffic-management/snippets#setup |
| additionalConfig.traefik | object | `{}` | Configuration for Traefik. |
| fullnameOverride | string | `""` |  |
| gateway | object | `{"annotations":{},"attachLbIp":false,"className":"nginx","enabled":true,"httpPort":80,"httpsPort":443,"infrastructure":{"annotations":{},"labels":{},"parametersRef":{}},"labels":{},"name":"jans-gateway","tlsSecretName":"tls-certificate"}` | Configuration for Gateway resource |
| gateway.annotations | object | `{}` | Specific annotations for the Gateway resource |
| gateway.attachLbIp | bool | `false` | Attach global.lbIp to Gateway spec.addresses with IPAddress type (enable this if loadbalancer doesn't assign IP address to Gateway automatically) |
| gateway.className | string | `"nginx"` | Set the gatewayClassName corresponding to your installed controller. |
| gateway.enabled | bool | `true` | Enable Gateway API and create a Gateway resource (if disabled, you will have to create and manage the Gateway resource externally). |
| gateway.httpPort | int | `80` | Gateway http port number |
| gateway.httpsPort | int | `443` | Gateway https port number |
| gateway.infrastructure | object | `{"annotations":{},"labels":{},"parametersRef":{}}` | Gateway spec.infrastructure |
| gateway.infrastructure.annotations | object | `{}` | Specific annotations for the infrastructure |
| gateway.infrastructure.labels | object | `{}` | Specific labels for the infrastructure |
| gateway.infrastructure.parametersRef | object | `{}` | Specific parametersRef for the infrastructure Some gateway implementation like `airlock-microgateway` may need to attach GatewayParameters to create Loadbalancer service automatically. |
| gateway.labels | object | `{}` | Specific labels for the Gateway resource |
| gateway.name | string | `"jans-gateway"` | The name of the Gateway resource to be created |
| gateway.tlsSecretName | string | `"tls-certificate"` | Secret containing the TLS certificate for the Gateway |
| nameOverride | string | `""` |  |
| routes | object | `{"annotations":{},"gatewayNamespace":"","httpSectionName":"http","httpsSectionName":"https","labels":{}}` | Configuration for HTTPRoute and its related resources |
| routes.annotations | object | `{}` | Specific annotations for the HTTPRoute resource |
| routes.gatewayNamespace | string | `""` | Namespace where the Gateway resource resides. Set this ONLY if the Gateway is externally managed in a different namespace than this Helm release. If set, ensure the target namespace exists and your Gateway controller has the required cross-namespace RBAC permissions. |
| routes.httpSectionName | string | `"http"` | Only set the httpSectionName and httpsSectionName if it doesn't work with the default values, according to your installed controller (e.g. some controller may require the listener name to be `default`). |
| routes.labels | object | `{}` | Specific labels for the HTTPRoute resource |
