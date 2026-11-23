"""Regression tests for AuthHandler.patch() when `privkey-push-delay` is used and
auth-server pods change while the job is waiting.

Run from docker-jans-cloudtools/: `pytest tests`
"""
import hashlib
import json
import os
import sys

import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scripts"))
certmanager = pytest.importorskip("certmanager")


class GoneError(Exception):
    """Stand-in for kubernetes ApiException(404)."""


class DockerResult(tuple):
    """Shape of docker-py ExecResult: (exit_code, output-as-bytes)."""

    exit_code = property(lambda self: self[0])
    output = property(lambda self: self[1])


class FakeMeta:
    """Minimal meta client with a per-pod fake filesystem; `pods` is the live cluster state."""

    def __init__(self, pods, docker_style=False):
        self.docker_style = docker_style
        self.pods = list(pods)
        self.files = {p: {} for p in pods}   # pod -> {path: bytes}
        self.jks_pushed = []
        self.jwks_pushed = []
        self.fail_on = set()      # pods that raise even if listed (died after listing)
        self.silent_fail = set()  # copy "succeeds" but nothing is written (remote tar exits nonzero)
        self.partial = set()      # copy writes a truncated file
        self.flaky = {}           # pod -> number of copy attempts that fail before it starts working

    def _check(self, c):
        if c not in self.pods or c in self.fail_on:
            raise GoneError(f'pods "{c}" not found')

    def get_containers(self, label):
        return list(self.pods)

    def get_container_name(self, c):
        return c

    def exec_cmd(self, c, cmd):
        self._check(c)
        parts = cmd.split()
        fs = self.files.setdefault(c, {})
        if parts[0] == "cp":
            if parts[1] in fs:
                fs[parts[2]] = fs[parts[1]]
        elif parts[0] == "sha256sum":
            if parts[1] not in fs:
                return DockerResult((1, b"")) if self.docker_style else "sha256sum: no such file"
            line = f"{hashlib.sha256(fs[parts[1]]).hexdigest()}  {parts[1]}\n"
            return DockerResult((0, line.encode())) if self.docker_style else line
        return ""

    def copy_to_container(self, c, path):
        self._check(c)
        if path.endswith(".jks") and self.flaky.get(c, 0) > 0:
            self.flaky[c] -= 1
            raise GoneError(f"transient error on {c}")
        if c in self.silent_fail:
            return
        with open(path, "rb") as f:
            data = f.read()
        if c in self.partial:
            data = data[:1]
        self.files.setdefault(c, {})[path] = data
        (self.jks_pushed if path.endswith(".jks") else self.jwks_pushed).append(c)


class FakeBackend:
    def __init__(self, *_):
        self.revisions = []

    def get_auth_config(self):
        return {
            "id": "1",
            "jansRevision": 10 + len(self.revisions),
            "jansConfWebKeys": {"keys": []},
            "jansConfDyn": {"keyRegenerationEnabled": False, "idTokenLifetime": 3600},
        }

    def modify_auth_config(self, id_, rev, conf_dynamic, keys):
        self.revisions.append(rev)
        return True


class FakeKV:
    def get(self, key, default=None):
        return "x"

    def set(self, key, value):
        pass


class FakeManager:
    secret = FakeKV()
    config = FakeKV()


@pytest.fixture
def handler(monkeypatch, tmp_path):
    jwks, jks = tmp_path / "auth-keys.json", tmp_path / "auth-keys.jks"
    jwks.write_text(json.dumps({"keys": []}))
    jks.write_text("jks")

    # redirect /etc/certs writes made by patch() into tmp_path
    import builtins
    real_open = builtins.open
    monkeypatch.setattr(
        builtins, "open",
        lambda p, *a, **k: real_open(str(p).replace("/etc/certs", str(tmp_path)), *a, **k),
    )

    monkeypatch.setitem(certmanager._backend_classes, "sql", FakeBackend)
    monkeypatch.setattr(certmanager.AuthHandler, "get_merged_keys", lambda self, h: (str(jwks), str(jks)))
    monkeypatch.setattr(certmanager.AuthHandler, "create_archived_jwks", lambda *a: None)
    monkeypatch.setattr(certmanager, "encode_jks", lambda m: "b64")
    monkeypatch.setattr(certmanager, "has_ext_jwks_uri", lambda *a: False)
    monkeypatch.setenv("CN_CONTAINER_METADATA", "kubernetes")

    h = certmanager.AuthHandler(FakeManager(), False, **{"privkey-push-delay": 60})
    h.meta_client = FakeMeta(["auth-a", "auth-b", "auth-c"])
    for pod in h.meta_client.pods:
        h.meta_client.files[pod][str(jks)] = b"old-jks"
    h.jks_path = str(jks)
    return h


