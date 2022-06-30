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
def gvault_secret():
    from jans.pycloudlib.secret import VaultSecret

    secret = VaultSecret()
    secret.prefix = "secret/testing"
    yield secret


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
            "ldap_binddn": "cn=Directory Manager",
            "couchbase_server_user": "admin",
            "jca_client_id": "1234",
        }
        return ctx.get(key) or default

    def get_secret(key, default=""):
        ctx = {
            "encoded_ox_ldap_pw": ENCODED_PW,
            "encoded_ldapTrustStorePass": ENCODED_PW,
            "ldap_pkcs12_base64": ENCODED_PW,
            "encoded_salt": "7MEDWVFAG3DmakHRyjMqp5EE",
            "sql_password": "secret",
            "couchbase_password": "secret",
            "couchbase_superuser_password": "secret",
            "random": ENCODED_PW,
        }
        return ctx.get(key) or default

    def set_secret(key, value):
        return

    gmanager = get_manager()

    gconsul_config.get = get_config
    gmanager.config.adapter = gconsul_config

    gvault_secret.get = get_secret
    gvault_secret.set = set_secret
    gmanager.secret.adapter = gvault_secret

    yield gmanager


@pytest.fixture
def gk8s_meta():
    from jans.pycloudlib.meta import KubernetesMeta

    meta = KubernetesMeta()
    meta.kubeconfig_file = "tests/kubeconfig"
    yield meta
