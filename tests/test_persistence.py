import os
# import shutil

import pytest


DUMMY_COUCHBASE_CERT = """-----BEGIN CERTIFICATE-----
MIIEGDCCAgCgAwIBAgIRANslKJCe/whYi01rkUOAxh0wDQYJKoZIhvcNAQELBQAw
DTELMAkGA1UEAxMCQ0EwHhcNMTkxMTI1MDQwOTQ4WhcNMjEwNTI1MDQwOTE4WjAP
MQ0wCwYDVQQDEwRnbHV1MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
05TqppxdpSP9vzQP42YFPM79K3TdOFmsCJLMnKRkeR994MGra6JQ75/+vYmKXJaU
Bo3/VieU2pGaAsXI7MqNfXQcKSwAoGU03xqoBUS8INIYX+Cr7q8jFp1q2VLqpNlt
zWZQsee2TUIsa7MzJ5UK7QnaqK4uadl9XHlkRdXC5APecJoRJK4K1UZ59TyiMisz
Dqf+DrmCaJpIPph4Ro9TZMdoE9CX2mFz6Q+ItaSXvyNqUabip7iIwFf3Mu1pal98
AogsfKcfvu+ki93slrJ6jiDIi5B+D0gbA4E03ncgdfQ8Vs55BZbI0N5uEypfI0ky
LQ6201p4bRRXX4LKooObCwIDAQABo3EwbzAOBgNVHQ8BAf8EBAMCA7gwHQYDVR0l
BBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMB0GA1UdDgQWBBROCOakMthTjAwM7MTP
RnkvLRHMOjAfBgNVHSMEGDAWgBTeSnpdqVZhjCRnCKJFfwiGwnVCvTANBgkqhkiG
9w0BAQsFAAOCAgEAjBOt4xgsiW3BN/ZZ6DehrdmRZRrezwhBWwUrnY9ajmwv0Trs
4sd8EP7RuJsGS5gdUy/qzogSEhUyMz4+iRy/OW9bdzOFe+WDU6Xh9Be/C2Dv9osa
5dsG+Q9/EM9Z2LqKB5/uJJi5xgXdYwRXATDsBdNI8LxQQz0RdCZIJlpqsDEd1qbH
8YX/4cnknuL/7NsqLvn5iZvQcYFA/mfsN8zN52StuRONf1RKdQ3rwT7KehGi7aUa
IWwLEnzLmeZFLUWBl6h2uUMOUe1J8Di176K3SP5pCeb8+gQd5b2ra/IutN7lpISD
7YSStLNCCT33sjbximvX0ur/VipQQO1B/dz9Ua1kPPKV/blTXCiKNf+PpepaFBIp
jIb/dBIq9pLPBWtGz4tCNQIORDBpQjfPpSNH3lEjTyWUOttJYkss6LHAnnQ8COyk
IsbroXkmDKy86qHKlUc7L4REBykLDL7Olm4yQC8Zg46PaG5ymfYVuHd+tC7IZj8H
FRnpMhUJ4+bn+h0kxS4agwb2uCSO4Ge7edViq6ZFZnnfOG6zsz3VJRV71Zw2CQAL
0MxrbeozSHyNrbT2uAGyV85pNJmwZVlBfyKywMWsG3HcoKAhxg//IqNv0pi48Ey9
2xLnWTK3GxoBMh3mpjub+jf6OYDwmh0eBxm+PRMVAe3QB1eG/GGKgEwaTrc=
-----END CERTIFICATE-----"""


def test_render_salt(tmpdir, gmanager, monkeypatch):
    from jans.pycloudlib.persistence import render_salt

    src = tmpdir.join("salt.tmpl")
    src.write("encodeSalt = %(encode_salt)s")

    dest = tmpdir.join("salt")
    render_salt(gmanager, str(src), str(dest))
    assert dest.read() == f"encodeSalt = {gmanager.secret.get('encoded_salt')}"


