[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4353/badge)](https://bestpractices.coreinfrastructure.org/projects/4353)

## Welcome to the Janssen Project

Janssen enables organizations to build a scalable centralized authentication and authorization service using free open source software. The components of the project include client and server implementations of the OAuth, OpenID Connect, SCIM and FIDO standards. New digital identity components may be added as the technology evolves.

## Project Goal

Using the Janssen Project components, you can build a world class cloud native identity and access management ("IAM") platform. But why should you?
1. You have economies of scale, and outsourcing this critical infrastructure to a third party does not make sense
1. You need to embed this component in your product or solution
1. The privacy or security profile of a hosted solution is not acceptable
1. You need more freedom to customize

Through the Janssen project, we can coalesce a community. Open source development results in more innovation and better code. And ultimately, more trust in the code--*trust* is foundational to digital identity infrastructure.

## Project Structure

Janssen is a Linux Foundation project, governed according to the [charter](./community/charter.md). Technical oversight of the project is the responsibility of the Technical Steering Committee ("TSC"). Day to day decision making is in the hands of the Contributors. The TSC helps to guide the direction of the project and to improve the quality and security of the development process.

## Quick Start

Try first, ask questions later? Here's how to deploy Janssen

### System Requirements for cloud deployments

Note:  
For local deployments like `minikube` and `microk8s` or cloud installations in demo mode, resources may be set to the minimum and hence can have `8GB RAM`, `4 CPU`, and `50GB disk` in total to run all services.

Releases of images are in style 1.0.0-beta.0, 1.0.0-0

Please calculate the minimum required resources as per services deployed. The following table contains default recommended resources to start with. Depending on the use of each service the resources may be increased or decreased.

| Service           | CPU Unit | RAM   | Disk Space | Processor Type | Required                           |
| ----------------- | -------- | ----- | ---------- | -------------- | ---------------------------------- |
| Auth server       | 2.5      | 2.5GB | N/A        | 64 Bit         | Yes                                |
| LDAP (OpenDJ)     | 1.5      | 2GB   | 10GB       | 64 Bit         | Only if couchbase is not installed |
| fido2             | 0.5      | 0.5GB | N/A        | 64 Bit         | No                                 |
| scim              | 1.0      | 1.0GB | N/A        | 64 Bit         | No                                 |
| config - job      | 0.5      | 0.5GB | N/A        | 64 Bit         | Yes on fresh installs              |
| persistence - job | 0.5      | 0.5GB | N/A        | 64 Bit         | Yes on fresh installs              |
| client-api        | 1        | 0.4GB | N/A        | 64 Bit         | No                                 |
| nginx             | 1        | 1GB   | N/A        | 64 Bit         | Yes if not ALB                     |
| auth-key-rotation | 0.3      | 0.3GB | N/A        | 64 Bit         | No [Strongly recommended]          |
| config-api        | 0.5      | 0.5GB | N/A        | 64 Bit         | No                                 |

### Quickstart Janssen with Rancher

For a more generic setup you may use Rancher UI to deploy the setup. For this quick start we will use a [single node kubernetes install in docker with a self-signed certificate](https://rancher.com/docs/rancher/v2.6/en/installation/other-installation-methods/single-node-docker/). For more options please follow this [link](https://rancher.com/docs/rancher/v2.6/en/installation/).

Summary of steps :

1. Provision a linux 4 CPU, 16 GB RAM, and 50GB SSD VM with ports `443` and `80` open. Save the VM IP address. For development environments, the VM can be set up using VMWare Workstation Player or VirtualBox with Ubuntu 20.0.4 operating system running on VM.
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
5. Head to `https://<VM-IP-ADDRESS-FROM-FIRST-STEP>` and login with the username `admin` and the password from the previous step.
6. After logging in from the top-left menu select `Apps & Marketplace` and you will be taken to the Charts page.
7. Search for `Gluu` and begin your installation.
8. During Step 1 of installation, be sure to select the `Customize Helm options before install` options.
9. In Step 2, customize the settings for the Janssen installation
10. In Step 3, unselect the `Wait` option

### Quickstart Janssen with microk8s

Start a fresh ubuntu `18.04` or `20.04` 4 CPU, 16 GB RAM, and 50GB SSD VM with ports `443` and `80` open. Then execute the following

```bash
sudo su -
wget https://raw.githubusercontent.com/JanssenProject/jans/main/automation/startjanssendemo.sh && chmod u+x startjanssendemo.sh && ./startjanssendemo.sh
```

This will install docker, microk8s, helm and Janssen with the default settings that can be found inside [values.yaml](https://github.com/GluuFederation/flex/blob/flex/pygluu/kubernetes/templates/helm/gluu/values.yaml).  The installer will automatically add a record to your hosts record in the VM but if you want access the endpoints outside the VM you must  map the `ip` of the instance running ubuntu to the FQDN you provided and then access the endpoints at your browser such in the example in the table below.

| Service     | Example endpoint                                              |
| ----------- |---------------------------------------------------------------|
| Auth server | `https://FQDN/.well-known/openid-configuration`                 |
| fido2       | `https://FQDN/.well-known/fido2-configuration` |
| scim        | `https://FQDN/.well-known/scim-configuration`  |

## Design Goals

The Janssen Project is aligned with the goals of cloud native infrastructure to enable:

1. High Concurrency: For digital identity infrastructure, the number of users is not necessarily related to performance. If you have a billion users who never login, you can do this with a monolithic platform. Concurrency is hard. Janssen is designed to scale horizontally--enabling hypothetically any concurrency by adding more compute and memory.

2. Highly Available: Digital identity infrastructure is mission critical. For many applications, if you can't login, you're dead in the water. Robustness is a fundamental consideration.

3. Flexible while Upgradable: Open source gives you the freedom to modify the code. But having your own fork of the code might make it hard to upgrade--you'll have to merge changes. Janssen provides standard interfaces that make it possible to implement custom business logic in an upgrade-friendly manner.

## History

The initial code was ported by [Gluu](https://gluu.org), based on version 4.2 of it's identity and access management (IAM) platform. Gluu launched in 2009 with the goal of creating an enterprise-grade open source distribution of IAM components. In 2012, Gluu started work on an OAuth Authorization Server to implement OpenID Connect, which they saw as a promising next-generation replacement for SAML. This project was called [oxAuth](https://github.com/GluuFederation/oxauth), and over time, became the core component of the Gluu Server.  Gluu has submitted many [self-certifications](https://openid.net/certification/) at the OpenID Foundation. Today, it is  one of the most comprehensive OpenID Connect Providers.

In 2020, Gluu decided to democratize the governance of the oxAuth project by moving it to the Linux Foundation. The name of the project was changed from oxAuth to Janssen, to avoid any potential trademark issues. Gluu felt that a collaboration with the Linux Foundation would help to build a larger ecosystem.

## Why the name Janssen?

Pigeons (or doves if you like...) are universally regarded as a symbol of peace. But they are also fast. Powered by a handful of seeds, a well trained racing pigeon can fly 1000 kilometers in a day. The Janssen brothers of Arendonk in Belgium bred the world's fastest family of racing pigeons. Complex open source infrastructure, like competitive animal husbandry, requires incremental improvement. Janssen racing pigeons revolutionized the sport. The Janssen Project seeks to revolutionize identity and access management.

## Support

[Docs](https://janssenproject.github.io/docs/) are a work in progress. You may want to also check Gluu Server [docs](https://gluu.org/docs), which have a lot in common with Janssen.

We have setup a [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). You can register for free their with your Github identity.

You can subscribe to the [Janssen Google Group](https://groups.google.com/u/2/g/janssen_project)
and post messages there.

If you find a bug in a Janssen project, or you would like to suggest a new feature, try the chat first. Then raise an issue on the respective repository. If you have a "howto" or "usage" question, [raise the issue](https://github.com/JanssenProject/jans/issues)! 

