import os
import shutil

import distro
import pytest


@pytest.mark.parametrize("val,expected", [
    ("t", True),  # truthy
    ("T", True),
    ("true", True),
    ("True", True),
    ("TRUE", True),
    ("1", True),
    (1, True),
    (True, True),
    ("f", False),  # falsy
    ("F", False),
    ("false", False),
    ("False", False),
    ("FALSE", False),
    ("0", False),
    (0, False),
    (False, False),
    ("random", False),  # misc
    (None, False),
])
def test_as_boolean(val, expected):
    from jans.pycloudlib.utils import as_boolean
    assert as_boolean(val) == expected


@pytest.mark.parametrize("value, expected", [
    ("a", "a"),
    (1, "1"),
    (b"b", "b"),
    (True, "true"),
    (False, "false"),
    (None, "null"),
    ([], "[]"),
])
def test_safe_value(value, expected):
    from jans.pycloudlib.utils import safe_value
    assert safe_value(value) == expected


@pytest.mark.parametrize("size", [12, 10])
def test_get_random_chars(size):
    from jans.pycloudlib.utils import get_random_chars
    assert len(get_random_chars(size)) == size


@pytest.mark.parametrize("size", [12, 10])
def test_get_sys_random_chars(size):
    from jans.pycloudlib.utils import get_sys_random_chars
    assert len(get_sys_random_chars(size)) == size


@pytest.mark.parametrize("cmd", ["echo foobar"])
def test_exec_cmd(cmd):
    from jans.pycloudlib.utils import exec_cmd

    out, err, code = exec_cmd(cmd)
    assert out == b"foobar"
    assert err == b""
    assert code == 0


@pytest.mark.parametrize("txt, ctx, expected", [
    ("%id", {}, "%id"),
    ("%(id)s", {"id": 1}, "1"),
])
def test_safe_render(txt, ctx, expected):
    from jans.pycloudlib.utils import safe_render
    assert safe_render(txt, ctx) == expected


@pytest.mark.parametrize("text, num_spaces, expected", [
    ("ab\n\tcd", 0, "ab\ncd"),
    ("ab\n\tcd", 1, " ab\n cd"),
])
def test_reindent(text, num_spaces, expected):
    from jans.pycloudlib.utils import reindent
    assert reindent(text, num_spaces) == expected


@pytest.mark.parametrize("text, num_spaces, expected", [
    ("abcd", 0, "YWJjZA=="),
    ("abcd", 1, " YWJjZA=="),
    (b"abcd", 0, "YWJjZA=="),
    (b"abcd", 1, " YWJjZA=="),
])
def test_generate_base64_contents(text, num_spaces, expected):
    from jans.pycloudlib.utils import generate_base64_contents
    assert generate_base64_contents(text, num_spaces) == expected


@pytest.mark.parametrize("text, key, expected", [
    ("abcd", "a" * 24, b"YgH8NDxhxmA="),
    ("abcd", b"a" * 24, b"YgH8NDxhxmA="),
    (b"abcd", "a" * 24, b"YgH8NDxhxmA="),
    (b"abcd", b"a" * 24, b"YgH8NDxhxmA="),
])
def test_encode_text(text, key, expected):
    from jans.pycloudlib.utils import encode_text
    assert encode_text(text, key) == expected


@pytest.mark.parametrize("encoded_text, key, expected", [
    ("YgH8NDxhxmA=", "a" * 24, b"abcd"),
    ("YgH8NDxhxmA=", b"a" * 24, b"abcd"),
    (b"YgH8NDxhxmA=", "a" * 24, b"abcd"),
    (b"YgH8NDxhxmA=", b"a" * 24, b"abcd"),
])
def test_decode_text(encoded_text, key, expected):
    from jans.pycloudlib.utils import decode_text
    assert decode_text(encoded_text, key) == expected


