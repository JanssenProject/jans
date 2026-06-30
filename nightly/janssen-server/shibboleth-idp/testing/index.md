# Shibboleth IDP Testing Guide

This guide covers testing procedures for the Janssen Shibboleth IDP integration.

## Test Types

1. **Unit Tests** - Java unit tests for Config API plugin
1. **Integration Tests** - API endpoint testing with REST Assured
1. **Helm Chart Tests** - Template rendering validation
1. **End-to-End Tests** - Full authentication flow testing

## Running Unit Tests

### Config API Plugin Tests

```
cd jans-config-api/plugins/shibboleth-plugin
mvn test
```

### Test Configuration

Configure test properties in `src/test/resources/test.properties`:

```
shibbolethUrl=http://localhost:8080/jans-config-api
tokenEndpoint=https://your-server/jans-auth/restv1/token
clientId=your-test-client
clientSecret=your-test-secret
scopes=https://jans.io/oauth/config/shibboleth.readonly https://jans.io/oauth/config/shibboleth.write
```

## Helm Chart Validation

### Template Rendering

Validate the Shibboleth IDP chart renders correctly:

```
# Standalone subchart
helm template test charts/janssen/charts/shibboleth-idp \
  --set replicaCount=2 \
  --set hpa.enabled=true

# With parent chart
helm template janssen charts/janssen \
  --set global.shibboleth-idp.enabled=true \
  --set global.cnPersistenceType=sql \
  --set global.fqdn=test.example.com
```

### Lint Chart

```
helm lint charts/janssen/charts/shibboleth-idp
helm lint charts/janssen --set global.shibboleth-idp.enabled=true
```

## End-to-End Testing

### Prerequisites

1. Running Janssen Auth Server
1. Configured OAuth client for Shibboleth IDP
1. Test Service Provider with SAML support

### Test Scenario 1: SP-Initiated SSO

1. **Access SP Application**
1. Navigate to a protected resource on the SP
1. SP generates SAML AuthnRequest
1. **Redirect to IDP**
1. SP redirects to Shibboleth IDP SSO endpoint
1. IDP receives SAML request
1. **Janssen Auth Authentication**
1. IDP redirects to Janssen Auth Server
1. User authenticates (password, MFA, etc.)
1. Auth Server returns OAuth tokens
1. **SAML Response**
1. IDP creates SAML assertion with user attributes
1. IDP POSTs SAML response to SP ACS
1. **Verify Access**
1. SP validates SAML response
1. User gains access to protected resource

### Test Scenario 2: Config API Operations

```
# Get access token
ACCESS_TOKEN=$(curl -s -X POST \
  "https://your-server/jans-auth/restv1/token" \
  -d "grant_type=client_credentials" \
  -d "client_id=test-client" \
  -d "client_secret=test-secret" \
  -d "scope=https://jans.io/oauth/config/shibboleth.readonly" \
  | jq -r '.access_token')

# Get IDP configuration
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://your-server/jans-config-api/shibboleth/config

# List trusted SPs
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://your-server/jans-config-api/shibboleth/trust
```

### Test Scenario 3: Add Trusted SP

```
# Create trusted SP
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entityId": "https://sp.example.org",
    "name": "Test SP",
    "enabled": true,
    "metadataUrl": "https://sp.example.org/metadata",
    "releasedAttributes": ["uid", "mail", "displayName"]
  }' \
  https://your-server/jans-config-api/shibboleth/trust

# Verify SP was added
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://your-server/jans-config-api/shibboleth/trust/https%3A%2F%2Fsp.example.org
```

## Kubernetes Testing

### Deploy Test Environment

```
# Create namespace
kubectl create namespace jans-test

# Deploy with Shibboleth enabled
helm install janssen charts/janssen \
  --namespace jans-test \
  --set global.shibboleth-idp.enabled=true \
  --set global.fqdn=test.example.com \
  --set global.cnPersistenceType=sql

# Wait for pods
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=shibboleth-idp -n jans-test --timeout=300s

# Check status
kubectl get pods -n jans-test -l app.kubernetes.io/name=shibboleth-idp
```

### Verify IDP Status

```
# Port forward
kubectl port-forward -n jans-test svc/shibboleth-idp 8080:8080 &

# Check IDP status
curl http://localhost:8080/idp/status

# Get IDP metadata
curl http://localhost:8080/idp/shibboleth
```

## Troubleshooting

### Common Issues

1. **IDP Status Returns 503**
1. Check pod logs: `kubectl logs -l app=shibboleth-idp`
1. Verify Janssen Auth Server is accessible
1. Check OAuth client configuration
1. **SAML Response Invalid**
1. Verify IDP signing certificate
1. Check attribute release policies
1. Validate SP metadata registration
1. **Authentication Fails**
1. Check Janssen Auth Server logs
1. Verify OAuth redirect URI configuration
1. Confirm OAuth client scopes

### Debug Logging

Enable debug logging in Shibboleth:

```
# Kubernetes
kubectl set env deployment/shibboleth-idp -n jans-test \
  IDP_LOG_LEVEL=DEBUG

# Docker
docker run -e IDP_LOG_LEVEL=DEBUG janssenproject/shibboleth:5.1.6_dev
```

## Test Coverage

| Component                  | Test Type   | Status |
| -------------------------- | ----------- | ------ |
| Config API - GET /config   | Integration | Ready  |
| Config API - PUT /config   | Integration | Ready  |
| Config API - GET /trust    | Integration | Ready  |
| Config API - POST /trust   | Integration | Ready  |
| Config API - DELETE /trust | Integration | Ready  |
| Helm Chart Rendering       | Unit        | Ready  |
| SAML SSO Flow              | E2E         | Manual |
| Janssen Auth Integration   | E2E         | Manual |
