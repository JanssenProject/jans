"""
pygluu.kubernetes.terminal.volumes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for volume terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

import click

from pygluu.kubernetes.helpers import get_logger

logger = get_logger("gluu-prompt-volumes")


class PromptVolumes:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_app_volume_type(self):
        """Prompts for volume type
        """
        gluu_volume_map = {
            1: "microk8sDynamic",
            2: "minikubeDynamic",
            6: "awsOpenEbsHostPathDynamic",
            7: "awsEbsDynamic",
            11: "gkeOpenEbsHostPathDynamic",
            12: "gkePdDynamic",
            16: "aksOpenEbsHostPathDynamic",
            17: "aksPdDynamic",
            21: "doksOpenEbsHostPathDynamic",
            22: "doksPdDynamic",
            26: "localOpenEbsHostPathDynamic"
        }
        vol_choice = 0
        if self.settings.get("global.storageClass.provisioner") == "kubernetes.io/aws-ebs":
            print("|------------------------------------------------------------------|")
            print("|Amazon Web Services - Elastic Kubernetes Service (Amazon EKS)     |")
            print("|                    MultiAZ - Supported                           |")
            print("|------------------------------------------------------------------|")
            print("| [6]  OpenEBS Local PV Hostpath (OpenEBS must be installed)       |")
            print("| [7]  EBS volumes dynamically provisioned [default]               |")
            vol_choice = click.prompt("What type of volume path", default=7)
        elif self.settings.get("global.storageClass.provisioner") == "kubernetes.io/gce-pd":
            print("|------------------------------------------------------------------|")
            print("|Google Cloud Engine - Google Kubernetes Engine                    |")
            print("|------------------------------------------------------------------|")
            print("| [11]  OpenEBS Local PV Hostpath (OpenEBS must be installed)      |")
            print("| [12]  Persistent Disk  dynamically provisioned [default]         |")
            vol_choice = click.prompt("What type of volume path", default=12)
        elif self.settings.get("global.storageClass.provisioner") == "kubernetes.io/azure-disk":
            print("|------------------------------------------------------------------|")
            print("|Microsoft Azure                                                   |")
            print("|------------------------------------------------------------------|")
            print("| [16] OpenEBS Local PV Hostpath (OpenEBS must be installed)       |")
            print("| [17] Persistent Disk  dynamically provisioned                    |")
            vol_choice = click.prompt("What type of volume path", default=17)
        elif self.settings.get("global.storageClass.provisioner") == "dobs.csi.digitalocean.com":
            print("|------------------------------------------------------------------|")
            print("|Digital Ocean                                                     |")
            print("|------------------------------------------------------------------|")
            print("| [21] OpenEBS Local PV Hostpath (OpenEBS must be installed)       |")
            print("| [22] Persistent Disk  dynamically provisioned                    |")
            vol_choice = click.prompt("What type of volume path", default=22)
        elif self.settings.get("global.storageClass.provisioner") == "openebs.io/local":
            print("|------------------------------------------------------------------|")
            print("|Local Deployment                                                  |")
            print("|------------------------------------------------------------------|")
            print("| [26] OpenEBS Local PV Hostpath                                   |")
            print("|------------------------------------------------------------------|")
            logger.info("OpenEBS must be installed before")
            vol_choice = click.prompt("What type of volume path", default=26)
        self.settings.set("installer-settings.volumeProvisionStrategy", gluu_volume_map.get(vol_choice))

    def prompt_storage(self):
        """Prompt for LDAP storage size
        """
        if self.settings.get("global.cnPersistenceType") in ("hybrid", "ldap") and self.settings.get(
                "opendj.persistence.size") in (None, ''):
            self.settings.set("opendj.persistence.size", click.prompt("Size of ldap volume storage", default="4Gi"))

    def prompt_volumes(self):
        """Prompts for all info needed for volume creation on cloud or onpremise
        """

        if self.settings.get("global.storageClass.provisioner") and \
                self.settings.get("installer-settings.volumeProvisionStrategy") in (None, ''):
            self.prompt_app_volume_type()

        if self.settings.get("installer-settings.volumeProvisionStrategy") == "aksPdDynamic":
            logger.info("Azure Options ('Standard_LRS', 'Premium_LRS', 'StandardSSD_LRS', 'UltraSSD_LRS')")
            self.settings.set("global.azureStorageAccountType",
                              click.prompt("Please enter the volume type.", default="StandardSSD_LRS"))

        elif self.settings.get("global.storageClass.provisioner") == "microk8s.io/hostpath":
            self.settings.set("installer-settings.volumeProvisionStrategy", "microk8sDynamic")

        elif self.settings.get("global.storageClass.provisioner") == "k8s.io/minikube-hostpath":
            self.settings.set("installer-settings.volumeProvisionStrategy", "minikubeDynamic")

        elif self.settings.get("installer-settings.volumeProvisionStrategy") == "awsEbsDynamic":
            logger.info("AWS EKS Options ('gp2', 'io1', `io2`, 'st1', 'sc1')")
            self.settings.set("global.awsStorageType",
                              click.prompt("Please enter the volume type.", default="io1"))

        elif self.settings.get("installer-settings.volumeProvisionStrategy") == "gkePdDynamic":
            logger.info("GCE GKE Options ('pd-standard', 'pd-ssd')")
            self.settings.set("global.gcePdStorageType",
                              click.prompt("Please enter the volume type.", default="pd-ssd"))

        elif "OpenEbsHostPathDynamic" in self.settings.get("installer-settings.volumeProvisionStrategy"):
            self.settings.set("global.storageClass.provisioner", "openebs.io/local")
