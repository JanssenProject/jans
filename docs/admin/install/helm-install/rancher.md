---
tags:
  - administration
  - installation
  - helm
---

# Install Janssen Server Using Rancher Marketplace

For this quick start we will use a [single node Kubernetes install in docker with a self-signed certificate](https://rancher.com/docs/rancher/v2.6/en/installation/other-installation-methods/single-node-docker/).

!!! note
    For a more generic setup, use Rancher UI to deploy the setup. For more options please follow this [link](https://rancher.com/docs/rancher/v2.6/en/installation/).


## Installation Steps

1. Provision a Linux 4 CPU, 16 GB RAM, and 50GB SSD VM with ports `443` and `80` open. Save the VM IP address. For development environments, the VM can be set up using VMWare Workstation Player or VirtualBox with Ubuntu 20.0.4 operating system running on VM.
2. Install [Docker](https://docs.docker.com/engine/install/).
3. Execute
    ```bash
    docker run -d --restart=unless-stopped -p 80:80 -p 443:443 --privileged rancher/rancher:latest
    ```
   The final line of the returned text is the `container-id`, which you'll need for the next step.
4. Execute the following command to get the [boostrap password](https://rancher.com/docs/rancher/v2.6/en/installation/resources/bootstrap-password/#specifying-the-bootstrap-password-in-docker-installs) for login.
    ```bash
    docker logs  <container-id>  2>&1 | grep "Bootstrap Password:"
    ```
5. Head to `https://<VM-IP-ADDRESS-FROM-FIRST-STEP>` and log in with the username `admin` and the password from the previous step. If you are logging into Rancher for the first time, you'll need to enter just the password, and on the next step, Rancher will ask you to reset your current password.
6. Next you'll see the Rancher home page with a list of existing clusters. By default, the name of the newly created cluster would be `local`. Click on the cluster name to go to the dashboard.
7. From the top-left menu expand `Apps & Marketplace` and click `charts`.
8. Search for `Gluu` and begin your installation.
9. During Step 1 of installation, be sure to select the `Customize Helm options before install` options.
10. In Step 2, customize the settings for the Janssen installation. Specifically `Optional Services` from where you can enable Janssen modules.
11. In Step 3, unselect the `Wait` option and start the installation.

