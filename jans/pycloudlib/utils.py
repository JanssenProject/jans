"""
jans.pycloudlib.utils
~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers.
"""

import base64
import contextlib
import json
import logging
import pathlib
import random
import re
import shlex
import socket
import ssl
import string
import subprocess
from typing import Any
from typing import AnyStr
from typing import Tuple

from cryptography import x509
from cryptography.hazmat.primitives.ciphers import Cipher
from cryptography.hazmat.primitives.ciphers import algorithms
from cryptography.hazmat.primitives.ciphers import modes
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from ldap3.utils import hashed

from jans.pycloudlib.pki import generate_private_key
from jans.pycloudlib.pki import generate_public_key
from jans.pycloudlib.pki import generate_csr
from jans.pycloudlib.pki import sign_csr

# Default charset
_DEFAULT_CHARS = "".join([string.ascii_letters, string.digits])

logger = logging.getLogger(__name__)


def as_boolean(val: Any) -> bool:
    """Convert value as boolean.

    If the value cannot be converted as boolean, return ``False`` instead.

    :param val: Given value with any type, though only a subset of types that supported.
    :return: ``True`` or ``False``.
    """
    default = False
    truthy = {"t", "T", "true", "True", "TRUE", "1", 1, True}
    falsy = {"f", "F", "false", "False", "FALSE", "0", 0, False}

    if val in truthy:
        return True
    if val in falsy:
        return False
    return default


def safe_value(value: Any) -> str:
    """Convert given value as JSON-friendly value.

    :param val: Given value with any type.
    :return: JSON string.
    """
    # bytes must be converted to str first, otherwise it will throws ``TypeError``
    if isinstance(value, bytes):
        value = value.decode()

    # other types must be serialized as JSON string
    if not isinstance(value, str):
        value = json.dumps(value)
    return value


def get_random_chars(size: int = 12, chars: str = "") -> str:
    """Generate random characters.

    If character set is not provided, the default set (consists of digits and ASCII letters)
    will be used instead.

    Example:

    .. code-block:: python

        get_random_chars(5, chars="abcde12345")

    :param size: The number of generated character.
    :param chars: Character set to lookup to.
    :return: A random string.
    """
    chars = chars or _DEFAULT_CHARS
    return "".join(random.choices(chars, k=size))


def get_sys_random_chars(size: int = 12, chars: str = "") -> str:
    """Generate random characters based on OS.

    :param size: The number of generated character.
    :param chars: Character set to lookup to.
    :return: A random string.
    """
    chars = chars or _DEFAULT_CHARS
    return "".join(random.SystemRandom().choices(chars, k=size))


