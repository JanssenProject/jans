"""
pygluu.kubernetes.terminal.license
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for terminal license prompt .

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
from pygluu.kubernetes.helpers import get_logger
import click

logger = get_logger("gluu-prompt-license")


class PromptLicense:

    def __init__(self, settings, accept_license=False):
        self.settings = settings
        if accept_license:
            self.settings.set("installer-settings", True)
        self.prompt_license()

    def prompt_license(self):
        """Prompts user to accept Apache 2.0 license
        """
        if not self.settings.get("installer-settings.acceptLicense"):
            with open("./LICENSE") as f:
                print(f.read())

            self.settings.set("installer-settings.acceptLicense",
                              click.confirm("Do you accept the Gluu license stated above"))
            if not self.settings.get("installer-settings.acceptLicense"):
                logger.info("License not accepted.")
                raise SystemExit(1)
