"""
pygluu.kubernetes.terminal.redis
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for terminal redis prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click

from pygluu.kubernetes.helpers import get_logger, prompt_password

logger = get_logger("gluu-prompt-redis  ")


class PromptRedis:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_redis(self):
        """Prompts for Redis
        """
        if self.settings.get("config.configmap.cnRedisType") in (None, ''):
            logger.info("STANDALONE, CLUSTER")
            self.settings.set("config.configmap.cnRedisType", click.prompt("Please enter redis type", default="CLUSTER"))

        if self.settings.get("installer-settings.redis.install"):
            logger.info("For the following prompt if placed [N] the Redis is assumed to be"
                        " installed or remotely provisioned")
            self.settings.set("installer-settings.redis.install", click.confirm("Install Redis using Bitnami helm chart"))

        if self.settings.get("installer-settings.redis.install"):
            if self.settings.get("installer-settings.redis.namespace") in (None, ''):
                namespace = click.prompt("Please enter a namespace for Redis cluster", default="gluu-redis-cluster")
                self.settings.set("installer-settings.redis.namespace", namespace)
                
            if self.settings.get("config.redisPassword") in (None, ''):
                self.settings.set("config.redisPassword", prompt_password("Redis"))

        if self.settings.get("config.configmap.cnRedisUrl") in (None, ''):
            if self.settings.get("installer-settings.redis.install"):
                redis_url_prompt = "redis-cluster.{}.svc.cluster.local:6379".format(
                    self.settings.get("installer-settings.redis.namespace"))
            else:
                logger.info(
                    "Redis URL can be : redis-cluster.gluu-redis-cluster.svc.cluster.local:6379 in a redis deployment")
                logger.info("Redis URL using AWS ElastiCach [Configuration Endpoint]: "
                            "clustercfg.testing-redis.icrbdv.euc1.cache.amazonaws.com:6379")
                logger.info("Redis URL using Google MemoryStore : <ip>:6379")
                redis_url_prompt = click.prompt(
                    "Please enter redis URL. If you are deploying redis",
                    default="redis-cluster.gluu-redis-cluster.svc.cluster.local:6379",
                )
            self.settings.set("config.configmap.cnRedisUrl", redis_url_prompt)
