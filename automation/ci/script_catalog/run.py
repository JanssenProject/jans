#!/usr/bin/env python3
"""Load every docs/script-catalog script into a live AIO and exercise the security-relevant ones.

Run on the CI VM after the Java integration suites (it enables global scripts that would disturb
them). Writes a JUnit report that the integration workflow's summary and gate already consume.

Env: JANS_FQDN, CN_CONFIG_API_TEST_CLIENT_ID, CN_CONFIG_API_TEST_CLIENT_SECRET,
     AIO_CA_FILE (default automation/jans-aio-demo/templates/ca.crt), REPORT_DIR (default test-reports)
"""
import os
import re
import secrets
import sys
import time
import traceback
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import manifest  # noqa: E402
from aio import Aio, HttpError, jwt_claims  # noqa: E402
from catalog import REPO, discover  # noqa: E402

RELOAD_INTERVAL = 30  # CustomScriptManager.DEFAULT_INTERVAL
SENTINEL_TYPE = {"jans-auth": "introspection", "jans-scim": "scim", "jans-config-api": "config_api_auth",
                 "jans-fido2": "fido2_extension"}
SENTINEL_BODY = "this is not valid python (\n"
INIT_FAILED = "Using default external type class"
TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange"


def log(msg):
    print(f"[script-catalog] {msg}", flush=True)


class Report:
    def __init__(self):
        self.cases = []

    def add(self, cls, name, status, message="", detail="", started=None):
        self.cases.append((cls, name, status, message, detail, time.time() - started if started else 0))
        log(f"{status.upper():5} {cls} :: {name}" + (f" -- {message}" if message else ""))

    def write(self, path):
        suite = ET.Element("testsuite", name="script-catalog", tests=str(len(self.cases)),
                           failures=str(sum(c[2] == "fail" for c in self.cases)),
                           skipped=str(sum(c[2] == "skip" for c in self.cases)))
        for cls, name, status, message, detail, secs in self.cases:
            tc = ET.SubElement(suite, "testcase", classname=cls, name=name, time=f"{secs:.1f}")
            if status == "fail":
                ET.SubElement(tc, "failure", message=message[:500]).text = detail or message
            elif status == "skip":
                ET.SubElement(tc, "skipped", message=message[:500])
        path.parent.mkdir(parents=True, exist_ok=True)
        ET.ElementTree(suite).write(path, encoding="utf-8", xml_declaration=True)


class Run:
    def __init__(self, aio, report):
        self.aio, self.report = aio, report
        self.tag = secrets.token_hex(3)
        self.scripts, self.users = [], []
        self.seq = 0

    def name(self, label):
        self.seq += 1
        return re.sub(r"[^a-zA-Z0-9_.-]", "-", f"ci-{self.tag}-{self.seq}-{label}")[:60]

    def create(self, script, label=None, body=None, script_type=None, language=None, props=None):
        created = self.aio.create_script(
            self.name(label or script.path.stem), script_type or script.script_type,
            language or script.language, body if body is not None else script.text(),
            props=props if props is not None else manifest.config_properties(script.rel, self.aio.fqdn))
        self.scripts.append(created["inum"])
        return created

    def delete(self, inums):
        for inum in inums:
            try:
                self.aio.delete_script(inum)
            except Exception as e:  # noqa: BLE001 - cleanup is best-effort
                log(f"cleanup: could not delete script {inum}: {e}")
            if inum in self.scripts:
                self.scripts.remove(inum)

    def errors(self, inums):
        out = {}
        for inum in inums:
            err = (self.aio.get_script(inum) or {}).get("scriptError") or {}
            out[inum] = err.get("stackTrace")
        return out

    def await_reload(self, services, timeout=360):
        """Block until each service has run a reload pass over everything created so far.

        A broken sentinel per service reports a load error once that service's reload pass reaches
        it; the remaining scripts are then read until two snapshots one reload apart agree, so slow
        init() calls later in the same pass are not mistaken for clean loads.
        """
        sentinels = {}
        for svc in services:
            sentinels[svc] = self.aio.create_script(self.name("sentinel"), SENTINEL_TYPE[svc], "python",
                                                    SENTINEL_BODY)["inum"]
            self.scripts.append(sentinels[svc])
        deadline, pending = time.time() + timeout, dict(sentinels)
        while pending and time.time() < deadline:
            time.sleep(10)
            for svc, inum in list(pending.items()):
                if self.errors([inum])[inum]:
                    del pending[svc]
        self.delete(sentinels.values())
        return set(pending)

    def settle(self, inums, rounds=4):
        snap = self.errors(inums)
        for _ in range(rounds):
            time.sleep(RELOAD_INTERVAL + 5)
            nxt = self.errors(inums)
            if nxt == snap:
                return nxt
            snap = nxt
        return snap


