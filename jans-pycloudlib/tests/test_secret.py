import base64
from collections import namedtuple

import pytest

KubeResult = namedtuple("KubeResult", ["data"])
VaultResponse = namedtuple("Response", ["status_code"])


# ============
# vault secret
# ============


def test_vault_secret_verify_cert(gvault_secret, tmpdir):
    cacert_file = tmpdir.join("cacert.pem")
    cacert_file.write("cacert")

    cert_file = tmpdir.join("cert.pem")
    cert_file.write("cert")

    key_file = tmpdir.join("key.pem")
    key_file.write("key")

    cert, verify = gvault_secret._verify_cert(
        "https", True, str(cacert_file), str(cert_file), str(key_file),
    )
    assert cert == (str(cert_file), str(key_file))
    assert verify == str(cacert_file)


def test_vault_secret_role_id(gvault_secret, tmpdir):
    file_ = tmpdir.join("vault_role_id.txt")
    file_.write("role-id")

    gvault_secret.settings["CN_SECRET_VAULT_ROLE_ID_FILE"] = str(file_)
    assert gvault_secret.role_id == "role-id"


def test_vault_secret_role_id_missing(gvault_secret):
    assert gvault_secret.role_id == ""


def test_vault_secret_secret_id(gvault_secret, tmpdir):
    file_ = tmpdir.join("vault_secret_id.txt")
    file_.write("secret-id")

    gvault_secret.settings["CN_SECRET_VAULT_SECRET_ID_FILE"] = str(file_)
    assert gvault_secret.secret_id == "secret-id"


def test_vault_secret_secret_id_missing(gvault_secret):
    assert gvault_secret.secret_id == ""


def test_vault_secret_authenticate_authenticated(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: True,
    )
    assert gvault_secret._authenticate() is None


def test_vault_secret_authenticate_not_authenticated(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: False,
    )

    monkeypatch.setattr(
        "hvac.api.auth_methods.approle.AppRole.login",
        lambda cls, role_id, secret_id, use_token, mount_point: {"auth": {"client_token": "token"}}
    )

    gvault_secret._authenticate()
    assert gvault_secret.client.token == "token"


def test_vault_secret_get(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: True,
    )

    monkeypatch.setattr(
        "hvac.api.secrets_engines.KvV1.read_secret",
        lambda cls, path, mount_point: {"data": {"value": "bar"}},
    )
    assert gvault_secret.get("foo") == "bar"


def test_vault_secret_get_default(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: True,
    )

    monkeypatch.setattr(
        "hvac.api.secrets_engines.KvV1.read_secret",
        lambda cls, path, mount_point: {},
    )
    assert gvault_secret.get("foo", "default") == "default"


def test_vault_secret_set(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: True,
    )
    monkeypatch.setattr(
        "hvac.api.secrets_engines.KvV1.create_or_update_secret",
        lambda cls, path, mount_point, secret: VaultResponse(204),
    )
    assert gvault_secret.set("foo", "bar") is True


def test_vault_secret_get_all(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: True,
    )
    monkeypatch.setattr(
        "hvac.api.secrets_engines.KvV1.list_secrets",
        lambda cls, path, mount_point: {"data": {"keys": ["foo"]}},
    )

    monkeypatch.setattr(
        "hvac.api.secrets_engines.KvV1.read_secret",
        lambda cls, path, mount_point: {"data": {"value": "bar"}},
    )
    assert gvault_secret.all() == {"foo": "bar"}


def test_vault_secret_get_all_empty(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: True,
    )
    monkeypatch.setattr(
        "hvac.api.secrets_engines.KvV1.list_secrets",
        lambda cls, path, mount_point: None,
    )
    assert gvault_secret.all() == {}


def test_vault_secret_request_warning(gvault_secret, caplog):
    gvault_secret._request_warning("https", False)
    assert "All requests to Vault will be unverified" in caplog.records[0].message


def test_vault_secret_set_all(gvault_secret, monkeypatch):
    monkeypatch.setattr(
        "hvac.Client.is_authenticated",
        lambda cls: True,
    )
    monkeypatch.setattr(
        "hvac.api.secrets_engines.KvV1.create_or_update_secret",
        lambda cls, path, mount_point, secret: VaultResponse(204),
    )
    assert gvault_secret.set_all({"a": 1}) is True


