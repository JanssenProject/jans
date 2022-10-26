---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

# Janssen CLI

`jans-cli` module is a command line interface for configuring the Janssen software, providing both interactive and simple single line options for configuration. This module interacts with Janssen server via RESTful configuration APIs that server exposes. Using these REST APIs, `jans-cli` allows configuration of authorization server and its major modules for FIDO2, SCIM, OpenID Connect etc. 
</br>
</br>

<p align="center">
 <img src="../../../assets/image-using-jans-cli-comp-04222022.png">
</p>

`jans-cli` offers two modes in which it can be used. Command-line and interactive mode. 

- **Command-line mode**: With command-line mode you can run a single command with all the required inputs to perform the operation you want. A sample command-line may look like below:

  ```
  /opt/jans/jans-cli/config-cli.py --operation-id get-attributes --endpoint-args limit:5
  ```

- **Interactive mode**: Interactive mode is a terminal based menu-driven mode where user can select intended action from available options and also provide input parameters required for that action.

<p align="center">
  <img src="../../../assets/gif-jans-cli-interactive-mode-04232022.gif" width="850" height="641" />
</p>
