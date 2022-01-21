import pygluu.kubernetes.terminal.redis as module0
import click
import pytest


def test_prompt_redis_type(monkeypatch, settings):
    from pygluu.kubernetes.terminal.redis import PromptRedis

    monkeypatch.setattr("click.prompt", lambda x, default: "CLUSTER")

    settings.set("config.configmap.cnRedisType", "")
    prompt = PromptRedis(settings)
    prompt.prompt_redis()
    assert settings.get("config.configmap.cnRedisType") == "CLUSTER"


@pytest.mark.parametrize("given, expected", [
    ("", "redis.redis.svc.cluster.local"),  # default
    ("random", "random"),
])
def test_redis_url(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.redis import PromptRedis

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("installer-settings.redis.namespace", "redis")
    settings.set("config.configmap.cnRedisUrl", "")

    prompt = PromptRedis(settings)
    prompt.prompt_redis()
    assert settings.get("config.configmap.cnRedisUrl") == expected


def test_prompt_redis_install(monkeypatch, settings):
    from pygluu.kubernetes.terminal.redis import PromptRedis

    monkeypatch.setattr("click.confirm", lambda x: True)

    settings.set("installer-settings.redis.install", True)
    prompt = PromptRedis(settings)
    prompt.prompt_redis()

    assert settings.get("installer-settings.redis.install")