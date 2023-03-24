import os

from jans.pycloudlib.utils import exec_cmd

DEFAULT_SIG_KEYS = "RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512 RSA1_5 RSA-OAEP"
DEFAULT_ENC_KEYS = DEFAULT_SIG_KEYS


def generate_keystore(suffix, hostname, keypasswd):
    # converts key to pkcs12
    _, err, retcode = exec_cmd(
        f"openssl pkcs12 -export -inkey /etc/certs/{suffix}.key "
        f"-in /etc/certs/{suffix}.crt -out /etc/certs/{suffix}.pkcs12 "
        f"-name {hostname} -passout pass:'{keypasswd}'"  # noqa: C812
    )
    assert retcode == 0, \
        f"Failed to generate PKCS12 keystore; reason={err.decode()}"

    # imports p12 to keystore
    _, err, retcode = exec_cmd(
        f"keytool -importkeystore -srckeystore /etc/certs/{suffix}.pkcs12 "
        f"-srcstorepass {keypasswd} -srcstoretype PKCS12 "
        f"-destkeystore /etc/certs/{suffix}.jks -deststorepass {keypasswd} "
        "-deststoretype JKS -keyalg RSA -noprompt"  # noqa: C812
    )
    assert retcode == 0, \
        f"Failed to generate JKS keystore; reason={err.decode()}"


def generate_openid_keys(passwd, jks_path, jwks_path, dn, exp=365, sig_keys=DEFAULT_SIG_KEYS, enc_keys=DEFAULT_ENC_KEYS):
    if os.path.isfile(jks_path):
        os.unlink(jks_path)
    cmd = (
        "java -Dlog4j.defaultInitOverride=true "
        "-cp /app/javalibs/* "
        "io.jans.as.client.util.KeyGenerator "
        f"-enc_keys {enc_keys} -sig_keys {sig_keys} "
        f"-dnname '{dn}' -expiration_hours {exp} "
        f"-keystore {jks_path} -keypasswd {passwd} "
        "-key_ops_type connect"
    )

    out, err, retcode = exec_cmd(cmd)
    if retcode == 0:
        with open(jwks_path, "w") as f:
            f.write(out.decode())
    return out, err, retcode


def export_openid_keys(keystore, keypasswd, alias, export_file):
    cmd = " ".join([
        "java",
        "-Dlog4j.defaultInitOverride=true",
        "-cp /app/javalibs/*",
        "io.jans.as.client.util.KeyExporter",
        "-keystore {}".format(keystore),
        "-keypasswd {}".format(keypasswd),
        "-alias {}".format(alias),
        "-exportfile {}".format(export_file),
    ])
    return exec_cmd(cmd)
