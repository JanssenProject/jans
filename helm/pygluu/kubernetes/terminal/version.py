"""
pygluu.kubernetes.terminal.version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for terminal gluu version prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click

from pygluu.kubernetes.helpers import get_supported_versions


class PromptVersion:

    def __init__(self, settings, version=""):
        self.settings = settings
        if not self.settings.get("installer-settings.currentVersion"):
            self.settings.set("installer-settings.currentVersion", version)
        self.prompt_version()

    def prompt_version(self):
        """Prompts for Gluu versions
        """
        versions, version_number = get_supported_versions()

        if self.settings.get("installer-settings.currentVersion") in (None, ''):
            self.settings.set("installer-settings.currentVersion", click.prompt(
                "Please enter the current version of Gluu or the version to be installed",
                default=version_number,
            ))

        image_names_and_tags = versions.get(self.settings.get("installer-settings.currentVersion"), {})
        # override non-empty image name and tag
        for k, v in image_names_and_tags.items():
            if self.settings.get(k) in (None, ''):
                self.settings.set(k, v)
