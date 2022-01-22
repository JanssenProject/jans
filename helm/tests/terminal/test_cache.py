import pytest


@pytest.mark.parametrize("given, expected", [
    ("", "NATIVE_PERSISTENCE"),  # default
    (1, "NATIVE_PERSISTENCE"),
    (2, "IN_MEMORY"),
    (3, "REDIS"),
])
def test_cache_type(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.cache import PromptCache

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)
    # mock PromptRedis as we will have separate testcases for it
    monkeypatch.setattr("pygluu.kubernetes.terminal.redis.PromptRedis.prompt_redis", lambda x: None)
    settings.set("config.configmap.cnCacheType", "")

    PromptCache(settings).prompt_cache_type()
    assert settings.get("config.configmap.cnCacheType") == expected
