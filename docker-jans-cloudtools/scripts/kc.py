import logging.config
import os
import shutil
from string import Template

from jans.pycloudlib import get_manager
from jans.pycloudlib import wait_for
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.utils import cert_to_truststore
from jans.pycloudlib.utils import get_server_certificate

from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("cloudtools")


def render_config_props(manager):
    hostname = manager.config.get("hostname")

    ctx = {
        # config-api
        "api_url": f"https://{hostname}/jans-config-api",
        "token_endpoint": f"https://{hostname}/jans-auth/restv1/token",
        "client_id": manager.config.get("kc_scheduler_api_client_id"),
        "client_secret": manager.secret.get("kc_scheduler_api_client_pw"),
        "scopes": "",
        "auth_method": "basic",

        # keycloak-api
        "keycloak_admin_url": f"https://{hostname}/kc",
        "keycloak_admin_realm": "master",
        "keycloak_admin_username": manager.config.get("kc_admin_username"),
        "keycloak_admin_password": manager.secret.get("kc_admin_password"),
        "keycloak_client_id": "admin-cli",
    }

    with open("/app/templates/kc-scheduler/config.properties") as f:
        tmpl = Template(f.read())

    with open("/opt/kc-scheduler/conf/config.properties", "w") as f:
        f.write(tmpl.safe_substitute(ctx))


def render_logback_config():
    src = "/app/templates/kc-scheduler/logback.xml"
    dst = "/opt/kc-scheduler/conf/logback.xml"
    shutil.copyfile(src, dst)


def kc_sync(manager):
    """Sync config between jans-config-api and Keycloak."""

    if not os.path.isfile("/etc/certs/web_https.crt"):
        if as_boolean(os.environ.get("CN_SSL_CERT_FROM_SECRETS", "true")):
            manager.secret.to_file("ssl_cert", "/etc/certs/web_https.crt")
        else:
            hostname = manager.config.get("hostname")
            logger.info(f"Pulling SSL certificate from {hostname}")
            get_server_certificate(hostname, 443, "/etc/certs/web_https.crt")

    cert_to_truststore(
        "web_https",
        "/etc/certs/web_https.crt",
        "/opt/java/lib/security/cacerts",
        "changeit",
    )

    render_config_props(manager)
    render_logback_config()


if __name__ == "__main__":
    manager = get_manager()
    wait_for(manager, deps=["config", "secret"])
    kc_sync(manager)
