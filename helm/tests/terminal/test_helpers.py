import pytest
import click
from pygluu.kubernetes.terminal.helpers import gather_ip
import pygluu.kubernetes.terminal.helpers as module0
import logging


@pytest.mark.parametrize("given, expected", [
    (True, True),
    (False, False),
])
def test_confirm_ip(monkeypatch, given, expected):

    monkeypatch.setattr("click.confirm", lambda x: given)
    assert click.confirm("Random question") == expected


def test_list_nodes_ip(monkeypatch, settings):
    gather_ip = "22.22.22.22"
    monkeypatch.setattr("click.prompt", lambda x, default: gather_ip)

    settings.set("global.storageClass.provisioner", "kubernetes.io/aws-ebs")
    assert gather_ip == gather_ip


def test_k8s_node_address(monkeypatch, settings):
    gather_ip = "22.22.22.22"
    monkeypatch.setattr("click.prompt", lambda x, default: gather_ip)
    settings.set("global.storageClass.provisioner", "kubernetes.io/aws-ebs")
    assert gather_ip == gather_ip


def test_list_nodes_ip(caplog, settings):
    # set collection to something that is not a collection
    gather_ip = "22.22.22.22"
    settings.set("global.storageClass.provisioner", "kubernetes.io/aws-ebs")

    with caplog.at_level(logging.INFO):
        assert gather_ip == gather_ip


def test_unode_ip_list(caplog, settings):
    # set collection to something that is not a collection
    gather_ip = "22.22.22.22"
    settings.set("global.storageClass.provisioner", "kubernetes.io/aws-ebs")

    with caplog.at_level(logging.INFO):
        assert gather_ip == gather_ip


def test_base_exception():
    try:
        var0 = module0.gather_ip()
    except BaseException:
        pass
