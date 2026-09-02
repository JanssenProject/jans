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
| gateway | object | `{"annotations":{},"attachLbIp":false,"className":"nginx","enabled":true,"gatewayNamespace":"","httpPort":80,"httpSectionName":"http","httpsPort":443,"httpsSectionName":"https","infrastructure":{"annotations":{},"labels":{},"parametersRef":{}},"labels":{},"name":"jans-gateway","tlsSecretName":"tls-certificate"}` | Configuration for Gateway resource |
| gateway.annotations | object | `{}` | Specific annotations for the Gateway resource |
| gateway.attachLbIp | bool | `false` | Attach global.lbIp to Gateway spec.addresses with IPAddress type (enable this if loadbalancer doesn't assign IP address to Gateway automatically) |
| gateway.className | string | `"nginx"` | Set the gatewayClassName corresponding to your installed controller. |
| gateway.enabled | bool | `true` | Enable Gateway API and create a Gateway resource (if disabled, you will have to create and manage the Gateway resource externally). |
| gateway.gatewayNamespace | string | `""` | Namespace the Gateway resource resides in. Set this ONLY if the Gateway is externally managed in a different namespace than this Helm release. That Gateway's listeners must allow routes from this release's namespace via spec.listeners[].allowedRoutes.namespaces, otherwise the HTTPRoutes will not attach. |
| gateway.httpPort | int | `80` | Gateway http port number |
| gateway.httpSectionName | string | `"http"` | Names of the Gateway listeners the HTTPRoutes attach to. Only change these if your controller requires different listener names (e.g. some controllers require the listener name to be `default`). When the Gateway is externally managed (gateway.enabled=false), these must match the listener names on that Gateway. |
| gateway.httpsPort | int | `443` | Gateway https port number |
| gateway.infrastructure | object | `{"annotations":{},"labels":{},"parametersRef":{}}` | Gateway spec.infrastructure |
| gateway.infrastructure.annotations | object | `{}` | Specific annotations for the infrastructure |
| gateway.infrastructure.labels | object | `{}` | Specific labels for the infrastructure |
| gateway.infrastructure.parametersRef | object | `{}` | Specific parametersRef for the infrastructure Some gateway implementation like `airlock-microgateway` may need to attach GatewayParameters to create Loadbalancer service automatically. |
| gateway.labels | object | `{}` | Specific labels for the Gateway resource |
| gateway.name | string | `"jans-gateway"` | The name of the Gateway resource to be created |
| gateway.tlsSecretName | string | `"tls-certificate"` | Secret containing the TLS certificate for the Gateway |
| nameOverride | string | `""` |  |
| routes | object | `{"annotations":{},"authServerEnabled":true,"authzenConfigEnabled":true,"casaEnabled":false,"configApiEnabled":true,"deviceCodeEnabled":true,"fido2ConfigEnabled":false,"fido2Enabled":false,"fido2WebauthnEnabled":false,"firebaseMessagingEnabled":true,"labels":{},"lockAuditEnabled":false,"lockConfigEnabled":false,"openidConfigEnabled":true,"scimConfigEnabled":false,"scimEnabled":false,"uma2ConfigEnabled":true,"webfingerEnabled":true}` | Configuration for HTTPRoute and its related resources |
| routes.annotations | object | `{}` | Specific annotations for the HTTPRoute resource |
| routes.authServerEnabled | bool | `true` | Enable Auth server endpoints /jans-auth |
| routes.authzenConfigEnabled | bool | `true` | Enable endpoint /.well-known/authzen-configuration |
| routes.casaEnabled | bool | `false` | Enable Casa endpoints /jans-casa |
| routes.configApiEnabled | bool | `true` | Enable Config API endpoints /jans-config-api |
| routes.deviceCodeEnabled | bool | `true` | Enable endpoint /device-code |
| routes.fido2ConfigEnabled | bool | `false` | Enable endpoint /.well-known/fido2-configuration |
| routes.fido2Enabled | bool | `false` | Enable all fido2 endpoints /jans-fido2 |
| routes.fido2WebauthnEnabled | bool | `false` | Enable endpoint /.well-known/webauthn |
| routes.firebaseMessagingEnabled | bool | `true` | Enable endpoint /firebase-messaging-sw.js |
| routes.labels | object | `{}` | Specific labels for the HTTPRoute resource |
| routes.lockAuditEnabled | bool | `false` | Enable gRPC endpoint /io.jans.lock.audit.AuditService (if enabled, global.auth-server.lockEnabled must be enabled) |
| routes.lockConfigEnabled | bool | `false` | Enable endpoint /.well-known/lock-server-configuration (if enabled, global.auth-server.lockEnabled must be enabled) |
| routes.openidConfigEnabled | bool | `true` | Enable endpoint /.well-known/openid-configuration |
| routes.scimConfigEnabled | bool | `false` | Enable endpoint /.well-known/scim-configuration |
| routes.scimEnabled | bool | `false` | Enable SCIM endpoints /jans-scim |
| routes.uma2ConfigEnabled | bool | `true` | Enable endpoint /.well-known/uma2-configuration |
| routes.webfingerEnabled | bool | `true` | Enable endpoint /.well-known/webfinger |
