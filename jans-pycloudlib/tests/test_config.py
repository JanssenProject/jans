from collections import namedtuple

import pytest


KubeResult = namedtuple("KubeResult", ["data"])

# =============
# consul config
# =============


def test_consul_config_token_from_file(gconsul_config, tmpdir):
    file_ = tmpdir.join("token_file")
    file_.write("random-token")
    assert gconsul_config._token_from_file(str(file_)) == "random-token"


def test_consul_config_verify_cert(gconsul_config, tmpdir):
    cacert_file = tmpdir.join("cacert.pem")
    cacert_file.write("cacert")

    cert_file = tmpdir.join("cert.pem")
    cert_file.write("cert")

    key_file = tmpdir.join("key.pem")
    key_file.write("key")

    cert, verify = gconsul_config._verify_cert(
        "https", True, str(cacert_file), str(cert_file), str(key_file),
    )
    assert cert == (str(cert_file), str(key_file))
    assert verify == str(cacert_file)


def test_consul_config_merge_path(gconsul_config):
    assert gconsul_config._merge_path("foo") == gconsul_config.prefix + "foo"


def test_consul_config_unmerge_path(gconsul_config):
    assert gconsul_config._unmerge_path(gconsul_config.prefix + "foo") == "foo"


def test_consul_config_get(gconsul_config, monkeypatch):
    monkeypatch.setattr(
        "consul.Consul.KV.get",
        lambda cls, k: (1, {"Value": b"bar"}),
    )
    assert gconsul_config.get("foo") == "bar"


def test_consul_config_get_default(gconsul_config, monkeypatch):
    monkeypatch.setattr(
        "consul.Consul.KV.get",
        lambda cls, k: (1, None),
    )
    assert gconsul_config.get("foo", "default") == "default"


def test_consul_config_set(gconsul_config, monkeypatch):
    monkeypatch.setattr(
        "consul.Consul.KV.put",
        lambda cls, k, v: True,
    )
    assert gconsul_config.set("foo", "bar") is True


def test_consul_config_get_all(gconsul_config, monkeypatch):
    monkeypatch.setattr(
        "consul.Consul.KV.get",
        lambda cls, k, recurse: (
            1,
            [
                {"Key": gconsul_config.prefix + "foo", "Value": b"bar"},
                {"Key": gconsul_config.prefix + "lorem", "Value": b"ipsum"},
            ],
        ),
    )
    assert gconsul_config.get_all() == {"foo": "bar", "lorem": "ipsum"}


def test_consul_config_get_all_empty(gconsul_config, monkeypatch):
    monkeypatch.setattr(
        "consul.Consul.KV.get",
        lambda cls, k, recurse: (1, []),
    )
    assert gconsul_config.get_all() == {}


def test_consul_config_request_warning(gconsul_config, caplog):
    gconsul_config._request_warning("https", False)
    assert "All requests to Consul will be unverified" in caplog.records[0].message


def test_consul_config_set_all(gconsul_config, monkeypatch):
    monkeypatch.setattr(
        "consul.Consul.KV.put",
        lambda cls, k, v: True,
    )
    assert gconsul_config.set_all({"foo": "bar"}) is True


# =================
# kubernetes config
# =================


def test_k8s_config_prepare_configmap_read(gk8s_config, monkeypatch):
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_config_map",
        lambda cls, n, ns: KubeResult(data={"foo": "bar"})
    )
    gk8s_config._prepare_configmap()
    assert gk8s_config.name_exists is True


def test_k8s_config_prepare_configmap_create(gk8s_config, monkeypatch):
    import kubernetes.client.rest

    def _raise_exc(status):
        raise kubernetes.client.rest.ApiException(status=status)

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_config_map",
        lambda cls, n, ns: _raise_exc(404),
    )
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.create_namespaced_config_map",
        lambda cls, n, ns: KubeResult(data={"foo": "bar"})
    )
    gk8s_config._prepare_configmap()
    assert gk8s_config.name_exists is True


def test_k8s_config_prepare_configmap_not_created(gk8s_config, monkeypatch):
    import kubernetes.client.rest

    def _raise_exc(status):
        raise kubernetes.client.rest.ApiException(status=status)

    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_config_map",
        lambda cls, n, ns: _raise_exc(500),
    )
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.create_namespaced_config_map",
        lambda cls, n, ns: KubeResult(data={"foo": "bar"})
    )

    with pytest.raises(kubernetes.client.rest.ApiException):
        gk8s_config._prepare_configmap()
    assert gk8s_config.name_exists is False


def test_k8s_config_get(gk8s_config, monkeypatch):
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_config_map",
        lambda cls, n, ns: KubeResult(data={"foo": "bar"})
    )
    assert gk8s_config.get("foo") == "bar"


def test_k8s_config_set(gk8s_config, monkeypatch):
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_config_map",
        lambda cls, n, ns: KubeResult(data={"foo": "bar"})
    )
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.patch_namespaced_config_map",
        lambda cls, n, ns, body: KubeResult(data={"foo": "bar"})
    )
    assert gk8s_config.set("foo", "bar") is True


def test_k8s_config_incluster():
    import kubernetes.config.config_exception
    from jans.pycloudlib.config import KubernetesConfig

    config = KubernetesConfig()

    with pytest.raises(kubernetes.config.config_exception.ConfigException):
        config.client


def test_k8s_config_set_all(gk8s_config, monkeypatch):
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.read_namespaced_config_map",
        lambda cls, n, ns: KubeResult(data={"foo": "bar"})
    )
    monkeypatch.setattr(
        "kubernetes.client.CoreV1Api.patch_namespaced_config_map",
        lambda cls, n, ns, body: KubeResult(data={"foo": "bar"})
    )
    assert gk8s_config.set_all({"foo": "bar"}) is True


def test_k8s_config_type(gk8s_config):
    # gk8s_config is a subclass of BaseConfig
    assert gk8s_config.type == "config"
