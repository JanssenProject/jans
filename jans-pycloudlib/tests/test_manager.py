import pytest


def test_base_configuration():
    from functools import cached_property
    from jans.pycloudlib.manager import BaseConfiguration

    class Configuration(BaseConfiguration):
        @cached_property
        def remote_adapter(self):
            return Adapter()

        @cached_property
        def local_adapter(self):
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
def test_config_remote_adapter(monkeypatch, adapter_name, adapter_cls):
    from jans.pycloudlib.manager import ConfigManager

    monkeypatch.setenv("CN_CONFIG_ADAPTER", adapter_name)
    assert ConfigManager().remote_adapter.__class__.__name__ == adapter_cls


def test_config_remote_adapter_invalid(monkeypatch):
    from jans.pycloudlib.manager import ConfigManager

    monkeypatch.setenv("CN_CONFIG_ADAPTER", "random")
    with pytest.raises(ValueError) as exc:
        ConfigManager().get("config1")
    assert "Unsupported config adapter" in str(exc.value)


def test_config_local_adapter():
    from jans.pycloudlib.manager import ConfigManager
    from jans.pycloudlib.manager import FileConfig
    assert isinstance(ConfigManager().local_adapter, FileConfig)


@pytest.mark.parametrize("adapter_name, adapter_cls", [
    ("vault", "VaultSecret"),
    ("kubernetes", "KubernetesSecret"),
    ("google", "GoogleSecret"),
    ("aws", "AwsSecret"),
])
def test_secret_remote_adapter(monkeypatch, adapter_name, adapter_cls):
    from jans.pycloudlib.manager import SecretManager

    monkeypatch.setenv("CN_SECRET_ADAPTER", adapter_name)
    assert SecretManager().remote_adapter.__class__.__name__ == adapter_cls


def test_secret_remote_adapter_invalid(monkeypatch):
    from jans.pycloudlib.manager import SecretManager

    monkeypatch.setenv("CN_SECRET_ADAPTER", "random")
    with pytest.raises(ValueError) as exc:
        SecretManager().get("secret1")
    assert "Unsupported secret adapter" in str(exc.value)


def test_secret_local_adapter():
    from jans.pycloudlib.manager import SecretManager
    from jans.pycloudlib.manager import FileSecret
    assert isinstance(SecretManager().local_adapter, FileSecret)


@pytest.mark.parametrize("key, expected, decode, binary_mode", [
    ("sql_password", "secret", False, False),
    ("random", "secret", True, False),
    ("random", "secret", False, True),
    ("random", "secret", True, True),
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
