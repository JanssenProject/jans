"""
pygluu.kubernetes.terminal.google
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for spanner terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click
from pathlib import Path
import base64
import json


class PromptGoogle:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_google(self):
        """Prompts for spanner ids
        """
        if self.settings.get("global.cnPersistenceType") == "spanner":
            if not self.settings.get("config,configmap.cnGoogleSpannerInstanceId"):
                self.settings.set("config,configmap.cnGoogleSpannerInstanceId",
                                  click.prompt("Please enter the google spanner instance ID.",
                                               default=""))

            if not self.settings.get("config,configmap.cnGoogleSpannerDatabaseId"):
                self.settings.set("config,configmap.cnGoogleSpannerDatabaseId",
                                  click.prompt("Please enter the google spanner database ID",
                                               default=""))
        # Feature not implemented yet
        self.settings.set("installer-settings.google.useSecretManager", False)
        if not self.settings.get("installer-settings.google.useSecretManager"):
            self.settings.set("installer-settings.google.useSecretManager",
                              click.confirm("[BETA] Use Google Secret Manager to hold gluu configuration layer. "
                                            "If answered with No, kubernetes secrets will be used", default=False))

        if self.settings.get("global.cnPersistenceType") == "spanner" or \
                self.settings.get("installer-settings.google.useSecretManager"):
            if not self.settings.get("config.configmap.cnGoogleSecretManagerServiceAccount"):
                try:
                    print("Place the google service account json file under the name google_service_account.json. at "
                          "the same location as the installation script. The service account must have "
                          "roles/secretmanager.admin to use Google secret manager and/or "
                          "roles/spanner.databaseUser to use Spanner")
                    _ = input("Hit 'enter' or 'return' when ready.")
                    with open(Path("./google_service_account.json")) as content_file:
                        sa = content_file.read()
                        encoded_sa_crt_bytes = base64.b64encode(sa.encode("utf-8"))
                        encoded_sa_crt_string = str(encoded_sa_crt_bytes, "utf-8")
                    self.settings.set("config.configmap.cnGoogleSecretManagerServiceAccount", encoded_sa_crt_string)
                except FileNotFoundError:
                    print("The google service account json was not found.")
                    raise SystemExit(1)

            if not self.settings.get("config.configmap.cnGoogleProjectId"):
                try:
                    with open("google_service_account.json", "r") as google_sa:
                        sa = json.load(google_sa)
                        self.settings.set("config.configmap.cnGoogleProjectId", sa["project_id"])
                except FileNotFoundError:
                    print("The google service account json was not found."
                          "your settings.json.")
                    if not self.settings.get("config.configmap.cnGoogleProjectId"):
                        self.settings.set("config.configmap.cnGoogleProjectId",
                                          click.prompt("Please enter the google project ID",
                                                       default=""))