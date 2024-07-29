import pytest


def test_base_configuration():
    from functools import cached_property
    from jans.pycloudlib.manager import BaseConfiguration

    class Configuration(BaseConfiguration):
        @cached_property
        def adapter(self):
            return Adapter()

    class Adapter:
        def get(self, k, default=None):
            return "random"

        def set(self, k, v):  # noqa: A003
            return True

        def all(self):  # noqa: A003
            return {}

        def get_all(self):
            return {}

        def set_all(self, data):
            return True

    config = Configuration()

    assert config.get("foo") == "random"
    assert config.set("foo", "bar") is True
    assert config.all() == {}  # ``all`` method is deprecated
    assert config.get_all() == {}
    assert config.set_all({"foo": "bar"}) is True


@pytest.mark.parametrize("adapter_name, adapter_cls", [
    ("consul", "ConsulConfig"),
    ("kubernetes", "KubernetesConfig"),
    ("google", "GoogleConfig"),
    ("aws", "AwsConfig"),
])
def test_config_adapter_backend(monkeypatch, adapter_name, adapter_cls):
    from jans.pycloudlib.manager import ConfigAdapter

    monkeypatch.setenv("CN_CONFIG_ADAPTER", adapter_name)
    adapter = ConfigAdapter()
    assert adapter.backend.__class__.__name__ == adapter_cls


def test_config_adapter_invalid_backend(monkeypatch):
    from jans.pycloudlib.manager import ConfigAdapter

    monkeypatch.setenv("CN_CONFIG_ADAPTER", "random")
    with pytest.raises(ValueError) as exc:
        _ = ConfigAdapter().get("config1")
    assert "Unsupported config adapter" in str(exc.value)


def test_config_manager_adapter():
    from jans.pycloudlib.manager import ConfigManager
    from jans.pycloudlib.manager import ConfigAdapter

    config_manager = ConfigManager()
    assert isinstance(config_manager.adapter, ConfigAdapter)


@pytest.mark.parametrize("adapter_name, adapter_cls", [
    ("vault", "VaultSecret"),
    ("kubernetes", "KubernetesSecret"),
    ("google", "GoogleSecret"),
    ("aws", "AwsSecret"),
])
def test_secret_adapter_backend(monkeypatch, adapter_name, adapter_cls):
    from jans.pycloudlib.manager import SecretAdapter

    monkeypatch.setenv("CN_SECRET_ADAPTER", adapter_name)
    adapter = SecretAdapter()
    assert adapter.backend.__class__.__name__ == adapter_cls


def test_secret_adapter_invalid_backend(monkeypatch):
    from jans.pycloudlib.manager import SecretAdapter

    monkeypatch.setenv("CN_SECRET_ADAPTER", "random")
    with pytest.raises(ValueError) as exc:
        _ = SecretAdapter().get("secret1")
    assert "Unsupported secret adapter" in str(exc.value)


def test_secret_manager_adapter():
    from jans.pycloudlib.manager import SecretManager
    from jans.pycloudlib.manager import SecretAdapter

    secret_manager = SecretManager()
    assert isinstance(secret_manager.adapter, SecretAdapter)


@pytest.mark.parametrize("key, expected, decode, binary_mode", [
    ("sql_password", "secret", False, False),
    ("encoded_ox_ldap_pw", "secret", True, False),
    ("encoded_ox_ldap_pw", "secret", False, True),
    ("encoded_ox_ldap_pw", "secret", True, True),
])
def test_manager_secret_to_file(
    gmanager,
    tmpdir,
    monkeypatch,
    key,
    expected,
    decode,
    binary_mode,
):
    dst = tmpdir.join("secret.txt")
    gmanager.secret.to_file(key, str(dst), decode, binary_mode)
    assert dst.read() == expected


@pytest.mark.parametrize("key, value, expected, encode, binary_mode", [
    ("random", "secret", "fHL54sT5qHk=", True, False),
    ("random", b"secret", "fHL54sT5qHk=", True, True),
])
def test_manager_secret_from_file(
    gmanager,
    tmpdir,
    monkeypatch,
    key,
    value,
    expected,
    encode,
    binary_mode,
):
    dst = tmpdir.join("secret_file")
    dst.write(value)

    gmanager.secret.from_file(key, str(dst), encode, binary_mode)
    assert gmanager.secret.get(key) == expected
