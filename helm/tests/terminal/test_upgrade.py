import pytest


@pytest.mark.parametrize("given, expected", [
    ("", "5.0"),
    ("5.0", "5.0"),
])
def test_upgrade_version(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.upgrade import PromptUpgrade

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)
    monkeypatch.setattr(
        "pygluu.kubernetes.terminal.images.PromptImages.prompt_image_name_tag",
        lambda cls: None,
    )

    settings.set("installer-settings.upgrade.targetVersion", "")
    PromptUpgrade(settings).prompt_upgrade()
    assert settings.get("installer-settings.upgrade.targetVersion") == expected
    assert settings.get("installer-settings.image.edit") == ""
