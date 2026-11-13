"""Offline checks for the script-catalog harness: python3 -m unittest discover -s automation/ci/script_catalog"""
import itertools
import unittest
from unittest import mock

import aio
import catalog
import manifest
import run


class CatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.scripts = catalog.discover()
        cls.by_rel = {s.rel: s for s in cls.scripts}

    def test_every_script_has_a_type(self):
        untyped = [s.rel for s in self.scripts if not s.script_type and s.rel not in manifest.LIBRARIES]
        self.assertEqual(untyped, [])

    def test_types_follow_class_not_directory(self):
        self.assertEqual(self.by_rel["config_api/config-api-interception/config_api_interception.py"].script_type,
                         "config_api_auth")
        self.assertEqual(self.by_rel["access_evaluation/AccessEvaluationDiscovery.java"].script_type,
                         "access_evaluation_discovery")
        self.assertEqual(self.by_rel["person_authentication/other/postauthn/postauthn.py"].script_type, "post_authn")

    def test_manifest_paths_exist(self):
        for rel in itertools.chain(manifest.KNOWN_BROKEN, manifest.INIT_REQUIRED, manifest.LIBRARIES,
                                   manifest.NOT_INSTALLED):
            self.assertIn(rel, self.by_rel, rel)

    def test_init_required_scripts_are_not_known_broken(self):
        self.assertEqual(manifest.INIT_REQUIRED & set(manifest.KNOWN_BROKEN), set())

    def test_key_matches_loader_placeholder(self):
        s = self.by_rel["person_authentication/otp-external-authenticator/OtpExternalAuthenticator.py"]
        self.assertEqual(s.key, "person_authentication_otp_external_authenticator_otpexternalauthenticator")

    def test_every_loaded_type_has_a_sentinel(self):
        for s in self.scripts:
            svc = manifest.loader_service(s.script_type) if s.script_type else None
            if svc and s.script_type not in manifest.ISOLATED_TYPES:
                self.assertIn(svc, run.SENTINEL_TYPE, s.rel)

    def test_no_sentinel_type_can_lock_out_config_api(self):
        self.assertNotIn("config_api_auth", run.SENTINEL_TYPE.values())


class ClassifyTest(unittest.TestCase):
    def script(self, rel):
        return catalog.CatalogScript(path=catalog.CATALOG / rel, language="python")

    def test_clean_load(self):
        self.assertEqual(run.classify(self.script("x.py"), None), ("pass", ""))

    def test_load_error_fails(self):
        status, msg = run.classify(self.script("x.py"), "Traceback ...\nImportError: No module named net")
        self.assertEqual(status, "fail")
        self.assertIn("failed to load", msg)

    def test_init_failure_gates_only_required(self):
        trace = "java.lang.Exception: Using default external type class\n\tat ..."
        required = next(iter(manifest.INIT_REQUIRED))
        self.assertEqual(run.classify(self.script(required), trace)[0], "fail")
        self.assertEqual(run.classify(self.script("other.py"), trace)[0], "skip")


class FormParserTest(unittest.TestCase):
    def test_jsf_login_form(self):
        html = """<form id="loginForm" name="loginForm" method="post" action="/jans-auth/login.htm">
          <input type="hidden" name="loginForm" value="loginForm" />
          <input id="loginForm:username" type="text" name="loginForm:username" />
          <input type="submit" name="loginForm:loginButton" value="Login" />
          <input type="hidden" name="jakarta.faces.ViewState" value="-123:456" />
        </form>"""
        f = aio.parse_forms(html)["loginForm"]
        self.assertEqual(f["action"], "/jans-auth/login.htm")
        self.assertEqual(f["fields"], {"loginForm": "loginForm", "loginForm:username": "",
                                       "jakarta.faces.ViewState": "-123:456"})


class FakeAio:
    """Config-api stand-in: a reload marks broken scripts with an error the way ExternalTypeCreator does."""
    fqdn = "aio.example.org"

    def __init__(self, traces):
        self.traces, self.store, self.ids = traces, {}, itertools.count()

    def create_script(self, name, script_type, language, body, **_):
        inum = f"I{next(self.ids)}"
        err = "SyntaxError: sentinel" if body == run.SENTINEL_BODY else self.traces.get(body)
        self.store[inum] = {"inum": inum, "name": name, "dn": f"inum={inum},ou=scripts,o=jans",
                            "scriptError": {"stackTrace": err} if err else None}
        return self.store[inum]

    def get_script(self, inum):
        return self.store[inum]

    def delete_script(self, inum):
        self.store.pop(inum, None)


class LoadCheckTest(unittest.TestCase):
    def test_load_check_reports_and_cleans_up(self):
        scripts = catalog.discover()
        ok = next(s for s in scripts if s.rel == "token_exchange/TokenExchange.java")
        broken = next(s for s in scripts if s.rel == "client_authn/ClientAuthn.java")
        known = next(s for s in scripts if s.rel in manifest.KNOWN_BROKEN and s.language == "python")
        fake = FakeAio({broken.text(): "error: cannot find symbol", known.text(): "ImportError: x"})
        report = run.Report()
        r = run.Run(fake, report)
        with mock.patch("time.sleep"):
            run.load_check(r, [ok, broken, known])
        statuses = {c[1]: c[2] for c in report.cases}
        self.assertEqual(statuses, {ok.rel: "pass", broken.rel: "fail", known.rel: "skip"})
        self.assertEqual(fake.store, {})
        self.assertEqual(r.scripts, [])


if __name__ == "__main__":
    unittest.main()
