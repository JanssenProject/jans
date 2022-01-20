import pytest
from pygluu.kubernetes.terminal.prompt import Prompt
check = Prompt()


def test_license(monkeypatch, settings):

    monkeypatch.setattr("click.confirm", lambda x: True)
    
    settings.set("installer-settings.acceptLicense", "Y")
    check.license()
    assert settings.get("installer-settings.acceptLicense")


@pytest.mark.skip(reason="this test needs fixing")
def test_versions(settings):

    settings.set("installer-settings.currentVersion", "5.2")
    check.versions()
    assert settings.get("installer-settings.currentVersion") == "5.2"


@pytest.mark.parametrize("given, expected", [
    (1, "microk8s.io/hostpath"),
])
def test_arch(monkeypatch, settings, given, expected):

    monkeypatch.setattr("click.prompt", lambda x, default: given)

    settings.set("global.storageClass.provisioner", "microk8s.io/hostpath")
    check.arch()
    assert settings.get("global.storageClass.provisioner") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "gluu"),
])
def test_namespace(monkeypatch, settings, given, expected):

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)
    settings.set("installer-settings.namespace", "gluu")
    check.namespace()
    assert settings.get("installer-settings.namespace") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_optional_services(monkeypatch, settings, given, expected):

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("config.configmap.cnPassportEnabled", True)
    check.optional_services()
    assert settings.get("config.configmap.cnPassportEnabled")


def test_istio(monkeypatch, settings):

    monkeypatch.setattr("click.prompt", lambda x, default: "istio-system")

    settings.set("global.istio.namespace", "istio-system")
    settings.set("global.istio.enabled", True)
    check.istio()
    assert settings.get("global.istio.namespace") == "istio-system"


def test_jackrabbit(monkeypatch, settings):

    monkeypatch.setattr("click.prompt", lambda x, default: "admin")

    settings.set("config.configmap.cnJackrabbitAdminId", "admin")
    check.jackrabbit()
    assert settings.get("config.configmap.cnJackrabbitAdminId") == "admin"


def test_persistence_backend(monkeypatch, settings):

    monkeypatch.setattr("click.prompt", lambda x, default: "hybrid")

    settings.set("global.cnPersistenceType", "hybrid")
    check.persistence_backend()
    assert settings.get("global.cnPersistenceType") == "hybrid"


@pytest.mark.parametrize("given, expected", [
    ("", "NATIVE_PERSISTENCE"),  # default
])
def test_cache(monkeypatch, settings, given, expected):

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)
    monkeypatch.setattr("pygluu.kubernetes.terminal.redis.PromptRedis.prompt_redis", lambda x: None)
    settings.set("config.configmap.cnCacheType", "NATIVE_PERSISTENCE")

    check.cache()
    assert settings.get("config.configmap.cnCacheType") == "NATIVE_PERSISTENCE"


def test_confirm_settings(monkeypatch, settings):

    monkeypatch.setattr("click.confirm", lambda x: True)

    settings.set("installer-settings.confirmSettings", True)
    check.confirm_settings()
    assert settings.get("installer-settings.confirmSettings")


def test_replicas(monkeypatch, settings):

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    settings.set("auth-server.replicas", 1)
    check.replicas()
    assert settings.get("auth-server.replicas") == 1


@pytest.mark.skip(reason="this test needs fixing")
@pytest.mark.parametrize("given, expected", [
    ("", "demoexample.gluu.org"),  # default
])
def test_configuration(monkeypatch, settings, given, expected):

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("config.countryCode", "US")
    settings.set("config.state", "TX")
    settings.set("config.city", "Austin")
    settings.set("config.email", "support@gluu.org")
    settings.set("config.orgName", "Gluu")
    settings.set("config.adminPassword", "Admin GUI")
    settings.set("global.fqdn", "demoexample.gluu.org")
    check.configuration()
    assert settings.get("global.fqdn") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
])
def test_images(monkeypatch, settings, given, expected):

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("config.configmap.cnCacheType", "ldap")
    check.images()
    assert settings.get("opendj.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
])
def test_test_environment(monkeypatch, settings, given, expected):

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.cloud.testEnviroment", False)
    settings.set("global.storageClass.provisioner", "awsEbsDynamic")
    check.test_enviornment()
    assert settings.get("global.cloud.testEnviroment") == expected


def test_ldap(monkeypatch, settings):

    monkeypatch.setattr("click.prompt", lambda x, default: "default")

    settings.set("config.configmap.cnPersistenceLdapMapping", "default")
    settings.set("global.cnPersistenceType", "hybrid")
    check.ldap()
    assert settings.get("config.configmap.cnPersistenceLdapMapping") == "default"


def test_volume(settings, monkeypatch):
    monkeypatch.setattr("click.prompt", lambda x, default: "microk8s.io/hostpath")

    settings.set("installer-settings.volumeProvisionStrategy", "microk8sDynamic")
    settings.set("global.storageClass.provisioner", "microk8s.io/hostpath")
    check.volumes()
    assert settings.get("global.storageClass.provisioner") == "microk8s.io/hostpath"


def test_couchbase(monkeypatch, settings):

    monkeypatch.setattr("click.prompt", lambda x, default: "cbns")

    settings.set("installer-settings.couchbase.namespace", "cbns")
    settings.set("global.cnPersistenceType", "couchbase")
    check.couchbase()
    assert settings.get("installer-settings.couchbase.namespace") == "cbns"


def test_backup(monkeypatch, settings):


    monkeypatch.setattr("click.prompt", lambda x, default: "0 2 * * 6")

    settings.set("global.storageClass.provisioner", "awsEbsDynamic")
    settings.set("global.cnPersistenceType", "couchbase")
    settings.set("installer-settings.couchbase.backup.fullSchedule", "0 2 * * 6")

    check.backup()

    assert settings.get("installer-settings.couchbase.backup.fullSchedule") == "0 2 * * 6"


def test_confirms_settings(settings, monkeypatch):

    monkeypatch.setattr("click.confirm", lambda x: False)
    monkeypatch.setattr("pygluu.kubernetes.terminal.prompt.Prompt.prompt", lambda x: None)
    settings.set("installer-settings.confirmSettings", False)
    check.confirm_settings()
    assert settings.get("installer-settings.confirmSettings") == False