def test_render_base_properties(tmpdir):
    from jans.pycloudlib.persistence import render_base_properties

    persistence_type = "ldap"
    namespace = "jans"
    os.environ["CN_PERSISTENCE_TYPE"] = persistence_type
    os.environ["CN_NAMESPACE"] = namespace

    src = tmpdir.join("jans.properties.tmpl")
    src.write("""
persistence.type=%(persistence_type)s
fido2_ConfigurationEntryDN=ou=fido2,ou=configuration,o=%(namespace)s
""".strip())
    dest = tmpdir.join("jans.properties")

    expected = f"""
persistence.type={persistence_type}
fido2_ConfigurationEntryDN=ou=fido2,ou=configuration,o={namespace}
""".strip()

    render_base_properties(str(src), str(dest))
    assert dest.read() == expected
    os.environ.clear()

# ====
# LDAP
# ====


def test_render_ldap_properties(tmpdir, gmanager):
    from jans.pycloudlib.persistence.ldap import render_ldap_properties

    tmpl = """
bindDN: %(ldap_binddn)s
bindPassword: %(encoded_ox_ldap_pw)s
servers: %(ldap_hostname)s:%(ldaps_port)s
ssl.trustStoreFile: %(ldapTrustStoreFn)s
ssl.trustStorePin: %(encoded_ldapTrustStorePass)s
""".strip()

    host, port = "localhost", 1636
    expected = f"""
bindDN: {gmanager.config.get("ldap_binddn")}
bindPassword: {gmanager.secret.get("encoded_ox_ldap_pw")}
servers: {host}:{port}
ssl.trustStoreFile: {gmanager.config.get("ldapTrustStoreFn")}
ssl.trustStorePin: {gmanager.secret.get("encoded_ldapTrustStorePass")}
""".strip()

    src = tmpdir.join("jans-ldap.properties.tmpl")
    src.write(tmpl)
    dest = tmpdir.join("jans-ldap.properties")

    render_ldap_properties(gmanager, str(src), str(dest))
    assert dest.read() == expected


# def test_sync_ldap_truststore(tmpdir, gmanager):
#     from jans.pycloudlib.persistence.ldap import sync_ldap_truststore

#     dest = tmpdir.join("opendj.pkcs12")
#     sync_ldap_truststore(gmanager, str(dest))
#     assert dest.read() == gmanager.secret.get("ldap_pkcs12_base64")

# =========
# Couchbase
# =========


