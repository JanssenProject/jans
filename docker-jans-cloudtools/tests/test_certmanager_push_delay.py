"""Regression tests for AuthHandler.patch() when `privkey-push-delay` is used and
auth-server containers change while the job is waiting.

Run from docker-jans-cloudtools/: `pytest tests`
"""
import builtins
import hashlib
import json
import os
import sys
from collections import namedtuple

import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scripts"))
certmanager = pytest.importorskip("certmanager")


class GoneError(Exception):
    """Stand-in for kubernetes ApiException(404)."""


# same shape as docker-py's ExecResult: (exit_code, output-as-bytes)
DockerResult = namedtuple("DockerResult", "exit_code output")


class FakeMeta:
    """Minimal meta client with a per-container fake filesystem; `pods` is the live cluster state."""

    def __init__(self, pods, docker_style=False):
        self.docker_style = docker_style
        self.pods = list(pods)
        self.files = {pod: {} for pod in pods}   # pod -> {path: bytes}
        self.jks_pushed = []
        self.fail_on = set()      # pods that raise even if listed (died after listing)
        self.silent_fail = set()  # copy "succeeds" but nothing is written (remote tar exits nonzero)
        self.partial = set()      # copy writes a truncated file
        self.flaky = {}           # pod -> number of copy attempts that fail before it starts working
        self.not_ready = set()    # Running but not Ready: reachable by exec/copy, left out of get_containers
        self.on_list = None       # called without arguments at each get_containers call, before listing

    def _check(self, container):
        if container not in self.pods or container in self.fail_on:
            raise GoneError(f'pods "{container}" not found')

    def get_containers(self, label):
        if self.on_list:
            self.on_list()
        return [pod for pod in self.pods if pod not in self.not_ready]

    def get_container_name(self, container):
        return container

    def exec_cmd(self, container, cmd):
        self._check(container)
        parts = cmd.split()
        pod_files = self.files.setdefault(container, {})
        if parts[0] == "cp":
            if parts[1] in pod_files:
                pod_files[parts[2]] = pod_files[parts[1]]
        elif parts[0] == "sha256sum":
            if parts[1] not in pod_files:
                return DockerResult(1, b"") if self.docker_style else "sha256sum: no such file"
            line = f"{hashlib.sha256(pod_files[parts[1]]).hexdigest()}  {parts[1]}\n"
            return DockerResult(0, line.encode()) if self.docker_style else line
        return ""

    def copy_to_container(self, container, path):
        self._check(container)
        if path.endswith(".jks") and self.flaky.get(container, 0) > 0:
            self.flaky[container] -= 1
            raise GoneError(f"transient error on {container}")
        if container in self.silent_fail:
            return
        with open(path, "rb") as local_file:
            data = local_file.read()
        if container in self.partial:
            data = data[:1]
        self.files.setdefault(container, {})[path] = data
        if path.endswith(".jks"):
            self.jks_pushed.append(container)


class FakeBackend:
    def __init__(self, *args):
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
        """Every secret and config value reads as the placeholder "x": the tested code only passes them on."""
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
    real_open = builtins.open
    monkeypatch.setattr(
        builtins, "open",
        lambda path, *args, **kwargs: real_open(str(path).replace("/etc/certs", str(tmp_path)), *args, **kwargs),
    )

    monkeypatch.setitem(certmanager._backend_classes, "sql", FakeBackend)
    monkeypatch.setattr(certmanager.AuthHandler, "get_merged_keys", lambda self, exp_hours: (str(jwks), str(jks)))
    monkeypatch.setattr(certmanager.AuthHandler, "create_archived_jwks", lambda *args: None)
    monkeypatch.setattr(certmanager, "encode_jks", lambda manager: "b64")
    monkeypatch.setattr(certmanager, "has_ext_jwks_uri", lambda *args: False)
    monkeypatch.setenv("CN_CONTAINER_METADATA", "kubernetes")

    auth_handler = certmanager.AuthHandler(FakeManager(), False, **{"privkey-push-delay": 60})
    auth_handler.meta_client = FakeMeta(["auth-a", "auth-b", "auth-c"])
    for pod in auth_handler.meta_client.pods:
        auth_handler.meta_client.files[pod][str(jks)] = b"old-jks"
    auth_handler.jks_path = str(jks)
    return auth_handler


def _add_not_ready_pod(meta, handler, name):
    """a Running pod that holds the previous JKS but is not listed because it is not Ready"""
    meta.pods.append(name)
    meta.not_ready.add(name)
    meta.files[name] = {handler.jks_path: b"old-jks"}


def _skip_sleeps(monkeypatch):
    """replace time.sleep with a no-op so the push delay and the retry waits take no time"""
    monkeypatch.setattr(certmanager.time, "sleep", lambda seconds: None)


