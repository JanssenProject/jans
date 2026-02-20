---
tags:
  - administration
  - shibboleth
  - testing
---

# Shibboleth IDP Testing Guide

This guide covers testing procedures for the Janssen Shibboleth IDP integration.

## Test Types

1. **Unit Tests** - Java unit tests for Config API plugin
2. **Integration Tests** - API endpoint testing with REST Assured
3. **Helm Chart Tests** - Template rendering validation
4. **End-to-End Tests** - Full authentication flow testing

## Running Unit Tests

### Config API Plugin Tests

```bash
cd jans-config-api/plugins/shibboleth-plugin
mvn test
```

### Test Configuration

Configure test properties in `src/test/resources/test.properties`:

```properties
shibbolethUrl=http://localhost:8080/jans-config-api
tokenEndpoint=https://your-server/jans-auth/restv1/token
clientId=your-test-client
clientSecret=your-test-secret
scopes=https://jans.io/oauth/config/shibboleth.readonly https://jans.io/oauth/config/shibboleth.write
```

## Helm Chart Validation

### Template Rendering

Validate the Shibboleth IDP chart renders correctly:

```bash
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

```bash
helm lint charts/janssen/charts/shibboleth-idp
helm lint charts/janssen --set global.shibboleth-idp.enabled=true
```

## End-to-End Testing

### Prerequisites

1. Running Janssen Auth Server
2. Configured OAuth client for Shibboleth IDP
3. Test Service Provider with SAML support

### Test Scenario 1: SP-Initiated SSO

1. **Access SP Application**
   - Navigate to a protected resource on the SP
   - SP generates SAML AuthnRequest

2. **Redirect to IDP**
   - SP redirects to Shibboleth IDP SSO endpoint
   - IDP receives SAML request

3. **Janssen Auth Authentication**
   - IDP redirects to Janssen Auth Server
   - User authenticates (password, MFA, etc.)
   - Auth Server returns OAuth tokens

4. **SAML Response**
   - IDP creates SAML assertion with user attributes
   - IDP POSTs SAML response to SP ACS

5. **Verify Access**
   - SP validates SAML response
   - User gains access to protected resource

### Test Scenario 2: Config API Operations

```bash
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

```bash
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

```bash
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

```bash
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
   - Check pod logs: `kubectl logs -l app=shibboleth-idp`
   - Verify Janssen Auth Server is accessible
   - Check OAuth client configuration

2. **SAML Response Invalid**
   - Verify IDP signing certificate
   - Check attribute release policies
   - Validate SP metadata registration

3. **Authentication Fails**
   - Check Janssen Auth Server logs
   - Verify OAuth redirect URI configuration
   - Confirm OAuth client scopes

### Debug Logging

Enable debug logging in Shibboleth:

```bash
# Kubernetes
kubectl set env deployment/shibboleth-idp -n jans-test \
  IDP_LOG_LEVEL=DEBUG

# Docker
docker run -e IDP_LOG_LEVEL=DEBUG janssenproject/shibboleth:5.1.6_dev
```

## Test Coverage

| Component | Test Type | Status |
|-----------|-----------|--------|
| Config API - GET /config | Integration | Ready |
| Config API - PUT /config | Integration | Ready |
| Config API - GET /trust | Integration | Ready |
| Config API - POST /trust | Integration | Ready |
| Config API - DELETE /trust | Integration | Ready |
| Helm Chart Rendering | Unit | Ready |
| SAML SSO Flow | E2E | Manual |
| Janssen Auth Integration | E2E | Manual |
