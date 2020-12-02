"""
jans.pycloudlib.utils
~~~~~~~~~~~~~~~~~~~~~

This module contains various helpers.
"""

import base64
import contextlib
import json
import os
import pathlib
import random
import re
import shlex
import socket
import ssl
import string
import subprocess
from datetime import datetime
from datetime import timedelta
from typing import Any
from typing import AnyStr
from typing import Tuple

from cryptography.hazmat.primitives.ciphers import Cipher
from cryptography.hazmat.primitives.ciphers import algorithms
from cryptography.hazmat.primitives.ciphers import modes
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.backends import default_backend
from cryptography import x509
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID


from ldap3.utils import hashed

# Default charset
_DEFAULT_CHARS = "".join([string.ascii_letters, string.digits])


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
        context = ssl.SSLContext(ssl.PROTOCOL_TLSv1_1)

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
                         state, city, base_dir="/etc/certs", extra_dn=""):
    # generate key
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)

    # generate cert
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, country_code),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, state),
        x509.NameAttribute(NameOID.LOCALITY_NAME, city),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, org_name),
        x509.NameAttribute(NameOID.COMMON_NAME, hostname),
        x509.NameAttribute(NameOID.EMAIL_ADDRESS, email),
    ])
    extra_dn = extra_dn or suffix

    now = datetime.utcnow()

    cert = x509.CertificateBuilder().subject_name(
        subject
    ).issuer_name(
        issuer
    ).public_key(
        key.public_key()
    ).serial_number(
        x509.random_serial_number()
    ).not_valid_before(
        now
    ).not_valid_after(
        now + timedelta(days=365)
    ).add_extension(
        x509.BasicConstraints(ca=False, path_length=None),
        critical=False,
    ).add_extension(
        x509.SubjectAlternativeName([
            x509.DNSName(hostname),
            x509.DNSName(extra_dn),
        ]),
        critical=False,
    ).add_extension(
        x509.KeyUsage(
            digital_signature=True,
            content_commitment=True,
            key_encipherment=True,
            data_encipherment=False,
            key_agreement=False,
            key_cert_sign=False,
            crl_sign=False,
            encipher_only=False,
            decipher_only=False,
        ),
        critical=False,
    ).sign(key, hashes.SHA256())

    # write cert and key to file
    cert_file = os.path.join(base_dir, f"{suffix}.crt")
    with open(cert_file, "wb") as f:
        cert_pem = cert.public_bytes(serialization.Encoding.PEM)
        f.write(cert_pem)

    key_file = os.path.join(base_dir, f"{suffix}.key")
    with open(key_file, "wb") as f:
        key_pem = key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.TraditionalOpenSSL,
            encryption_algorithm=serialization.NoEncryption(),
        )
        f.write(key_pem)

    # paths to cert and key respectively
    return cert_file, key_file
