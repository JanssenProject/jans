"""
pygluu.kubernetes.terminal.sql
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for jackrabbit terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click

from pygluu.kubernetes.helpers import get_logger, prompt_password

logger = get_logger("gluu-prompt-sql")


class PromptSQL:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_sql(self):
        """Prompts for SQL server
        """
        sql_dialect = {
            1: "mysql",
            2: "pgsql",
        }

        if self.settings.get("config.configmap.cnSqlDbDialect") not in sql_dialect.values():
            print("|------------------------------------------------------------------|")
            print("|                     SQL DIALECT                                  |")
            print("|------------------------------------------------------------------|")
            print("| [1] MySQL                                                        |")
            print("| [2] PostgreSQL                                                   |")
            print("|------------------------------------------------------------------|")

            choice = click.prompt("SQL dialect", default=1)
            self.settings.set("config.configmap.cnSqlDbDialect", sql_dialect.get(choice, "mysql"))

        if not self.settings.get("installer-settings.sql.install"):
            logger.info(
                "Install SQL dialect from Bitnamis charts.If the following prompt is answered with N it is assumed "
                "the SQL server is installed remotely or locally by the user."
                " A managed service such as Amazon Aurora or CloudSQL should be used in production setups.")
            self.settings.set("installer-settings.sql.install",
                              click.confirm("Install SQL dialect from Bitnamis charts", default=True))

        if self.settings.get("installer-settings.sql.install"):
            self.settings.set("config.configmap.cnSqlDbPort", 3306)
            if not self.settings.get("installer-settings.sql.namespace"):
                self.settings.set("installer-settings.sql.namespace",
                                  click.prompt("Please enter a namespace for the SQL server", default="sql"))

            self.settings.set("config.configmap.cnSqlDbHost",
                              f'gluu-mysql.{self.settings.get("installer-settings.sql.namespace")}.svc.cluster.local')
            if self.settings.get("config.configmap.cnSqlDbDialect") == "pgsql":
                self.settings.set("installer-settings.postgres.install", True)
                self.settings.set("config.configmap.cnSqlDbHost",
                                  f'gluu-postgresql.{self.settings.get("installer-settings.sql.namespace")}.svc.cluster.local')
                self.settings.set("config.configmap.cnSqlDbPort", 5432)
        if not self.settings.get("config.configmap.cnSqlDbHost"):
            self.settings.set("config.configmap.cnSqlDbHost",
                              click.prompt("Please enter  SQL (remote or local) URL base name",
                                           default="gluu.sql.svc.cluster.local"))
        if not self.settings.get("config.configmap.cnSqlDbPort"):
            self.settings.set("config.configmap.cnSqlDbPort", click.prompt("Please enter  SQL (remote or local) port "
                                                                           "number", default=3306))
        if not self.settings.get("config.configmap.cnSqlDbUser"):
            self.settings.set("config.configmap.cnSqlDbUser", click.prompt("Please enter a user for Gluu SQL database ",
                                                                           default="gluu"))

        if not self.settings.get("config.configmap.cnSqldbUserPassword"):
            self.settings.set("config.configmap.cnSqldbUserPassword", prompt_password("gluu-db-sql"))

        if not self.settings.get("config.configmap.cnSqlDbName"):
            self.settings.set("config.configmap.cnSqlDbName", click.prompt("Please enter Gluu SQL database name",
                                                                           default="gluu"))
