---
tags:
  - administration
  - kubernetes
  - operations
  - tui
---

# TUI K8s

## Janssen TUI for Kubernetes

This guide describes how to install and use the Janssen TUI (`jans-cli-tui`) to manage a Kubernetes deployment.

### Download `jans-cli-tui`

- Download the latest release of `jans-cli-tui` for your OS from the [Janssen releases](https://github.com/JanssenProject/jans/releases/latest).  

  ```bash title="Command"
  wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-cli-tui-linux-ubuntu-X86-64.pyz
  ```

This will download the file `jans-cli-tui-linux-ubuntu-X86-64.pyz`.

### Retrieve Connection Credentials

Retrieve the fully qualified domain name (FQDN), client ID, and client secret from your Kubernetes cluster before connecting using the command below.

!!! Note
    The commands below use the default values for the FQDN (`demoexample.jans.io`) and namespace (`jans`). If you have configured different values, update the commands accordingly based on the settings in the [values.yaml](https://github.com/JanssenProject/jans/blob/main/charts/janssen/values.yaml) file.

```bash title="Command"
FQDN=demoexample.jans.io
TUI_CLIENT_ID=$(kubectl get cm cn -n jans --template={{.data.tui_client_id}})
TUI_CLIENT_SECRET=$(kubectl get secret cn -n jans --template={{.data.tui_client_pw}} | base64 -d)
```

### Connect to the TUI

First, retrieve the `admin password` required to log in to the TUI interface by running the following command.

```bash title="Command"
  kubectl get secret cn -n jans -o jsonpath="{.data.admin_password}" | base64 -d
  echo
```

Use the following command to launch the TUI client:

```bash title="Command"
python3 jans-cli-tui-linux-ubuntu-X86-64.pyz \
    --host $FQDN \
    --client-id $TUI_CLIENT_ID \
    --client-secret $TUI_CLIENT_SECRET
```

  <div>
    <a href="https://www.loom.com/share/36688669f0aa4c22be2eaf3f711fe488">
      <p>Using the TUI with a Janssen Kubernetes Setup - Watch Video</p>
    </a>
    <a href="https://www.loom.com/share/36688669f0aa4c22be2eaf3f711fe488">
      <img style="max-width:300px;" src="https://cdn.loom.com/sessions/thumbnails/36688669f0aa4c22be2eaf3f711fe488-with-play.gif">
    </a>
  </div>
</div>