def _sleep_hook(monkeypatch, on_sleep):
    monkeypatch.setattr(certmanager.time, "sleep", lambda sec: on_sleep())


def test_no_disruption_pushes_all_and_reloads(handler, monkeypatch):
    _sleep_hook(monkeypatch, lambda: None)
    handler.patch()
    assert handler.meta_client.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert len(handler.backend.revisions) == 2


def test_pod_removed_during_delay_does_not_abort_push(handler, monkeypatch):
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.pods.remove("auth-b"))

    handler.patch()  # must not raise

    assert meta.jks_pushed == ["auth-a", "auth-c"]
    # second revision bump (reload private keys) must still happen
    assert len(handler.backend.revisions) == 2


def test_pod_dies_after_refresh_does_not_abort_push(handler, monkeypatch):
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.fail_on.add("auth-a"))  # first pod in list is unreachable

    handler.patch()  # must not raise

    assert meta.jks_pushed == ["auth-b", "auth-c"]
    assert len(handler.backend.revisions) == 2


def test_pod_added_during_delay_receives_jks(handler, monkeypatch):
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.pods.append("auth-d"))

    handler.patch()

    assert "auth-d" in meta.jks_pushed


def test_all_pods_gone_skips_reload(handler, monkeypatch):
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.pods.clear())

    handler.patch()

    assert meta.jks_pushed == []
    assert len(handler.backend.revisions) == 1  # only the initial bump


def test_silent_copy_failure_is_not_counted_and_reload_still_runs(handler, monkeypatch):
    """remote tar exits nonzero but copy_to_container returns normally"""
    meta = handler.meta_client
    meta.silent_fail.add("auth-b")
    _sleep_hook(monkeypatch, lambda: None)

    handler.patch()

    assert meta.jks_pushed == ["auth-a", "auth-c"]
    assert len(handler.backend.revisions) == 2
    # failed pod still has its previous, loadable keystore
    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"


def test_only_silent_failures_skips_reload(handler, monkeypatch):
    meta = handler.meta_client
    meta.silent_fail.update(meta.pods)
    _sleep_hook(monkeypatch, lambda: None)

    handler.patch()

    assert meta.jks_pushed == []
    assert len(handler.backend.revisions) == 1  # only the initial bump


def test_partial_copy_is_restored_from_backup(handler, monkeypatch):
    meta = handler.meta_client
    meta.partial.add("auth-a")
    _sleep_hook(monkeypatch, lambda: None)

    handler.patch()

    # truncated keystore must not be left on the pod
    assert meta.files["auth-a"][handler.jks_path] == b"old-jks"
    # healthy pods hold the full new keystore
    assert meta.files["auth-b"][handler.jks_path] == b"jks"
    assert len(handler.backend.revisions) == 2


def test_docker_style_exec_result_is_handled(handler, monkeypatch):
    """DockerMeta.exec_cmd returns ExecResult(exit_code, bytes), not a string"""
    meta = handler.meta_client
    meta.docker_style = True
    _sleep_hook(monkeypatch, lambda: None)

    handler.patch()

    assert meta.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert len(handler.backend.revisions) == 2


def test_docker_style_partial_copy_is_restored(handler, monkeypatch):
    meta = handler.meta_client
    meta.docker_style = True
    meta.partial.add("auth-a")
    _sleep_hook(monkeypatch, lambda: None)

    handler.patch()

    assert meta.files["auth-a"][handler.jks_path] == b"old-jks"
    assert len(handler.backend.revisions) == 2


def test_failed_restore_is_logged_as_error(handler, monkeypatch, caplog):
    meta = handler.meta_client
    meta.partial.add("auth-a")
    meta.no_restore = True  # `cp backup jks` silently does nothing
    orig = meta.exec_cmd

    def exec_cmd(c, cmd):
        if meta.no_restore and cmd.startswith("cp") and cmd.endswith(handler.jks_path):
            return ""
        return orig(c, cmd)

    meta.exec_cmd = exec_cmd
    _sleep_hook(monkeypatch, lambda: None)

    with caplog.at_level("ERROR"):
        handler.patch()

    assert any("unable to verify restore" in r.message for r in caplog.records)
    assert len(handler.backend.revisions) == 2


