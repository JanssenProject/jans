"""
pygluu.kubernetes.terminal.image
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for image names and tags terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click


class PromptImages:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """
    def __init__(self, settings):
        self.settings = settings

    def prompt_image_name_tag(self):
        """Manual prompts for image names and tags if changed from default or at a different repository.
        """

        def prompt_and_set_setting(service, image):
            repository = f'{image}.image.repository'
            tag = f'{image}.image.tag'
            settings = self.settings
            settings.set(repository,
                         click.prompt(f"{service} image name",
                                      default=self.settings.get(repository)))
            settings.set(tag,
                         click.prompt(f"{service} image tag",
                                      default=self.settings.get(tag)))

        if self.settings.get("installer-settings.images.edit") in (None, ''):
            self.settings.set("installer-settings.images.edit", click.confirm(
                "Would you like to manually edit the image source/name and tag"))

        if self.settings.get("installer-settings.images.edit"):
            # CASA
            if self.settings.get("config.configmap.cnCasaEnabled"):
                prompt_and_set_setting("Casa", "casa")
            # CONFIG
            prompt_and_set_setting("Config", "config")
            # CACHE_REFRESH_ROTATE
            if self.settings.get("global.cr-rotate.enabled"):
                prompt_and_set_setting("CR-rotate", "cr-rotate")
            # KEY_ROTATE
            if self.settings.get("global.auth-server-key-rotation.enabled"):
                prompt_and_set_setting("Key rotate", "auth-server-key-rotation")
            # LDAP
            if self.settings.get("config.configmap.cnCacheType") in ("hybrid", "ldap"):
                prompt_and_set_setting("OpenDJ", "opendj")
            # Jackrabbit
            prompt_and_set_setting("jackrabbit", "jackrabbit")
            # AUTH_SERVER
            prompt_and_set_setting("Auth-Server", "auth-server")
            # CONFIG_API
            if self.settings.get("global.config-api.enabled"):
                prompt_and_set_setting("Config-API", "config-api")
            # CLIENT_API
            if self.settings.get("global.client-api.enabled"):
                prompt_and_set_setting("CLIENT_API server", "client-api")
            # OXPASSPORT
            if self.settings.get("config.configmap.cnPassportEnabled"):
                prompt_and_set_setting("oxPassport", "oxpassport")
            # OXSHIBBBOLETH
            if self.settings.get("global.oxshibboleth.enabled"):
                prompt_and_set_setting("oxShibboleth", "oxshibboleth")
            # PERSISTENCE
            prompt_and_set_setting("Persistence", "persistence")
            self.settings.set("installer-settings.images.edit", False)