def _make_restore_do_nothing(meta, jks):
    """the restore command `cp <jks>.backup <jks>` returns normally on every container but copies nothing"""
    original_exec_cmd = meta.exec_cmd

    def exec_cmd(container, cmd):
        if cmd == f"cp {jks}.backup {jks}":
            return ""
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd


def _sleep_hook(monkeypatch, on_sleep):
    """replace time.sleep: call on_sleep() instead of waiting"""
    monkeypatch.setattr(certmanager.time, "sleep", lambda seconds: on_sleep())


PUSH_DELAY_SLEEP = 1   # the first time.sleep of patch() is the push delay
FIRST_RETRY_SLEEP = 2  # the second one is the wait before round 2


def _record_sleeps(monkeypatch, on_sleep=None):
    """replace time.sleep: return the list of waits; call on_sleep(sleep_number) at each one (1 = push delay)"""
    sleeps = []

    def sleep(seconds):
        sleeps.append(seconds)
        if on_sleep:
            on_sleep(len(sleeps))

    monkeypatch.setattr(certmanager.time, "sleep", sleep)
    return sleeps


def _not_verified_summaries(caplog):
    """the final warning that names the containers without the verified new JKS"""
    return [record.getMessage() for record in caplog.records if "not verified on" in record.getMessage()]


def _handler_with_options(handler, options):
    """a handler set up like the fixture's, but created with the given --opts values"""
    other_handler = certmanager.AuthHandler(FakeManager(), False, **options)
    other_handler.meta_client = handler.meta_client
    other_handler.jks_path = handler.jks_path
    return other_handler


def test_no_disruption_pushes_all_and_reloads(handler, monkeypatch):
    _skip_sleeps(monkeypatch)
    handler.patch()
    assert handler.meta_client.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert handler.backend.revisions == [11, 12]


def test_pod_removed_during_delay_does_not_abort_push(handler, monkeypatch):
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.pods.remove("auth-b"))

    handler.patch()  # must not raise

    assert meta.jks_pushed == ["auth-a", "auth-c"]
    # second revision bump (reload private keys) must still happen
    assert handler.backend.revisions == [11, 12]


def test_pod_dies_after_refresh_does_not_abort_push(handler, monkeypatch):
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.fail_on.add("auth-a"))  # first pod in list is unreachable

    handler.patch()  # must not raise

    assert meta.jks_pushed == ["auth-b", "auth-c"]
    assert handler.backend.revisions == [11, 12]


def test_pod_added_during_delay_receives_jks(handler, monkeypatch):
    meta = handler.meta_client

    def add_pod():
        meta.pods.append("auth-d")
        meta.files["auth-d"] = {handler.jks_path: b"old-jks"}  # a new pod writes the JKS from the secret at start

    _sleep_hook(monkeypatch, add_pod)

    handler.patch()

    assert "auth-d" in meta.jks_pushed


def test_all_pods_gone_skips_reload(handler, monkeypatch):
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.pods.clear())

    handler.patch()

    assert meta.jks_pushed == []
    assert handler.backend.revisions == [11]  # only the initial bump


def test_silent_copy_failure_is_not_counted_and_reload_still_runs(handler, monkeypatch):
    """remote tar exits nonzero but copy_to_container returns normally"""
    meta = handler.meta_client
    meta.silent_fail.add("auth-b")
    _skip_sleeps(monkeypatch)

    handler.patch()

    assert meta.jks_pushed == ["auth-a", "auth-c"]
    assert handler.backend.revisions == [11, 12]
    # failed pod still has its previous, loadable keystore
    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"


def test_only_silent_failures_skips_reload(handler, monkeypatch, caplog):
    meta = handler.meta_client
    meta.silent_fail.update(meta.pods)
    _skip_sleeps(monkeypatch)

    with caplog.at_level("WARNING"):
        handler.patch()

    messages = [record.getMessage() for record in caplog.records]
    assert meta.jks_pushed == []
    assert handler.backend.revisions == [11]  # only the initial bump
    assert any(message.startswith("no auth-server container verified the new") for message in messages)


@pytest.mark.parametrize(
    "docker_style",
    [pytest.param(False, id="kubernetes-result"), pytest.param(True, id="docker-result")],
)
def test_partial_copy_is_restored_from_backup(handler, monkeypatch, docker_style):
    meta = handler.meta_client
    meta.docker_style = docker_style
    meta.partial.add("auth-a")
    _skip_sleeps(monkeypatch)

    handler.patch()

    # truncated keystore must not be left on the container
    assert meta.files["auth-a"][handler.jks_path] == b"old-jks"
    # healthy containers hold the full new keystore
    assert meta.files["auth-b"][handler.jks_path] == b"jks"
    assert handler.backend.revisions == [11, 12]


