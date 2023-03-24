import json
import os
import shutil
from collections import namedtuple
from io import StringIO

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


def test_render_base_properties(monkeypatch, tmpdir):
    from jans.pycloudlib.persistence import render_base_properties

    persistence_type = "ldap"
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", persistence_type)

    src = tmpdir.join("jans.properties.tmpl")
    src.write("""
persistence.type=%(persistence_type)s
fido2_ConfigurationEntryDN=ou=fido2,ou=configuration,o=jans
""".strip())
    dest = tmpdir.join("jans.properties")

    expected = f"""
persistence.type={persistence_type}
fido2_ConfigurationEntryDN=ou=fido2,ou=configuration,o=jans
""".strip()

    render_base_properties(str(src), str(dest))
    assert dest.read() == expected

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


def test_sync_ldap_truststore(tmpdir, gmanager):
    from jans.pycloudlib.persistence.ldap import sync_ldap_truststore

    dest = tmpdir.join("opendj.pkcs12")
    sync_ldap_truststore(gmanager, str(dest))
    assert dest.read()


@pytest.mark.parametrize("url, host", [
    ("localhost", "localhost"),
    ("localhost:1636", "localhost"),
])
def test_extract_ldap_host(url, host):
    from jans.pycloudlib.persistence.ldap import extract_ldap_host

    assert extract_ldap_host(url) == host


@pytest.mark.parametrize("use_ssl, port", [
    ("True", 1636),
    ("False", 1389),
])
def test_resolve_ldap_port(monkeypatch, use_ssl, port):
    from jans.pycloudlib.persistence.ldap import resolve_ldap_port

    monkeypatch.setenv("CN_LDAP_USE_SSL", use_ssl)
    assert resolve_ldap_port() == port


def test_ldap_client_init(gmanager):
    from jans.pycloudlib.persistence.ldap import LdapClient

    client = LdapClient(gmanager)
    assert str(client.server) == "ldaps://localhost:1636 - ssl"


# =========
# Couchbase
# =========


