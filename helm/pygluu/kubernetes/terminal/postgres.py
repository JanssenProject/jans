"""
pygluu.kubernetes.terminal.postgres
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for postgres terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

import click


class PromptPostgres:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_postgres(self):
        """Prompts for Postgres.
        """
        if not self.settings.get("installer-settings.postgres.install"):
            self.settings.set("installer-settings.postgres.install",
                              click.confirm("For the following prompt if N is placed "
                                            "Postgres is assumed to be"
                                            " installed or remotely provisioned. "
                                            "Install Bitnami Postgres chart?",
                                            default=True))
        if self.settings.get("installer-settings.postgres.install"):
            if not self.settings.get("installer-settings.postgres.namespace"):
                namespace = click.prompt("Please enter a namespace for postgres.", default="postgres")
                self.settings.set("installer-settings.postgres.namespace", namespace)

            self.settings.set("config.configmap.cnSqlDbHost",
                              f"postgresql.{self.settings.get('installer-settings.postgres.namespace')}."
                              f"svc.cluster.local")

            self.settings.set("config.configmap.cnJackrabbitPostgresHost",
                              f"postgresql.jackrabbit{self.settings.get('installer-settings.postgres.namespace')}."
                              f"svc.cluster.local")

        if not self.settings.get("config.configmap.cnSqlDbHost"):
            url = click.prompt(
                "Please enter  postgres (remote or local) "
                "URL base name.",
                default=f"postgresql.{self.settings.get('installer-settings.postgres.namespace')}.svc.cluster.local",
            )
            self.settings.set("config.configmap.cnSqlDbHost", url)

        if not self.settings.get("config.configmap.cnJackrabbitPostgresHost"):
            url = click.prompt(
                "Please enter  postgres (remote or local) "
                "URL base name. If postgres is to be installed",
                default=f"postgresql.jackrabbit{self.settings.get('installer-settings.postgres.namespace')}.svc.cluster.local",
            )
            self.settings.set("config.configmap.cnJackrabbitPostgresHost", url)
