The Jans Server should be backed up frequently, **we recommend once daily**.

There are multiple methods for backing up jans server. One way is manually using imperative commands. The other is automatically using open source tools.

## Manual Backup and Restore

### Manual Backup

1. Configmap backup:

   ```
   kubectl get configmap cn -n <namespace> -o yaml > configmap-backup.yaml
   ```

1. Secret backup:

   ```
   kubectl get secret cn -n <namespace> -o yaml > secret-backup.yaml
   ```

1. Get the user supplied values:

   Save the values.yaml that was used in the initial jans installation using helm.

   In the event that the user supplied or override values yaml was lost, you can obtain it by executing the following command:

   ```
   helm get values <release name> -n <namespace>
   ```

1. Keep note of installed chart version:

   ```
   helm list -n <namespace>
   ```

Keep note of the chart version. For example: `replace-janssen-version`

### Manual Restore

1. Create namespace

   ```
   kubectl create namespace <namespace>
   ```

1. Configmap restore:

   ```
   kubectl create -f configmap-backup.yaml
   ```

1. Secret restore:

   ```
   kubectl create -f secret-backup.yaml
   ```

1. Insall jans using the override or user supplied values with the same chart version:

```
helm install <release-name> janssen/janssen -f values.yaml --version=<replace-janssen-version> -n <namespace>
```

## Automatic Backup and Restore

There are several tools that helps in automatic backups and restore, like [Kasten K10](https://www.kasten.io/kubernetes/use-cases/backup-restore).

You can follow online [guides](https://medium.com/geekculture/kubernetes-backup-restore-is-now-effortless-e788fccd8cde) to deploy it and use it to configure automatic backups.
