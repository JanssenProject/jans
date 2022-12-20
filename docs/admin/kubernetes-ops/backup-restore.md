---
tags:
  - administration
  - kubernetes
  - operations
  - backup
  - restore
---

The Jans Server should be backed up frequently, **we recommend once daily**.

There are multiple methods for backing up jans server. One way is manually using imperative commands. The other is automatically using open source tools.

## Manual Backup and Restore

### Manual Backup
1.  Configmap backup:
```bash
kubectl get configmap cn -n <namespace> -o yaml > configmap-backup.yaml
```

2.  Secret backup:
```bash
kubectl get secret cn -n <namespace> -o yaml > secret-backup.yaml
```

3.  yaml configuration backup:

Save the values.yaml that was used in the initial jans installation using helm

4.  Keep note of installed chart version:
```bash
helm show chart janssen/janssen | grep version: | awk '{print $2}' |  head -1
```


### Manual Restore

1.  Create namespace
```bash
kubectl create namespace <namespace>
```

2.  Configmap restore:
```bash
kubectl create -f configmap-backup.yaml
```

3.  Secret restore:
```bash
kubectl create -f secret-backup.yaml
```

4.  Install jans using values.yaml with the same chart version:

```bash
helm install <release-name> janssen/janssen -f values.yaml --version=<backup-chart-verion> -n <namespace>
```

## Automatic Backup and Restore

There are several tools that helps in automatic backups and restore, like [Kasten K10](https://www.kasten.io/kubernetes/use-cases/backup-restore).

You can follow online [guides](https://medium.com/geekculture/kubernetes-backup-restore-is-now-effortless-e788fccd8cde) to deploy it and using it to configure automatic backups.