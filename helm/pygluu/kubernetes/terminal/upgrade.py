"""
pygluu.kubernetes.terminal.upgrade
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for upgrade terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

import click

from pygluu.kubernetes.helpers import get_supported_versions
from pygluu.kubernetes.terminal.images import PromptImages


class PromptUpgrade:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_upgrade(self):
        """Prompts for upgrade and returns updated settings.
        :return:
        """
        versions, version_number = get_supported_versions()
        if self.settings.get("installer-settings.upgrade.targetVersion") in (None, ''):
            self.settings.set("installer-settings.upgrade.targetVersion", click.prompt(
                "Please enter the version to upgrade Gluu to", default=version_number,
            ))

        image_names_and_tags = versions.get(self.settings.get("installer-settings.upgrade.targetVersion"), {})
        self.settings.update(image_names_and_tags)

        # reset this config to force image prompt
        self.settings.set("installer-settings.image.edit", "")
        PromptImages(self.settings).prompt_image_name_tag()
