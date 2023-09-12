---
tags:
  - administration
  - kubernetes
  - operations
  - tui
---

## Overview 

[![Using the TUI with a Janssen Kubernetes Setup](https://www.loom.com/share/36688669f0aa4c22be2eaf3f711fe488?sid=ec28d4fd-3c73-45a6-ac8d-d550fbaf863f)](https://www.loom.com/share/36688669f0aa4c22be2eaf3f711fe488?sid=ec28d4fd-3c73-45a6-ac8d-d550fbaf863f "Using the TUI with a Janssen Kubernetes Setup")
   
   Installing TUI and connecting to a Kubernetes installation.
   

1.  Download jans-cli-tui from the [release](https://github.com/JanssenProject/jans/releases) assets depending on your OS. For example: 
    
    `wget https://github.com/JanssenProject/jans/releases/download/vreplace-janssen-version/jans-cli-tui-linux-ubuntu-X86-64.pyz`
    
    Now we have `jans-cli-tui-linux-ubuntu-X86-64.pyz` downloaded.


2. Now we can grab the FQDN, client-id, client-secret, and connect using the following commands:
    ```
    FQDN= #Add your FQDN here
    # We are assuming janssen is deployed in `jans` namespace
    TUI_CLIENT_ID=$(kubectl get cm cn -o json -n jans | grep '"tui_client_id":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]')
    TUI_CLIENT_SECRET=$(kubectl get secret cn -o json -n jans | grep '"tui_client_pw":' | sed -e 's#.*:\(\)#\1#' | tr -d '"' | tr -d "," | tr -d '[:space:]' | base64 -d)
    #add -noverify if your FQDN is not registered
    python3 jans-cli-tui-linux-ubuntu-X86-64.pyz --host $FQDN --client-id $TUI_CLIENT_ID --client-secret $TUI_CLIENT_SECRET
    ```
