"""
pygluu.kubernetes.terminal.distribution
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for Gluu distribution terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click


class PromptDistribution:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_distribution(self):
        """Prompt distribution
        """
        gluu_distribution_map = {
            1: "default",
            2: "openbanking",
        }
        if self.settings.get("global.distribution") not in gluu_distribution_map.values() \
                and self.settings.get("global.distribution") in ("None", ''):
            print("|------------------------------------------------------------------|")
            print("|                     Gluu Distribution                            |")
            print("|------------------------------------------------------------------|")
            print("| [1] default [default]                                            |")
            print("| [2] OpenBanking                                                  |")
            print("|------------------------------------------------------------------|")
            choice = click.prompt("Gluu distribution", default=1)
            self.settings.set("global.distribution", gluu_distribution_map.get(choice, "default"))
