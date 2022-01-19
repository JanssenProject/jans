def test_prompt_persistence_backend_ldap(monkeypatch, settings):
    from pygluu.kubernetes.terminal.persistencebackend import PromptPersistenceBackend

    monkeypatch.setattr("click.prompt", lambda x, default: 1)

    settings.set("global.cnPersistenceType", "")
    PromptPersistenceBackend(settings).prompt_persistence_backend()

    assert settings.get("global.cnPersistenceType") == "ldap"


def test_prompt_persistence_backend_couchbase(monkeypatch, settings):
    from pygluu.kubernetes.terminal.persistencebackend import PromptPersistenceBackend

    monkeypatch.setattr("click.prompt", lambda x, default: 2)

    settings.set("global.cnPersistenceType", "")
    PromptPersistenceBackend(settings).prompt_persistence_backend()
    assert settings.get("global.cnPersistenceType") == "couchbase"


def test_prompt_persistence_backend_hybrid(monkeypatch, settings):
    from pygluu.kubernetes.terminal.persistencebackend import PromptPersistenceBackend

    monkeypatch.setattr("click.prompt", lambda x, default: 3)

    settings.set("global.cnPersistenceType", "")
    PromptPersistenceBackend(settings).prompt_persistence_backend()
    assert settings.get("global.cnPersistenceType") == "hybrid"
