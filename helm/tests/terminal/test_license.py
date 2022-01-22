import pytest


def test_license_accepted(monkeypatch, settings):
    from pygluu.kubernetes.terminal.license import PromptLicense

    monkeypatch.setattr("click.confirm", lambda x: True)

    PromptLicense(settings)
    assert settings.get("installer-settings.acceptLicense")


def test_license_rejected(monkeypatch, settings):
    from pygluu.kubernetes.terminal.license import PromptLicense

    monkeypatch.setattr("click.confirm", lambda x: False)

    with pytest.raises(SystemExit):
        PromptLicense(settings)
