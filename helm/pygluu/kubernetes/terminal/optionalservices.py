"""
pygluu.kubernetes.terminal.optionalservices
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for optional services terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

import click


class PromptOptionalServices:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_optional_services(self):
        if self.settings.get("global.cr-rotate.enabled") in (None, ''):
            self.settings.set("global.cr-rotate.enabled", click.confirm("Deploy Cr-Rotate"))

        if self.settings.get("global.auth-server-key-rotation.enabled") in (None, ''):
            self.settings.set("global.auth-server-key-rotation.enabled", click.confirm("Deploy Key-Rotation"))

        if self.settings.get("global.auth-server-key-rotation.enabled"):
            if self.settings.get("auth-server-key-rotation.keysLife") in (None, ''):
                self.settings.set("auth-server-key-rotation.keysLife",
                                  click.prompt("Auth-Server keys life in hours", default=48))

        if self.settings.get("config.configmap.cnPassportEnabled") in (None, ''):
            self.settings.set("config.configmap.cnPassportEnabled", click.confirm("Deploy Passport"))

        if self.settings.get("global.oxshibboleth.enabled") in (None, ''):
            self.settings.set("global.oxshibboleth.enabled", click.confirm("Deploy Shibboleth SAML IDP"))

        if self.settings.get("config.configmap.cnCasaEnabled") in (None, ''):
            self.settings.set("config.configmap.cnCasaEnabled", click.confirm("Deploy Casa"))
        if self.settings.get("config.configmap.cnCasaEnabled"):
            self.settings.set("global.client-api.enabled", True)

        if self.settings.get("global.fido2.enabled") in (None, ''):
            self.settings.set("global.fido2.enabled", click.confirm("Deploy fido2"))

        if self.settings.get("global.config-api.enabled") in (None, ''):
            self.settings.set("global.config-api.enabled", click.confirm("Deploy Config API"))

        if self.settings.get("global.scim.enabled") in (None, ''):
            self.settings.set("global.scim.enabled", click.confirm("Deploy scim"))

            if self.settings.get("global.scim.enabled") in (None, ''):
                self.settings.set("config.configmap.cnScimProtectionMode",
                                  click.prompt("SCIM Protection mode", default="OAUTH",
                                               type=click.Choice(["OAUTH", "TEST", "UMA"])))

        if self.settings.get("global.client-api.enabled") in (None, ''):
            self.settings.set("global.client-api.enabled", click.confirm("Deploy Client API"))

        if self.settings.get("global.client-api.enabled"):
            if self.settings.get("config.configmap.cnClientApiApplicationCertCn") in (None, ''):
                self.settings.set("config.configmap.cnClientApiApplicationCertCn",
                                  click.prompt("Client API application keystore name",
                                               default="client-api"))
            if self.settings.get("config.configmap.cnClientApiAdminCertCn") in (None, ''):
                self.settings.set("config.configmap.cnClientApiAdminCertCn",
                                  click.prompt("Client API admin keystore name",
                                               default="client-api"))
