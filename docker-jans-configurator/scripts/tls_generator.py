import base64
import logging.config

import click
from kubernetes import client
from kubernetes import config

from jans.pycloudlib import get_manager
from jans.pycloudlib.wait import wait_for

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("tls-generator")


def patch_or_create_namespaced_secret(
    name,
    literal,
    value_of_literal,
    namespace="default",
    secret_type="Opaque",  # nosec: B107
    second_literal=None,
    value_of_second_literal=None,
    data=None,
):
    """Patch secret and if not exist create
    :param name:
    :param literal:
    :param value_of_literal:
    :param namespace:
    :param secret_type:
    :param second_literal:
    :param value_of_second_literal:
    :param data:
    :return:
    """
    # use the serviceAccount k8s gives to pods
    config.load_incluster_config()
    core_cli = client.CoreV1Api()

    # Instantiate the Secret object
    body = client.V1Secret()
    metadata = client.V1ObjectMeta(name=name)
    body.data = data

    if not data:
        body.data = {literal: value_of_literal}

    body.metadata = metadata
    body.type = secret_type

    if second_literal:
        body.data = {literal: value_of_literal, second_literal: value_of_second_literal}
    try:
        core_cli.patch_namespaced_secret(name, namespace, body)
        logger.info("Secret  %s in namespace %s has been patched", name, namespace)
        return
    except client.rest.ApiException as e:
        if e.status == 404 or not e.status:
            try:
                core_cli.create_namespaced_secret(namespace=namespace, body=body)
                logger.info("Created secret %s of type %s in namespace %s", name, secret_type, namespace)
                return True
            except client.rest.ApiException as e:
                logger.exception(e)
                return False
        logger.exception(e)
        return False


# check if janssen secret exists
def get_certs():
    """Get cert and key from secret layer.

    :return: ssl cert and key (if any)
    """
    def b64encode(value):
        return base64.b64encode(value.encode()).decode()

    manager = get_manager()
    wait_for(manager, deps=["secret"])

    # returns empty string if not found
    ssl_cert = manager.secret.get("ssl_cert")
    if ssl_cert:
        ssl_cert = b64encode(ssl_cert)

    # returns empty string if not found
    ssl_key = manager.secret.get("ssl_key")
    if ssl_key:
        ssl_key = b64encode(ssl_key)
    return ssl_cert, ssl_key


@click.command()
@click.option("--namespace", default="default")
@click.option("--secret-name", default="tls-certificate")
@click.option("--secret-type", default="kubernetes.io/tls")
def main(namespace, secret_name, secret_type):
    cert, key = get_certs()

    if cert and key:
        patch_or_create_namespaced_secret(
            name=secret_name,
            namespace=namespace,
            literal="tls.crt",
            value_of_literal=cert,
            secret_type=secret_type,
            second_literal="tls.key",
            value_of_second_literal=key,
        )
    else:
        logger.error(
            "No certificate or key was found in secrets. "
            "This can happen when the ssl certificate for the domain is able to be pulled. "
            "In that scenario the ssl_cert will be pulled from the domain provided."
        )


if __name__ == "__main__":
    main(prog_name="tls-generator")
