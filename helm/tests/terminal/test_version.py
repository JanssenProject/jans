import contextlib
import os

import pytest


def test_version_no_prompt(settings):
    from pygluu.kubernetes.terminal.version import PromptVersion

    prompt = PromptVersion(settings, version="5.0")
    prompt.prompt_version()
    assert settings.get("installer-settings.currentVersion") == "5.0"


@pytest.mark.parametrize("given, expected", [
    ("", "5.0.0_01"),  # default if empty
    ("5.0.0_dev", "5.0.0_dev"),  # non-empty shouldn't be overriden
])
def test_version_merge_names_tags(settings, given, expected):
    import json
    from pygluu.kubernetes.terminal.version import PromptVersion

    with open("./gluu_versions.json", "w") as f:
        json.dump({"5.0": {"LDAP_IMAGE_TAG": "5.0.0_01"}}, f)

    settings.set("installer-settings.currentVersion", "5.0")
    settings.set("LDAP_IMAGE_TAG", given)

    PromptVersion(settings)
    assert settings.get("LDAP_IMAGE_TAG") == expected

    with contextlib.suppress(FileNotFoundError):
        os.unlink("./gluu_versions.json")


@pytest.mark.parametrize("given, expected", [
    ("", "5.0"),
    ("5.0", "5.0"),
])
def test_version(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.version import PromptVersion

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    prompt = PromptVersion(settings)

    # unset CN_VERSION in order to prompt user-input
    settings.set("installer-settings.currentVersion", "")

    prompt.prompt_version()
    assert settings.get("installer-settings.currentVersion") == expected
