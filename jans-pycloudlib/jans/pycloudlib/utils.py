"""This module contains various helpers."""

import base64
import json
import logging
import pathlib
import random
import re
import shlex
import socket
import ssl
import string
import subprocess  # nosec: B404
import typing as _t

from cryptography import x509
from cryptography.hazmat.primitives.ciphers import Cipher
from cryptography.hazmat.primitives.ciphers import algorithms
from cryptography.hazmat.primitives.ciphers import modes
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.rsa import RSAPrivateKey
from ldap3.utils import hashed

from jans.pycloudlib.pki import generate_private_key
from jans.pycloudlib.pki import generate_public_key
from jans.pycloudlib.pki import generate_csr
from jans.pycloudlib.pki import sign_csr

# Default charset
_DEFAULT_CHARS = "".join([string.ascii_letters, string.digits])

logger = logging.getLogger(__name__)


def as_boolean(val: _t.Any) -> bool:
    """Convert value as boolean.

    If the value cannot be converted as boolean, return `False` instead.

    Args:
        val: Given value with any type, though only a subset of types that supported.

    Returns:
        `True` or `False`.
    """
    default = False
    truthy = {"t", "T", "true", "True", "TRUE", "1", 1, True}
    falsy = {"f", "F", "false", "False", "FALSE", "0", 0, False}

    if val in truthy:
        return True
    if val in falsy:
        return False
    return default


def safe_value(value: _t.Any) -> str:
    """Convert given value as JSON-friendly value.

    Args:
        value: Given value with any type.

    Returns:
        JSON string.
    """
    # `bytes` must be converted to `str` first, otherwise it will throws `TypeError`
    if isinstance(value, bytes):
        value = value.decode()

    # other types must be serialized as JSON string
    if not isinstance(value, str):
        retval = json.dumps(value)
    else:
        retval = value
    return retval


def get_random_chars(size: int = 12, chars: str = "") -> str:
    """Generate random characters.

    If character set is not provided, the default set (consists of digits and ASCII letters)
    will be used instead.

    Args:
        size: The number of generated character.
        chars: Character set to lookup to.

    Returns:
        A random string.

    Examples:
        ```py
        get_random_chars(5, chars="abcde12345")
        ```
    """
    chars = chars or _DEFAULT_CHARS
    # ignore bandit rule due to compatibility with CE
    return "".join(random.choices(chars, k=size))  # nosec: B311


def get_sys_random_chars(size: int = 12, chars: str = "") -> str:
    """Generate random characters based on OS.

    Args:
        size: The number of generated character.
        chars: Character set to lookup to.

    Returns:
        A random string.
    """
    chars = chars or _DEFAULT_CHARS
    return "".join(random.SystemRandom().choices(chars, k=size))