def test_vault_deprecated_envs(gvault_secret, monkeypatch, caplog):
    monkeypatch.setenv("CN_SECRET_VAULT_HOST", "localhost")
    monkeypatch.setenv("CN_SECRET_VAULT_PORT", "8200")
    monkeypatch.setenv("CN_SECRET_VAULT_SCHEME", "http")

    gvault_secret.addr
    assert "Specifying host via CN_SECRET_VAULT_HOST environment variable is deprecated" in caplog.records[0].message


@pytest.mark.parametrize("host, port, scheme", [
    ("localhost", "8200", "https"),
    ("localhost", "8200", "http"),
])
def test_vault_scheme(gvault_secret, monkeypatch, host, port, scheme):
    monkeypatch.setenv("CN_SECRET_VAULT_HOST", host)
    monkeypatch.setenv("CN_SECRET_VAULT_PORT", port)
    monkeypatch.setenv("CN_SECRET_VAULT_SCHEME", scheme)
    assert gvault_secret.scheme == scheme


# =================
# kubernetes secret
# =================


def test_k8s_secret_prepare_secret_read(gk8s_secret, monkeypatch):
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_secret",
        lambda cls, n, ns: KubeResult(data={"foo": base64.b64encode(b"bar")}),
    )
    gk8s_secret._prepare_secret()
    assert gk8s_secret.name_exists is True


def test_k8s_secret_prepare_secret_create(gk8s_secret, monkeypatch):
    import kubernetes.client.rest

    def _raise_exc(status):
        raise kubernetes.client.rest.ApiException(status=status)

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_secret",
        lambda cls, n, ns: _raise_exc(404),
    )

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.create_namespaced_secret",
        lambda cls, n, ns: KubeResult(data={"foo": base64.b64encode(b"bar")}),
    )

    gk8s_secret._prepare_secret()
    assert gk8s_secret.name_exists is True


def test_k8s_secret_prepare_secret_not_created(gk8s_secret, monkeypatch):
    import kubernetes.client.rest

    def _raise_exc(status):
        raise kubernetes.client.rest.ApiException(status=status)

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_secret",
        lambda cls, n, ns: _raise_exc(500),
    )

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.create_namespaced_secret",
        lambda cls, n, ns: KubeResult(data={"foo": base64.b64encode(b"bar")}),
    )

    with pytest.raises(kubernetes.client.rest.ApiException):
        gk8s_secret._prepare_secret()
    assert gk8s_secret.name_exists is False


def test_k8s_secret_get(gk8s_secret, monkeypatch):
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_secret",
        lambda cls, n, ns: KubeResult(data={"foo": base64.b64encode(b"bar")}),
    )
    assert gk8s_secret.get("foo") == "bar"


def test_k8s_secret_get_default(gk8s_secret, monkeypatch):
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_secret",
        lambda cls, n, ns: KubeResult(data={}),
    )
    assert gk8s_secret.get("foo", "default") == "default"


def test_k8s_secret_set(gk8s_secret, monkeypatch):
    gk8s_secret.name_exists = True

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.patch_namespaced_secret",
        lambda cls, n, ns, body: KubeResult(data={})
    )
    assert gk8s_secret.set("foo", "bar") is True


def test_k8s_secret_incluster():
    import kubernetes.config.config_exception
    from jans.pycloudlib.secret import KubernetesSecret

    secret = KubernetesSecret()

    with pytest.raises(kubernetes.config.config_exception.ConfigException):
        secret.client


def test_k8s_secret_set_all(gk8s_secret, monkeypatch):
    gk8s_secret.name_exists = True

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.patch_namespaced_secret",
        lambda cls, n, ns, body: KubeResult(data={})
    )
    assert gk8s_secret.set_all({"foo": "bar"}) is True


def test_k8s_secret_type(gk8s_secret):
    # gk8s_secret is a subclass of BaseSecret
    assert gk8s_secret.type == "secret"
