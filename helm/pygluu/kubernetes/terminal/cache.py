"""
pygluu.kubernetes.terminal.cache
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for cache terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click
from pygluu.kubernetes.terminal.redis import PromptRedis


class PromptCache:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_cache_type(self):
        """Prompt cache type
        """
        gluu_cache_map = {
            1: "NATIVE_PERSISTENCE",
            2: "IN_MEMORY",
            3: "REDIS",
        }
        if self.settings.get("config.configmap.cnCacheType") not in gluu_cache_map.values():
            print("|------------------------------------------------------------------|")
            print("|                     Cache layer                                  |")
            print("|------------------------------------------------------------------|")
            print("| [1] NATIVE_PERSISTENCE [default]                                 |")
            print("| [2] IN_MEMORY                                                    |")
            print("| [3] REDIS                                                        |")
            print("|------------------------------------------------------------------|")
            choice = click.prompt("Cache layer", default=1)
            self.settings.set("config.configmap.cnCacheType", gluu_cache_map.get(choice, "NATIVE_PERSISTENCE"))
        if self.settings.get("config.configmap.cnCacheType") == "REDIS":
            redis = PromptRedis(self.settings)
            redis.prompt_redis()
