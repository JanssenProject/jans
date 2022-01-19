import pytest

def test_prompt_couchbase_ip(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase
    from pygluu.kubernetes.terminal.helpers import gather_ip

    monkeypatch.setattr("click.prompt", lambda x, default: gather_ip)

    settings.set("global.lbIp", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase()
    assert settings.get("global.lbIp") == gather_ip


def test_prompt_couchbase_namespace(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "cbns")

    settings.set("installer-settings.couchbase.namespace", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase()
    assert settings.get("installer-settings.couchbase.namespace") == "cbns"


def test_prompt_couchbase_cluster(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "cbgluu")

    settings.set("installer-settings.couchbase.clusterName", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase()
    assert settings.get("installer-settings.couchbase.clusterName") == "cbgluu"


def test_prompt_couchbase_bucket(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "gluu")

    settings.set("config.configmap.cnCouchbaseBucketPrefix", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase()
    assert settings.get("config.configmap.cnCouchbaseBucketPrefix") == "gluu"


def test_prompt_couchbase_replicanum(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "0")

    settings.set("config.configmap.cnCouchbaseIndexNumReplica", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase()
    assert settings.get("config.configmap.cnCouchbaseIndexNumReplica") == "0"


def test_prompt_couchbase_superuser(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "admin")

    settings.set("config.configmap.cnCouchbaseSuperUser", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase()
    assert settings.get("config.configmap.cnCouchbaseSuperUser") == "admin"


def test_prompt_couchbase_user(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "gluu")

    settings.set("config.configmap.cnCouchbaseUser", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase()
    assert settings.get("config.configmap.cnCouchbaseUser") == "gluu"


def test_prompt_couchbase_users(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "1000000")

    settings.set("installer-settings.couchbase.totalNumberOfExpectedUsers", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase_yaml()
    assert settings.get("installer-settings.couchbase.totalNumberOfExpectedUsers") == "1000000"


def test_prompt_couchbase_transactions(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: 2000)

    settings.set("installer-settings.couchbase.totalNumberOfExpectedTransactionsPerSec", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase_yaml()
    assert settings.get("installer-settings.couchbase.totalNumberOfExpectedTransactionsPerSec") == 2000


def test_prompt_couchbase_volumetype(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "io1")

    settings.set("installer-settings.couchbase.volumeType", "")
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase_yaml()
    assert settings.get("installer-settings.couchbase.volumeType") == "io1"


def test_prompt_couchbase_commonname(monkeypatch, settings):
    from pygluu.kubernetes.terminal.couchbase import PromptCouchbase

    monkeypatch.setattr("click.prompt", lambda x, default: "Couchbase CA")
    cm = "Couchbase CA"
    settings.set("installer-settings.couchbase.commonName", cm)
    prompt = PromptCouchbase(settings)
    prompt.prompt_couchbase_yaml()
    assert settings.get("installer-settings.couchbase.commonName") == cm