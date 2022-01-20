import pytest


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_istio_ingress(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.istio import PromptIstio

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.istio.ingress", "")
    settings.set("global.storageClass.provisioner", "kubernetes.io/azure-disk")
    prompt = PromptIstio(settings)
    prompt.prompt_istio()
    assert settings.get("global.istio.ingress") == expected


def test_istio_enabled_prompt(monkeypatch, settings):
    from pygluu.kubernetes.terminal.istio import PromptIstio

    monkeypatch.setattr("click.prompt", lambda x, default: True)

    settings.set("global.istio.ingress", True)
    settings.set("global.istio.enabled", "False")
    prompt = PromptIstio(settings)
    prompt.prompt_istio()
    assert settings.get("global.istio.enabled")


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_global_istio_enabled(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.istio import PromptIstio

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.istio.enabled", "")
    prompt = PromptIstio(settings)
    prompt.prompt_istio()
    assert settings.get("global.istio.enabled") == expected


def test_istio_namespace(monkeypatch, settings):
    from pygluu.kubernetes.terminal.istio import PromptIstio

    monkeypatch.setattr("click.prompt", lambda x, default: "istio-system")

    settings.set("global.istio.namespace", "")
    settings.set("global.istio.enabled", True)
    prompt = PromptIstio(settings)
    prompt.prompt_istio()
    assert settings.get("global.istio.namespace") == "istio-system"


def test_istio_lbaddr(monkeypatch, settings):
    from pygluu.kubernetes.terminal.istio import PromptIstio

    monkeypatch.setattr("click.prompt", lambda x, default: "")

    settings.set("global.istio.namespace", "")
    settings.set("global.istio.enabled", True)
    settings.set("config.configmap.lbAddr", "")
    prompt = PromptIstio(settings)
    prompt.prompt_istio()
    assert settings.get("config.configmap.lbAddr") == ""
