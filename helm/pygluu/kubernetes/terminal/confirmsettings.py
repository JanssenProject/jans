"""
pygluu.kubernetes.terminal.confirmsettings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for  confirming user settings terminal prompt.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

import click


class PromptConfirmSettings:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def confirm_params(self):
        """Formats output of settings from prompts to the user. Passwords are not displayed.
        """
        print("{:<1} {:<40} {:<10} {:<35} {:<1}".format('|', 'Setting', '|', 'Value', '|'))

        def iterate_dict(dictionary):
            for k, v in dictionary.items():
                if isinstance(v, dict):
                    iterate_dict(v)
                else:
                    if "Password" not in dictionary[k] and \
                            "subjectAlternativeName" not in dictionary[k]:
                        print("{:<1} {:<40} {:<10} {:<35} {:<1}".format('|', k, '|', v, '|'))
                        print("{:<1} {:<40} {:<10} {:<35} {:<1}".format('-', 'Setting', '-', 'Value', '-'))

        if click.confirm("Please confirm the above settings"):
            self.settings.set("installer-settings.confirmSettings", True)
        else:
            self.settings.reset_data()
            # Prompt for settings again
            from pygluu.kubernetes.terminal.prompt import Prompt
            initialize_prompts = Prompt()
            initialize_prompts.prompt()
