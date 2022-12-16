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

Configmap backup:
```bash
kubectl get configmap -n <namespace> --field-selector metadata.name!=kube-root-ca.crt -o yaml > configmap-backup.yaml
```

Secret backup:
```bash
kubectl get secret -n <namespace> -o yaml > secret-backup.yaml
```

Configmap restore:
```bash
kubectl create -f configmap-backup.yaml
```

Secret restore:
```bash
kubectl create -f secret-backup.yaml
```

## Automatic Backup and Restore

There are several tools that helps in automatic backups and restore, like [Kasten K10](https://www.kasten.io/kubernetes/use-cases/backup-restore).

You can follow online [guides](https://medium.com/geekculture/kubernetes-backup-restore-is-now-effortless-e788fccd8cde) to deploy it and using it to configure automatic backups.