def classify(script, trace):
    if not trace:
        return "pass", ""
    first = trace.strip().splitlines()[0] if trace.strip() else trace
    if INIT_FAILED in first:
        if script.rel in manifest.INIT_REQUIRED:
            return "fail", "init() failed or returned false"
        return "skip", "init() failed or returned false; needs external configuration the AIO lacks"
    return "fail", "script failed to load: " + first[:300]


def check_installed(run, scripts):
    """The AIO under test must carry this checkout's catalog, not the loader image's pinned copy."""
    t = time.time()
    if os.environ.get("AIO_IMAGE"):
        run.report.add("ScriptCatalog.install", "installed-scripts-match-checkout", "skip",
                       "prebuilt AIO image under test; its catalog comes from that image")
        return
    ldif = (REPO / "jans-linux-setup/jans_setup/templates/scripts.ldif").read_text(encoding="utf-8")
    placeholders = set(re.findall(r"%\((\w+)\)s", ldif))
    expected = {s.key: s for s in scripts if s.key in placeholders}
    bodies, start = set(), 0
    while True:
        entries = run.aio.api("GET", f"/api/v1/config/scripts?limit=100&startIndex={start}").get("entries") or []
        bodies.update((e.get("script") or "").strip() for e in entries)
        start += len(entries)
        if len(entries) < 100:
            break
    missing = sorted(s.rel for s in expected.values() if s.text().strip() not in bodies)
    if missing:
        run.report.add("ScriptCatalog.install", "installed-scripts-match-checkout", "fail",
                       f"{len(missing)} installed script(s) differ from this checkout", "\n".join(missing), t)
    else:
        run.report.add("ScriptCatalog.install", "installed-scripts-match-checkout", "pass", started=t)


def load_check(run, scripts, cls="ScriptCatalog.load"):
    t = time.time()
    created = {}
    for s in scripts:
        try:
            created[run.create(s)["inum"]] = s
        except HttpError as e:
            run.report.add(cls, s.rel, "fail", f"config-api rejected the script: HTTP {e.status}", e.body, t)
    services = {manifest.loader_service(s.script_type) for s in created.values()}
    stalled = run.await_reload(services)
    traces = run.settle(list(created))
    for inum, s in created.items():
        svc = manifest.loader_service(s.script_type)
        if svc in stalled:
            run.report.add(cls, s.rel, "fail", f"{svc} did not reload scripts", started=t)
            continue
        status, msg = classify(s, traces.get(inum))
        if s.rel in manifest.KNOWN_BROKEN:
            if status == "fail" and "failed to load" in msg:
                status, msg = "skip", "known broken: " + manifest.KNOWN_BROKEN[s.rel]
            else:
                log(f"{s.rel} is listed in KNOWN_BROKEN but now loads; remove it from manifest.py")
        run.report.add(cls, s.rel, status, msg, traces.get(inum) or "", t)
    run.delete(list(created))


def by_rel(scripts, rel):
    return next(s for s in scripts if s.rel == rel)


def wait_for(fn, timeout=240, step=10):
    """Poll fn until it returns a truthy value; scripts become active on the next reload pass."""
    deadline = time.time() + timeout
    while True:
        val = fn()
        if val or time.time() > deadline:
            return val
        time.sleep(step)


def behavior(run, cls, name, fn):
    t = time.time()
    try:
        fn()
        run.report.add(cls, name, "pass", started=t)
    except AssertionError as e:
        run.report.add(cls, name, "fail", str(e), traceback.format_exc(), t)
    except Exception as e:  # noqa: BLE001 - report harness/server errors as test failures
        run.report.add(cls, name, "fail", f"{type(e).__name__}: {e}", traceback.format_exc(), t)


