---
tags:
  - administration
  - installation
  - helm
  - rancher
---

# Install Janssen Server Using Rancher Marketplace

For this quick start we will use a [single node Kubernetes install in docker with a self-signed certificate](https://ranchermanager.docs.rancher.com/pages-for-subheaders/rancher-on-a-single-node-with-docker).

!!! note
    For a more generic setup, use Rancher UI to deploy the setup. For more options please follow this [link](https://ranchermanager.docs.rancher.com/pages-for-subheaders/installation-and-upgrade).


## Installation Steps

!!! note
    If you are deploying an Ingress controller on a single node deployment, in which Ingress utilizes ports 80 and 443, then you have to adjust the host ports mapped for the rancher/rancher container.
    Here's an [example](https://ranchermanager.docs.rancher.com/reference-guides/single-node-rancher-in-docker/advanced-options#running-rancherrancher-and-rancherrancher-agent-on-the-same-node) on how to do that. 

1. Provision a Linux 4 CPU, 16 GB RAM, and 50GB SSD VM with ports `443` and `80` open. Save the VM IP address. For development environments, the VM can be set up using VMWare Workstation Player or VirtualBox with an Ubuntu 20.04 operating system running on a VM.
2. Install [Docker](https://docs.docker.com/engine/install/).
3. Execute
    ```bash
    docker run -d --restart=unless-stopped -p 80:80 -p 443:443 --privileged rancher/rancher:latest
    ```
   The final line of the returned text is the `container-id`, which you'll need for the next step.
4. Execute the following command to get the [bootstrap password](https://ranchermanager.docs.rancher.com/getting-started/installation-and-upgrade/resources/bootstrap-password#specifying-the-bootstrap-password-in-docker-installs) for login.
    ```bash
    docker logs  <container-id>  2>&1 | grep "Bootstrap Password:"
    ```
5. Head to `https://<VM-IP-ADDRESS-FROM-FIRST-STEP>` and log in with the username `admin` and the password from the previous step. If you are logging into Rancher for the first time, you'll need to enter just the password, and on the next step, Rancher will ask you to reset your current password.
6. Next, you'll see the Rancher home page with a list of existing clusters. By default, the name of the newly created cluster would be `local`. Click on the cluster name to go to the dashboard.

7. Add Janssen repository: From the top-left menu expand `Apps` > `Repositories` > `Create` > Add a repo name and the Index URL `https://docs.jans.io/charts` > `Create`
8. From the top-left menu expand `Apps` and click `Charts`.
9. Search for `Janssen` and begin your installation.
10. During Step 1 of installation, be sure to select the `Customize Helm options before install` option.
11. In Step 2, customize the settings for the Janssen installation.
12. In Step 3, unselect the `Wait` option and start the installation.