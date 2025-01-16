import base64

import pytest

VALID_CERT = """-----BEGIN CERTIFICATE-----
MIIDITCCAgmgAwIBAgIIF8QQcKwv7CAwDQYJKoZIhvcNAQELBQAwJDEiMCAGA1UE
AxMZQ291Y2hiYXNlIFNlcnZlciA5NjA2ZDc4ZTAeFw0xMzAxMDEwMDAwMDBaFw00
OTEyMzEyMzU5NTlaMCQxIjAgBgNVBAMTGUNvdWNoYmFzZSBTZXJ2ZXIgOTYwNmQ3
OGUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC+Qx1ieQSFfheRkUMX
f4twp8UXXOonM0uON5SEhCA18Mjs8zigf3OPCsyFfEZG5dTacN9Pv4VeH73Jg55K
p2SkFVj7uZUdI2J/e5DrHPFoiXzIsPNYFgzVesp6XWVEVuACvdpp21oeQB6OA3Af
6ooU2Zqgup7PgAJJwe8VBfc/78Z6I+zLp7ln97L1GTVkgcxVRoATgsazvnu5oKGm
90pHGJNv+25PafB7PWD77LnB3XaTlRc0QYiwhK/Y97JnihAZHA9QVMoB9PN2RInO
APWNhFHkzEsc6Nf8qw7WJOzhHctrLbow/w35Qaakdxjas7b9Tc29x2SygyYGKLie
Ci4xAgMBAAGjVzBVMA4GA1UdDwEB/wQEAwICpDATBgNVHSUEDDAKBggrBgEFBQcD
ATAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBS7+qDLfgdibAUThjRJJdvA1fua
sjANBgkqhkiG9w0BAQsFAAOCAQEAtAEeGSsipaUmDQXXdLZzuWV/65ULp7zMGP3n
M4N68iexrmm4LUVZ/SKxD3JLIY3/g535SrI5zY0o1CLyGbvw4t5Tr23FlR8uDjU/
+W2EqWbXe/cH9voLyEmmroBPkmvSSNjpv2ODUkPQ366VKjUDn64Dj7VAyLz72hhX
FLY3jyGaKhjzlzW+TmNql9ij5VbjdU4qK9joFCNo2M/00GND3OfONy8PjwARCXRk
d37wlAIqAg8ssO1PLGmv20Qbb/s0UKbA2i/Pf+j6JB2ZyKjrruvQG4C+s3kQ0mk3
/BCC77VV1YZs+B/7z7TBcIbb48SOuXLnIL3AfFT98+FDXMLycw==
-----END CERTIFICATE-----
"""

VALID_CERT_B64 = base64.b64encode(VALID_CERT.encode())

AWS_CONFIG = """[default]
region = us-west-1
"""

AWS_CONFIG_B64 = base64.b64encode(AWS_CONFIG.encode())


@pytest.mark.parametrize("value", [
    VALID_CERT,
    VALID_CERT_B64,
])
def test_valid_certkey(value):
    from jans.pycloudlib.schema import CertKey
    assert CertKey()._deserialize(value, "ssl_cert", None) == VALID_CERT


def test_invalid_certkey():
    from marshmallow import ValidationError
    from jans.pycloudlib.schema import CertKey

    with pytest.raises(ValidationError):
        CertKey()._deserialize("not-cert-string", "ssl_cert", None)


def test_empty_certkey():
    from jans.pycloudlib.schema import CertKey
    assert CertKey()._deserialize("", "ssl_cert", None) == ""


@pytest.mark.parametrize("value", [
    AWS_CONFIG,
    AWS_CONFIG_B64,
])
def test_secret_transform_data(value):
    from jans.pycloudlib.schema import SecretSchema

    given = SecretSchema().transform_data({
        "aws_config": value
    })
    assert given["aws_config"] == AWS_CONFIG


@pytest.mark.parametrize("value", [
    # length not 24 chars
    "abcdef",
    # non-alphanumeric chars
    "abcdef123456abcdef!!!!!!",
])
def test_secret_validate_salt(value):
    from marshmallow import ValidationError
    from jans.pycloudlib.schema import SecretSchema

    with pytest.raises(ValidationError):
        SecretSchema().validate_salt(value)


