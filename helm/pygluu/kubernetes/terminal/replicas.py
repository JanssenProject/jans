"""
pygluu.kubernetes.terminal.replicas
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for terminal replicas prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click


class PromptReplicas:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_replicas(self):
        """Prompt number of replicas for Gluu apps
        """
        if self.settings.get("auth-server.replicas") in (None, ''):
            self.settings.set("auth-server.replicas", click.prompt("Number of Auth-Server replicas", default=1))

        if self.settings.get("global.config-api.enabled") and self.settings.get("config-api.replicas") in (None, ''):
            self.settings.set("config-api.replicas", click.prompt("Number of configAPI replicas", default=1))

        if self.settings.get("global.fido2.enabled") and self.settings.get("fido2.replicas") in (None, ''):
            self.settings.set("fido2.replicas", click.prompt("Number of fido2 replicas", default=1))

        if self.settings.get("global.scim.enabled") and self.settings.get("scim.replicas") in (None, ''):
            self.settings.set("scim.replicas", click.prompt("Number of scim replicas", default=1))

        if self.settings.get("global.cnPersistenceType") in ("hybrid", "ldap") and \
                self.settings.get("opendj.replicas") in (None, ''):
            self.settings.set("opendj.replicas", click.prompt("Number of LDAP replicas", default=1))

        if self.settings.get("global.oxshibboleth.enabled") and \
                self.settings.get("oxshibboleth.replicas") in (None, ''):
            self.settings.set("oxshibboleth.replicas", click.prompt("Number of oxShibboleth replicas", default=1))

        if self.settings.get("config.configmap.cnPassportEnabled") and \
                self.settings.get("oxpassport.replicas") in (None, ''):
            self.settings.set("oxpassport.replicas", click.prompt("Number of oxPassport replicas", default=1))

        if self.settings.get("global.client-api.enabled") and self.settings.get("client-api.replicas") in (None, ''):
            self.settings.set("client-api.replicas", click.prompt("Number of client-api replicas", default=1))

        if self.settings.get("config.configmap.cnCasaEnabled") and self.settings.get("casa.replicas") in (None, ''):
            self.settings.set("casa.replicas", click.prompt("Number of Casa replicas", default=1))

