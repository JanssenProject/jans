import pytest


def test_generate_private_key(tmpdir):
    from jans.pycloudlib.pki import generate_private_key

    key_fn = tmpdir.join("priv.key")
    generate_private_key(str(key_fn))
    assert key_fn.read().startswith("-----BEGIN RSA PRIVATE KEY-----")


@pytest.mark.parametrize("extra_dns, extra_ips", [
    (["localhost"], ["127.0.0.1"]),
    ([], None),
    (None, []),
])
def test_generate_public_key(tmpdir, extra_dns, extra_ips):
    from jans.pycloudlib.pki import generate_private_key
    from jans.pycloudlib.pki import generate_public_key

    key_fn = tmpdir.join("priv.key")
    priv_key = generate_private_key(str(key_fn))

    cert_fn = tmpdir.join("pub.crt")
    generate_public_key(
        str(cert_fn),
        priv_key,
        is_ca=True,
        add_san=True,
        add_key_usage=True,
        email="s@example.com",
        hostname="example.com",
        org_name="Organization",
        country_code="US",
        state="TX",
        city="Austin",
        extra_dns=extra_dns,
        extra_ips=extra_ips,
    )
    assert cert_fn.read().startswith("-----BEGIN CERTIFICATE-----")


@pytest.mark.parametrize("extra_dns, extra_ips", [
    (["localhost"], ["127.0.0.1"]),
    ([], None),
    (None, []),
])
def test_generate_csr(tmpdir, extra_dns, extra_ips):
    from jans.pycloudlib.pki import generate_private_key
    from jans.pycloudlib.pki import generate_csr

    key_fn = tmpdir.join("priv.key")
    priv_key = generate_private_key(str(key_fn))

    csr_fn = tmpdir.join("pub.csr")
    generate_csr(
        str(csr_fn),
        priv_key,
        add_san=True,
        add_key_usage=True,
        email="s@example.com",
        hostname="example.com",
        org_name="Organization",
        country_code="US",
        state="TX",
        city="Austin",
        extra_dns=extra_dns,
        extra_ips=extra_ips,
    )
    assert csr_fn.read().startswith("-----BEGIN CERTIFICATE REQUEST-----")


def test_sign_csr(tmpdir):
    from jans.pycloudlib.pki import generate_private_key
    from jans.pycloudlib.pki import generate_public_key
    from jans.pycloudlib.pki import generate_csr
    from jans.pycloudlib.pki import sign_csr

    ca_key_fn = tmpdir.join("ca_priv.key")
    ca_key = generate_private_key(str(ca_key_fn))

    ca_cert_fn = tmpdir.join("ca_pub.crt")
    ca_cert = generate_public_key(
        str(ca_cert_fn),
        ca_key,
        is_ca=True,
        email="s@example.com",
        hostname="CA",
        org_name="Organization",
        country_code="US",
        state="TX",
        city="Austin",
    )

    key_fn = tmpdir.join("priv.key")
    priv_key = generate_private_key(str(key_fn))

    csr_fn = tmpdir.join("pub.csr")
    csr = generate_csr(
        str(csr_fn),
        priv_key,
        add_san=True,
        add_key_usage=True,
        email="s@example.com",
        hostname="example.com",
        org_name="Organization",
        country_code="US",
        state="TX",
        city="Austin",
        extra_dns=["localhost"],
        extra_ips=["127.0.0.1"],
    )

    cert_fn = tmpdir.join("pub.crt")
    sign_csr(
        str(cert_fn),
        csr,
        ca_key,
        ca_cert,
    )
    assert cert_fn.read().startswith("-----BEGIN CERTIFICATE-----")