def test_secret_validate_password():
    from marshmallow import ValidationError
    from jans.pycloudlib.schema import SecretSchema

    with pytest.raises(ValidationError):
        # no special chars
        SecretSchema().validate_password("abcD3f")


def test_configmap_validate_fqdn():
    from marshmallow import ValidationError
    from jans.pycloudlib.schema import ConfigmapSchema

    with pytest.raises(ValidationError):
        # no special chars
        ConfigmapSchema().validate_fqdn("local")


def test_configmap_transform_data():
    from jans.pycloudlib.schema import ConfigmapSchema
    from jans.pycloudlib.schema import AUTH_SIG_KEYS
    from jans.pycloudlib.schema import AUTH_ENC_KEYS

    given = ConfigmapSchema().transform_data({
        "auth_sig_keys": "",
        "auth_enc_keys": "",
    })
    expected = {
        "auth_sig_keys": " ".join(AUTH_SIG_KEYS),
        "auth_enc_keys": " ".join(AUTH_ENC_KEYS),
    }
    assert given == expected


def test_configmap_partial_transform_data():
    from jans.pycloudlib.schema import ConfigmapSchema

    given = ConfigmapSchema().transform_data({
        "auth_sig_keys": "RS256 random",
        "auth_enc_keys": "RSA1_5 random",
    })
    expected = {
        "auth_sig_keys": "RS256",
        "auth_enc_keys": "RSA1_5",
    }
    assert given == expected


@pytest.mark.parametrize("value, retcode", [
    ("", 1),
    ('{"_secret": {"admin_pw": "abcD3f"}}', 1),
])
def test_load_schema_from_file_invalid(tmpdir, value, retcode):
    from jans.pycloudlib.schema import load_schema_from_file

    src = tmpdir.join("configuration.json")
    src.write(value)

    _, _, code = load_schema_from_file(str(src))
    assert code == retcode


def test_valid_optional_scopes():
    from jans.pycloudlib.schema import ConfigmapSchema
    assert ConfigmapSchema().validate_optional_scopes('["redis", "sql"]') is None


@pytest.mark.parametrize("value", [
    '["random"]',
    "random",
    '{"key": "value"}',
])
def test_random_optional_scopes(value):
    from marshmallow import ValidationError
    from jans.pycloudlib.schema import ConfigmapSchema

    with pytest.raises(ValidationError):
        ConfigmapSchema().validate_optional_scopes(value)


def test_load_schema_key(tmpdir):
    from jans.pycloudlib.schema import load_schema_key

    src = tmpdir.join("configuration.key")
    src.write("abcd")
    assert load_schema_key(str(src)) == "abcd"


def test_maybe_encrypted_schema_file_missing():
    from jans.pycloudlib.schema import maybe_encrypted_schema

    _, err, _ = maybe_encrypted_schema("/path/to/schema/file", "/path/to/schema/key")
    assert "error" in err


def test_maybe_encrypted_schema(tmpdir):
    from jans.pycloudlib.schema import maybe_encrypted_schema

    src = tmpdir.join("configuration.json")
    src.write("zLBGM41dAfA2JuIkVHRKa+/WwVo/8oQAdD0LUT3jGfhqp/euYdDhf+kTiKwfb1Sv28zYL12JlO+3oSl6ZlhiTw==")

    src_key = tmpdir.join("configuration.key")
    src_key.write("6Jsv61H7fbkeIkRvUpnZ98fu")

    out, _, _ = maybe_encrypted_schema(str(src), str(src_key))
    assert out == {"_configmap": {"hostname": "example.com"}}


def test_schema_exclude_configmap(tmpdir):
    from jans.pycloudlib.schema import load_schema_from_file

    src = tmpdir.join("configuration.json")
    src.write('{"_configmap": {}, "_secret": {"admin_password": "Test1234#"}}')

    out, _, code = load_schema_from_file(str(src), exclude_configmap=True)
    assert "_configmap" not in out and code == 0


def test_schema_exclude_secret(tmpdir):
    from jans.pycloudlib.schema import load_schema_from_file

    src = tmpdir.join("configuration.json")
    src.write('{"_configmap": {"city": "Austin", "country_code": "US", "admin_email": "s@example.com", "hostname": "example.com", "orgName": "Example Inc.", "state": "TX"}, "_secret": {}}')

    out, _, code = load_schema_from_file(str(src), exclude_secret=True)
    assert "_secret" not in out and code == 0
