import json

import pytest


@pytest.fixture()
def gconsul_config():
    from jans.pycloudlib.config import ConsulConfig

    config = ConsulConfig()
    config.prefix = "testing/config/"
    yield config


@pytest.fixture()
def gk8s_config():
    from jans.pycloudlib.config import KubernetesConfig

    config = KubernetesConfig()
    config.settings["CN_CONFIG_KUBERNETES_USE_KUBE_CONFIG"] = True
    config.settings["CN_CONFIG_KUBERNETES_NAMESPACE"] = "testing"
    config.settings["CN_CONFIG_KUBERNETES_CONFIGMAP"] = "testing"
    config.kubeconfig_file = "tests/kubeconfig"
    yield config


@pytest.fixture()
def gvault_secret(tmpdir, monkeypatch):
    from jans.pycloudlib.secret import VaultSecret

    role_id_file = tmpdir.join("vault_role_id")
    role_id_file.write("")
    secret_id_file = tmpdir.join("vault_secret_id")
    secret_id_file.write("")

    monkeypatch.setenv("CN_SECRET_VAULT_ROLE_ID_FILE", str(role_id_file))
    monkeypatch.setenv("CN_SECRET_VAULT_SECRET_ID_FILE", str(secret_id_file))
    monkeypatch.setenv("CN_SECRET_VAULT_NAMESPACE", "testing")
    monkeypatch.setenv("CN_SECRET_VAULT_KV_PATH", "secret")
    yield VaultSecret()


@pytest.fixture()
def gk8s_secret():
    from jans.pycloudlib.secret import KubernetesSecret

    secret = KubernetesSecret()
    secret.settings["CN_SECRET_KUBERNETES_USE_KUBE_CONFIG"] = True
    secret.settings["CN_SECRET_KUBERNETES_NAMESPACE"] = "testing"
    secret.settings["CN_SECRET_KUBERNETES_SECRET"] = "testing"
    secret.kubeconfig_file = "tests/kubeconfig"
    yield secret


@pytest.fixture
def gmanager(gconsul_config, gvault_secret):
    from jans.pycloudlib.manager import get_manager

    ENCODED_PW = "fHL54sT5qHk="

    def get_config(key, default=""):
        ctx = {
            "jca_client_id": "1234",
        }
        return ctx.get(key) or default

    def get_secret(key, default=""):
        ctx = {
            "encoded_salt": "7MEDWVFAG3DmakHRyjMqp5EE",
            "sql_password": "secret",
            "random": ENCODED_PW,
        }
        return ctx.get(key) or default

    def set_secret(key, value):
        return True

    gmanager = get_manager()

    gconsul_config.get = get_config
    gmanager.config.remote_adapter = gconsul_config

    gvault_secret.get = get_secret
    gvault_secret.set = set_secret
    gmanager.secret.remote_adapter = gvault_secret
    yield gmanager


@pytest.fixture
def gk8s_meta():
    from jans.pycloudlib.meta import KubernetesMeta

    meta = KubernetesMeta()
    meta.kubeconfig_file = "tests/kubeconfig"
    yield meta


@pytest.fixture
def google_creds(tmpdir):
    creds = tmpdir.join("google-credentials.json")
    creds.write(json.dumps({
        "client_id": "random-id",
        "client_secret": "random-secret",
        "refresh_token": "random-refresh-token",
        "type": "authorized_user"
    }))
    yield creds


@pytest.fixture
def sql_client(gmanager):
    from jans.pycloudlib.persistence.sql import SqlClient

    client = SqlClient(gmanager)
    yield client
