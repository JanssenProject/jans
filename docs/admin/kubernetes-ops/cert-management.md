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
              image: janssenproject/certmanager:1.0.4-1
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
              image: janssenproject/certmanager:1.0.4-1
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
      schedule: "0 */48 * * *"
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
                  image: janssenproject/certmanager:1.0.4-1
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
    
!!! Note
    Subject Alt Name must match opendj service.
    
| Associated certificates and keys    |
| ----------------------------------- |
| /etc/certs/opendj.crt               |
| /etc/certs/opendj.key               |   
| /etc/certs/opendj.pem               |   
| /etc/certs/opendj.pkcs12            |   

1.  Create a file named `ldap-key-rotation.yaml` with the following contents :

    ```yaml
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: ldap-key-rotation
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"              
        spec:
          restartPolicy: Never
          containers:
            - name: ldap-key-rotation
              image: janssenproject/certmanager:1.0.4-1
              envFrom:
              - configMapRef:
                  name: janssen-config-cm
              args: ["patch", "ldap", "--opts", "subj-alt-name:opendj", "--opts", "valid-to:365"] 
    ```

2.  Apply job

    ```bash
    kubectl apply -f ldap-key-rotation.yaml -n <jans-namespace>
    ``` 