import pytest


class GAdapter(object):
    def get(self, k, default=None):
        return "GET"

    def set(self, k, v):
        return "SET"

    def all(self):
        return {}


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


@pytest.mark.skip(reason="need to rewrite testcase")
@pytest.mark.parametrize("value, expected, decode, binary_mode", [
    ("abcd", "abcd", False, False),
    ("YgH8NDxhxmA=", "abcd", True, False),
    ("abcd", b"abcd", False, True),
    ("YgH8NDxhxmA=", b"abcd", True, True),
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
    def maybe_salt(*args, **kwargs):
        if args[1] == "encoded_salt":
            return "a" * 24
        return value

    monkeypatch.setattr(
        "jans.pycloudlib.secret.VaultSecret.get",
        maybe_salt,
    )

    dst = tmpdir.join("secret.txt")

    result = gmanager.secret.to_file(
        "secret_key", str(dst), decode, binary_mode,
    )
    assert result == expected


@pytest.mark.skip(reason="need to rewrite testcase")
@pytest.mark.parametrize("value, expected, encode, binary_mode", [
    ("abcd", "abcd", False, False),
    ("abcd", "YgH8NDxhxmA=", True, False),
    (b"abcd", "abcd", False, True),
    (b"abcd", "YgH8NDxhxmA=", True, True),
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
    def maybe_salt(*args, **kwargs):
        if args[1] == "encoded_salt":
            return "a" * 24
        return expected

    monkeypatch.setattr(
        "jans.pycloudlib.secret.VaultSecret.get",
        maybe_salt,
    )

    monkeypatch.setattr(
        "jans.pycloudlib.secret.VaultSecret.set",
        lambda instance, key, value: True,
    )

    dst = tmpdir.join("secret_file")
    dst.write(value)

    result = gmanager.secret.from_file(
        "secret_key", str(dst), encode, binary_mode,
    )
    assert result == expected
