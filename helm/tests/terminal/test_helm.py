import pytest


@pytest.mark.parametrize("given, expected", [
    ("", "gluu"),  # default
    ("random", "random"),
])
def test_helm_release_name(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.helm import PromptHelm

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("installer-settings.nginxIngress.releaseName", "ningress")
    settings.set("installer-settings.nginxIngress.namespace", "ingress-nginx")
    settings.set("opendj.multiCluster.enabled", False)
    settings.set("installer-settings.releaseName", "")

    prompt = PromptHelm(settings)
    prompt.prompt_helm()
    assert settings.get("installer-settings.releaseName") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "ningress"),  # default
    ("random", "random"),
])
def test_helm_ingress_release_name(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.helm import PromptHelm

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("installer-settings.releaseName", "gluu")
    settings.set("installer-settings.nginxIngress.namespace", "ingress-nginx")
    settings.set("opendj.multiCluster.enabled", False)
    settings.set("installer-settings.nginxIngress.releaseName", "")

    prompt = PromptHelm(settings)
    prompt.prompt_helm()
    assert settings.get("installer-settings.nginxIngress.releaseName") == expected


@pytest.mark.parametrize("given, expected", [
    ("", "ingress-nginx"),  # default
    ("random", "random"),
])
def test_helm_ingress_namespace(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.helm import PromptHelm

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("installer-settings.nginxIngress.releaseName", "ningress")
    settings.set("installer-settings.releaseName", "gluu")
    settings.set("opendj.multiCluster.enabled", False)
    settings.set("installer-settings.nginxIngress.namespace", "")

    prompt = PromptHelm(settings)
    prompt.prompt_helm()
    assert settings.get("installer-settings.nginxIngress.namespace") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
])
def test_aws_arn(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.helm import PromptHelm

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.cnPersistenceType", "ldap")
    settings.set("opendj.multiCluster.enabled", False)
    prompt = PromptHelm(settings)
    prompt.prompt_helm()
    assert settings.get("opendj.multiCluster.enabled") == expected
