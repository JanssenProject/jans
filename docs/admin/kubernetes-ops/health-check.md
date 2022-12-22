---
tags:
  - administration
  - kubernetes
  - operations
  - health checks
---

## Overview

Health checks are used to determine if a container is working as it should or not. This is done in Kubernetes using probes.

Jans deployed components uses two types of probes:

1.  Readiness probes: used to know when a container is ready to start accepting traffic

2.  Liveness probes: used to know when to restart a container


## Jans Liveness and Readiness probes 

Here is a list of the liveness and readiness probes of the deployed jans components

### Opendj 

Opendj uses [healthckeck.py](https://github.com/GluuFederation/docker-opendj/blob/master/scripts/healthcheck.py) in liveness probe.
This python script connects to opendj to test its liveness.

```yaml
  livenessProbe:
    # Executes the python3 healthcheck.
    exec:
      command:
      - python3
      - /app/scripts/healthcheck.py
    # Configure the liveness healthcheck for the OpenDJ if needed.
    initialDelaySeconds: 30
    periodSeconds: 30
    timeoutSeconds: 5
    failureThreshold: 20
  readinessProbe:
    tcpSocket:
      port: 1636
    # Configure the readiness healthcheck for the OpenDJ if needed.  
    initialDelaySeconds: 60
    timeoutSeconds: 5
    periodSeconds: 25
    failureThreshold: 20
```
### auth-server

Auth-sever executes the python3 [healthckeck.py](https://github.com/JanssenProject/jans/blob/main/docker-jans-auth-server/scripts/healthcheck.py) in liveness and readiness probes.
This python scripts parses the healthcheck endpoint to make sure the status is up.

```yaml
  livenessProbe:
  # Executes the python3 healthcheck.
    exec:
      command:
        - python3
        - /app/scripts/healthcheck.py
    # Configure the liveness healthcheck for the auth-server if needed.    
    initialDelaySeconds: 30
    periodSeconds: 30
    timeoutSeconds: 5
  readinessProbe:
  # Executes the python3 healthcheck.
    exec:
      command:
        - python3
        - /app/scripts/healthcheck.py
    # Configure the readiness healthcheck for the auth-server if needed.    
    initialDelaySeconds: 25
    periodSeconds: 25
    timeoutSeconds: 5
```
### config-api

The health check of liveness and readiness probes is a HTTP GET request against a config-api endpoint

```yaml
  livenessProbe:
    # http liveness probe endpoint
    httpGet:
      path: /jans-config-api/api/v1/health/live
      port: 8074
    # Configure the liveness healthcheck for the config-api if needed.
    initialDelaySeconds: 30
    periodSeconds: 30
    timeoutSeconds: 5
  readinessProbe:
    # http readiness probe endpoint
    httpGet:
      path: jans-config-api/api/v1/health/ready
      port: 8074
    # Configure the readiness healthcheck for the config-api if needed.
    initialDelaySeconds: 25
    periodSeconds: 25
    timeoutSeconds: 5
```
### fido2

The health check of liveness and readiness probes is a HTTP GET request against a fido2 endpoint

```yaml
  livenessProbe:
    # http liveness probe endpoint
    httpGet:
      path: /jans-fido2/sys/health-check
      port: http-fido2
    # Configure the liveness healthcheck for the fido2 if needed.
    initialDelaySeconds: 25
    periodSeconds: 25
    timeoutSeconds: 5
  readinessProbe:
    # http readiness probe endpoint
    httpGet:
      path: /jans-fido2/sys/health-check
      port: http-fido2
    # Configure the readiness healthcheck for the fido2 if needed.
    initialDelaySeconds: 30
    periodSeconds: 30
    timeoutSeconds: 5
```

### scim

The health check of liveness and readiness probes is a HTTP GET request against a scim endpoint

```yaml
  livenessProbe:
    httpGet:
      # http liveness probe endpoint
      path: /jans-scim/sys/health-check
      port: 8080
    # Configure the liveness healthcheck for the SCIM if needed.  
    initialDelaySeconds: 30
    periodSeconds: 30
    timeoutSeconds: 5
  readinessProbe:
    httpGet:
      # http readiness probe endpoint
      path: /jans-scim/sys/health-check
      port: 8080
    # Configure the readiness healthcheck for the SCIM if needed.  
    initialDelaySeconds: 25
    periodSeconds: 25
    timeoutSeconds: 5
```
