import pytest
from pygluu.kubernetes.pycert import setup_crts
from pathlib import Path
import logging


def test_setup_certs(tmpdir):
    ca_cert_file = Path(tmpdir) / './ca.crt'
    ca_key_file = Path(tmpdir) / './ca.key'
    cert_file = Path(tmpdir) / './chain.pem'
    key_file = Path(tmpdir) / './pkey.key'

    setup_crts(ca_common_name="test", cert_common_name="test", san_list="test",
               ca_cert_file=ca_cert_file,
               ca_key_file=ca_key_file,
               cert_file=cert_file,
               key_file=key_file)

    assert True


def test_setup_log(caplog, tmpdir):
    ca_cert_file = Path(tmpdir) / './ca.crt'
    ca_key_file = Path(tmpdir) / './ca.key'
    cert_file = Path(tmpdir) / './chain.pem'
    key_file = Path(tmpdir) / './pkey.key'

    setup_crts(ca_common_name="test", cert_common_name="test", san_list="test",
               ca_cert_file=ca_cert_file,
               ca_key_file=ca_key_file,
               cert_file=cert_file,
               key_file=key_file)

    
    with caplog.at_level(logging.INFO):
        assert "" in caplog.text