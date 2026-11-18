"""Offline checks for the script-catalog harness: python3 -m unittest discover -s automation/ci/script_catalog"""
import itertools
import unittest
import unittest.mock as mock

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


class ReportTest(unittest.TestCase):
    def test_control_characters_stay_parseable(self):
        import tempfile, xml.etree.ElementTree as ET
        from pathlib import Path
        report = run.Report()
        report.add("C", "n\x1b", "fail", "bad\x1b[0m", "trace\x00")
        with tempfile.TemporaryDirectory() as d:
            out = Path(d) / "r.xml"
            report.write(out)
            self.assertEqual(ET.parse(out).find("testcase/failure").get("message"), "bad\\x1b[0m")


class DiscoverRootTest(unittest.TestCase):
    def test_rel_and_key_follow_the_discovery_root(self):
        import tempfile
        from pathlib import Path
        with tempfile.TemporaryDirectory() as d:
            f = Path(d) / "token_exchange" / "Tx.java"
            f.parent.mkdir()
            f.write_text("public class TokenExchange implements TokenExchangeType {}")
            s = catalog.discover(d)[0]
            self.assertEqual((s.rel, s.key), ("token_exchange/Tx.java", "token_exchange_tx"))


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
        self.undeletable = set()

    def create_script(self, name, script_type, language, body, **_):
        inum = f"I{next(self.ids)}"
        err = "SyntaxError: sentinel" if body == run.SENTINEL_BODY else self.traces.get(body)
        self.store[inum] = {"inum": inum, "name": name, "dn": f"inum={inum},ou=scripts,o=jans",
                            "scriptError": {"stackTrace": err} if err else None}
        return self.store[inum]

    def get_script(self, inum):
        return self.store[inum]

    def delete_script(self, inum):
        if inum in self.undeletable:
            raise aio.HttpError(503, "busy", inum)
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

    def test_failed_delete_is_kept_for_cleanup(self):
        fake = FakeAio({})
        r = run.Run(fake, run.Report())
        inum = fake.create_script("x", "introspection", "python", "")["inum"]
        r.scripts.append(inum)
        fake.undeletable.add(inum)
        with mock.patch("time.sleep"):
            self.assertEqual(r.delete([inum]), [inum])
        self.assertEqual(r.scripts, [inum])



LOGIN_PAGE = """<form id="loginForm" method="post" action="/jans-auth/login.htm">
  <input type="hidden" name="jakarta.faces.ViewState" value="1:2" /></form>"""
CALLBACK = "https://aio.example.org/cb"


class LoginFlowTest(unittest.TestCase):
    """aio.login against scripted responses: only an explicit refusal may read as a denied login."""

    def flow(self, *responses, direct=False):
        script = list(responses)

        class FakeBrowser:
            def __init__(self, _ssl):
                pass

            def request(self, method, url, **_):
                status, body, location = script.pop(0)
                return aio.Response(status, {"Location": location} if location else {}, body, url)

        client = aio.Aio.__new__(aio.Aio)
        client.ssl, client._verifiers = None, {}
        client.discovery = {"authorization_endpoint": "https://aio.example.org/authorize"}
        with mock.patch.object(aio, "Browser", FakeBrowser):
            login = client.login if direct else lambda *a: run.retrying_login(client, *a)
            return login({"client_id": "c", "redirect_uris": [CALLBACK]}, "acr", "u", "p")

    def test_code_is_returned(self):
        self.assertEqual(self.flow((200, LOGIN_PAGE, None), (302, "", CALLBACK + "?code=abc")), "abc")

    def test_login_form_shown_again_is_a_denial(self):
        self.assertIsNone(self.flow((200, LOGIN_PAGE, None), (200, LOGIN_PAGE, None)))

    def test_5xx_before_credentials_is_retryable(self):
        self.assertIsNone(self.flow((503, "busy", None)))

    def test_5xx_after_credentials_is_not_retried(self):
        with self.assertRaises(aio.LoginFlowError) as e:
            self.flow((200, LOGIN_PAGE, None), (503, "busy", None))
        self.assertTrue(e.exception.credentials_sent)

    def test_lock_probe_raises_on_any_5xx(self):
        with self.assertRaises(aio.LoginFlowError):
            self.flow((503, "busy", None), direct=True)

    def test_access_denied_callback_is_a_denial(self):
        self.assertIsNone(self.flow((200, LOGIN_PAGE, None), (302, "", CALLBACK + "?error=access_denied")))

    def test_other_callbacks_are_not_a_denial(self):
        for query in ("?error=server_error", "?state=x"):
            with self.assertRaises(aio.LoginFlowError):
                self.flow((302, "", CALLBACK + query), direct=True)

    def test_unrecognized_page_is_not_a_denial(self):
        with self.assertRaises(aio.LoginFlowError):
            self.flow((200, LOGIN_PAGE, None), (200, "<html>maintenance</html>", None))

    def test_endless_redirects_are_not_a_denial(self):
        with self.assertRaises(aio.LoginFlowError):
            self.flow(*[(302, "", "https://aio.example.org/loop")] * 16)


if __name__ == "__main__":
    unittest.main()
