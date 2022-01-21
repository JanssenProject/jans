"""
pygluu.kubernetes.terminal.couchbase
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for couchbase terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

from pathlib import Path
import shutil
import base64

import click
from pygluu.kubernetes.helpers import get_logger, prompt_password
from pygluu.kubernetes.terminal.backup import PromptBackup
from pygluu.kubernetes.terminal.architecture import PromptArch
from pygluu.kubernetes.terminal.helpers import gather_ip
from pygluu.kubernetes.terminal.namespace import PromptNamespace

logger = get_logger("gluu-prompt-couchbase")


class PromptCouchbase:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings
        self.backup = PromptBackup(self.settings)
        self.arch = PromptArch(self.settings)
        self.namespace = PromptNamespace(self.settings)

    def prompt_couchbase(self):
        self.arch.prompt_arch()
        self.namespace.prompt_gluu_namespace()

        if self.settings.get("global.storageClass.provisioner") \
                not in ("microk8s.io/hostpath", "k8s.io/minikube-hostpath"):
            self.backup.prompt_backup()

        if self.settings.get("global.lbIp") in (None, ''):
            ip = gather_ip
            self.settings.set("global.lbIp", ip)

        if self.settings.get("installer-settings.couchbase.install") in (None, ''):
            logger.info("For the following prompt  if placed [N] the couchbase is assumed to be"
                        " installed or remotely provisioned")
            self.settings.set("installer-settings.couchbase.install", click.confirm("Install Couchbase",
                                                                                    default=True))

        if not self.settings.get("installer-settings.couchbase.install"):
            if self.settings.get("config.configmap.cnCouchbaseCrt") in (None, ''):
                print("Place the Couchbase certificate authority certificate in a file called couchbase.crt at "
                      "the same location as the installation script.")
                print("This can also be found in your couchbase UI Security > Root Certificate")
                _ = input("Hit 'enter' or 'return' when ready.")
                with open(Path("./couchbase.crt")) as content_file:
                    ca_crt = content_file.read()
                    encoded_ca_crt_bytes = base64.b64encode(ca_crt.encode("utf-8"))
                    encoded_ca_crt_string = str(encoded_ca_crt_bytes, "utf-8")
                self.settings.set("config.configmap.cnCouchbaseCrt", encoded_ca_crt_string)
        else:
            self.settings.set("config.configmap.cnCouchbaseCrt", "")

        self.prompt_override_couchbase_files()

        if self.settings.get("global.storageClass.provisioner") \
                in ("microk8s.io/hostpath", "k8s.io/minikube-hostpath"):
            self.settings.set("installer-settings.couchbase.lowResourceInstall", True)

        if self.settings.get("installer-settings.couchbase.lowResourceInstall") in (None, ''):
            self.settings.set("installer-settings.couchbase.lowResourceInstall", click.confirm(
                "Setup CB nodes using low resources for demo purposes"))

        if not self.settings.get("installer-settings.couchbase.lowResourceInstall") and \
                not self.settings.get("installer-settings.couchbase.customFileOverride") and \
                self.settings.get("installer-settings.couchbase.install"):
            self.prompt_couchbase_yaml()

        if self.settings.get("installer-settings.couchbase.namespace") in (None, ''):
            self.settings.set("installer-settings.couchbase.namespace",
                              click.prompt("Please enter a namespace for CB objects.", default="cbns"))

        if self.settings.get("installer-settings.couchbase.clusterName") in (None, ''):
            self.settings.set("installer-settings.couchbase.clusterName",
                              click.prompt("Please enter a cluster name.", default="cbgluu"))

        if self.settings.get("config.configmap.cnCouchbaseUrl") in (None, ''):
            self.settings.set("config.configmap.cnCouchbaseUrl", click.prompt(
                "Please enter  couchbase (remote or local) URL base name",
                default=f"{self.settings.get('installer-settings.couchbase.clusterName')}."
                        f"{self.settings.get('installer-settings.couchbase.namespace')}.svc.cluster.local",
            ))

        if self.settings.get("config.configmap.cnCouchbaseBucketPrefix") in (None, ''):
            self.settings.set("config.configmap.cnCouchbaseBucketPrefix", click.prompt(
                "Please enter a  prefix name for all couchbase gluu buckets",
                default="gluu"
            ))

        if self.settings.get("config.configmap.cnCouchbaseIndexNumReplica") in (None, ''):
            self.settings.set("config.configmap.cnCouchbaseIndexNumReplica", click.prompt(
                "Please enter the number of replicas per index created. "
                "Please note that the number of index nodes must be one greater than the number of replicas. "
                "That means if your couchbase cluster only has 2 "
                "index nodes you cannot place the number of replicas to be higher than 1.",
                default="0",
            ))

        if self.settings.get("config.configmap.cnCouchbaseSuperUser") in (None, ''):
            self.settings.set("config.configmap.cnCouchbaseSuperUser",
                              click.prompt("Please enter couchbase superuser username.", default="admin"))

        if self.settings.get("config.configmap.cnCouchbaseSuperUserPassword") in (None, ''):
            self.settings.set("config.configmap.cnCouchbaseSuperUserPassword", prompt_password("Couchbase superuser"))

        if self.settings.get("config.configmap.cnCouchbaseUser") in (None, ''):
            self.settings.set("config.configmap.cnCouchbaseUser",
                              click.prompt("Please enter gluu couchbase username.", default="gluu"))

        if self.settings.get("config.configmap.cnCouchbasePassword") in (None, ''):
            self.settings.set("config.configmap.cnCouchbasePassword", prompt_password("Couchbase Gluu user"))

        self.find_couchbase_certs_or_set_san_cn()

    def prompt_override_couchbase_files(self):
        if self.settings.get("installer-settings.couchbase.customFileOverride") in (None, ''):
            self.settings.set("installer-settings.couchbase.customFileOverride", click.confirm(
                "Override couchbase-cluster.yaml with a custom couchbase-cluster.yaml",
            ))

        if self.settings.get("installer-settings.couchbase.customFileOverride"):
            try:
                shutil.copy(Path("./couchbase-cluster.yaml"), Path("./couchbase/couchbase-cluster.yaml"))
                shutil.copy(Path("./couchbase-buckets.yaml"), Path("./couchbase/couchbase-buckets.yaml"))
                shutil.copy(Path("./couchbase-ephemeral-buckets.yaml"),
                            Path("./couchbase/couchbase-ephemeral-buckets.yaml"))

            except FileNotFoundError:
                logger.error("An override option has been chosen but there is a missing couchbase file that "
                             "could not be found at the current path. Please place the override files under the name"
                             " couchbase-cluster.yaml, couchbase-buckets.yaml, and couchbase-ephemeral-buckets.yaml"
                             " in the same directory pygluu-kubernetes.pyz exists ")
                raise SystemExit(1)

    def find_couchbase_certs_or_set_san_cn(self):
        """Finds couchbase certs inside couchbase_crts-keys folder and if not existent sets couchbase SAN and prompts
        for couchbase common name.
        """
        custom_cb_ca_crt = Path("./couchbase_crts_keys/ca.crt")
        custom_cb_crt = Path("./couchbase_crts_keys/chain.pem")
        custom_cb_key = Path("./couchbase_crts_keys/pkey.key")
        if not custom_cb_ca_crt.exists() or not custom_cb_crt.exists() and not custom_cb_key.exists():
            if self.settings.get('installer-settings.couchbase.subjectAlternativeName') in (None, ''):
                self.settings.set('installer-settings.couchbase.subjectAlternativeName', [
                    "*.{}".format(self.settings.get("installer-settings.couchbase.clusterName")),
                    "*.{}.{}".format(self.settings.get("installer-settings.couchbase.clusterName"),
                                     self.settings.get("installer-settings.couchbase.namespace")),
                    "*.{}.{}.svc".format(self.settings.get("installer-settings.couchbase.clusterName"),
                                         self.settings.get("installer-settings.couchbase.namespace")),
                    "*.{}.{}.svc.cluster.local".format(self.settings.get("installer-settings.couchbase.clusterName"),
                                                       self.settings.get("installer-settings.couchbase.namespace")),
                    "{}-srv".format(self.settings.get("installer-settings.couchbase.clusterName")),
                    "{}-srv.{}".format(self.settings.get("installer-settings.couchbase.clusterName"),
                                       self.settings.get("installer-settings.couchbase.namespace")),
                    "{}-srv.{}.svc".format(self.settings.get("installer-settings.couchbase.clusterName"),
                                           self.settings.get("installer-settings.couchbase.namespace")),
                    "*.{}-srv.{}.svc.cluster.local".format(
                        self.settings.get("installer-settings.couchbase.clusterName"),
                        self.settings.get("installer-settings.couchbase.namespace")),
                    "localhost"
                ])
            if self.settings.get("installer-settings.couchbase.commonName") in (None, ''):
                self.settings.set("installer-settings.couchbase.commonName",
                                  click.prompt("Enter Couchbase certificate common name.", default="Couchbase CA"))

    def prompt_couchbase_yaml(self):
        """
        Used to generate couchbase cluster yaml
        """
        if not self.settings.get('installer-settings.couchbase.totalNumberOfExpectedUsers'):
            self.settings.set('installer-settings.couchbase.totalNumberOfExpectedUsers',
                              click.prompt("Please enter the number of expected users", default="1000000"))

        if not self.settings.get('installer-settings.couchbase.totalNumberOfExpectedTransactionsPerSec'):
            self.settings.set('installer-settings.couchbase.totalNumberOfExpectedTransactionsPerSec',
                              click.prompt("Expected transactions per second [alpha]",
                                           default=2000))

        if not self.settings.get('installer-settings.couchbase.volumeType'):
            logger.info("GCE GKE Options ('pd-standard', 'pd-ssd')")
            logger.info("AWS EKS Options ('gp2', 'io1', 'st1', 'sc1')")
            logger.info("Azure Options ('Standard_LRS', 'Premium_LRS', 'StandardSSD_LRS', 'UltraSSD_LRS')")
            self.settings.set('installer-settings.couchbase.volumeType', click.prompt("Please enter the volume type.",
                                                                                      default="io1"))