def test_docker_style_exec_result_is_handled(handler, monkeypatch):
    """DockerMeta.exec_cmd returns ExecResult(exit_code, bytes), not a string"""
    meta = handler.meta_client
    meta.docker_style = True
    _skip_sleeps(monkeypatch)

    handler.patch()

    assert meta.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert handler.backend.revisions == [11, 12]


def test_failed_restore_is_logged_as_error(handler, monkeypatch, caplog):
    meta = handler.meta_client
    meta.partial.add("auth-a")
    _make_restore_do_nothing(meta, handler.jks_path)
    _skip_sleeps(monkeypatch)

    with caplog.at_level("ERROR"):
        handler.patch()

    assert any("unable to verify restore" in record.message for record in caplog.records)
    assert handler.backend.revisions == [11, 12]


def test_summary_for_a_container_whose_restore_could_not_be_verified_has_the_restore_caveat(handler, monkeypatch, caplog):
    """the final warning names the container and says that a container whose restore failed is the exception to
    "keeps its previous keys" """
    meta = handler.meta_client
    handler.privkey_push_retries = 0
    meta.partial.add("auth-a")
    _make_restore_do_nothing(meta, handler.jks_path)
    _skip_sleeps(monkeypatch)

    with caplog.at_level("WARNING"):
        handler.patch()

    summaries = _not_verified_summaries(caplog)
    assert len(summaries) == 1
    assert "auth-a" in summaries[0]
    assert "unless its restore failed" in summaries[0]


def test_restore_command_that_raises_is_logged_as_error_and_the_job_goes_on(handler, monkeypatch, caplog):
    """the restore command itself raises on a container with a partial copy: the error is logged with the reason,
    the job does not raise, and the reload still runs because the other containers verified the new JKS"""
    meta = handler.meta_client
    jks = handler.jks_path
    handler.privkey_push_retries = 0
    meta.partial.add("auth-a")
    original_exec_cmd = meta.exec_cmd

    def exec_cmd(container, cmd):
        if container == "auth-a" and cmd == f"cp {jks}.backup {jks}":
            raise GoneError("connection reset")
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd
    _skip_sleeps(monkeypatch)

    with caplog.at_level("ERROR"):
        handler.patch()  # must not raise

    errors = [record.getMessage() for record in caplog.records if record.levelname == "ERROR"]
    assert len(errors) == 1
    assert errors[0].startswith("unable to restore previous") and "auth-a" in errors[0] and "connection reset" in errors[0]
    assert handler.backend.revisions == [11, 12]


def _backup_step_raises():
    raise GoneError("connection reset")


def _backup_step_does_nothing():
    return ""


@pytest.mark.parametrize(
    "backup_step",
    [pytest.param(_backup_step_raises, id="backup-raises"), pytest.param(_backup_step_does_nothing, id="backup-does-nothing")],
)
def test_stale_backup_is_not_restored_when_backup_step_fails(handler, monkeypatch, backup_step):
    """the backup step raises or silently does nothing (the .backup keeps stale content) -> the copy never starts and
    an old .backup must not replace the current JKS"""
    meta = handler.meta_client
    jks = handler.jks_path
    meta.files["auth-a"][jks + ".backup"] = b"stale-from-earlier-rotation"
    original_exec_cmd = meta.exec_cmd

    def exec_cmd(container, cmd):
        if container == "auth-a" and cmd == f"cp {jks} {jks}.backup":
            return backup_step()
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd
    _skip_sleeps(monkeypatch)

    handler.patch()

    assert meta.files["auth-a"][jks] == b"old-jks"
    assert "auth-a" not in meta.jks_pushed
    assert meta.jks_pushed == ["auth-b", "auth-c"]
    assert handler.backend.revisions == [11, 12]


def test_failed_pod_is_retried_and_receives_jks(handler, monkeypatch):
    """transient failure on one container: job must not fail, it must get the new JKS on a later round"""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 2
    sleeps = _record_sleeps(monkeypatch)

    handler.patch()

    assert meta.files["auth-b"][handler.jks_path] == b"jks"
    assert meta.jks_pushed.count("auth-a") == 1 and meta.jks_pushed.count("auth-c") == 1
    assert meta.jks_pushed.count("auth-b") == 1
    # reload happens once, after every pod was updated
    assert handler.backend.revisions == [11, 12]
    # delay + two retry waits of the default 10 seconds
    assert sleeps == [60, 10, 10]


def test_pod_that_never_recovers_does_not_fail_job(handler, monkeypatch):
    meta = handler.meta_client
    meta.flaky["auth-b"] = 999
    sleeps = _record_sleeps(monkeypatch)

    handler.patch()  # must not raise

    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"
    assert meta.files["auth-a"][handler.jks_path] == b"jks"
    assert meta.files["auth-c"][handler.jks_path] == b"jks"
    assert handler.backend.revisions == [11, 12]
    # bounded: delay + one wait before each of the default 3 extra rounds
    assert sleeps == [60, 10, 10, 10]


