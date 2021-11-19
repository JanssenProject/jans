"""
jans.pycloudlib.pki
~~~~~~~~~~~~~~~~~~~

This module contains various Public Key Infrastucture (PKI) helpers.
"""

import os
from datetime import datetime
from datetime import timedelta
from ipaddress import IPv4Address

from cryptography.hazmat.backends import default_backend
from cryptography import x509
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID


def generate_private_key(filename):
    """Generate private key.

    :param filename: Path to generated private key.
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


def generate_public_key(filename, private_key, is_ca=False, add_san=False, add_key_usage=False, **kwargs):
    """Generate public key (cert).

    :param filename: Path to generated public key.
    :param private_key: An instance of PrivateKey object.
    :param is_ca: Whether add constraint extension as CA.
    :param add_san: Whether to add SubjectAlternativeName extension.
    :param add_key_usage: Whether to add KeyUsage extension.
    :param kwargs: Optional arguments.

    Keyword arguments:

        - ``email``: Email address for subject/issuer.
        - ``hostname``: Hostname (common name) for subject/issuer.
        - ``org_name``: Organization name for subject/issuer.
        - ``country_code``: Country name in ISO format for subject/issuer.
        - ``state``: State/province name for subject/issuer.
        - ``city``: City/locality name for subject/issuer.
        - ``extra_dns``: Additional DNS names (added if ``add_san`` argument is set to ``True``).
        - ``extra_ips``: Additional IP addresses (added if ``add_san`` argument is set to ``True``).
    """

    valid_from = datetime.utcnow()
    valid_to = valid_from + timedelta(days=365)

    # issuer equals subject because we use self-signed
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, kwargs.get("country_code")),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, kwargs.get("state")),
        x509.NameAttribute(NameOID.LOCALITY_NAME, kwargs.get("city")),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, kwargs.get("org_name")),
        x509.NameAttribute(NameOID.COMMON_NAME, kwargs.get("hostname")),
        x509.NameAttribute(NameOID.EMAIL_ADDRESS, kwargs.get("email")),
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

        sans = [
            x509.DNSName(kwargs.get("hostname")),
            x509.DNSName(suffix),
        ]

        # add Domains to SAN
        extra_dns = kwargs.get("extra_dns") or []
        for dn in extra_dns:
            sans.append(x509.DNSName(dn))

        # add IPs to SAN
        extra_ips = kwargs.get("extra_ips") or []
        for ip in extra_ips:
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


def generate_csr(filename, private_key, add_san=False, add_key_usage=False, **kwargs):
    """Generate a certificate signing request (CSR).

    :param filename: Path to generate CSR.
    :param private_key: An instance of PrivateKey object.
    :param add_san: Whether to add SubjectAlternativeName extension.
    :param add_key_usage: Whether to add KeyUsage extension.
    :param kwargs: Optional arguments.

    Keyword arguments:

        - ``email``: Email address for subject/issuer.
        - ``hostname``: Hostname (common name) for subject/issuer.
        - ``org_name``: Organization name for subject/issuer.
        - ``country_code``: Country name in ISO format for subject/issuer.
        - ``state``: State/province name for subject/issuer.
        - ``city``: City/locality name for subject/issuer.
        - ``extra_dns``: Additional DNS names (added if ``add_san`` argument is set to ``True``).
        - ``extra_ips``: Additional IP addresses (added if ``add_san`` argument is set to ``True``).
    """

    subject = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, kwargs.get("country_code")),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, kwargs.get("state")),
        x509.NameAttribute(NameOID.LOCALITY_NAME, kwargs.get("city")),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, kwargs.get("org_name")),
        x509.NameAttribute(NameOID.COMMON_NAME, kwargs.get("hostname")),
        x509.NameAttribute(NameOID.EMAIL_ADDRESS, kwargs.get("email")),
    ])

    builder = (
        x509.CertificateSigningRequestBuilder()
        .subject_name(subject)
    )

    if add_san:
        # SANs
        suffix, _ = os.path.splitext(os.path.basename(filename))

        sans = [
            x509.DNSName(kwargs.get("hostname")),
            x509.DNSName(suffix),
        ]

        # add Domains to SAN
        extra_dns = kwargs.get("extra_dns") or []
        for dn in extra_dns:
            sans.append(x509.DNSName(dn))

        # add IPs to SAN
        extra_ips = kwargs.get("extra_ips") or []
        for ip in extra_ips:
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


def sign_csr(filename, csr, ca_private_key, ca_public_key):
    """Sign a certificate signing request (CSR).

    :param filename: Path to signed certificate.
    :param csr: An instance of CertificateSigningRequest object.
    :param ca_private_key: An instance of CA PrivateKey object.
    :param ca_public_key: An instance of CA Certificate object.
    """

    valid_from = datetime.utcnow()
    valid_to = valid_from + timedelta(days=365)

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