@pytest.mark.skipif(
    shutil.which("keytool") is None,
    reason="requires keytool executable"
)
def test_cert_to_truststore(tmpdir):
    from jans.pycloudlib.utils import cert_to_truststore

    tmp = tmpdir.mkdir("jans")
    keystore_file = tmp.join("jans.jks")
    cert_file = tmp.join("jans.crt")

    # dummy cert
    cert_file.write("""-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----""")

    _, _, code = cert_to_truststore(
        "jans_https", str(cert_file), str(keystore_file), "secret",
    )
    assert code == 0


@pytest.mark.skipif(
    distro.id() == "ubuntu" and distro.version().startswith("20"),
    reason="need to lower security level on current OS",
)
def test_get_server_certificate(tmpdir, httpsserver):
    from jans.pycloudlib.utils import get_server_certificate

    host, port = httpsserver.server_address
    filepath = tmpdir.mkdir("jans").join("jans.crt")

    cert = get_server_certificate(host, port, str(filepath))
    assert cert == filepath.read()


def test_ldap_encode():
    from jans.pycloudlib.utils import ldap_encode

    assert ldap_encode("secret").startswith("{ssha}")


def test_generate_ssl_certkey(tmpdir):
    from jans.pycloudlib.utils import generate_ssl_certkey

    base_dir = tmpdir.mkdir("certs")
    generate_ssl_certkey(
        "my-suffix",
        "email@org.local",
        "my.org.local",
        "org",
        "US",
        "TX",
        "Austin",
        base_dir=str(base_dir),
        extra_dns=["custom.org.local"],
        extra_ips=["127.0.0.1"],
    )
    assert os.path.isfile(str(base_dir.join("my-suffix.crt")))
    assert os.path.isfile(str(base_dir.join("my-suffix.key")))


def test_generate_ssl_ca_certkey(tmpdir):
    from jans.pycloudlib.utils import generate_ssl_ca_certkey

    base_dir = tmpdir.mkdir("certs")
    generate_ssl_ca_certkey(
        "cert-auth",
        "email@org.local",
        "my.org.local",
        "org",
        "US",
        "TX",
        "Austin",
        base_dir=str(base_dir),
    )
    assert os.path.isfile(str(base_dir.join("cert-auth.crt")))
    assert os.path.isfile(str(base_dir.join("cert-auth.key")))


def test_generate_signed_ssl_certkey(tmpdir):
    from jans.pycloudlib.utils import generate_ssl_ca_certkey
    from jans.pycloudlib.utils import generate_signed_ssl_certkey

    base_dir = tmpdir.mkdir("certs")

    ca_cert, ca_key = generate_ssl_ca_certkey(
        "cert-auth",
        "email@org.local",
        "my.org.local",
        "org",
        "US",
        "TX",
        "Austin",
        base_dir=str(base_dir),
    )

    generate_signed_ssl_certkey(
        "my-suffix",
        ca_key,
        ca_cert,
        "email@org.local",
        "my.org.local",
        "org",
        "US",
        "TX",
        "Austin",
        base_dir=str(base_dir),
        extra_dns=["custom.org.local"],
        extra_ips=["127.0.0.1"],
    )

    assert os.path.isfile(str(base_dir.join("my-suffix.crt")))
    assert os.path.isfile(str(base_dir.join("my-suffix.csr")))
    assert os.path.isfile(str(base_dir.join("my-suffix.key")))


@pytest.mark.parametrize("password, encoded_password", [
    ("secret", "fHL54sT5qHk="),
    ("", "U7niJiK7IV8="),
])
def test_secure_password_file(tmpdir, password, encoded_password):
    from jans.pycloudlib.utils import secure_password_file

    src = tmpdir.join("password_file")
    src.write(password)
    salt = "7MEDWVFAG3DmakHRyjMqp5EE"
    assert secure_password_file(str(src), salt) == password

    with open(str(src)) as f:
        assert f.read() == encoded_password