def test_get_couchbase_user(gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_user

    os.environ["CN_COUCHBASE_USER"] = "root"
    assert get_couchbase_user(gmanager) == "root"
    os.environ.clear()


def test_get_couchbase_password(tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_password

    passwd_file = tmpdir.join("couchbase_password")
    passwd_file.write("secret")

    os.environ["CN_COUCHBASE_PASSWORD_FILE"] = str(passwd_file)
    assert get_couchbase_password(gmanager) == "secret"
    os.environ.clear()


def test_get_encoded_couchbase_password(tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_encoded_couchbase_password

    passwd_file = tmpdir.join("couchbase_password")
    passwd_file.write("secret")

    os.environ["CN_COUCHBASE_PASSWORD_FILE"] = str(passwd_file)
    assert get_encoded_couchbase_password(gmanager) != "secret"
    os.environ.clear()


def test_get_couchbase_superuser(gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser

    os.environ["CN_COUCHBASE_SUPERUSER"] = ""
    assert get_couchbase_superuser(gmanager) == ""
    os.environ.clear()


def test_get_couchbase_superuser_password(tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser_password

    passwd_file = tmpdir.join("couchbase_superuser_password")
    passwd_file.write("secret")

    os.environ["CN_COUCHBASE_SUPERUSER_PASSWORD_FILE"] = str(passwd_file)
    assert get_couchbase_superuser_password(gmanager) == "secret"
    os.environ.clear()


def test_get_encoded_couchbase_superuser_password(tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_encoded_couchbase_superuser_password

    passwd_file = tmpdir.join("couchbase_superuser_password")
    passwd_file.write("secret")

    os.environ["CN_COUCHBASE_SUPERUSER_PASSWORD_FILE"] = str(passwd_file)
    assert get_encoded_couchbase_superuser_password(gmanager) != "secret"
    os.environ.clear()


# @pytest.mark.skipif(
#     shutil.which("keytool") is None,
#     reason="requires keytool executable"
# )
# def test_sync_couchbase_truststore(tmpdir, gmanager):
#     from jans.pycloudlib.persistence.couchbase import sync_couchbase_truststore

#     keystore_file = tmpdir.join("couchbase.jks")
#     cert_file = tmpdir.join("couchbase.crt")

#     # dummy cert
#     cert_file.write(DUMMY_COUCHBASE_CERT)

#     os.environ["CN_COUCHBASE_CERT_FILE"] = str(cert_file)
#     # gmanager.config.set("couchbaseTrustStoreFn", str(keystore_file))
#     sync_couchbase_truststore(gmanager)
#     assert os.path.exists(str(keystore_file))
#     os.environ.clear()


@pytest.mark.parametrize("timeout, expected", [
    (5000, 5000),
    ("random", 10000),
])
def test_get_couchbase_conn_timeout(timeout, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_conn_timeout

    os.environ["CN_COUCHBASE_CONN_TIMEOUT"] = str(timeout)
    assert get_couchbase_conn_timeout() == expected


@pytest.mark.parametrize("max_wait, expected", [
    (5000, 5000),
    ("random", 20000),
])
def test_get_couchbase_conn_max_wait(max_wait, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_conn_max_wait

    os.environ["CN_COUCHBASE_CONN_MAX_WAIT"] = str(max_wait)
    assert get_couchbase_conn_max_wait() == expected


@pytest.mark.parametrize("scan, expected", [
    ("not_bounded", "not_bounded"),
    ("request_plus", "request_plus"),
    ("statement_plus", "statement_plus"),
    ("random", "not_bounded"),
])
def test_get_couchbase_scan_consistency(scan, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_scan_consistency

    os.environ["CN_COUCHBASE_SCAN_CONSISTENCY"] = scan
    assert get_couchbase_scan_consistency() == expected


def test_sync_couchbase_cert(tmpdir):
    from jans.pycloudlib.persistence.couchbase import sync_couchbase_cert

    cert_file = tmpdir.join("couchbase.crt")
    cert_file.write(DUMMY_COUCHBASE_CERT)
    os.environ["CN_COUCHBASE_CERT_FILE"] = str(cert_file)
    assert sync_couchbase_cert() == DUMMY_COUCHBASE_CERT
    os.environ.clear()


def test_exec_api_unsupported_method():
    from jans.pycloudlib.persistence.couchbase import RestClient

    client = RestClient("localhost", "admin", "password")
    with pytest.raises(ValueError):
        client.exec_api("pools/default/buckets", method="DELETE")


@pytest.mark.parametrize("client_prop", [
    "rest_client",
    "n1ql_client",
])
def test_no_couchbase_hosts(client_prop):
    from jans.pycloudlib.persistence.couchbase import CouchbaseClient

    client = CouchbaseClient("", "admin", "password")
    with pytest.raises(ValueError):
        getattr(client, client_prop)


def test_client_session_unverified():
    from jans.pycloudlib.persistence.couchbase import BaseClient

    client = BaseClient("localhost", "admin", "password")
    assert client.session.verify is False


@pytest.mark.parametrize("given, expected", [
    ("", "/etc/certs/couchbase.crt"),  # default
    ("/etc/certs/custom-cb.crt", "/etc/certs/custom-cb.crt"),
])
def test_client_session_verified(given, expected):
    from jans.pycloudlib.persistence.couchbase import BaseClient

    os.environ["CN_COUCHBASE_VERIFY"] = "true"
    os.environ["CN_COUCHBASE_CERT_FILE"] = given
    client = BaseClient("localhost", "admin", "password")
    assert client.session.verify == expected
    os.environ.clear()


@pytest.mark.parametrize("given, expected", [
    ("", "localhost"),  # default
    ("127.0.0.1", "127.0.0.1"),
])
def test_client_session_verified_host(given, expected):
    from jans.pycloudlib.persistence.couchbase import BaseClient

    os.environ["CN_COUCHBASE_VERIFY"] = "true"
    os.environ["CN_COUCHBASE_HOST_HEADER"] = given
    client = BaseClient("localhost", "admin", "password")
    assert client.session.headers["Host"] == expected
    os.environ.clear()


def test_n1ql_request_body_positional_params():
    from jans.pycloudlib.persistence.couchbase import build_n1ql_request_body

    body = build_n1ql_request_body(
        "SELECT * FROM jans WHERE del = $1 and active = $2",
        False,
        True,
    )
    assert body["args"] == "[false, true]"


def test_n1ql_request_body_named_params():
    from jans.pycloudlib.persistence.couchbase import build_n1ql_request_body

    body = build_n1ql_request_body(
        "SELECT * FROM jans WHERE del = $deleted and active = $active",
        deleted=False,
        active=True,
    )
    assert body["$deleted"] == "false"
    assert body["$active"] == "true"


# ======
# Hybrid
# ======


def test_render_hybrid_properties_default(tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"

    expected = """
storages: ldap, couchbase
storage.default: ldap
storage.ldap.mapping: default
storage.couchbase.mapping: people, groups, authorizations, cache, cache-refresh, tokens, sessions
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected
    os.environ.pop("CN_PERSISTENCE_TYPE", None)


def test_render_hybrid_properties_user(tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
    os.environ["CN_PERSISTENCE_LDAP_MAPPING"] = "user"

    expected = """
storages: ldap, couchbase
storage.default: couchbase
storage.ldap.mapping: people, groups, authorizations
storage.couchbase.mapping: cache, cache-refresh, tokens, sessions
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected

    os.environ.pop("CN_PERSISTENCE_TYPE", None)
    os.environ.pop("CN_PERSISTENCE_LDAP_MAPPING", None)


def test_render_hybrid_properties_token(tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
    os.environ["CN_PERSISTENCE_LDAP_MAPPING"] = "token"

    expected = """
storages: ldap, couchbase
storage.default: couchbase
storage.ldap.mapping: tokens
storage.couchbase.mapping: people, groups, authorizations, cache, cache-refresh, sessions
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected

    os.environ.pop("CN_PERSISTENCE_TYPE", None)
    os.environ.pop("CN_PERSISTENCE_LDAP_MAPPING", None)


def test_render_hybrid_properties_session(tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
    os.environ["CN_PERSISTENCE_LDAP_MAPPING"] = "session"

    expected = """
storages: ldap, couchbase
storage.default: couchbase
storage.ldap.mapping: sessions
storage.couchbase.mapping: people, groups, authorizations, cache, cache-refresh, tokens
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected

    os.environ.pop("CN_PERSISTENCE_TYPE", None)
    os.environ.pop("CN_PERSISTENCE_LDAP_MAPPING", None)


def test_render_hybrid_properties_cache(tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
    os.environ["CN_PERSISTENCE_LDAP_MAPPING"] = "cache"

    expected = """
storages: ldap, couchbase
storage.default: couchbase
storage.ldap.mapping: cache
storage.couchbase.mapping: people, groups, authorizations, cache-refresh, tokens, sessions
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected

    os.environ.pop("CN_PERSISTENCE_TYPE", None)
    os.environ.pop("CN_PERSISTENCE_LDAP_MAPPING", None)


def test_render_hybrid_properties_site(tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
    os.environ["CN_PERSISTENCE_LDAP_MAPPING"] = "site"

    expected = """
storages: ldap, couchbase
storage.default: couchbase
storage.ldap.mapping: cache-refresh
storage.couchbase.mapping: people, groups, authorizations, cache, tokens, sessions
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected

    os.environ.pop("CN_PERSISTENCE_TYPE", None)
    os.environ.pop("CN_PERSISTENCE_LDAP_MAPPING", None)
