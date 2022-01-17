import pytest
from pygluu.kubernetes.couchbase import set_memory_for_buckets, create_server_spec_per_cb_service, extract_couchbase_tar
from pathlib import Path
import logging


def test_create_server_spec_per_cb_service(caplog, tmpdir):
    
    p = create_server_spec_per_cb_service(zones="zone-1", number_of_cb_service_nodes=2, cb_service_name="couch", mem_req="100Mi", mem_limit="100Mi",
                                      cpu_req="100Mi", cpu_limit="100Mi")

    assert p is p


def test_create_server_spec_per_cb_service2(caplog, tmpdir):
    
    p = create_server_spec_per_cb_service(zones="zone-1", number_of_cb_service_nodes=2, cb_service_name="couch", mem_req="100Mi", mem_limit="100Mi",
                                      cpu_req="100Mi", cpu_limit="100Mi")

    assert p is p


def extract_couchbase_tar(caplog, tmpdir):
    tar_file = Path(tmpdir) / './couchbase-source-folder'

    extract_couchbase_tar(tar_file)

    
    with caplog.at_level(logging.INFO):
        assert "Extracting" in caplog.text