def test_stale_backup_is_not_restored_when_backup_step_fails(handler, monkeypatch):
    """backup creation fails -> copy never starts -> an old .backup must not replace the current JKS"""
    meta = handler.meta_client
    jks = handler.jks_path
    meta.files["auth-a"][jks + ".backup"] = b"stale-from-earlier-rotation"
    orig = meta.exec_cmd

    def exec_cmd(c, cmd):
        if c == "auth-a" and cmd == f"cp {jks} {jks}.backup":
            raise GoneError("connection reset")
        return orig(c, cmd)

    meta.exec_cmd = exec_cmd
    _sleep_hook(monkeypatch, lambda: None)

    handler.patch()

    assert meta.files["auth-a"][jks] == b"old-jks"
    assert "auth-a" not in meta.jks_pushed
    assert meta.jks_pushed == ["auth-b", "auth-c"]
    assert len(handler.backend.revisions) == 2


def test_stale_backup_is_not_restored_when_backup_silently_fails(handler, monkeypatch):
    """cp returns normally but did not refresh the backup (stale content) -> do not copy, do not restore"""
    meta = handler.meta_client
    jks = handler.jks_path
    meta.files["auth-a"][jks + ".backup"] = b"stale-from-earlier-rotation"
    orig = meta.exec_cmd

    def exec_cmd(c, cmd):
        if c == "auth-a" and cmd == f"cp {jks} {jks}.backup":
            return ""  # silently does nothing
        return orig(c, cmd)

    meta.exec_cmd = exec_cmd
    _sleep_hook(monkeypatch, lambda: None)

    handler.patch()

    assert meta.files["auth-a"][jks] == b"old-jks"
    assert "auth-a" not in meta.jks_pushed


def test_failed_pod_is_retried_and_receives_jks(handler, monkeypatch):
    """transient failure on one pod: job must not fail, pod must get the new JKS on a later round"""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 2
    sleeps = []
    monkeypatch.setattr(certmanager.time, "sleep", lambda sec: sleeps.append(sec))

    handler.patch()

    assert meta.files["auth-b"][handler.jks_path] == b"jks"
    assert meta.jks_pushed.count("auth-a") == 1 and meta.jks_pushed.count("auth-c") == 1
    assert meta.jks_pushed.count("auth-b") == 1
    # reload happens once, after every pod was updated
    assert len(handler.backend.revisions) == 2
    # delay + two retry waits
    assert sleeps == [60, 10, 10]


def test_pod_that_never_recovers_does_not_fail_job(handler, monkeypatch):
    meta = handler.meta_client
    meta.flaky["auth-b"] = 999
    sleeps = []
    monkeypatch.setattr(certmanager.time, "sleep", lambda sec: sleeps.append(sec))

    handler.patch()  # must not raise

    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"
    assert meta.files["auth-a"][handler.jks_path] == b"jks"
    assert meta.files["auth-c"][handler.jks_path] == b"jks"
    assert len(handler.backend.revisions) == 2
    # bounded: delay + 3 retry waits
    assert sleeps == [60, 10, 10, 10]


def test_pod_replaced_during_retry_is_pushed(handler, monkeypatch):
    """failing pod is evicted between rounds and its replacement becomes Ready"""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 999
    calls = {"n": 0}

    def sleep(sec):
        calls["n"] += 1
        if calls["n"] == 2:  # first retry wait
            meta.pods.remove("auth-b")
            meta.pods.append("auth-d")

    monkeypatch.setattr(certmanager.time, "sleep", sleep)

    handler.patch()

    assert "auth-d" in meta.jks_pushed
    assert len(handler.backend.revisions) == 2


def test_retries_can_be_disabled(handler, monkeypatch):
    handler.privkey_push_retries = 0
    meta = handler.meta_client
    meta.flaky["auth-b"] = 1
    sleeps = []
    monkeypatch.setattr(certmanager.time, "sleep", lambda sec: sleeps.append(sec))

    handler.patch()

    assert sleeps == [60]
    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"
    assert len(handler.backend.revisions) == 2
