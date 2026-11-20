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


class FakeMeta:
    """Minimal meta client with a per-pod fake filesystem; `pods` is the live cluster state."""

    def __init__(self, pods):
        self.pods = list(pods)
        self.files = {p: {} for p in pods}   # pod -> {path: bytes}
        self.jks_pushed = []
        self.jwks_pushed = []
        self.fail_on = set()      # pods that raise even if listed (died after listing)
        self.silent_fail = set()  # copy "succeeds" but nothing is written (remote tar exits nonzero)
        self.partial = set()      # copy writes a truncated file

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
            data = fs.get(parts[1], b"")
            return f"{hashlib.sha256(data).hexdigest()}  {parts[1]}\n"
        return ""

    def copy_to_container(self, c, path):
        self._check(c)
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
