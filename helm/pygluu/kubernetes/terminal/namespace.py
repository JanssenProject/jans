"""
pygluu.kubernetes.terminal.namespace
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for namespace terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

import click


class PromptNamespace:

    def __init__(self, settings):
        self.settings = settings

    def prompt_gluu_namespace(self):
        """Prompt to enable optional services
        """
        if self.settings.get("installer-settings.namespace") in (None, ''):
            self.settings.set("installer-settings.namespace",
                              click.prompt("Namespace to deploy Gluu in", default="gluu"))