def test_pod_that_never_recovers_is_named_once_with_the_impact(handler, monkeypatch, caplog):
    """one final warning names the container and says what it cannot do until it is restarted; a container that recovered in
    a later round is not named"""
    handler.meta_client.flaky["auth-b"] = 999
    handler.meta_client.flaky["auth-c"] = 1
    _skip_sleeps(monkeypatch)

    with caplog.at_level("WARNING"):
        handler.patch()

    summaries = _not_verified_summaries(caplog)
    assert len(summaries) == 1
    assert "auth-b" in summaries[0] and "auth-c" not in summaries[0]
    assert "cannot sign with an algorithm added in this rotation" in summaries[0]


def test_pod_replaced_during_retry_is_pushed(handler, monkeypatch):
    """failing pod is evicted between rounds and its replacement becomes Ready"""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 999

    def replace_auth_b(sleep_number):
        if sleep_number == FIRST_RETRY_SLEEP:
            meta.pods.remove("auth-b")
            meta.pods.append("auth-d")
            meta.files["auth-d"] = {handler.jks_path: b"old-jks"}

    _record_sleeps(monkeypatch, replace_auth_b)

    handler.patch()

    assert "auth-d" in meta.jks_pushed
    assert handler.backend.revisions == [11, 12]


def test_retries_can_be_disabled(handler, monkeypatch):
    handler.privkey_push_retries = 0
    meta = handler.meta_client
    meta.flaky["auth-b"] = 1
    sleeps = _record_sleeps(monkeypatch)

    handler.patch()

    assert sleeps == [60]
    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"
    assert handler.backend.revisions == [11, 12]


def test_failed_sha256sum_errors_do_not_verify_backup(handler, monkeypatch):
    """both sha256sum calls fail with the same error text -> they must not compare equal / verify the backup"""
    meta = handler.meta_client
    jks = handler.jks_path
    original_exec_cmd = meta.exec_cmd

    def exec_cmd(container, cmd):
        if container == "auth-a" and cmd.startswith("sha256sum"):
            return f"sha256sum: {cmd.split()[1]}: Permission denied"  # error text, not a digest
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd
    _skip_sleeps(monkeypatch)

    handler.patch()

    assert "auth-a" not in meta.jks_pushed
    assert meta.files["auth-a"][jks] == b"old-jks"
    assert meta.jks_pushed == ["auth-b", "auth-c"]
    assert handler.backend.revisions == [11, 12]


def test_retry_keeps_original_backup_after_failed_copy_and_restore(handler, monkeypatch):
    """partial copy + failed restore on every round: the partial JKS must never replace the original backup"""
    meta = handler.meta_client
    jks = handler.jks_path
    meta.partial.add("auth-a")
    original_exec_cmd = meta.exec_cmd
    backups = []

    def exec_cmd(container, cmd):
        if container == "auth-a" and cmd == f"cp {jks} {jks}.backup":
            backups.append(meta.files["auth-a"][jks])
        if container == "auth-a" and cmd == f"cp {jks}.backup {jks}":
            return ""  # restore silently does nothing, partial JKS stays on the pod
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd
    _skip_sleeps(monkeypatch)

    handler.patch()

    # the backup was taken once, from the original keystore, and still holds it after all rounds
    assert backups == [b"old-jks"]
    assert meta.files["auth-a"][jks + ".backup"] == b"old-jks"
    assert handler.backend.revisions == [11, 12]


def test_changed_original_backup_stops_the_push_to_that_pod(handler, monkeypatch, caplog):
    """auth-a fails once after its backup was verified, then its backup changes: no later round copies over it"""
    meta = handler.meta_client
    jks = handler.jks_path
    meta.flaky["auth-a"] = 1

    def change_backup_of_auth_a(sleep_number):
        if sleep_number == FIRST_RETRY_SLEEP:
            meta.files["auth-a"][jks + ".backup"] = b"changed"

    _record_sleeps(monkeypatch, change_backup_of_auth_a)

    with caplog.at_level("WARNING"):
        handler.patch()

    failures_of_auth_a = [record.getMessage() for record in caplog.records if "auth-a" in record.getMessage()]
    assert "auth-a" not in meta.jks_pushed
    assert meta.files["auth-a"][jks] == b"old-jks"
    assert any("original backup of JKS is missing or changed" in message for message in failures_of_auth_a)


def test_reload_runs_when_the_only_updated_pod_was_evicted_afterwards(handler, monkeypatch):
    """auth-a verifies in round 1 and is evicted before round 2; the reload still runs (its replacement loads the
    new JKS from the secret, the pods that failed keep reading their previous JKS)"""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 999
    meta.flaky["auth-c"] = 999

    def evict_auth_a(sleep_number):
        if sleep_number == FIRST_RETRY_SLEEP:
            meta.pods.remove("auth-a")

    _record_sleeps(monkeypatch, evict_auth_a)

    handler.patch()

    assert meta.jks_pushed == ["auth-a"]
    assert handler.backend.revisions == [11, 12]