def exec_cmd(cmd: str) -> tuple[bytes, bytes, int]:
    """Execute shell command.

    Args:
        cmd: Shell command to be executed.

    Returns:
        A sequence consists of stdout, stderr, and return code from executed shell command.
    """
    args = shlex.split(cmd)
    # ignore bandit rule as input is escaped via `shlex.split`
    popen = subprocess.Popen(  # nosec: B603
        args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    stdout, stderr = popen.communicate()
    retcode = popen.returncode
    return stdout.strip(), stderr.strip(), retcode


def safe_render(text: str, ctx: dict[str, _t.Any]) -> str:
    """Safely render formatted text.

    Common usecase is to escape `%` character when using string formatting.

    Args:
        text: A text string.
        ctx: A `dict` of context passed to string formatting.

    Returns:
        Rendered text.
    """
    text = re.sub(r"%([^\(])", r"%%\1", text)
    # There was a % at the end?
    text = re.sub(r"%$", r"%%", text)
    return text % ctx


def reindent(text: str, num_spaces: int = 1) -> str:
    """Reindent given text with indentation per line.

    Args:
        text: A `str` or `bytes` of text.
        num_spaces: The size of indentation per line.

    Returns:
        Reindented string.
    """
    text_seq = [
        "{0}{1}".format(num_spaces * " ", line.lstrip())
        for line in text.splitlines()
    ]
    texts = "\n".join(text_seq)
    return texts


def generate_base64_contents(text: _t.AnyStr, num_spaces: int = 1) -> str:
    """Generate base64 string.

    Args:
        text: A `str` or `bytes` of text.
        num_spaces: The size of indentation per line.

    Returns:
        base64 string.
    """
    text_bytes = base64.b64encode(anystr_to_bytes(text))
    return reindent(text_bytes.decode(), num_spaces)


def cert_to_truststore(
    alias: str, cert_file: str, keystore_file: str, store_pass: str
) -> tuple[bytes, bytes, int]:
    """Import certificate into a Java Truststore using `keytool` executable.

    Args:
        alias: Alias name.
        cert_file: Path to certificate file.
        keystore_file: Path to Java Keystore/Truststore file.
        store_pass: Password of the Java Keystore/Truststore file.

    Returns:
        A sequence consists of stdout, stderr, and return code from executed shell command.
    """
    cmd = (
        "keytool -importcert -trustcacerts -alias {0} "
        "-file {1} -keystore {2} -storepass {3} "
        "-noprompt".format(alias, cert_file, keystore_file, store_pass)
    )
    return exec_cmd(cmd)


def get_server_certificate(
    host: str,
    port: int,
    filepath: str,
    server_hostname: str = ""
) -> str:
    """Get PEM-formatted certificate of a given address.

    Args:
        host: Hostname of a server.
        port: Port of SSL-secured server.
        filepath: Path to save the downloaded certificate.
        server_hostname: Optional hostname of the server.

    Returns:
        Certificate text.
    """
    server_hostname = server_hostname or host

    with socket.create_connection((host, port)) as conn:
        # use the default `PROTOCOL_TLS` constant
        context = ssl.SSLContext(ssl.PROTOCOL_TLS)

        # by default, `SSLContext.options` only excludes insecure protocols
        # SSLv2 and SSLv3; hence we need to exclude TLSv1 as well
        context.options |= ssl.OP_NO_TLSv1 | ssl.OP_NO_TLSv1_1

        with context.wrap_socket(conn, server_hostname=server_hostname) as sock:
            # getpeercert may returns `None` if there's no certificate
            der = sock.getpeercert(True)
            if der:
                cert = ssl.DER_cert_to_PEM_cert(der)
            else:
                cert = ""
            pathlib.Path(filepath).write_text(cert)
            return cert


def ldap_encode(password: _t.AnyStr) -> _t.Any:
    """Encode the password string to comply to LDAP specification.

    Args:
        password: A password with `str` or `bytes` type.

    Returns:
        A string of encoded password.
    """
    return hashed.hashed(hashed.HASHED_SALTED_SHA, password)


def anystr_to_bytes(val: _t.AnyStr) -> bytes:
    """Convert `str` or `bytes` as `bytes`.

    If given value is a `str`, encode it into `bytes`.

    Args:
        val: A `str` or `bytes` that need to be converted (if necessary).

    Returns:
        A `bytes` type of given value.
    """
    if isinstance(val, str):
        val_bytes = val.encode()
    else:
        val_bytes = val
    return val_bytes


def encode_text(text: _t.AnyStr, key: _t.AnyStr) -> bytes:
    """Encode text using triple DES and ECB mode.

    Args:
        text: Plain text (`str` or `bytes`) need to be encoded.
        key: Key used for encoding salt.

    Returns:
        Encoded `bytes` text.

    Examples:
        ```py
        # output: b'OdiOLVWUv7f8OzfNsuB5Fg=='
        encode_text("secret text", "a" * 24)
        ```
    """
    if isinstance(key, str):
        key_bytes = key.encode()
    else:
        key_bytes = key

    if isinstance(text, str):
        text_bytes = text.encode()
    else:
        text_bytes = text

    # ignore bandit rule due to compatibility with CE
    cipher = Cipher(
        algorithms.TripleDES(key_bytes), modes.ECB(), backend=default_backend(),  # nosec: B305
    )
    encryptor = cipher.encryptor()

    padder = padding.PKCS7(algorithms.TripleDES.block_size).padder()
    padded_data = padder.update(text_bytes) + padder.finalize()

    encrypted_text = encryptor.update(padded_data) + encryptor.finalize()
    return base64.b64encode(encrypted_text)


def decode_text(text: _t.AnyStr, key: _t.AnyStr) -> bytes:
    """Decode text using triple DES and ECB mode.

    Args:
        text: Encoded text (`str` or `bytes`) need to be decoded.
        key: Key used for decoding salt.

    Returns:
        Decoded `bytes` text.

    Examples:
        ```py
        # output: b'secret text'
        decode_text(b'OdiOLVWUv7f8OzfNsuB5Fg==', "a" * 24)
        ```
    """
    encoded_text = base64.b64decode(text)

    if isinstance(key, str):
        key_bytes = key.encode()
    else:
        key_bytes = key

    # ignore bandit rule due to compatibility with CE
    cipher = Cipher(
        algorithms.TripleDES(key_bytes), modes.ECB(), backend=default_backend(),  # nosec: B305
    )
    decryptor = cipher.decryptor()

    unpadder = padding.PKCS7(algorithms.TripleDES.block_size).unpadder()
    padded_data = decryptor.update(encoded_text) + decryptor.finalize()

    # decrypt the encrypted text
    return unpadder.update(padded_data) + unpadder.finalize()


#: Base directory where certs are located
CERT_BASE_DIR = "/etc/certs"


def generate_ssl_certkey(
    suffix: str,
    email: str,
    hostname: str,
    org_name: str,
    country_code: str,
    state: str,
    city: str,
    base_dir: str = CERT_BASE_DIR,
    extra_dns: _t.Union[list[str], None] = None,
    extra_ips: _t.Union[list[str], None] = None,
    valid_to: int = 365,
) -> tuple[str, str]:
    """Generate SSL public and private keys.

    Args:
        suffix: Suffix as basename (i.e. `auth-server`)
        email: Email address for subject/issuer.
        hostname: Hostname (common name) for subject/issuer.
        org_name: Organization name for subject/issuer.
        country_code: Country name in ISO format for subject/issuer.
        state: State/province name for subject/issuer.
        city: City/locality name for subject/issuer.
        base_dir: Directory to store generated public and private keys.
        extra_dns: Additional DNS names.
        extra_ips: Additional IP addresses.
        valid_to: Validity length in days.

    Returns:
        A pair of path to generated public and private keys.
    """
    key_fn = f"{base_dir}/{suffix}.key"
    priv_key = generate_private_key(key_fn)

    cert_fn = f"{base_dir}/{suffix}.crt"
    kwargs: dict[str, _t.Any] = {
        "hostname": hostname,
        "country_code": country_code,
        "state": state,
        "city": city,
        "email": email,
        "org_name": org_name,
        "extra_dns": extra_dns,
        "extra_ips": extra_ips,
        "valid_to": valid_to,
    }
    generate_public_key(cert_fn, priv_key, add_san=True, add_key_usage=True, **kwargs)
    return cert_fn, key_fn


# ignore bandit rule as invalid input will throw error
def generate_keystore(  # nosec: B107
    suffix: str,
    hostname: str,
    keypasswd: str,
    jks_fn: str = "",
    in_key: str = "",
    in_cert: str = "",
    alias: str = "",
    in_passwd: str = "",
) -> None:
    """Generate Java keystore (JKS).

    Args:
        suffix: Suffix as basename (i.e. `auth-server`)
        hostname: Hostname
        keypasswd: Password for generated JKS file
        jks_fn: Path to generated JKS file
        in_key: Path to key file
        in_cert: Path to certificate file
        alias: Alias used in generated JKS file
        in_passwd: Password/passphrase for key file (if any)
    """
    in_key = in_key or f"/etc/certs/{suffix}.key"
    in_cert = in_cert or f"/etc/certs/{suffix}.crt"
    jks_fn = jks_fn or f"/etc/certs/{suffix}.jks"
    pkcs_fn = f"/etc/certs/{suffix}.pkcs12"
    name = alias or hostname

    # converts key to pkcs12
    cmd = [
        "openssl",
        "pkcs12",
        "-export",
        f"-inkey {in_key}",
        f"-in {in_cert}",
        f"-out {pkcs_fn}",
        f"-name {name}",
        f"-passout pass:{keypasswd}",
    ]
    if in_passwd:
        cmd.append(f"-passin pass:{in_passwd}")

    cmds = " ".join(cmd)
    out, err, retcode = exec_cmd(cmds)
    if retcode != 0:
        err = err or out
        raise RuntimeError(f"Failed to generate PKCS12 keystore {pkcs_fn}; reason={err.decode()}")

    # imports p12 to keystore
    cmd = [
        "keytool",
        "-importkeystore",
        f"-srckeystore {pkcs_fn}",
        f"-srcstorepass {keypasswd}",
        "-srcstoretype PKCS12",
        f"-destkeystore {jks_fn}",
        f"-deststorepass {keypasswd}",
        "-deststoretype JKS",
        "-keyalg RSA",
        "-noprompt",
    ]
    if alias:
        cmd.append(f"-alias {alias}")
    cmds = " ".join(cmd)

    out, err, retcode = exec_cmd(cmds)
    if retcode != 0:
        err = err or out
        raise RuntimeError(f"Failed to generate JKS keystore {jks_fn}; reason={err.decode()}")


def generate_ssl_ca_certkey(
    suffix: str,
    email: str,
    hostname: str,
    org_name: str,
    country_code: str,
    state: str,
    city: str,
    base_dir: str = CERT_BASE_DIR,
    valid_to: int = 365,
) -> tuple[str, str]:
    """Generate SSL public and private keys for CA.

    Args:
        suffix: Suffix as basename (i.e. `auth-server`)
        email: Email address for subject/issuer.
        hostname: Hostname (common name) for subject/issuer.
        org_name: Organization name for subject/issuer.
        country_code: Country name in ISO format for subject/issuer.
        state: State/province name for subject/issuer.
        city: City/locality name for subject/issuer.
        base_dir: Directory to store generated public and private keys.
        valid_to: Validity length in days.

    Returns:
        A pair of path to generated public and private keys.
    """
    key_fn = f"{base_dir}/{suffix}.key"
    priv_key = generate_private_key(key_fn)

    cert_fn = f"{base_dir}/{suffix}.crt"
    kwargs: dict[str, _t.Any] = {
        "hostname": hostname,
        "country_code": country_code,
        "state": state,
        "city": city,
        "email": email,
        "org_name": org_name,
        "valid_to": valid_to,
    }
    generate_public_key(cert_fn, priv_key, is_ca=True, **kwargs)
    return cert_fn, key_fn


def generate_signed_ssl_certkey(
    suffix: str,
    ca_key_fn: str,
    ca_cert_fn: str,
    email: str,
    hostname: str,
    org_name: str,
    country_code: str,
    state: str,
    city: str,
    base_dir: str = CERT_BASE_DIR,
    extra_dns: _t.Union[list[str], None] = None,
    extra_ips: _t.Union[list[str], None] = None,
    valid_to: int = 365,
) -> tuple[str, str]:
    """Generate SSL public and private keys signed by CA.

    Args:
        suffix: Suffix as basename (i.e. `auth-server`)
        ca_key_fn: Path to CA private key.
        ca_cert_fn: Path to CA public key.
        email: Email address for subject/issuer.
        hostname: Hostname (common name) for subject/issuer.
        org_name: Organization name for subject/issuer.
        country_code: Country name in ISO format for subject/issuer.
        state: State/province name for subject/issuer.
        city: City/locality name for subject/issuer.
        base_dir: Directory to store generated public and private keys.
        extra_dns: Additional DNS names.
        extra_ips: Additional IP addresses.
        valid_to: Validity length in days.

    Returns:
        A pair of path to generated public and private keys.
    """
    key_fn = f"{base_dir}/{suffix}.key"
    priv_key = generate_private_key(key_fn)

    csr_fn = f"{base_dir}/{suffix}.csr"
    gen_kwargs: dict[str, _t.Any] = {
        "hostname": hostname,
        "country_code": country_code,
        "state": state,
        "city": city,
        "email": email,
        "org_name": org_name,
        "extra_dns": extra_dns,
        "extra_ips": extra_ips,
    }
    csr = generate_csr(csr_fn, priv_key, add_san=True, add_key_usage=True, **gen_kwargs)

    cert_fn = f"{base_dir}/{suffix}.crt"

    with open(ca_key_fn, "rb") as f:
        ca_key = serialization.load_pem_private_key(
            f.read(),
            None,
            default_backend(),
        )

        # The generated `ca_key` object has the following type:
        #
        # `Union[DHPrivateKey, Ed25519PrivateKey, Ed448PrivateKey,
        #         RSAPrivateKey, DSAPrivateKey, EllipticCurvePrivateKey,
        #         X25519PrivateKey, X448PrivateKey]`
        #
        # Passing the `ca_key` to `sign_csr` function will produces
        # incompatible type error as reported by `mypy`, hence we're casting
        # the type as `RSAPrivateKey` for type-checking only.
        #
        # Note that the actual type and value of `ca_key` are left intact.
        ca_key = _t.cast(RSAPrivateKey, ca_key)

    with open(ca_cert_fn, "rb") as f:
        ca_cert = x509.load_pem_x509_certificate(f.read())

    sign_csr(cert_fn, csr, ca_key, ca_cert, valid_to=valid_to)
    return cert_fn, key_fn
