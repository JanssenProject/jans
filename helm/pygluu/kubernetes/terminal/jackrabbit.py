"""
pygluu.kubernetes.terminal.jackrabbit
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for jackrabbit terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click

from pygluu.kubernetes.helpers import get_logger, prompt_password
from pygluu.kubernetes.terminal.postgres import PromptPostgres

logger = get_logger("gluu-prompt-jackrabbit")


class PromptJackrabbit:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings
        self.postgres = PromptPostgres(self.settings)

    def prompt_jackrabbit(self):
        """Prompts for Jackrabbit content repository
        """
        if self.settings.get("global.jackrabbit.enabled") in (None, ''):
            logger.info("Jackrabbit must be installed. If the following prompt is answered with N it is assumed "
                        "the jackrabbit content repository is either installed locally or remotely")
            self.settings.set("global.jackrabbit.enabled",
                              click.confirm("Install Jackrabbit content repository", default=True))

        jackrabbit_cluster_prompt = "Is"
        if self.settings.get("global.jackrabbit.enabled"):
            if self.settings.get("jackrabbit.storage.size") in (None, ''):
                self.settings.set("jackrabbit.storage.size", click.prompt(
                    "Size of Jackrabbit content repository volume storage", default="4Gi"))
            self.settings.set("config.configmap.cnJackrabbitUrl", "http://jackrabbit:8080")
            jackrabbit_cluster_prompt = "Enable"

        if self.settings.get("config.configmap.cnJackrabbitUrl") in (None, ''):
            self.settings.set("config.configmap.cnJackrabbitUrl", click.prompt("Please enter jackrabbit url.",
                                                                               default="http://jackrabbit:8080"))
        if self.settings.get("config.configmap.cnJackrabbitAdminId") in (None, ''):
            self.settings.set("config.configmap.cnJackrabbitAdminId",
                              click.prompt("Please enter Jackrabbit admin user", default="admin"))

        if self.settings.get("jackrabbit.secrets.cnJackrabbitAdminPassword") in (None, ''):
            self.settings.set("jackrabbit.secrets.cnJackrabbitAdminPassword", prompt_password("jackrabbit-admin", 24))

        if self.settings.get("installer-settings.jackrabbit.clusterMode") in (None, ''):
            self.settings.set("installer-settings.jackrabbit.clusterMode",
                              click.confirm("{} Jackrabbit in cluster mode[beta] "
                                            "Recommended in production"
                                            .format(jackrabbit_cluster_prompt), default=True))
        if self.settings.get("installer-settings.jackrabbit.clusterMode"):
            self.postgres.prompt_postgres()
            if self.settings.get("config.configmap.cnJackrabbitPostgresUser") in (None, ''):
                self.settings.set("config.configmap.cnJackrabbitPostgresUser",
                                  click.prompt("Please enter a user for jackrabbit postgres database",
                                               default="jackrabbit"))

            if self.settings.get("jackrabbit.secrets.cnJackrabbitPostgresPassword") in (None, ''):
                self.settings.set("jackrabbit.secrets.cnJackrabbitPostgresPassword",
                                  prompt_password("jackrabbit-postgres"))

            if self.settings.get("config.configmap.cnJackrabbitPostgresDatabaseName") in (None, ''):
                self.settings.set("config.configmap.cnJackrabbitPostgresDatabaseName",
                                  click.prompt("Please enter jackrabbit postgres database name",
                                               default="jackrabbit"))
