"""Minimal stdlib client for the AIO: config-api, dynamic registration and the JSF login flow."""
import base64
import http.cookiejar
import json
import secrets
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser

SCRIPT_SCOPES = " ".join("https://jans.io/oauth/config/" + s for s in (
    "scripts.readonly", "scripts.write", "scripts.delete", "user.readonly", "user.write", "user.delete"))


class HttpError(Exception):
    def __init__(self, status, body, url):
        super().__init__(f"HTTP {status} from {url}: {body[:500]}")
        self.status, self.body = status, body


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Response:
    def __init__(self, status, headers, body, url):
        self.status, self.headers, self.body, self.url = status, headers, body, url

    def json(self):
        return json.loads(self.body)

    @property
    def location(self):
        return self.headers.get("Location")


class Browser:
    """Cookie-keeping HTTP client that never follows redirects on its own."""

    def __init__(self, ssl_context):
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPSHandler(context=ssl_context),
            urllib.request.HTTPCookieProcessor(self.cookies), _NoRedirect)

    def request(self, method, url, data=None, headers=None, form=None, timeout=60):
        headers = dict(headers or {})
        if form is not None:
            data = urllib.parse.urlencode(form).encode()
            headers.setdefault("Content-Type", "application/x-www-form-urlencoded")
        elif data is not None and not isinstance(data, bytes):
            data = json.dumps(data).encode()
            headers.setdefault("Content-Type", "application/json")
        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with self.opener.open(req, timeout=timeout) as r:
                return Response(r.status, r.headers, r.read().decode("utf-8", "replace"), url)
        except urllib.error.HTTPError as e:
            return Response(e.code, e.headers, e.read().decode("utf-8", "replace"), url)


def basic(client_id, secret):
    pair = f"{urllib.parse.quote(client_id, safe='')}:{urllib.parse.quote(secret, safe='')}"
    return "Basic " + base64.b64encode(pair.encode()).decode()


def jwt_claims(token):
    payload = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))


class _FormParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.forms, self._cur = {}, None

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "form":
            self._cur = a.get("id") or a.get("name") or ""
            self.forms[self._cur] = {"action": a.get("action"), "fields": {}}
        elif tag == "input" and self._cur is not None and a.get("name"):
            kind = (a.get("type") or "text").lower()
            if kind not in ("submit", "button", "checkbox", "radio") or "checked" in a:
                self.forms[self._cur]["fields"][a["name"]] = a.get("value") or ""

    def handle_endtag(self, tag):
        if tag == "form":
            self._cur = None


def parse_forms(html):
    p = _FormParser()
    p.feed(html)
    return p.forms


