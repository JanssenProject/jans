"""
pygluu.kubernetes.terminal.persistencebackend
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for persistence backend terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click


class PromptPersistenceBackend:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_persistence_backend(self):
        """Prompts for persistence backend layer
        """
        persistence_map = {
            1: "ldap",
            2: "couchbase",
            3: "hybrid",
            4: "spanner",
            5: "sql"
        }

        if self.settings.get("global.cnPersistenceType") not in persistence_map.values():
            print("|------------------------------------------------------------------|")
            print("|                     Persistence layer                            |")
            print("|------------------------------------------------------------------|")
            print("| [1] OpenDJ [default]                                             |")
            print("| [2] Couchbase                                                    |")
            print("| [3] Hybrid(OpenDJ + Couchbase)                                   |")
            print("| [4] Google Spanner                                               |")
            print("| [5] SQL(MySQL or PostgreSQL)                                     |")
            print("|------------------------------------------------------------------|")

            choice = click.prompt("Persistence layer", default=1)
            self.settings.set("global.cnPersistenceType", persistence_map.get(choice, "ldap"))

        self.settings.set("global.opendj.enabled", False)
        if self.settings.get("global.cnPersistenceType") == "ldap":
            self.settings.set("global.opendj.enabled", True)