UNREADABLE_CHECKSUMS = [
    pytest.param("sha256sum: error: Permission denied", id="kubernetes-error-text"),
    pytest.param(DockerResult(1, b""), id="docker-exit-code-1"),
]


@pytest.mark.parametrize("unreadable", UNREADABLE_CHECKSUMS)
def test_unreadable_checksums_after_restore_are_not_counted_as_verified(handler, monkeypatch, caplog, unreadable):
    """restore ran, but neither checksum can be read afterwards -> two empty checksums must not count as a match"""
    meta = handler.meta_client
    meta.docker_style = isinstance(unreadable, DockerResult)
    jks = handler.jks_path
    handler.privkey_push_retries = 0
    meta.partial.add("auth-a")
    original_exec_cmd = meta.exec_cmd
    restore_ran = False

    def exec_cmd(container, cmd):
        nonlocal restore_ran
        if container == "auth-a" and cmd == f"cp {jks}.backup {jks}":
            restore_ran = True
        elif container == "auth-a" and restore_ran and cmd.startswith("sha256sum"):
            return unreadable
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd
    _skip_sleeps(monkeypatch)

    with caplog.at_level("WARNING"):
        handler.patch()

    messages = [record.getMessage() for record in caplog.records]
    assert restore_ran
    assert any("unable to verify restore" in message for message in messages)
    assert not any(message.startswith("restored previous") for message in messages)


@pytest.mark.parametrize("unreadable", UNREADABLE_CHECKSUMS)
def test_unreadable_remote_checksum_after_copy_is_a_failure_and_restores_the_previous_jks(
    handler, monkeypatch, caplog, unreadable
):
    """the copy returns normally but the checksum of the new JKS cannot be read -> the container is not counted as
    updated, the previous JKS is restored, and the log says checksum mismatch with remote=n/a"""
    meta = handler.meta_client
    meta.docker_style = isinstance(unreadable, DockerResult)
    jks = handler.jks_path
    handler.privkey_push_retries = 0
    original_exec_cmd = meta.exec_cmd

    def exec_cmd(container, cmd):
        if container == "auth-a" and cmd == f"sha256sum {jks}" and meta.files["auth-a"][jks] == b"jks":
            return unreadable  # only while the new JKS is on the container
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd
    _skip_sleeps(monkeypatch)

    with caplog.at_level("WARNING"):
        handler.patch()

    messages = [record.getMessage() for record in caplog.records]
    summaries = _not_verified_summaries(caplog)
    assert len(summaries) == 1 and "auth-a" in summaries[0] and "auth-b" not in summaries[0]
    assert meta.files["auth-a"][jks] == b"old-jks"
    assert any("checksum mismatch" in message and "remote=n/a" in message and "auth-a" in message for message in messages)
    assert any(message.startswith("restored previous") and "auth-a" in message for message in messages)
    assert handler.backend.revisions == [11, 12]


def test_verified_restore_is_logged_as_warning_not_error(handler, monkeypatch, caplog):
    """partial copy, restore succeeds -> the restore is reported as verified, not as an unverified keystore"""
    meta = handler.meta_client
    handler.privkey_push_retries = 0
    meta.partial.add("auth-a")
    _skip_sleeps(monkeypatch)

    with caplog.at_level("WARNING"):
        handler.patch()

    messages = [record.getMessage() for record in caplog.records]
    assert any(message.startswith("restored previous") and "auth-a" in message for message in messages)
    assert not any("unable to verify restore" in message for message in messages)


def test_pods_are_not_listed_again_after_a_round_without_failures(handler, monkeypatch):
    """one listing before the delay and one for round 1; the reload decision needs no further listing"""
    meta = handler.meta_client
    listings = []
    meta.on_list = lambda: listings.append("listed")
    _skip_sleeps(monkeypatch)

    handler.patch()

    assert len(listings) == 2
    assert handler.backend.revisions == [11, 12]


def test_pod_unlisted_before_delay_and_not_ready_at_round_one_is_not_pushed_and_not_listed_again(handler, monkeypatch):
    """pins the remaining known limitation: a pod that is not listed before the delay and not Ready at the
    listing of round 1 is not pushed; no round follows, so it is never listed again (two listings in all)"""
    meta = handler.meta_client
    listings = []
    meta.on_list = lambda: listings.append("listed")
    _sleep_hook(monkeypatch, lambda: _add_not_ready_pod(meta, handler, "auth-d"))

    handler.patch()

    assert len(listings) == 2
    assert meta.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert meta.files["auth-d"][handler.jks_path] == b"old-jks"
    assert handler.backend.revisions == [11, 12]