def behavior_tests(run, scripts):
    aio = run.aio
    password = secrets.token_urlsafe(12) + "aA1!"
    basic_acr = run.create(by_rel(scripts, "person_authentication/basic-external-authenticator/BasicExternalAuthenticator.py"),
                           label="basic")["name"]
    intro_py = run.create(by_rel(scripts, "introspection/introspection-custom-parameters/introspection_custom_params.py"))
    tx = run.create(by_rel(scripts, "token_exchange/TokenExchange.java"))
    lock_acr = run.create(by_rel(scripts, "person_authentication/basic-lock-account-external-authenticator/"
                                          "BasicLockAccountExternalAuthenticator.py"), label="basic-lock")["name"]
    # an acr that is not loaded yet falls back to the default method, which would void the lock test
    stalled = run.await_reload({"jans-auth"})
    assert not stalled, "jans-auth did not reload the behavior-test scripts"
    time.sleep(RELOAD_INTERVAL + 5)

    def new_user(label):
        uid = f"ci{run.tag}{label}"
        user = aio.create_user(uid, password)
        run.users.append(user["inum"])
        return uid, user["inum"]

    uid, _ = new_user("intro")

    def login_code(client, acr, username, pwd, scope="openid profile"):
        return wait_for(lambda: aio.login(client, acr, username, pwd, scope))

    def introspection_sid():
        client = aio.register_client(introspection_script_dns=[intro_py["dn"]])
        code = login_code(client, basic_acr, uid, password)
        assert code, f"login through {basic_acr} did not return an authorization code"
        tokens = aio.code_to_tokens(client, code)
        resp = wait_for(lambda: (lambda r: r if "user_id" in r else None)(aio.introspect(client, tokens["access_token"])),
                        timeout=120)
        assert resp, "introspection script never modified the response (no user_id claim)"
        assert resp.get("user_id") == uid, f"user_id={resp.get('user_id')!r}, expected {uid!r}"
        assert "session_id" not in resp, "introspection exposes session_id (the SSO session DN)"
        sid = jwt_claims(tokens["id_token"]).get("sid")
        assert resp.get("sid") and resp.get("sid") == sid, f"introspection sid={resp.get('sid')!r}, id_token sid={sid!r}"

    def token_exchange():
        client = aio.register_client(grant_types=["authorization_code", TOKEN_EXCHANGE],
                                     scope="openid profile device_sso", token_exchange_script_dns=[tx["dn"]])
        code = login_code(client, basic_acr, uid, password, scope="openid profile device_sso")
        assert code, "login did not return an authorization code"
        tokens = aio.code_to_tokens(client, code)
        assert tokens.get("device_token"), "no device_token issued for the device_sso scope"
        good = {"grant_type": TOKEN_EXCHANGE, "audience": aio.discovery["issuer"], "scope": "openid",
                "subject_token": tokens["id_token"], "subject_token_type": "urn:ietf:params:oauth:token-type:id_token",
                "actor_token": tokens["device_token"],
                "actor_token_type": "urn:x-oath:params:oauth:token-type:device-secret"}
        r = wait_for(lambda: (lambda r: r if r.status == 200 and "key_from_script" in r.body else None)(
            aio.token_request(client, good)), timeout=120)
        assert r, "a valid native-SSO exchange never succeeded through the token-exchange script"
        bad = dict(good, subject_token="not-a-token", actor_token="not-a-device-secret")
        r = aio.token_request(client, bad)
        assert r.status in (400, 401) and "access_token" not in r.body, \
            f"forged subject/actor tokens were exchanged (HTTP {r.status}): built-in validation is skipped"

    def basic_lock():
        lock_uid, lock_inum = new_user("lock")
        client = aio.register_client()
        assert login_code(client, lock_acr, lock_uid, password), f"{lock_acr} login with the right password failed"
        for _ in range(3):
            assert not aio.login(client, lock_acr, lock_uid, "wrong-" + password), "wrong password accepted"
        status = (aio.get_user(lock_inum) or {}).get("jansStatus")
        assert status == "inactive", f"jansStatus={status!r} after 3 failed logins, expected 'inactive'"
        assert not aio.login(client, lock_acr, lock_uid, password), "locked account still logs in"

    behavior(run, "ScriptCatalog.behavior", "introspection_custom_params.py returns public sid", introspection_sid)
    behavior(run, "ScriptCatalog.behavior", "TokenExchange.java keeps built-in validation", token_exchange)
    behavior(run, "ScriptCatalog.behavior", "BasicLockAccount locks after max attempts", basic_lock)


