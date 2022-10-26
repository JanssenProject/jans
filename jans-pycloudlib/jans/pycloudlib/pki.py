"""This module contains various Public Key Infrastucture (PKI) helpers."""

import os
import typing as _t
from datetime import datetime
from datetime import timedelta
from ipaddress import IPv4Address

from cryptography.hazmat.backends import default_backend
from cryptography import x509
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID


def generate_private_key(filename: str) -> rsa.RSAPrivateKeyWithSerialization:
    """Generate private key.

    Args:
        filename: Path to generated private key.
    """
    private_key = rsa.generate_private_key(
        public_exponent=65537, key_size=2048, backend=default_backend(),
    )

    alg = serialization.NoEncryption()

    with open(filename, "wb") as f:
        f.write(private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.TraditionalOpenSSL,
            encryption_algorithm=alg,
        ))
    return private_key


def generate_public_key(
    filename: str,
    private_key: rsa.RSAPrivateKey,
    is_ca: bool = False,
    add_san: bool = False,
    add_key_usage: bool = False,
    **kwargs: _t.Any
) -> x509.Certificate:
    """Generate public key (cert).

    Args:
        filename: Path to generated public key.
        private_key: An instance of PrivateKey object.
        is_ca: Whether add constraint extension as CA.
        add_san: Whether to add SubjectAlternativeName extension.
        add_key_usage: Whether to add KeyUsage extension.
        **kwargs: Keyword arguments.

    Keyword Arguments:
        email (str): Email address for subject/issuer.
        hostname (str): Hostname (common name) for subject/issuer.
        org_name (str): Organization name for subject/issuer.
        country_code (str): Country name in ISO format for subject/issuer.
        state (str): State/province name for subject/issuer.
        city (str): City/locality name for subject/issuer.
        extra_dns (list[str]): Additional DNS names (added if `add_san` argument is set to `True`).
        extra_ips (list[str]): Additional IP addresses (added if `add_san` argument is set to `True`).
        valid_to (int): Validity length in days.
    """
    valid_from = datetime.utcnow()
    validity = kwargs.get("valid_to", 365)
    valid_to = valid_from + timedelta(days=float(validity))

    country_code = kwargs.get("country_code", "")
    state = kwargs.get("state", "")
    city = kwargs.get("city", "")
    org_name = kwargs.get("org_name", "")
    hostname = kwargs.get("hostname", "")
    email = kwargs.get("email", "")

    # issuer equals subject because we use self-signed
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, country_code),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, state),
        x509.NameAttribute(NameOID.LOCALITY_NAME, city),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, org_name),
        x509.NameAttribute(NameOID.COMMON_NAME, hostname),
        x509.NameAttribute(NameOID.EMAIL_ADDRESS, email),
    ])

    builder = (
        x509.CertificateBuilder()
        .subject_name(subject)
        .issuer_name(issuer)
        .public_key(private_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(valid_from)
        .not_valid_after(valid_to)
        .add_extension(
            x509.BasicConstraints(ca=is_ca, path_length=None),
            critical=is_ca,
        )
    )

    if add_san:
        # SANs
        suffix, _ = os.path.splitext(os.path.basename(filename))

        sans: list[x509.GeneralName] = [
            x509.DNSName(hostname),
            x509.DNSName(suffix),
        ]

        # add Domains to SAN
        for dn in kwargs.get("extra_dns") or []:
            sans.append(x509.DNSName(dn))

        # add IPs to SAN
        for ip in kwargs.get("extra_ips") or []:
            sans.append(x509.IPAddress(IPv4Address(ip)))

        # make SANs unique
        sans = list(set(sans))

        builder = builder.add_extension(
            x509.SubjectAlternativeName(sans),
            critical=False,
        )

    if add_key_usage:
        builder = builder.add_extension(
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
        )

    public_key = builder.sign(
        private_key, hashes.SHA256(), backend=default_backend(),
    )

    with open(filename, "wb") as f:
        f.write(public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
        ))
    return public_key


