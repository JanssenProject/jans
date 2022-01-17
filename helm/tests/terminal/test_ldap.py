import pytest


@pytest.mark.parametrize("given, expected", [
    (1, "default"),
    (2, "user"),
    (3, "site"),
    (4, "cache"),
    (5, "token"),
    (6, "session"),
    (0, "default"),
])
def test_prompt_hybrid_ldap_held_data(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.ldap import PromptLdap

    monkeypatch.setattr("click.prompt", lambda x, default: given)

    settings.set("config.configmap.cnPersistenceLdapMapping", "")
    PromptLdap(settings).prompt_hybrid_ldap_held_data()
    assert settings.get("config.configmap.cnPersistenceLdapMapping") == expected
