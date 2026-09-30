---
tags:
  - administration
  - kubernetes
  - operations
  - change fqdn
---
# Changing FQDN (Fully Qualified Domain Name)


Changing FQDN in Kubernetes setup

!!! Note
    `janssen-config-cm` in all examples refer to jans installation configuration parameters where `janssen` is the `helm-release-name`.

!!! Warning
    This feature is experimental and in alpha state, mostly used for development and testing.

## Mandatory steps

1.  Create a file named `change-fqdn.yaml` with the following contents :

    ```yaml
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: change-fqdn
    spec:
      template:
        metadata:
          annotations:
            sidecar.istio.io/inject: "false"
        spec:
          restartPolicy: Never
          containers:
            - name: change-fqdn
              image: ghcr.io/janssenproject/jans/cloudtools:replace-janssen-version-1
              envFrom:
              - configMapRef:
                  name: janssen-config-cm # This may be different in Helm
              args: ["change-fqdn", "new-demoexample.jans.io", "--old-fqdn", "demoexample.jans.io"]
    ```

    The `change-fqdn` command supports options (passed into `args`) to change the behavior of the running process:

    1. `--old-fqdn`: The old FQDN that need to changed (If omitted or empty, the value will be taken from existing configmaps).
    2. `--dry-run`: Simulate the process without persisting the changes (disabled by default).

2.  Apply job:
    ```bash
    kubectl apply -f change-fqdn.yaml -n <jans-namespace>
    ```

3.  Wait for the `job/change-fqdn` become completed and no failure before proceeding to additional steps.

## Additional steps

!!! Warning
    Do not execute the following steps if dry-run mode is enabled.

1.  Replace the certificate using `certmanager`, see [Certificate Management](cert-management.md#web-ingress) for further instructions.

2.  Modify the customized `values.yaml` and change all of the occurrences of old FQDN with the new one.

3.  Upgrade the setup, for example:

    ```bash
    helm upgrade <helm-release-name> janssen-auth-server/janssen \
      --version <helm-chart-version> \
      -f values.yaml \
      -n <jans-namespace>
    ```
