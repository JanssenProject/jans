---
tags:
  - administration
  - VM
  - operations
  - upgrade
---

This guide shows how to upgrade a Janssen VM deployment.

!!! Note
    VM deployments don't provide automatic upgrade/update support. 
    Uninstalling and re-installing with newer binaries is the only option that requires re-configuration of auth-server. 
    Though this is made easy using [Terraform](../terraform/README.md). 
    We recommend using Kubernetes installations over VM, to avail smooth upgrades and better HA support.

!!! Note
    Custom scripts are considered external configuration and are not updated automatically during an upgrade.
    Please review and adapt your custom scripts to work with the newer version of the Janssen Server.

Let's assume we are upgrading Jans VM installation from `current version` to `vreplace-janssen-version`

1. Keep the old VM installation running.

2. [Install](../install/vm-install/README.md) on a separate VM the target new Jans installation, i.e. `vreplace-janssen-version`.

    You can install with a test client. For example:

    `sudo python3 /opt/jans/jans-setup/setup.py -test-client-id 6382c9da-f25d-435f-ac63-6acde36f4859 -test-client-pw secret1172023`

    This `client-id` and `client-pw` will then be used to import Terraform configurations

3. Use our Terraform [docs](../terraform/README.md) on the new installation, i.e. `vreplace-janssen-version` to:
    - import all the global configurations from the new installation using `terraform import`
    - define all the custom IDP configurations and apply them using `terraform apply`
   
4. At this point there should be two versions up, `old version` and `vreplace-janssen-version`.

5. Traffic should be switched gradually from the old setup to the new setup.
   Once confidence is gained, drain the old VM.