def client_authn_test(run, scripts):
    """Last: while enabled, any request without Basic auth (config-api's own calls included) gets 401."""
    aio, t = run.aio, time.time()
    rel = "client_authn/ClientAuthn.java"
    basic_client = aio.register_client(grant_types=["authorization_code", "client_credentials"])
    post_client = aio.register_client(grant_types=["authorization_code", "client_credentials"],
                                      token_endpoint_auth_method="client_secret_post")
    cc = {"grant_type": "client_credentials"}
    run.create(by_rel(scripts, rel))
    active = wait_for(lambda: aio.token_request(post_client, cc, auth="post").status == 401, timeout=300)
    if not active:
        trace = run.errors([run.scripts[-1]])[run.scripts[-1]]
        run.report.add("ScriptCatalog.load", rel, "fail", "ClientAuthn never took effect at the token endpoint",
                       trace or "", t)
        return
    run.report.add("ScriptCatalog.load", rel, "pass", started=t)

    def basic_only():
        r = aio.token_request(basic_client, cc)
        assert r.status == 200, f"client_secret_basic client rejected with Basic auth (HTTP {r.status}): {r.body[:200]}"
        r = aio.token_request(post_client, cc, auth="basic")
        assert r.status in (400, 401) and "access_token" not in r.body, \
            f"client_secret_post client authenticated with Basic (HTTP {r.status}): registered method bypassed"

    behavior(run, "ScriptCatalog.behavior", "ClientAuthn.java honours registered auth method", basic_only)


def main():
    fqdn = os.environ["JANS_FQDN"]
    report = Report()
    out = Path(os.environ.get("REPORT_DIR", "test-reports")) / "script-catalog-TEST-ScriptCatalog.xml"
    run = None
    try:
        aio = Aio(fqdn, os.environ.get("AIO_CA_FILE", str(REPO / "automation/jans-aio-demo/templates/ca.crt")),
                  os.environ["CN_CONFIG_API_TEST_CLIENT_ID"], os.environ["CN_CONFIG_API_TEST_CLIENT_SECRET"])
        run = Run(aio, report)
        scripts = discover()
        log(f"{len(scripts)} catalog scripts, run tag {run.tag}")

        check_installed(run, scripts)

        batch, isolated = [], {t: [] for t in manifest.ISOLATED_TYPES}
        for s in scripts:
            if s.rel in manifest.LIBRARIES:
                report.add("ScriptCatalog.load", s.rel, "skip", "helper module, not a script")
            elif not s.script_type:
                report.add("ScriptCatalog.load", s.rel, "fail", "no custom script type matches its classes")
            elif not manifest.loader_service(s.script_type):
                report.add("ScriptCatalog.load", s.rel, "skip", f"no AIO service loads {s.script_type} scripts")
            elif s.script_type in isolated:
                isolated[s.script_type].append(s)
            else:
                batch.append(s)

        load_check(run, batch)
        behavior_tests(run, scripts)
        run.delete(list(run.scripts))
        load_check(run, isolated["persistence_extension"])
        client_authn_test(run, scripts)
    except Exception as e:  # noqa: BLE001 - a harness crash must still leave a failing report
        report.add("ScriptCatalog.harness", "run", "fail", f"{type(e).__name__}: {e}", traceback.format_exc())
    finally:
        if run:
            run.delete(list(run.scripts))
            for inum in run.users:
                try:
                    run.aio.delete_user(inum)
                except Exception as e:  # noqa: BLE001
                    log(f"cleanup: could not delete user {inum}: {e}")
        report.write(out)
        log(f"report: {out} ({len(report.cases)} cases)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
