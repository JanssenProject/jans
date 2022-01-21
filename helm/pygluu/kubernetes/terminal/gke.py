"""
pygluu.kubernetes.terminal.gke
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for gke terminal prompt.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click
from pygluu.kubernetes.helpers import exec_cmd


class PromptGke:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_gke(self):
        """GKE prompts
        """
        if not self.settings.get("GMAIL_ACCOUNT"):
            self.settings.set("GMAIL_ACCOUNT", click.prompt("Please enter valid email for Google Cloud account"))

        if self.settings.get("APP_VOLUME_TYPE") == 11:
            for node_name in self.settings.get("NODES_NAMES"):
                for zone in self.settings.get("NODES_ZONES"):
                    response, error, retcode = exec_cmd("gcloud compute ssh user@{} --zone={} "
                                                        "--command='echo $HOME'".format(node_name, zone))
                    self.settings.set("GOOGLE_NODE_HOME_DIR", str(response, "utf-8"))
                    if self.settings.get("GOOGLE_NODE_HOME_DIR"):
                        break
                if self.settings.get("GOOGLE_NODE_HOME_DIR"):
                    break
