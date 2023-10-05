---
tags:
- Casa
- administration
- installation
---

# Gluu Casa Installation Guide

## Installation

### System Requirements

Gluu Casa gets installed as a component of Gluu Flex Server. Please refer to system requirements for 
[Gluu Flex Server](../administration/installation.md#system-requirements). 

### Installation via Linux Packages 

Casa is offered as one of the several components of the Gluu Flex Server. To include Casa in your instance, just ensure 
to check it when prompted at [installation](https://gluu.org/docs/gluu-server/4.4/installation-guide/) time.

To add Casa post-install do the following:

1. Login to chroot
1. `cd /install/community-edition-setup`
1. Run `./setup.py --install-casa`

### Finish setup

**Important notes:**

- Ensure your server has "dynamic registration" of clients enabled and that "returnClientSecretOnRead" is set to *true*.
  These settings can be reverted once your Casa installation is fully operational

After installation, you can access the application at `https://<host>/casa`. 

For the first time the application will try to register an OpenID Connect client via oxd. If this operation fails due to
network problems or SSL cert issues, login will not work. Please refer to the [FAQ](./faq.md#oxd) for troubleshooting.

!!! Note 
    To change the default URL path for Casa follow the steps listed [here](change-context-path.md). It is advisable to apply this customization **before** credentials are enrolled. 

### Unlocking admin features

Recall admin capabilities are disabled by default. To unlock admin features follow these steps:

1. Navigate inside chroot to `/opt/gluu/jetty/casa/`
1. Create an empty file named `.administrable` (ie. `touch .administrable`)
1. Run `chown casa:casa .administrable` (do this only if you are on FIPS environment)
1. Logout in case you have an open browser session

!!! Warning
    Once you have configured, tailored, and tested your deployment thoroughly, you are strongly encouraged to remove the marker file. This will prevent problems in case a user can escalate privileges or if some administrative account is compromised.

<!--
### A word on security

In a clustered or containerized deployment, admin features and user features should run on different nodes. It is responsibility of the administrator to enable admin features on a specific (small) set of nodes and make those publically inaccessible, for instance, by removing them from the load balancer.
-->

## Uninstall Gluu Casa

Follow the steps below to remove Casa from your Gluu Flex Server installation:

1. Update acr: Uninstallation will remove `casa` acr and its corresponding custom script from your server.
   So, before you uninstall Casa, update the acr value if it is set to `casa`. In case you have OpenId Connect clients
   requesting this acr_value they you'll need to update their configuration. Also, check if the default authentication
   method is set to `casa`. Do this using Admin-UI <TODO>

1. Login to chroot.

1. Run the cleanup utility. It will remove configurations added to your Gluu Flex Server when Casa was installed,
   as well as any data which is no longer needed. In the chroot run:

    ```
    # cd /install/community-edition-setup/
    # ./casa_cleanup.py
    ```