---
tags:
  - administration
  - kubernetes
  - operations
  - tui
---

# TUI K8s

## Overview 


  <div>
    <a href="https://www.loom.com/share/36688669f0aa4c22be2eaf3f711fe488">
      <p>Using the TUI with a Janssen Kubernetes Setup - Watch Video</p>
    </a>
    <a href="https://www.loom.com/share/36688669f0aa4c22be2eaf3f711fe488">
      <img style="max-width:300px;" src="https://cdn.loom.com/sessions/thumbnails/36688669f0aa4c22be2eaf3f711fe488-with-play.gif">
    </a>
  </div>
   
   Installing TUI and connecting to a Kubernetes installation.
   

1.  Download jans-cli-tui from the [release](https://github.com/JanssenProject/jans/releases) assets depending on your OS. For example: 
    
    `wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-cli-tui-linux-ubuntu-X86-64.pyz`
    
    Now we have `jans-cli-tui-linux-ubuntu-X86-64.pyz` downloaded.


2. Now we can grab the FQDN, client-id, client-secret, and connect using the following commands:
    ```
    FQDN= #Add your FQDN here
    TUI_CLIENT_ID=$(kubectl get cm cn -n <namespace> --template={{.data.tui_client_id}})
    TUI_CLIENT_SECRET=$(kubectl get secret cn -n <namespace> --template={{.data.tui_client_pw}} | base64 -d)
    #add -noverify if your FQDN is not registered
    python3 jans-cli-tui-linux-ubuntu-X86-64.pyz --host $FQDN --client-id $TUI_CLIENT_ID --client-secret $TUI_CLIENT_SECRET
    ```