def test_pod_not_ready_after_delay_still_receives_jks(handler, monkeypatch, caplog):
    """a pod listed before the delay that is not Ready after it is still reachable: it gets the new JKS, as on main"""
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.not_ready.add("auth-c"))

    with caplog.at_level("INFO"):
        handler.patch()

    messages = [record.getMessage() for record in caplog.records]
    assert meta.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert meta.files["auth-c"][handler.jks_path] == b"jks"
    assert handler.backend.revisions == [11, 12]
    assert any("auth-c" in message and "listed before the delay but not now" in message for message in messages)


def test_pod_gone_after_delay_is_tried_once_and_never_retried(handler, monkeypatch, caplog):
    """a container listed before the delay is gone after it: one failed attempt, no retry round spent on it, named once
    in the final warning"""
    meta = handler.meta_client
    meta.flaky["auth-c"] = 1  # another pod needs exactly one retry round

    def remove_auth_b(sleep_number):
        if sleep_number == PUSH_DELAY_SLEEP:
            meta.pods.remove("auth-b")

    sleeps = _record_sleeps(monkeypatch, remove_auth_b)

    with caplog.at_level("WARNING"):
        handler.patch()

    failures_of_gone_pod = [
        record.getMessage() for record in caplog.records
        if record.getMessage().startswith("unable to push") and "auth-b" in record.getMessage()
    ]
    assert len(failures_of_gone_pod) == 1
    assert sleeps == [60, 10]
    assert meta.jks_pushed == ["auth-a", "auth-c"]
    summaries = _not_verified_summaries(caplog)
    assert len(summaries) == 1 and "auth-b" in summaries[0]
    assert handler.backend.revisions == [11, 12]


def test_reload_runs_when_only_pods_not_listed_after_the_delay_were_updated(handler, monkeypatch, caplog):
    """no pod is Ready after the delay (for example the only replica is restarting its probes): each still gets
    the new JKS and the reload runs, as on main, so the pods load it"""
    meta = handler.meta_client
    _sleep_hook(monkeypatch, lambda: meta.not_ready.update(meta.pods))

    with caplog.at_level("WARNING"):
        handler.patch()

    messages = [record.getMessage() for record in caplog.records]
    assert meta.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert handler.backend.revisions == [11, 12]
    assert not any("skipping reload" in message for message in messages)


def _record_sleeps_with_auth_c_not_ready_in_round_one(handler, monkeypatch):
    """auth-c is not Ready after the delay (so not listed in round 1) and Ready again at the listing of round 2;
    auth-b fails once, so round 2 happens. Returns the recorded sleeps."""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 1

    def toggle_auth_c(sleep_number):
        if sleep_number == PUSH_DELAY_SLEEP:
            meta.not_ready.add("auth-c")
        elif sleep_number == FIRST_RETRY_SLEEP:
            meta.not_ready.discard("auth-c")

    return _record_sleeps(monkeypatch, toggle_auth_c)


def test_unlisted_pod_that_failed_is_pushed_when_listed_in_a_retry_round(handler, monkeypatch):
    """the round-1 attempt on the not-Ready auth-c fails after its backup was verified; round 2 lists it and
    completes the push"""
    handler.meta_client.flaky["auth-c"] = 1
    sleeps = _record_sleeps_with_auth_c_not_ready_in_round_one(handler, monkeypatch)

    handler.patch()

    assert handler.meta_client.files["auth-c"][handler.jks_path] == b"jks"
    assert sleeps == [60, 10]
    assert handler.backend.revisions == [11, 12]


def test_unlisted_pod_pushed_again_in_a_retry_round_reuses_its_original_backup(handler, monkeypatch):
    """the second push to auth-c reuses the backup of its first attempt instead of taking a new one"""
    meta = handler.meta_client
    jks = handler.jks_path
    meta.flaky["auth-c"] = 1
    _record_sleeps_with_auth_c_not_ready_in_round_one(handler, monkeypatch)
    backups_of_auth_c = []
    original_exec_cmd = meta.exec_cmd

    def exec_cmd(container, cmd):
        if container == "auth-c" and cmd == f"cp {jks} {jks}.backup":
            backups_of_auth_c.append(cmd)
        return original_exec_cmd(container, cmd)

    meta.exec_cmd = exec_cmd

    handler.patch()

    assert len(backups_of_auth_c) == 1


def test_unlisted_pod_updated_in_round_one_is_not_pushed_again_when_listed_later(handler, monkeypatch):
    """auth-c is updated in round 1 while not listed; round 2 (forced by auth-b) lists it but must not push again"""
    sleeps = _record_sleeps_with_auth_c_not_ready_in_round_one(handler, monkeypatch)

    handler.patch()

    assert handler.meta_client.jks_pushed.count("auth-c") == 1
    assert sleeps == [60, 10]


