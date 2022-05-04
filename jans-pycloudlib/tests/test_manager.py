import pytest


class GAdapter(object):
    def get(self, k, default=None):
        return "GET"

    def set(self, k, v):  # noqa: A003
        return "SET"

    def all(self):  # noqa: A003
        return {}

    def get_all(self):
        return {}

    def set_all(self, data):
        return True


@pytest.mark.parametrize("adapter, adapter_cls", [
    ("consul", "ConsulConfig"),
    ("kubernetes", "KubernetesConfig"),
    ("random", "NoneType"),
])
def test_config_manager(monkeypatch, adapter, adapter_cls):
    from jans.pycloudlib.manager import ConfigManager

    monkeypatch.setenv("CN_CONFIG_ADAPTER", adapter)
    manager = ConfigManager()
    assert manager.adapter.__class__.__name__ == adapter_cls


@pytest.mark.parametrize("adapter, adapter_cls", [
    ("vault", "VaultSecret"),
    ("kubernetes", "KubernetesSecret"),
    ("random", "NoneType"),
])
def test_secret_manager(monkeypatch, adapter, adapter_cls):
    from jans.pycloudlib.manager import SecretManager

    monkeypatch.setenv("CN_SECRET_ADAPTER", adapter)
    manager = SecretManager()
    assert manager.adapter.__class__.__name__ == adapter_cls


def test_config_manager_methods():
    from jans.pycloudlib.manager import ConfigManager

    gadapter = GAdapter()
    manager = ConfigManager()
    manager.adapter = gadapter

    assert manager.get("foo") == gadapter.get("foo")
    assert manager.set("foo", "bar") == gadapter.set("foo", "bar")
    assert manager.all() == gadapter.all()


def test_secret_manager_methods():
    from jans.pycloudlib.manager import SecretManager

    gadapter = GAdapter()
    manager = SecretManager()
    manager.adapter = gadapter

    assert manager.get("foo") == gadapter.get("foo")
    assert manager.set("foo", "bar") == gadapter.set("foo", "bar")
    assert manager.all() == gadapter.all()


@pytest.mark.parametrize("value, expected, decode, binary_mode", [
    ("secret", "secret", False, False),
    ("fHL54sT5qHk=", "secret", True, False),
    ("secret", b"secret", False, True),
    ("fHL54sT5qHk=", b"secret", True, True),
])
def test_manager_secret_to_file(
    gmanager,
    tmpdir,
    monkeypatch,
    value,
    expected,
    decode,
    binary_mode,
):
    dst = tmpdir.join("secret.txt")
    gmanager.secret.to_file("encoded_ox_ldap_pw", str(dst), decode, binary_mode)
    assert dst.read()


@pytest.mark.parametrize("value, expected, encode, binary_mode", [
    ("secret", "fHL54sT5qHk=", True, False),
    (b"secret", "fHL54sT5qHk=", True, True),
])
def test_manager_secret_from_file(
    gmanager,
    tmpdir,
    monkeypatch,
    value,
    expected,
    encode,
    binary_mode,
):
    dst = tmpdir.join("secret_file")
    dst.write(value)

    gmanager.secret.from_file("encoded_ox_ldap_pw", str(dst), encode, binary_mode)
    assert gmanager.secret.get("encoded_ox_ldap_pw") == expected
