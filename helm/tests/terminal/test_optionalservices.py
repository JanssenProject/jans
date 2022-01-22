import pytest


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_prompt_casa(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)

    settings.set("config.configmap.cnCasaEnabled", True)
    settings.set("global.client-api.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.client-api.enabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_crrotate(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.cr-rotate.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.cr-rotate.enabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
])
def test_testenv_kyerotation(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.auth-server-key-rotation.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.auth-server-key-rotation.enabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_passport(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("config.configmap.cnPassportEnabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("config.configmap.cnPassportEnabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_cncasat(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("config.configmap.cnCasaEnabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("config.configmap.cnCasaEnabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_oxshibboleth(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.oxshibboleth.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.oxshibboleth.enabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_fido2(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.fido2.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.fido2.enabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_configapit(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.config-api.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.config-api.enabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_scim(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.scim.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.scim.enabled") == expected


@pytest.mark.parametrize("given, expected", [
    (False, False),
    (True, True),
])
def test_testenv_prompt_clientapi(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices

    monkeypatch.setattr("click.confirm", lambda x: given)
    settings.set("global.client-api.enabled", "")
    prompt = PromptOptionalServices(settings)
    prompt.prompt_optional_services()
    assert settings.get("global.client-api.enabled") == expected