def generate_csr(
    filename: str,
    private_key: rsa.RSAPrivateKeyWithSerialization,
    add_san: bool = False,
    add_key_usage: bool = False,
    **kwargs: _t.Any
) -> x509.CertificateSigningRequest:
    """Generate a certificate signing request (CSR).

    Args:
        filename: Path to generate CSR.
        private_key: An instance of PrivateKey object.
        add_san: Whether to add SubjectAlternativeName extension.
        add_key_usage: Whether to add KeyUsage extension.
        **kwargs: Keyword arguments.

    Keyword Arguments:
        email (str): Email address for subject/issuer.
        hostname (str): Hostname (common name) for subject/issuer.
        org_name (str): Organization name for subject/issuer.
        country_code (str): Country name in ISO format for subject/issuer.
        state (str): State/province name for subject/issuer.
        city (str): City/locality name for subject/issuer.
        extra_dns (list[str]): Additional DNS names (added if `add_san` argument is set to `True`).
        extra_ips (list[str]): Additional IP addresses (added if `add_san` argument is set to `True`).
    """
    country_code = kwargs.get("country_code", "")
    state = kwargs.get("state", "")
    city = kwargs.get("city", "")
    org_name = kwargs.get("org_name", "")
    hostname = kwargs.get("hostname", "")
    email = kwargs.get("email", "")

    subject = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, country_code),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, state),
        x509.NameAttribute(NameOID.LOCALITY_NAME, city),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, org_name),
        x509.NameAttribute(NameOID.COMMON_NAME, hostname),
        x509.NameAttribute(NameOID.EMAIL_ADDRESS, email),
    ])

    builder = (
        x509.CertificateSigningRequestBuilder()
        .subject_name(subject)
    )

    if add_san:
        # SANs
        suffix, _ = os.path.splitext(os.path.basename(filename))

        sans: list[x509.GeneralName] = [
            x509.DNSName(hostname),
            x509.DNSName(suffix),
        ]

        # add Domains to SAN
        for dn in kwargs.get("extra_dns") or []:
            sans.append(x509.DNSName(dn))

        # add IPs to SAN
        for ip in kwargs.get("extra_ips") or []:
            sans.append(x509.IPAddress(IPv4Address(ip)))

        # make SANs unique
        sans = list(set(sans))

        builder = builder.add_extension(
            x509.SubjectAlternativeName(sans),
            critical=False,
        )

    if add_key_usage:
        builder = builder.add_extension(
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
        )

    csr = builder.sign(private_key, hashes.SHA256(), backend=default_backend())

    with open(filename, "wb") as f:
        f.write(csr.public_bytes(
            serialization.Encoding.PEM
        ))
    return csr


def sign_csr(
    filename: str,
    csr: x509.CertificateSigningRequest,
    ca_private_key: rsa.RSAPrivateKeyWithSerialization,
    ca_public_key: x509.Certificate,
    **kwargs: _t.Any
) -> x509.Certificate:
    """Sign a certificate signing request (CSR).

    Args:
        filename: Path to signed certificate.
        csr: An instance of CertificateSigningRequest object.
        ca_private_key: An instance of CA PrivateKey object.
        ca_public_key: An instance of CA Certificate object.
        **kwargs: Keyword arguments.

    Keyword Arguments:
        valid_to (int): Validity length in days.
    """
    valid_from = datetime.utcnow()
    validity = kwargs.get("valid_to", 365)
    valid_to = valid_from + timedelta(days=float(validity))

    builder = (
        x509.CertificateBuilder()
        .subject_name(csr.subject)
        .issuer_name(ca_public_key.subject)
        .public_key(csr.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(valid_from)
        .not_valid_after(valid_to)
    )

    for ext in csr.extensions:
        builder = builder.add_extension(ext.value, ext.critical)

    public_key = builder.sign(
        ca_private_key, hashes.SHA256(), backend=default_backend(),
    )

    with open(filename, "wb") as f:
        f.write(public_key.public_bytes(
            serialization.Encoding.PEM
        ))
    return public_key