def exec_cmd(cmd: str) -> Tuple[bytes, bytes, int]:
    """Execute shell command.

    :param str: Shell command to be executed.
    :return: A ``tuple`` consists of stdout, stderr, and return code from executed shell command.
    """
    args = shlex.split(cmd)
    popen = subprocess.Popen(
        args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    stdout, stderr = popen.communicate()
    retcode = popen.returncode
    return stdout.strip(), stderr.strip(), retcode


def safe_render(text: str, ctx: dict) -> str:
    """Safely render formatted text.

    Common usecase is to escape ``%`` character when using string formatting.

    :param text: A text string.
    :param ctx: A ``dict`` of context passed to string formatting.
    :return: Rendered text.
    """
    text = re.sub(r"%([^\(])", r"%%\1", text)
    # There was a % at the end?
    text = re.sub(r"%$", r"%%", text)
    return text % ctx


def reindent(text: str, num_spaces: int = 1) -> str:
    """Reindent given text with indentation per line.

    :param text: A ``str`` or ``bytes`` of text.
    :param num_spaces: The size of indentation per line.
    :return: Reindented string.
    """
    text = [
        "{0}{1}".format(num_spaces * " ", line.lstrip()) for line in text.splitlines()
    ]
    text = "\n".join(text)
    return text


def generate_base64_contents(text: AnyStr, num_spaces: int = 1) -> str:
    """Generate base64 string.

    :param text: A ``str`` or ``bytes`` of text.
    :param num_spaces: The size of indentation per line.
    :return: base64 string.
    """
    text = base64.b64encode(anystr_to_bytes(text))
    return reindent(text.decode(), num_spaces)


def cert_to_truststore(
    alias: str, cert_file: str, keystore_file: str, store_pass: str
) -> Tuple[bytes, bytes, int]:
    """Import certificate into a Java Truststore using ``keytool`` executable.

    :param alias: Alias name.
    :param cert_file: Path to certificate file.
    :param keystore_file: Path to Java Keystore/Truststore file.
    :param store_pass: Password of the Java Keystore/Truststore file.
    :return: A ``tuple`` consists of stdout, stderr, and return code from executed shell command.
    """
    cmd = (
        "keytool -importcert -trustcacerts -alias {0} "
        "-file {1} -keystore {2} -storepass {3} "
        "-noprompt".format(alias, cert_file, keystore_file, store_pass)
    )
    return exec_cmd(cmd)


def get_server_certificate(
    host: str, port: int, filepath: str, server_hostname: str = ""
) -> str:
    """Get PEM-formatted certificate of a given address.

    :param host: Hostname of a server.
    :param port: Port of SSL-secured server.
    :param filepath: Path to save the downloaded certificate.
    :param server_hostname: Optional hostname of the server.
    :return: Certificate text.
    """
    server_hostname = server_hostname or host

    with socket.create_connection((host, port)) as conn:
        # use the default ``PROTOCOL_TLS`` constant
        context = ssl.SSLContext(ssl.PROTOCOL_TLS)

        # by default, ``SSLContext.options`` only excludes insecure protocols
        # SSLv2 and SSLv3; hence we need to exclude TLSv1 as well
        context.options |= ssl.OP_NO_TLSv1 | ssl.OP_NO_TLSv1_1

        with context.wrap_socket(conn, server_hostname=server_hostname) as sock:
            der = sock.getpeercert(True)
            cert = ssl.DER_cert_to_PEM_cert(der)
            pathlib.Path(filepath).write_text(cert)
            return cert


def ldap_encode(password: AnyStr) -> str:
    """Encode the password string to comply to LDAP specification.

    :param password: A password with ``str`` or ``bytes`` type.
    :return: A string of encoded password.
    """
    return hashed.hashed(hashed.HASHED_SALTED_SHA, password)


def anystr_to_bytes(val: AnyStr) -> bytes:
    """Convert ``str`` or ``bytes`` as ``bytes``.

    If given value is a ``str``, encode it into ``bytes``.

    :param val: A ``str`` or ``bytes`` that need to be converted (if necessary).
    :return: A ``bytes`` type of given value.
    """
    if isinstance(val, str):
        val = val.encode()
    return val


def encode_text(text: AnyStr, key: AnyStr) -> bytes:
    """Encode text using triple DES and ECB mode.

    .. code-block:: python

        # output: b'OdiOLVWUv7f8OzfNsuB5Fg=='
        encode_text("secret text", "a" * 24)

    :params text: Plain text (``str`` or ``bytes``) need to be encoded.
    :params key: Key used for encoding salt.
    :returns: Encoded ``bytes`` text.
    """
    with contextlib.suppress(AttributeError):
        # ``key`` must be a ``bytes``
        key = key.encode()

    with contextlib.suppress(AttributeError):
        # ``text`` must be a ``bytes``
        text = text.encode()

    cipher = Cipher(
        algorithms.TripleDES(key), modes.ECB(), backend=default_backend(),
    )
    encryptor = cipher.encryptor()

    padder = padding.PKCS7(algorithms.TripleDES.block_size).padder()
    padded_data = padder.update(text) + padder.finalize()

    encrypted_text = encryptor.update(padded_data) + encryptor.finalize()
    return base64.b64encode(encrypted_text)


def decode_text(text: AnyStr, key: AnyStr) -> bytes:
    """Decode text using triple DES and ECB mode.

    .. code-block:: python

        # output: b'secret text'
        decode_text(b'OdiOLVWUv7f8OzfNsuB5Fg==', "a" * 24)

    :params text: Encoded text (``str`` or ``bytes``) need to be decoded.
    :params key: Key used for decoding salt.
    :returns: Decoded ``bytes`` text.
    """
    encoded_text = base64.b64decode(text)

    with contextlib.suppress(AttributeError):
        # ``key`` must be a ``bytes``
        key = key.encode()

    cipher = Cipher(
        algorithms.TripleDES(key), modes.ECB(), backend=default_backend(),
    )
    decryptor = cipher.decryptor()

    unpadder = padding.PKCS7(algorithms.TripleDES.block_size).unpadder()
    padded_data = decryptor.update(encoded_text) + decryptor.finalize()

    # decrypt the encrypted text
    return unpadder.update(padded_data) + unpadder.finalize()


def generate_ssl_certkey(suffix, email, hostname, org_name, country_code,
                         state, city, base_dir="/etc/certs",
                         extra_dns=None, extra_ips=None):
    key_fn = f"{base_dir}/{suffix}.key"
    priv_key = generate_private_key(key_fn)

    cert_fn = f"{base_dir}/{suffix}.crt"
    generate_public_key(
        cert_fn,
        priv_key,
        add_san=True,
        add_key_usage=True,
        hostname=hostname,
        country_code=country_code,
        state=state,
        city=city,
        email=email,
        org_name=org_name,
        extra_dns=extra_dns,
        extra_ips=extra_ips,
    )
    return cert_fn, key_fn


def generate_keystore(suffix, hostname, keypasswd, jks_fn="", in_key="", in_cert="", alias="", in_passwd=""):
    """
    Generate Java keystore (JKS).

    :param suffix: Suffix as basename (i.e. ``auth-server``)
    :param hostname: Hostname
    :param keypasswd: Password for generated JKS file
    :param jks_fn: Path to generated JKS file
    :param in_key: Path to key file
    :param in_cert: Path to certificate file
    :param alias: Alias used in generated JKS file
    :param in_passwd: Password/passphrase for key file (if any)
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

    cmd = " ".join(cmd)
    out, err, retcode = exec_cmd(cmd)
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
    cmd = " ".join(cmd)

    out, err, retcode = exec_cmd(cmd)
    if retcode != 0:
        err = err or out
        raise RuntimeError(f"Failed to generate JKS keystore {jks_fn}; reason={err.decode()}")


def generate_ssl_ca_certkey(suffix, email, hostname, org_name, country_code,
                            state, city, base_dir="/etc/certs"):

    key_fn = f"{base_dir}/{suffix}.key"
    priv_key = generate_private_key(key_fn)

    cert_fn = f"{base_dir}/{suffix}.crt"
    generate_public_key(
        cert_fn,
        priv_key,
        is_ca=True,
        hostname=hostname,
        country_code=country_code,
        state=state,
        city=city,
        email=email,
        org_name=org_name,
    )
    return cert_fn, key_fn


def generate_signed_ssl_certkey(suffix, ca_key_fn, ca_cert_fn, email, hostname, org_name,
                                country_code, state, city, base_dir="/etc/certs",
                                extra_dns=None, extra_ips=None):
    key_fn = f"{base_dir}/{suffix}.key"
    priv_key = generate_private_key(key_fn)

    csr_fn = f"{base_dir}/{suffix}.csr"
    csr = generate_csr(
        csr_fn,
        priv_key,
        add_san=True,
        add_key_usage=True,
        hostname=hostname,
        country_code=country_code,
        state=state,
        city=city,
        email=email,
        org_name=org_name,
        extra_dns=extra_dns,
        extra_ips=extra_ips,
    )

    cert_fn = f"{base_dir}/{suffix}.crt"

    with open(ca_key_fn, "rb") as f:
        ca_key = serialization.load_pem_private_key(
            f.read(),
            None,
            default_backend(),
        )

    with open(ca_cert_fn, "rb") as f:
        ca_cert = x509.load_pem_x509_certificate(f.read())

    sign_csr(cert_fn, csr, ca_key, ca_cert)
    return cert_fn, key_fn


def secure_password_file(password_file, salt):
    """Secure password file by encoding the contents (if required).

    :param password_file: Path to password file.
    :param salt: Salt string.
    :returns: Contents of password file (in plaintext format).
    """
    password = ""

    # get password
    with open(password_file) as f:
        password = f.read().strip()

    # check if password is encoded; non-encoded and empty password will throw incorrect
    # padding/bytes which will be handled by encoding the password;
    # other errors will be thrown automatically by interpreter
    should_encode = False

    try:
        password = decode_text(password, salt).decode()
    except ValueError:
        if not password:
            msg = f"Got empty password in {password_file}"
        else:
            msg = f"Current password in {password_file} is not encoded"
        logger.warning(msg)
        should_encode = True

    if should_encode:
        logger.warning(f"Attempting to encode the password in {password_file}")
        with open(password_file, "w") as f:
            f.write(encode_text(password, salt).decode())

    # returns plain password for compatibility
    return password