def test_failed_pod_no_longer_listed_is_not_retried_and_is_named_at_the_end(handler, monkeypatch, caplog):
    """auth-b fails in round 1 and is not Ready at the listing of round 2: no second attempt, final warning names it"""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 1  # a second attempt would succeed

    def make_auth_b_not_ready(sleep_number):
        if sleep_number == FIRST_RETRY_SLEEP:
            meta.not_ready.add("auth-b")

    sleeps = _record_sleeps(monkeypatch, make_auth_b_not_ready)

    with caplog.at_level("WARNING"):
        handler.patch()

    assert "auth-b" not in meta.jks_pushed
    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"
    assert sleeps == [60, 10]
    summaries = _not_verified_summaries(caplog)
    assert len(summaries) == 1 and "auth-b" in summaries[0]
    assert handler.backend.revisions == [11, 12]


LISTING_ERRORS = [
    pytest.param(GoneError("connection refused"), id="api-error"),
    pytest.param(AttributeError("'NoneType' object has no attribute 'items'"), id="attribute-error"),
]


@pytest.mark.parametrize("listing_error", LISTING_ERRORS)
def test_listing_failure_after_delay_still_pushes_every_known_pod(handler, monkeypatch, caplog, listing_error):
    """every listing after the delay raises: the job goes on, every container of the old list gets the new JKS once"""
    meta = handler.meta_client

    def fail_listing():
        raise listing_error

    def break_listing_after_delay(sleep_number):
        if sleep_number == PUSH_DELAY_SLEEP:
            meta.on_list = fail_listing

    _record_sleeps(monkeypatch, break_listing_after_delay)

    with caplog.at_level("WARNING"):
        handler.patch()  # must not raise

    messages = [record.getMessage() for record in caplog.records]
    assert meta.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert handler.backend.revisions == [11, 12]
    assert any(message.startswith("unable to list auth-server containers") for message in messages)


def test_listing_failure_in_retry_round_ends_the_retries(handler, monkeypatch, caplog):
    """auth-b fails in round 1 and the listing of round 2 raises: no further round, auth-b is named, reload runs"""
    meta = handler.meta_client
    meta.flaky["auth-b"] = 1

    def fail_listing():
        raise GoneError("connection refused")

    def break_listing_before_round_two(sleep_number):
        if sleep_number == FIRST_RETRY_SLEEP:
            meta.on_list = fail_listing

    sleeps = _record_sleeps(monkeypatch, break_listing_before_round_two)

    with caplog.at_level("WARNING"):
        handler.patch()  # must not raise

    messages = [record.getMessage() for record in caplog.records]
    assert sleeps == [60, 10]
    assert meta.files["auth-b"][handler.jks_path] == b"old-jks"
    assert any(message.startswith("unable to list auth-server containers") for message in messages)
    summaries = _not_verified_summaries(caplog)
    assert len(summaries) == 1 and "auth-b" in summaries[0]
    assert handler.backend.revisions == [11, 12]


def test_listing_failure_before_delay_stops_before_keys_are_rotated(handler, monkeypatch):
    """the listing before the delay stays unguarded, as on main: the job stops before persistence or secrets change"""
    meta = handler.meta_client

    def fail_listing():
        raise GoneError("connection refused")

    meta.on_list = fail_listing
    _skip_sleeps(monkeypatch)

    with pytest.raises(GoneError):
        handler.patch()

    assert handler.backend.revisions == []
    assert meta.jks_pushed == []


RETRY_OPTIONS = ["privkey-push-retries", "privkey-push-retry-interval"]


@pytest.mark.parametrize("option", RETRY_OPTIONS)
def test_invalid_retry_option_stops_patch_before_keys_are_rotated(handler, monkeypatch, caplog, option):
    """with a push delay, a value that is not an integer stops the job with an error before anything changes"""
    invalid_handler = _handler_with_options(handler, {"privkey-push-delay": "60", option: "three"})
    _skip_sleeps(monkeypatch)

    with caplog.at_level("ERROR"), pytest.raises(SystemExit) as exit_info:
        invalid_handler.patch()

    assert exit_info.value.code == 1
    assert invalid_handler.backend.revisions == []
    assert handler.meta_client.jks_pushed == []
    errors = [record.getMessage() for record in caplog.records if record.levelname == "ERROR"]
    assert any("privkey-push-retries" in message and "privkey-push-retry-interval" in message for message in errors)


@pytest.mark.parametrize("option", RETRY_OPTIONS)
def test_invalid_retry_option_is_ignored_without_push_delay(handler, monkeypatch, option):
    """the retry options are used only with a push delay; without one, an invalid value changes nothing"""
    other_handler = _handler_with_options(handler, {option: "three"})

    other_handler.patch()  # must not raise

    assert handler.meta_client.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert other_handler.backend.revisions == [11]  # no delay: one bump, keys pushed before it


