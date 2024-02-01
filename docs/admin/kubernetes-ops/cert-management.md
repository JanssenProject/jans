---
tags:
  - administration
  - kubernetes
  - operations
  - certificate management
  - certification and key rotation
---

## Overview

Rotating Certificates and Keys in Kubernetes setup

!!! Note
    `janssen-config-cm` in all examples refer to jans installation configuration parameters where `janssen` is the `helm-release-name`.
       
    
## Web (Ingress)
        
| Associated certificates and keys |
| -------------------------------- |
| /etc/certs/web_https.crt         |
| /etc/certs/web_https.key         |

!!! Note
    During fresh installation, the config-job checks if SSL certificates and keys are mounted as files. 
    If no mounted files are found, it attempts to download SSL certificates from the FQDN supplied. If the download is successful, an empty key file is generated.
    If no mounted or downloaded files are found, it generates self-signed SSL certificates, CA certificates, and keys.

### Rotate
        
1.  Create a file named `web-key-rotation.yaml` with the following contents :
        
    ```yaml
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: web-key-rotation
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"                  
        spec:
          restartPolicy: Never
          containers:
            - name: web-key-rotation
              image: ghcr.io/janssenproject/jans/certmanager:replace-janssen-version-1
              envFrom:
              - configMapRef:
                  name: janssen-config-cm # This may be differnet in Helm
              args: ["patch", "web", "--opts", "valid-to:365"]
    ```
            
2.  Apply job        
    ```bash
    kubectl apply -f web-key-rotation.yaml -n <jans-namespace>
    ```            
        
### Load from existing source
        
!!! Note
    This will load `web_https.crt` and `web_https.key` from `/etc/certs`.
                
1. Create a secret with `web_https.crt` and `web_https.key`. Note that this may already exist in your deployment.
            
    ```bash
    kubectl create secret generic web-cert-key --from-file=web_https.crt --from-file=web_https.key -n <jans-namespace>` 
    ```
                
2.  Create a file named `load-web-key-rotation.yaml` with the following contents :
                               
    ```yaml
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: load-web-key-rotation
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"                  
        spec:
          restartPolicy: Never
          volumes:
          - name: web-cert
            secret:
              secretName: web-cert-key
              items:
                - key: web_https.crt
                  path: web_https.crt
          - name: web-key
            secret:
              secretName: web-cert-key
              items:
                - key: web_https.key
                  path: web_https.key                              
          containers:
            - name: load-web-key-rotation
              image: ghcr.io/janssenproject/jans/certmanager:replace-janssen-version-1
              envFrom:
              - configMapRef:
                  name: janssen-config-cm  #This may be differnet in Helm
              volumeMounts:
                - name: web-cert
                  mountPath: /etc/certs/web_https.crt
                  subPath: web_https.crt
                - name: web-key
                  mountPath: /etc/certs/web_https.key
                  subPath: web_https.key
              args: ["patch", "web", "--opts", "source:from-files"]
    ```
            
3.  Apply job

```bash
kubectl apply -f load-web-key-rotation.yaml -n <jans-namespace>
```            

## Auth-server
    
!!! Warning
    key rotation CronJob is usually installed with jans. Please make sure before deploying using `kubectl get cronjobs -n <jans-namespace>`

| Associated certificates and keys |
| -------------------------------- |
| /etc/certs/auth-keys.json      |
| /etc/certs/auth-keys.jks       |

1.  Create a file named `auth-key-rotation.yaml` with the following contents :

    ```yaml
    kind: CronJob
    apiVersion: batch/v1
    metadata:
      name: auth-key-rotation
    spec:
      # runs the job every 48 hours
      schedule: "@every 48h"
      concurrencyPolicy: Forbid
      jobTemplate:
        spec:
          template:
            metadata:
              annotations:
                sidecar.istio.io/inject: "false"
            spec:
              containers:
                - name: auth-key-rotation
                  image: ghcr.io/janssenproject/jans/certmanager:replace-janssen-version-1
                  resources:
                    requests:
                      memory: "300Mi"
                      cpu: "300m"
                    limits:
                      memory: "300Mi"
                      cpu: "300m"
                  envFrom:
                    - configMapRef:
                        name: janssen-config-cm
                  args: ["patch", "auth", "--opts", "interval:48", "--opts", "key-strategy:OLDER", "--opts", "privkey-push-delay:300", "--opts", "privkey-push-strategy:NEWER"]
              restartPolicy: Never
    ```

          
2.  Apply cron job

    ```bash
    kubectl apply -f auth-key-rotation.yaml -n <jans-namespace>
    ```

## LDAP

!!! Warning
    Starting from `1.0.20`, the `ghcr.io/janssenproject/jans/certmanager` image is no longer handle LDAP certificate and key rotation in favor of re-generating them manually.

| Associated certificates and keys    |
| ----------------------------------- |
| /etc/certs/opendj.crt               |
| /etc/certs/opendj.key               |   

1.  Prepare cert and key for OpenDJ, for example:

    ```
    openssl req -x509 -newkey rsa:2048 -sha256 -days 365 -nodes -keyout opendj.key -out opendj.crt -subj '/CN=demoexample.jans.io' -addext 'subjectAltName=DNS:ldap,DNS:opendj'
    ```

1.  Extract the contents of OpenDJ cert and key files as base64 string:

    ```
    OPENDJ_CERT_B64=$(base64 opendj.crt -w0)
    OPENDJ_KEY_B64=$(base64 opendj.key -w0)
    ```

1.  Add the following yaml snippet to your `override.yaml` file, for example:

    ```yaml
    global:
      cnPersistenceType: ldap
      storageClass:
        provisioner: kubernetes.io/aws-ebs
      opendj:
        enabled: true
    config:
      configmap:
        # -- contents of OpenDJ cert file in base64-string
        cnLdapCrt: <OPENDJ_CERT_B64>
        # -- contents of OpenDJ key file in base64-string
        cnLdapKey: <OPENDJ_KEY_B64>
    ```

1.  Rollout restart OpenDJ statefulset:

    ```
    kubectl -n <jans-namespace> rollout restart sts <opendj-statefulset-name>
    ```

    Wait until pods are re-deployed successfully (this will update OpenDJ certificate and key in Kubernetes secrets).
    Afterwards, do rollout restart for other deployments/statefulsets to ensure latest OpenDJ certificate and key are pulled into pods.
