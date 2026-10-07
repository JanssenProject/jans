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
              image: ghcr.io/janssenproject/jans/cloudtools:2.5.0-1
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

## Terraform users

If the deployment is managed with the [Janssen Terraform provider](../terraform/README.md), the configuration and state still hold the old FQDN after `change-fqdn` runs. A later `terraform apply` sends the old values back (e.g. `issuer` and `base_endpoint` in `jans_app_configuration`, `redirect_uris` in `jans_oidc_client`) and undoes the change.

!!! Warning
    Do not run `terraform apply` between the `change-fqdn` job and the steps below.

1.  Point the provider at the new FQDN, either `url` in the `provider "jans"` block or the `JANS_URL` environment variable.

2.  Replace the old FQDN with the new one in all `.tf` and `.tfvars` files, for example:

    ```bash
    grep -rlF demoexample.jans.io --include='*.tf' --include='*.tfvars' . | xargs sed -i 's/demoexample\.jans\.io/new-demoexample.jans.io/g'
    ```

3.  Sync the state with the values already on the server:

    ```bash
    terraform apply -refresh-only
    ```

4.  Run `terraform plan`. It should report no changes. If it still shows the old FQDN, fix the configuration files, not the server.