# each retry option with its upper bound
BOUNDED_RETRY_OPTIONS = [
    pytest.param("privkey-push-retries", 100, id="retries"),
    pytest.param("privkey-push-retry-interval", 86400, id="interval"),
]
# the same, with the waits of a job that has one failing pod while that option is at its bound (the interval defaults to 10)
OPTIONS_AT_THEIR_MAXIMUM = [
    pytest.param("privkey-push-retries", 100, [60, 10], id="retries"),
    pytest.param("privkey-push-retry-interval", 86400, [60, 86400], id="interval"),
]


@pytest.mark.parametrize("option, maximum", BOUNDED_RETRY_OPTIONS)
def test_retry_option_above_the_maximum_stops_patch_before_keys_are_rotated(handler, monkeypatch, caplog, option, maximum):
    """with a push delay, a retry option one above its maximum stops the job with an error before anything changes"""
    too_big_handler = _handler_with_options(handler, {"privkey-push-delay": "60", option: str(maximum + 1)})
    _skip_sleeps(monkeypatch)

    with caplog.at_level("ERROR"), pytest.raises(SystemExit) as exit_info:
        too_big_handler.patch()

    errors = [record.getMessage() for record in caplog.records if record.levelname == "ERROR"]
    assert exit_info.value.code == 1
    assert too_big_handler.backend.revisions == []
    assert handler.meta_client.jks_pushed == []
    assert len(errors) == 1 and option in errors[0] and str(maximum + 1) in errors[0]


def test_both_retry_options_above_their_maximum_log_both_errors_retries_first(handler, monkeypatch, caplog):
    """when both options are too big, one run reports both problems (retries, then interval) and changes nothing"""
    options = {"privkey-push-delay": "60", "privkey-push-retries": "101", "privkey-push-retry-interval": "86401"}
    too_big_handler = _handler_with_options(handler, options)
    _skip_sleeps(monkeypatch)

    with caplog.at_level("ERROR"), pytest.raises(SystemExit) as exit_info:
        too_big_handler.patch()

    errors = [record.getMessage() for record in caplog.records if record.levelname == "ERROR"]
    assert exit_info.value.code == 1
    assert too_big_handler.backend.revisions == []
    assert handler.meta_client.jks_pushed == []
    assert len(errors) == 2
    assert "privkey-push-retries" in errors[0] and "101" in errors[0]
    assert "privkey-push-retry-interval" in errors[1] and "86401" in errors[1]


@pytest.mark.parametrize("option, maximum, sleeps_at_maximum", OPTIONS_AT_THEIR_MAXIMUM)
def test_retry_option_of_exactly_the_maximum_is_accepted(handler, monkeypatch, option, maximum, sleeps_at_maximum):
    """a retry option equal to its maximum is allowed: the job runs (auth-b fails once, so the extra round runs)"""
    maximum_handler = _handler_with_options(handler, {"privkey-push-delay": "60", option: str(maximum)})
    handler.meta_client.flaky["auth-b"] = 1
    sleeps = _record_sleeps(monkeypatch)

    maximum_handler.patch()

    assert sleeps == sleeps_at_maximum
    assert handler.meta_client.jks_pushed == ["auth-a", "auth-c", "auth-b"]
    assert maximum_handler.backend.revisions == [11, 12]


@pytest.mark.parametrize("option, maximum", BOUNDED_RETRY_OPTIONS)
def test_retry_option_above_the_maximum_is_ignored_without_push_delay(handler, monkeypatch, option, maximum):
    """the retry options are used only with a push delay; without one, a value above the maximum changes nothing"""
    other_handler = _handler_with_options(handler, {option: str(maximum + 1)})

    other_handler.patch()  # must not raise

    assert handler.meta_client.jks_pushed == ["auth-a", "auth-b", "auth-c"]
    assert other_handler.backend.revisions == [11]


@pytest.mark.parametrize(
    ("options", "expected_sleeps", "expected_pushed"),
    [
        pytest.param({"privkey-push-retries": "-1"}, [60], ["auth-a", "auth-c"], id="retries"),
        pytest.param(
            {"privkey-push-retries": "1", "privkey-push-retry-interval": "-5"}, [60, 0], ["auth-a", "auth-c", "auth-b"],
            id="retry-interval",
        ),
    ],
)
def test_negative_retry_options_count_as_zero(handler, monkeypatch, options, expected_sleeps, expected_pushed):
    """a negative number of retries means no extra round; a negative interval means no wait before the extra round
    (auth-b fails once, so it is pushed only if an extra round runs)"""
    negative_handler = _handler_with_options(handler, {"privkey-push-delay": "60", **options})
    handler.meta_client.flaky["auth-b"] = 1
    sleeps = _record_sleeps(monkeypatch)

    negative_handler.patch()

    assert sleeps == expected_sleeps
    assert handler.meta_client.jks_pushed == expected_pushed
