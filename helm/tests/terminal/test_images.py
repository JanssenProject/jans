import pytest


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_casa(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("config.configmap.cnCasaEnabled", True)
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("casa.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_crrotate(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("global.cr-rotate.enabled", True)
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("cr-rotate.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_keyauth(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("global.auth-server-key-rotation.enabled", True)
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("auth-server-key-rotation.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_hybrid(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("config.configmap.cnCacheType", "hybrid")
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("opendj.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_ldap(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("config.configmap.cnCacheType", "ldap")
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("opendj.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_clientapi(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("global.client-api.enabled", True)
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("client-api.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_oxpassport(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("config.configmap.cnPassportEnabled", True)
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("oxpassport.image.tag") == expected


@pytest.mark.parametrize("given, expected", [
    (True, True),
])
def test_testenv_prompt_test_edit_oxshiboleth(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.images import PromptImages

    monkeypatch.setattr("click.prompt", lambda x, default: given)
    settings.set("installer-settings.images.edit", True)
    settings.set("global.oxshibboleth.enabled", True)
    prompt = PromptImages(settings)
    prompt.prompt_image_name_tag()
    assert settings.get("oxshibboleth.image.tag") == expected

