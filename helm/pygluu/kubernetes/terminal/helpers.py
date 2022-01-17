"""
pygluu.kubernetes.terminal.common
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers for terminal prompt classes

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import base64
from pathlib import Path

import click
from pygluu.kubernetes.helpers import get_logger

logger = get_logger("gluu-prompt-common")


def gather_ip():
    """Attempts to detect and return ip automatically.
    Also set node names, zones, and addresses in a cloud deployment.

    :return:
    """
    from pygluu.kubernetes.kubeapi import Kubernetes
    from pygluu.kubernetes.settings import ValuesHandler
    import ipaddress
    kubernetes = Kubernetes()
    settings = ValuesHandler()
    logger.info("Determining OS type and attempting to gather external IP address")
    ip = ""

    # detect IP address automatically (if possible)
    try:
        node_ip_list = []
        node_zone_list = []
        node_name_list = []
        node_list = kubernetes.list_nodes().items

        for node in node_list:
            node_name = node.metadata.name
            node_addresses = kubernetes.read_node(name=node_name).status.addresses
            if settings.get("global.storageClass.provisioner") in ("microk8s.io/hostpath",
                                                                   "k8s.io/minikube-hostpath"):
                for add in node_addresses:
                    if add.type == "InternalIP":
                        ip = add.address
                        node_ip_list.append(ip)
            else:
                for add in node_addresses:
                    if add.type == "ExternalIP":
                        ip = add.address
                        node_ip_list.append(ip)
                # Digital Ocean does not provide zone support yet
                if settings.get("global.storageClass.provisioner") not in ("dobs.csi.digitalocean.com",
                                                                           "openebs.io/local"):
                    node_zone = node.metadata.labels["failure-domain.beta.kubernetes.io/zone"]
                    node_zone_list.append(node_zone)
                node_name_list.append(node_name)

        settings.set("installer-settings.nodes.names", node_name_list)
        settings.set("installer-settings.nodes.zones", node_zone_list)
        settings.set("installer-settings.nodes.ips", node_ip_list)

        if settings.get("global.storageClass.provisioner") in ("kubernetes.io/aws-ebs",
                                                               "kubernetes.io/gce-pd",
                                                               "kubernetes.io/azure-disk",
                                                               "dobs.csi.digitalocean.com",
                                                               "openebs.io/local"):
            #  Assign random IP. IP will be changed by either the update ip script, GKE external ip or nlb ip
            return "22.22.22.22"

    except Exception as e:
        logger.error(e)
        # prompt for user-inputted IP address
        logger.warning("Cannot determine IP address")
        ip = click.prompt("Please input the host's external IP address")

    if click.confirm(f"Is this the correct external IP address: {ip}", default=True):
        return ip

    while True:
        ip = click.prompt("Please input the host's external IP address")
        try:
            ipaddress.ip_address(ip)
            return ip
        except ValueError as exc:
            # raised if IP is invalid
            logger.warning(f"Cannot determine IP address; reason={exc}")


def read_file(file):
    """

    @param file:
    @return:
    """
    try:
        _ = input("Hit 'enter' or 'return' when ready.")
        with open(Path(file)) as content_file:
            content = content_file.read()
            encoded_content_bytes = base64.b64encode(content.encode("utf-8"))
            encoded_content_string = str(encoded_content_bytes, "utf-8")
            return encoded_content_string
    except FileNotFoundError:
        logger.error(f"File {file} not found.")
        raise SystemExit(1)


def read_file_bytes(file):
    """

    @param file:
    @return:
    """
    try:
        _ = input("Hit 'enter' or 'return' when ready.")
        with open(Path(file), 'rb') as content_file:
            content = content_file.read()
            encoded_content_bytes = base64.b64encode(content)
            encoded_content_string = str(encoded_content_bytes, "utf-8")
            return encoded_content_string
    except FileNotFoundError:
        logger.error(f"File {file} not found.")
        raise SystemExit(1)
