---
tags:
  - administration
  - kubernetes
  - operations
  - certificate management
  - certification and key rotation
---
# Certificate Management


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
              image: ghcr.io/janssenproject/jans/cloudtools:replace-janssen-version-1
              envFrom:
              - configMapRef:
                  name: janssen-config-cm # This may be different in Helm
              args: ["certmanager", "patch", "web", "--opts", "valid-to:365"]
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
    kubectl create secret generic web-cert-key --from-file=web_https.crt --from-file=web_https.key -n <jans-namespace>
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
              image: ghcr.io/janssenproject/jans/cloudtools:replace-janssen-version-1
              envFrom:
              - configMapRef:
                  name: janssen-config-cm  #This may be different in Helm
              volumeMounts:
                - name: web-cert
                  mountPath: /etc/certs/web_https.crt
                  subPath: web_https.crt
                - name: web-key
                  mountPath: /etc/certs/web_https.key
                  subPath: web_https.key
              args: ["certmanager", "patch", "web", "--opts", "source:from-files"]
    ```

3.  Apply job

```bash
kubectl apply -f load-web-key-rotation.yaml -n <jans-namespace>
```

### Optional: update TLS secret

Ingress or Gateway API controllers may use TLS secret named `tls-certificate` in release namespace. The TLS secret need to be updated to reflect the changes.

1.  Extract TLS cert and key from Janssen:

    ```bash
    umask 077
    kubectl -n <jans-namespace> get secret cn --template={{.data.ssl_cert}} | base64 -d > tls.crt
    kubectl -n <jans-namespace> get secret cn --template={{.data.ssl_key}} | base64 -d > tls.key
    ```

1.  Update the TLS secret:

    ```bash
    kubectl -n <jans-namespace> create secret tls tls-certificate --cert=tls.crt --key=tls.key --dry-run=client -o yaml | kubectl apply -f -
    ```

1.  Remove temporary files:

    ```bash
    rm -f tls.crt tls.key
    ```

1.  Rollout restart (or re-deploy) the Ingress/Gateway API controller, for example:

    ```bash
    kubectl -n <jans-namespace> rollout restart deployment jans-gateway-nginx
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
                  image: ghcr.io/janssenproject/jans/cloudtools:replace-janssen-version-1
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
                  args: ["certmanager", "patch", "auth", "--opts", "interval:48", "--opts", "key-strategy:OLDER", "--opts", "privkey-push-delay:300", "--opts", "privkey-push-strategy:NEWER"]
              restartPolicy: Never
    ```


2.  Apply cron job

    ```bash
    kubectl apply -f auth-key-rotation.yaml -n <jans-namespace>
    ```