def test_get_couchbase_user(monkeypatch, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_user

    monkeypatch.setenv("CN_COUCHBASE_USER", "root")
    assert get_couchbase_user(gmanager) == "root"


def test_get_couchbase_password_from_file(monkeypatch, tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_password

    passwd_file = tmpdir.join("couchbase_password")
    passwd_file.write("secret")
    monkeypatch.setenv("CN_COUCHBASE_PASSWORD_FILE", str(passwd_file))
    assert get_couchbase_password(gmanager) == "secret"


def test_get_couchbase_password_from_secrets(gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_password
    assert get_couchbase_password(gmanager) == "secret"


def test_get_couchbase_superuser(monkeypatch, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser

    monkeypatch.setenv("CN_COUCHBASE_SUPERUSER", "")
    assert get_couchbase_superuser(gmanager) == ""


def test_get_couchbase_superuser_password(monkeypatch, tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_superuser_password

    passwd_file = tmpdir.join("couchbase_superuser_password")
    passwd_file.write("secret")

    monkeypatch.setenv("CN_COUCHBASE_SUPERUSER_PASSWORD_FILE", str(passwd_file))
    assert get_couchbase_superuser_password(gmanager) == "secret"


@pytest.mark.skipif(
    shutil.which("keytool") is None,
    reason="requires keytool executable"
)
def test_sync_couchbase_truststore(monkeypatch, tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import sync_couchbase_truststore

    keystore_file = tmpdir.join("couchbase.jks")
    cert_file = tmpdir.join("couchbase.crt")

    # dummy cert
    cert_file.write(DUMMY_COUCHBASE_CERT)

    monkeypatch.setenv("CN_COUCHBASE_CERT_FILE", str(cert_file))
    sync_couchbase_truststore(gmanager, str(keystore_file))
    assert os.path.exists(str(keystore_file))


@pytest.mark.parametrize("timeout, expected", [
    (5000, 5000),
    ("random", 10000),
])
def test_get_couchbase_conn_timeout(monkeypatch, timeout, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_conn_timeout

    monkeypatch.setenv("CN_COUCHBASE_CONN_TIMEOUT", str(timeout))
    assert get_couchbase_conn_timeout() == expected


@pytest.mark.parametrize("max_wait, expected", [
    (5000, 5000),
    ("random", 20000),
])
def test_get_couchbase_conn_max_wait(monkeypatch, max_wait, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_conn_max_wait

    monkeypatch.setenv("CN_COUCHBASE_CONN_MAX_WAIT", str(max_wait))
    assert get_couchbase_conn_max_wait() == expected


@pytest.mark.parametrize("scan, expected", [
    ("not_bounded", "not_bounded"),
    ("request_plus", "request_plus"),
    ("statement_plus", "statement_plus"),
    ("random", "not_bounded"),
])
def test_get_couchbase_scan_consistency(monkeypatch, scan, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_scan_consistency

    monkeypatch.setenv("CN_COUCHBASE_SCAN_CONSISTENCY", scan)
    assert get_couchbase_scan_consistency() == expected


def test_sync_couchbase_cert(monkeypatch, tmpdir):
    from jans.pycloudlib.persistence.couchbase import sync_couchbase_cert

    cert_file = tmpdir.join("couchbase.crt")
    cert_file.write(DUMMY_COUCHBASE_CERT)

    monkeypatch.setenv("CN_COUCHBASE_CERT_FILE", str(cert_file))
    assert sync_couchbase_cert() == DUMMY_COUCHBASE_CERT


def test_exec_api_unsupported_method():
    from jans.pycloudlib.persistence.couchbase import RestApi

    client = RestApi("localhost", "admin", "password")
    with pytest.raises(ValueError):
        client.exec_api("pools/default/buckets", method="DELETE")


@pytest.mark.parametrize("client_prop", [
    "rest_client",
    "n1ql_client",
])
def test_no_couchbase_hosts(gmanager, client_prop):
    from jans.pycloudlib.persistence.couchbase import CouchbaseClient

    client = CouchbaseClient(gmanager, {"hosts": "", "user": "admin", "password": "password"})
    with pytest.raises(ValueError):
        getattr(client, client_prop)


def test_client_session_unverified():
    from jans.pycloudlib.persistence.couchbase import RestApi

    client = RestApi("localhost", "admin", "password")
    assert client.session.verify is False


@pytest.mark.parametrize("given, expected", [
    ("", "/etc/certs/couchbase.crt"),  # default
    ("/etc/certs/custom-cb.crt", "/etc/certs/custom-cb.crt"),
])
def test_client_session_verified(monkeypatch, given, expected):
    from jans.pycloudlib.persistence.couchbase import RestApi

    monkeypatch.setenv("CN_COUCHBASE_VERIFY", "true")
    monkeypatch.setenv("CN_COUCHBASE_CERT_FILE", given)

    client = RestApi("localhost", "admin", "password")
    assert client.session.verify == expected


@pytest.mark.parametrize("given, expected", [
    ("", "localhost"),  # default
    ("127.0.0.1", "127.0.0.1"),
])
def test_client_session_verified_host(monkeypatch, given, expected):
    from jans.pycloudlib.persistence.couchbase import RestApi

    monkeypatch.setenv("CN_COUCHBASE_VERIFY", "true")
    monkeypatch.setenv("CN_COUCHBASE_HOST_HEADER", given)
    client = RestApi("localhost", "admin", "password")
    assert client.session.headers["Host"] == expected


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


@pytest.mark.parametrize("enable_ssl, scheme, port", [
    ("true", "https", 18091),
    ("false", "http", 8091),
])
def test_couchbase_rest_client_conn(monkeypatch, enable_ssl, scheme, port):
    from jans.pycloudlib.persistence.couchbase import RestApi

    monkeypatch.setenv("CN_COUCHBASE_TRUSTSTORE_ENABLE", enable_ssl)

    client = RestApi("localhost", "admin", "password")
    assert client.port == port
    assert client.scheme == scheme


@pytest.mark.parametrize("enable_ssl, scheme, port", [
    ("true", "https", 18093),
    ("false", "http", 8093),
])
def test_couchbase_n1ql_client_conn(monkeypatch, enable_ssl, scheme, port):
    from jans.pycloudlib.persistence.couchbase import N1qlApi

    monkeypatch.setenv("CN_COUCHBASE_TRUSTSTORE_ENABLE", enable_ssl)

    client = N1qlApi("localhost", "admin", "password")
    assert client.port == port
    assert client.scheme == scheme


@pytest.mark.parametrize("interval, expected", [
    (4000, 4000),
    (None, 30000),
])
def test_get_couchbase_keepalive_interval(monkeypatch, interval, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_keepalive_interval

    monkeypatch.setenv("CN_COUCHBASE_KEEPALIVE_INTERVAL", str(interval))
    assert get_couchbase_keepalive_interval() == expected


@pytest.mark.parametrize("timeout, expected", [
    (5000, 5000),
    (None, 2500),
])
def test_get_couchbase_keepalive_timeout(monkeypatch, timeout, expected):
    from jans.pycloudlib.persistence.couchbase import get_couchbase_keepalive_timeout

    monkeypatch.setenv("CN_COUCHBASE_KEEPALIVE_TIMEOUT", str(timeout))
    assert get_couchbase_keepalive_timeout() == expected


@pytest.mark.parametrize("bucket_prefix", ["jans", "myprefix"])
def test_render_couchbase_properties(monkeypatch, tmpdir, gmanager, bucket_prefix):
    from jans.pycloudlib.persistence.couchbase import render_couchbase_properties

    passwd = tmpdir.join("couchbase_password")
    passwd.write("secret")

    monkeypatch.setenv("CN_COUCHBASE_PASSWORD_FILE", str(passwd))
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "couchbase")
    monkeypatch.setenv("CN_COUCHBASE_BUCKET_PREFIX", bucket_prefix)

    tmpl = """
connection.connect-timeout: %(couchbase_conn_timeout)s
connection.connection-max-wait-time: %(couchbase_conn_max_wait)s
connection.scan-consistency: %(couchbase_scan_consistency)s
buckets: %(couchbase_buckets)s
bucket.default: %(default_bucket)s
%(couchbase_mappings)s
""".strip()

    expected = """
connection.connect-timeout: 10000
connection.connection-max-wait-time: 20000
connection.scan-consistency: not_bounded
buckets: {bucket_prefix}, {bucket_prefix}_user, {bucket_prefix}_cache, {bucket_prefix}_site, {bucket_prefix}_token, {bucket_prefix}_session
bucket.default: {bucket_prefix}
bucket.{bucket_prefix}_user.mapping: people, groups, authorizations
bucket.{bucket_prefix}_cache.mapping: cache
bucket.{bucket_prefix}_site.mapping: cache-refresh
bucket.{bucket_prefix}_token.mapping: tokens
bucket.{bucket_prefix}_session.mapping: sessions
""".strip().format(bucket_prefix=bucket_prefix)

    src = tmpdir.join("jans-couchbase.properties.tmpl")
    src.write(tmpl)
    dest = tmpdir.join("jans-couchbase.properties")

    render_couchbase_properties(gmanager, str(src), str(dest))
    assert dest.read() == expected


def test_render_couchbase_properties_hybrid(monkeypatch, tmpdir, gmanager):
    from jans.pycloudlib.persistence.couchbase import render_couchbase_properties

    passwd = tmpdir.join("couchbase_password")
    passwd.write("secret")

    monkeypatch.setenv("CN_COUCHBASE_PASSWORD_FILE", str(passwd))
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps({
        "default": "ldap",
        "user": "couchbase",
        "site": "ldap",
        "cache": "ldap",
        "token": "couchbase",
        "session": "ldap",
    }))

    tmpl = """
connection.connect-timeout: %(couchbase_conn_timeout)s
connection.connection-max-wait-time: %(couchbase_conn_max_wait)s
connection.scan-consistency: %(couchbase_scan_consistency)s
buckets: %(couchbase_buckets)s
bucket.default: %(default_bucket)s
%(couchbase_mappings)s
""".strip()

    expected = """
connection.connect-timeout: 10000
connection.connection-max-wait-time: 20000
connection.scan-consistency: not_bounded
buckets: jans, jans_user, jans_token
bucket.default: jans
bucket.jans_user.mapping: people, groups, authorizations
bucket.jans_token.mapping: tokens
""".strip()

    src = tmpdir.join("jans-couchbase.properties.tmpl")
    src.write(tmpl)
    dest = tmpdir.join("jans-couchbase.properties")

    render_couchbase_properties(gmanager, str(src), str(dest))
    assert dest.read() == expected


@pytest.mark.parametrize("dn, id_", [
    ("o=jans", "_"),
    ("ou=jans-auth,ou=configuration,o=jans", "configuration_jans-auth"),
])
def test_id_from_dn(dn, id_):
    from jans.pycloudlib.persistence.couchbase import id_from_dn
    assert id_from_dn(dn) == id_


@pytest.mark.parametrize("key, bucket", [
    ("people_1", "jans_user"),
    ("site_1", "jans_site"),
    ("tokens_1", "jans_token"),
    ("cache_1", "jans_cache"),
    ("configuration_jans-auth", "jans"),
])
def test_get_bucket_for_key(key, bucket):
    from jans.pycloudlib.persistence.couchbase import get_bucket_for_key
    assert get_bucket_for_key(key) == bucket


# ======
# Hybrid
# ======


def test_resolve_hybrid_storages(monkeypatch):
    from jans.pycloudlib.persistence.hybrid import resolve_hybrid_storages
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps({
        "default": "sql",
        "user": "spanner",
        "site": "couchbase",
        "cache": "ldap",
        "token": "sql",
        "session": "sql",
    }))
    expected = {
        "storages": "couchbase, ldap, spanner, sql",
        "storage.default": "sql",
        "storage.couchbase.mapping": "cache-refresh",
        "storage.ldap.mapping": "cache",
        "storage.spanner.mapping": "people, groups, authorizations",
        "storage.sql.mapping": "tokens, sessions",
    }
    mapper = PersistenceMapper()
    assert resolve_hybrid_storages(mapper) == expected


def test_render_hybrid_properties(monkeypatch, tmpdir):
    from jans.pycloudlib.persistence.hybrid import render_hybrid_properties

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv(
        "CN_HYBRID_MAPPING",
        json.dumps({
            "default": "ldap",
            "user": "couchbase",
            "site": "sql",
            "cache": "sql",
            "token": "spanner",
            "session": "sql",
        })
    )

    expected = """
storages: couchbase, ldap, spanner, sql
storage.default: ldap
storage.couchbase.mapping: people, groups, authorizations
storage.spanner.mapping: tokens
storage.sql.mapping: cache-refresh, cache, sessions
""".strip()

    dest = tmpdir.join("jans-hybrid.properties")
    render_hybrid_properties(str(dest))
    assert dest.read() == expected


# ===
# SQL
# ===


def test_get_sql_password_from_file(monkeypatch, tmpdir, gmanager):
    from jans.pycloudlib.persistence.sql import get_sql_password

    src = tmpdir.join("sql_password")
    src.write("secret")
    monkeypatch.setenv("CN_SQL_PASSWORD_FILE", str(src))
    assert get_sql_password(gmanager) == "secret"


def test_get_sql_password_from_secrets(gmanager):
    from jans.pycloudlib.persistence.sql import get_sql_password
    assert get_sql_password(gmanager) == "secret"


@pytest.mark.parametrize("dialect, port, schema, jdbc_driver", [
    ("mysql", 3306, "jans", "mysql"),
    ("pgsql", 5432, "public", "postgresql"),
])
def test_render_sql_properties(monkeypatch, tmpdir, gmanager, dialect, port, schema, jdbc_driver):
    from jans.pycloudlib.persistence.sql import render_sql_properties

    passwd = tmpdir.join("sql_password")
    passwd.write("secret")

    monkeypatch.setenv("CN_SQL_PASSWORD_FILE", str(passwd))
    monkeypatch.setenv("CN_SQL_DB_DIALECT", dialect)
    monkeypatch.setenv("CN_SQL_DB_PORT", str(port))

    tmpl = """
db.schema.name=%(rdbm_schema)s
connection.uri=jdbc:%(rdbm_type)s://%(rdbm_host)s:%(rdbm_port)s/%(rdbm_db)s
connection.driver-property.serverTimezone=%(server_time_zone)s
auth.userName=%(rdbm_user)s
auth.userPassword=%(rdbm_password_enc)s
""".strip()

    expected = f"""
db.schema.name={schema}
connection.uri=jdbc:{jdbc_driver}://localhost:{port}/jans
connection.driver-property.serverTimezone=UTC
auth.userName=jans
auth.userPassword=fHL54sT5qHk=
""".strip()

    src = tmpdir.join("jans-sql.properties.tmpl")
    src.write(tmpl)
    dest = tmpdir.join("jans-sql.properties")

    render_sql_properties(gmanager, str(src), str(dest))
    assert dest.read() == expected


class PGException(Exception):
    def __init__(self, code):
        orig_attrs = namedtuple("OrigAttrs", "pgcode")
        self.orig = orig_attrs(code)


def test_postgresql_adapter_on_create_table_error():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    with pytest.raises(Exception):
        exc = PGException("10P01")
        PostgresqlAdapter().on_create_table_error(exc)


def test_postgresql_adapter_on_create_index_error():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    with pytest.raises(Exception):
        exc = PGException("10P01")
        PostgresqlAdapter().on_create_index_error(exc)


def test_postgresql_adapter_on_insert_into_error():
    from jans.pycloudlib.persistence.sql import PostgresqlAdapter

    with pytest.raises(Exception):
        exc = PGException("10P01")
        PostgresqlAdapter().on_insert_into_error(exc)


class MSException(Exception):
    def __init__(self, code):
        orig_attrs = namedtuple("OrigAttrs", "args")
        self.orig = orig_attrs([code])


def test_mysql_adapter_on_create_table_error():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    with pytest.raises(Exception):
        exc = MSException(1001)
        MysqlAdapter().on_create_table_error(exc)


def test_mysql_adapter_on_create_index_error():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    with pytest.raises(Exception):
        exc = MSException(1001)
        MysqlAdapter().on_create_index_error(exc)


def test_mysql_adapter_on_insert_into_error():
    from jans.pycloudlib.persistence.sql import MysqlAdapter

    with pytest.raises(Exception):
        exc = MSException(1001)
        MysqlAdapter().on_insert_into_error(exc)


@pytest.mark.parametrize("dn, doc_id", [
    ("o=jans", "_"),
    ("ou=jans-auth,ou=configuration,o=jans", "jans-auth"),
])
def test_doc_id_from_dn(dn, doc_id):
    from jans.pycloudlib.persistence.sql import doc_id_from_dn
    assert doc_id_from_dn(dn) == doc_id


def test_sql_client_engine(sql_client):
    from sqlalchemy.engine import Engine
    assert isinstance(sql_client.engine, Engine)


@pytest.mark.parametrize("dialect, word, quoted_word", [
    ("pgsql", "random", '"random"'),
    ("mysql", "random", "`random`"),
])
def test_sql_client_quoted_id(monkeypatch, gmanager, dialect, word, quoted_word):
    from jans.pycloudlib.persistence.sql import SqlClient

    monkeypatch.setenv("CN_SQL_DB_DIALECT", dialect)

    client = SqlClient(gmanager)
    assert client.quoted_id(word) == quoted_word


BUILTINS_OPEN = "builtins.open"


def test_sql_sql_data_types(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = '{"dat": {"mysql": {"type": "TEXT"}}}'
    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))
    assert SqlSchemaMixin().sql_data_types == json.loads(types_str)


def test_sql_sql_data_types_mapping(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = """{
    "1.3.6.1.4.1.1466.115.121.1.11": {
        "mysql": {"size": 2, "type": "VARCHAR"}
    }
}"""

    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))
    assert SqlSchemaMixin().sql_data_types_mapping == json.loads(types_str)


def test_sql_attr_types(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = """{
    "schemaFile": "101-jans.ldif",
    "attributeTypes": [
        {
            "desc": "Description",
            "names": ["jansAssociatedClnt"]
        }
    ]
}"""
    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))

    item = {
        "desc": "Description",
        "names": ["jansAssociatedClnt"],
    }
    assert item in SqlSchemaMixin().attr_types


def test_sql_opendj_attr_types(monkeypatch):
    from jans.pycloudlib.persistence.sql import SqlSchemaMixin

    types_str = '{"ds-task-reset-change-number-base-dn": "1.3.6.1.4.1.1466.115.121.1.12"}'
    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO(types_str))
    assert SqlSchemaMixin().opendj_attr_types == json.loads(types_str)


def test_sql_metadata_prop(sql_client, monkeypatch):
    from unittest.mock import patch
    from sqlalchemy import MetaData

    with patch("sqlalchemy.MetaData.reflect") as patched:
        assert isinstance(sql_client.metadata, MetaData)
        patched.assert_called()


# =======
# SPANNER
# =======


def test_render_spanner_properties(monkeypatch, tmpdir, gmanager, google_creds):
    from jans.pycloudlib.persistence.spanner import render_spanner_properties

    monkeypatch.setenv("GOOGLE_APPLICATION_CREDENTIALS", str(google_creds))
    monkeypatch.setenv("GOOGLE_PROJECT_ID", "testing-project")
    monkeypatch.setenv("CN_GOOGLE_SPANNER_INSTANCE_ID", "testing-instance")
    monkeypatch.setenv("CN_GOOGLE_SPANNER_DATABASE_ID", "testing-db")

    tmpl = """
connection.project=%(spanner_project)s
connection.instance=%(spanner_instance)s
connection.database=%(spanner_database)s
%(spanner_creds)s
""".strip()

    expected = """
connection.project=testing-project
connection.instance=testing-instance
connection.database=testing-db
connection.credentials-file={}
""".format(str(google_creds)).strip()

    src = tmpdir.join("jans-spanner.properties.tmpl")
    src.write(tmpl)
    dest = tmpdir.join("jans-spanner.properties")

    render_spanner_properties(gmanager, str(src), str(dest))
    assert dest.read() == expected


def test_render_spanner_properties_emulator(monkeypatch, tmpdir, gmanager):
    from jans.pycloudlib.persistence.spanner import render_spanner_properties

    monkeypatch.setenv("SPANNER_EMULATOR_HOST", "localhost:9010")
    monkeypatch.setenv("GOOGLE_PROJECT_ID", "testing-project")
    monkeypatch.setenv("CN_GOOGLE_SPANNER_INSTANCE_ID", "testing-instance")
    monkeypatch.setenv("CN_GOOGLE_SPANNER_DATABASE_ID", "testing-db")

    tmpl = """
connection.project=%(spanner_project)s
connection.instance=%(spanner_instance)s
connection.database=%(spanner_database)s
%(spanner_creds)s
""".strip()

    expected = """
connection.project=testing-project
connection.instance=testing-instance
connection.database=testing-db
connection.emulator-host=localhost:9010
""".strip()

    src = tmpdir.join("jans-spanner.properties.tmpl")
    src.write(tmpl)
    dest = tmpdir.join("jans-spanner.properties")

    render_spanner_properties(gmanager, str(src), str(dest))
    assert dest.read() == expected


def test_spanner_quoted_id(spanner_client):
    assert spanner_client.quoted_id("random") == "`random`"


def test_spanner_sub_tables(monkeypatch, spanner_client):
    monkeypatch.setattr(BUILTINS_OPEN, lambda p: StringIO("{}"))
    assert isinstance(spanner_client.sub_tables, dict)


def test_spanner_client_prop(spanner_client):
    from google.cloud.spanner_v1.client import Client
    assert isinstance(spanner_client.client, Client)


def test_spanner_instance_prop(spanner_client):
    from google.cloud.spanner_v1.instance import Instance
    assert isinstance(spanner_client.instance, Instance)


def test_spanner_database_prop(spanner_client):
    from google.cloud.spanner_v1.database import Database
    assert isinstance(spanner_client.database, Database)


# =====
# utils
# =====


@pytest.mark.parametrize("type_", [
    "ldap",
    "couchbase",
    "sql",
    "spanner",
])
def test_persistence_mapper_mapping(monkeypatch, type_):
    from jans.pycloudlib.persistence import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", type_)
    expected = dict.fromkeys([
        "default",
        "user",
        "site",
        "cache",
        "token",
        "session",
    ], type_)
    assert PersistenceMapper().mapping == expected


def test_persistence_mapper_hybrid_mapping(monkeypatch):
    from jans.pycloudlib.persistence import PersistenceMapper

    mapping = {
        "default": "sql",
        "user": "spanner",
        "site": "ldap",
        "cache": "sql",
        "token": "couchbase",
        "session": "sql",
    }
    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps(mapping))
    assert PersistenceMapper().mapping == mapping


@pytest.mark.parametrize("mapping", [
    "ab",
    "1",
    "[]",
    "{}",  # empty dict
    {"user": "sql"},  # missing remaining keys
    {"default": "sql", "user": "spanner", "cache": "ldap", "site": "couchbase", "token": "sql", "session": "random"},  # invalid type
    {"default": "sql", "user": "spanner", "cache": "ldap", "site": "couchbase", "token": "sql", "foo": "sql"},  # invalid key
])
def test_persistence_mapper_validate_hybrid_mapping(monkeypatch, mapping):
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps(mapping))

    with pytest.raises(ValueError):
        PersistenceMapper().validate_hybrid_mapping()


def test_persistence_mapper_groups(monkeypatch):
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps({
        "default": "sql",
        "user": "spanner",
        "site": "ldap",
        "cache": "sql",
        "token": "couchbase",
        "session": "sql",
    }))

    groups = {
        "couchbase": ["token"],
        "ldap": ["site"],
        "spanner": ["user"],
        "sql": ["default", "cache", "session"],
    }
    assert PersistenceMapper().groups() == groups


def test_persistence_mapper_groups_rdn(monkeypatch):
    from jans.pycloudlib.persistence.utils import PersistenceMapper

    monkeypatch.setenv("CN_PERSISTENCE_TYPE", "hybrid")
    monkeypatch.setenv("CN_HYBRID_MAPPING", json.dumps({
        "default": "sql",
        "user": "spanner",
        "site": "ldap",
        "cache": "sql",
        "token": "couchbase",
        "session": "sql",
    }))

    groups = {
        "couchbase": ["tokens"],
        "ldap": ["cache-refresh"],
        "spanner": ["people, groups, authorizations"],
        "sql": ["", "cache", "sessions"],
    }
    assert PersistenceMapper().groups_with_rdn() == groups
