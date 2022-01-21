"""
pygluu.kubernetes.terminal.helm
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for helm terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click


class PromptHelm:

    def __init__(self, settings):
        self.settings = settings

    def prompt_helm(self):
        """Prompts for helm installation and returns updated settings.

        :return:
        """
        if self.settings.get("installer-settings.releaseName") in ("None", ''):
            self.settings.set("installer-settings.releaseName",
                              click.prompt("Please enter Gluu helm name", default="gluu"))

        # ALPHA-FEATURE: Multi cluster ldap replication
        if self.settings.get("global.cnPersistenceType") in ("hybrid", "ldap") and \
                not self.settings.get("opendj.multiCluster.enabled"):
            self.settings.set("opendj.multiCluster.enabled",
                              click.confirm("Are you setting up a multi kubernetes cluster"))

        if self.settings.get("opendj.multiCluster.enabled"):

            if self.settings.get("opendj.multiCluster.serfAdvertiseAddrSuffix") in (None, ''):
                self.settings.set("opendj.multiCluster.serfAdvertiseAddrSuffix",
                                  click.prompt("Please enter Serf advertise "
                                               "address suffix. You must be able to "
                                               "resolve this address in your DNS",
                                               default="regional.gluu.org"))

            if self.settings.get("opendj.multiCluster.replicaCount") in (None, ''):
                self.settings.set("opendj.multiCluster.replicaCount",
                                  int(click.prompt("Enter the number of opendj statefulsets to create."
                                                   " Each will have an advertise address of"
                                                   " RELEASE-NAME-opendj-regional-"
                                                   "{{statefulset number}}-{Serf address suffix }} ", default="1",
                                                   type=click.Choice(["1", "2", "3", "4", "5", "6", "7", "8", "9"]))))

            if self.settings.get("installer-settings.ldap.subsequentCluster") in (None, ''):
                self.settings.set("installer-settings.ldap.subsequentCluster",
                                  click.confirm("Is this a subsequent kubernetes cluster "
                                                "( 2nd and above)"))

            if not self.settings.get("opendj.multiCluster.clusterId"):
                self.settings.set("opendj.multiCluster.clusterId",
                                  click.prompt("Please enter a cluster ID that distinguishes "
                                               "this cluster from any subsequent clusters. i.e "
                                               "west, east, north, south, test..", default="test"))

            if self.settings.get("opendj.multiCluster.namespaceIntId") in (None, ''):
                self.settings.set("opendj.multiCluster.namespaceIntId",
                                  int(click.prompt("Namespace int id. This id needs to be a unique number 0-9 per gluu "
                                                   "installation per namespace. Used when gluu is installed in the "
                                                   "same kubernetes cluster more than once.", default="0",
                                                   type=click.Choice(["0", "1", "2", "3",
                                                                      "4", "5", "6", "7", "8", "9"]))))

            if not self.settings.get("installer-settings.ldap.multiClusterIds") or \
                    not isinstance(self.settings.get("installer-settings.ldap.multiClusterIds"), list):
                temp = click.prompt("Please enter the cluster IDs for all other subsequent "
                                    "clusters i.e west, east, north, south, test..seperated by a comma with "
                                    "no quotes , or brackets "
                                    "Forexample, if there was three other clusters ( not including this one)"
                                    " that Gluu will be installed three cluster IDs will be needed. "
                                    "This is to help generate the serf addresses automatically.",
                                    default="dev,stage,prod")
                temp = temp.replace(" ", "")
                temp_array = temp.split(",")
                self.settings.set("installer-settings.ldap.multiClusterIds", list(temp_array))

            if self.settings.get("opendj.multiCluster.serfPeers") in (None, '') or \
                    not isinstance(self.settings.get("opendj.multiCluster.serfPeers"), list):
                alist = []
                # temp list to hold all cluster ids including the id of the cluster Gluu is being installed on
                cluster_ids = self.settings.get("installer-settings.ldap.multiClusterIds")
                if self.settings.get("installer-settings.ldap.clusterId") not in cluster_ids:
                    cluster_ids.append(self.settings.get("installer-settings.ldap.clusterId"))
                for i in range(self.settings.get("installer-settings.ldap.multiClusterIds")):
                    for cluster_id in cluster_ids:
                        alist.append(f'{self.settings.get("installer-settings.releaseName")}'
                                     f'-opendj-{cluster_id}-regional-{i}-'
                                     f'{self.settings.get("opendj.multiCluster.serfAdvertiseAddrSuffix")}:3094{i}')
                self.settings.set("opendj.multiCluster.serfAdvertiseAddrSuffix", alist)

        if self.settings.get("installer-settings.nginxIngress.releaseName") in (None, '') and \
                self.settings.get("installer-settings.aws.lbType") != "alb":
            self.settings.set("installer-settings.nginxIngress.releaseName",
                              click.prompt("Please enter nginx-ingress helm name",
                                           default="ningress"))

        if self.settings.get("installer-settings.nginxIngress.namespace") in (None, '') and self.settings.get(
                "installer-settings.aws.lbType") != "alb":
            self.settings.set("installer-settings.nginxIngress.namespace",
                              click.prompt("Please enter nginx-ingress helm namespace",
                                           default="ingress-nginx"))