class Aio:
    def __init__(self, fqdn, ca_file, client_id, client_secret):
        self.fqdn = fqdn
        self.base = f"https://{fqdn}"
        self.ssl = ssl.create_default_context(cafile=ca_file)
        self.http = Browser(self.ssl)
        self.client_id, self.client_secret = client_id, client_secret
        self._token, self._token_exp = None, 0
        self.discovery = self.http.request("GET", self.base + "/.well-known/openid-configuration").json()

    # ---- config-api ----------------------------------------------------------------------------------
    def token(self):
        if self._token and time.time() < self._token_exp - 30:
            return self._token
        r = self.http.request("POST", self.discovery["token_endpoint"],
                              headers={"Authorization": basic(self.client_id, self.client_secret)},
                              form={"grant_type": "client_credentials", "scope": SCRIPT_SCOPES})
        if r.status != 200:
            raise HttpError(r.status, r.body, r.url)
        body = r.json()
        self._token, self._token_exp = body["access_token"], time.time() + int(body.get("expires_in", 300))
        return self._token

    def api(self, method, path, data=None, ok=(200, 201, 204)):
        r = self.http.request(method, self.base + "/jans-config-api" + path, data=data,
                              headers={"Authorization": "Bearer " + self.token(), "Accept": "application/json"})
        if r.status not in ok:
            raise HttpError(r.status, r.body, r.url)
        return r.json() if r.body.strip() else None

    def create_script(self, name, script_type, language, body, enabled=True, props=None, level=1):
        return self.api("POST", "/api/v1/config/scripts", {
            "name": name, "description": "script-catalog CI", "script": body, "scriptType": script_type,
            "programmingLanguage": language, "level": level, "enabled": enabled, "locationType": "db",
            "moduleProperties": [{"value1": "location_type", "value2": "db"},
                                 {"value1": "usage_type", "value2": "interactive"}],
            "configurationProperties": props or []})

    def get_script(self, inum):
        return self.api("GET", f"/api/v1/config/scripts/inum/{inum}")

    def delete_script(self, inum):
        self.api("DELETE", f"/api/v1/config/scripts/{inum}", ok=(200, 204, 404))

    def create_user(self, uid, password):
        return self.api("POST", "/mgt/configuser", {
            "userId": uid, "mail": f"{uid}@example.org", "displayName": uid, "givenName": uid,
            "jansStatus": "active", "userPassword": password,
            "customAttributes": [{"name": "sn", "multiValued": False, "values": [uid]}]})

    def get_user(self, inum):
        return self.api("GET", f"/mgt/configuser/{inum}")

    def delete_user(self, inum):
        self.api("DELETE", f"/mgt/configuser/{inum}", ok=(200, 204, 404))

    # ---- OAuth -----------------------------------------------------------------------------------------
    def register_client(self, **extra):
        body = {"redirect_uris": [self.base + "/ci-script-catalog/cb"], "application_type": "web",
                "client_name": "script-catalog-ci", "response_types": ["code"],
                "grant_types": ["authorization_code"], "token_endpoint_auth_method": "client_secret_basic",
                "scope": "openid profile"}
        body.update(extra)
        r = self.http.request("POST", self.discovery["registration_endpoint"], data=body)
        if r.status not in (200, 201):
            raise HttpError(r.status, r.body, r.url)
        return r.json()

    def login(self, client, acr, username, password, scope="openid profile"):
        """Authorization code flow through the JSF login (and consent) pages; returns the code or None."""
        b = Browser(self.ssl)
        redirect = client["redirect_uris"][0]
        url = self.discovery["authorization_endpoint"] + "?" + urllib.parse.urlencode({
            "response_type": "code", "client_id": client["client_id"], "redirect_uri": redirect,
            "scope": scope, "acr_values": acr, "state": secrets.token_hex(8),
            "nonce": secrets.token_hex(8)})
        r = b.request("GET", url)
        submitted = set()
        for _ in range(15):
            if r.status in (301, 302, 303, 307):
                loc = urllib.parse.urljoin(r.url, r.location)
                if loc.startswith(redirect):
                    q = urllib.parse.parse_qs(urllib.parse.urlparse(loc).query)
                    return q.get("code", [None])[0]
                r = b.request("GET", loc)
                continue
            forms = parse_forms(r.body)
            if "loginForm" in forms and "loginForm" not in submitted:
                f = forms["loginForm"]
                fields = dict(f["fields"], **{"loginForm:username": username, "loginForm:password": password,
                                             "loginForm:loginButton": "Login"})
                submitted.add("loginForm")
            elif "authorizeForm" in forms and "authorizeForm" not in submitted:
                f = forms["authorizeForm"]
                # allowButton is an h:commandLink: JS posts its client id as both name and value
                fields = dict(f["fields"], **{"authorizeForm:allowButton": "authorizeForm:allowButton"})
                submitted.add("authorizeForm")
            else:
                return None
            r = b.request("POST", urllib.parse.urljoin(r.url, f["action"]), form=fields)
        return None

    def token_request(self, client, form, auth="basic"):
        headers = {}
        form = dict(form)
        if auth == "basic":
            headers["Authorization"] = basic(client["client_id"], client["client_secret"])
        elif auth == "post":
            form.update(client_id=client["client_id"], client_secret=client["client_secret"])
        return self.http.request("POST", self.discovery["token_endpoint"], form=form, headers=headers)

    def code_to_tokens(self, client, code):
        r = self.token_request(client, {"grant_type": "authorization_code", "code": code,
                                        "redirect_uri": client["redirect_uris"][0]})
        if r.status != 200:
            raise HttpError(r.status, r.body, r.url)
        return r.json()

    def introspect(self, client, token):
        r = self.http.request("POST", self.discovery["introspection_endpoint"], form={"token": token},
                              headers={"Authorization": basic(client["client_id"], client["client_secret"])})
        if r.status != 200:
            raise HttpError(r.status, r.body, r.url)
        return r.json()
