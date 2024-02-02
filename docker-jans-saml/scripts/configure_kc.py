import base64
import os
import json
import logging.config
import sys
import tempfile
from string import Template

import backoff
from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import exec_cmd
from jans.pycloudlib.wait import get_wait_max_time
from jans.pycloudlib.wait import get_wait_interval

from healthcheck import run_healthcheck
from settings import LOGGING_CONFIG

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger("jans-saml")


def _on_backoff(details):
    error = sys.exc_info()[1]
    logger.warning(f"Unable to connect to Keycloak; reason={error}; retrying in {details['wait']:0.1f} seconds")


@backoff.on_exception(
    backoff.constant,
    Exception,
    max_time=get_wait_max_time,
    on_backoff=_on_backoff,
    on_success=None,
    on_giveup=None,
    jitter=None,
    interval=get_wait_interval,
)
def wait_for_keycloak():
    if not run_healthcheck():
        raise RuntimeError("Unable to connect to Keycloak")


class KC:
    def __init__(self, admin_username, admin_password, base_dir, ctx):
        self.admin_username = admin_username
        self.admin_password = admin_password
        self.base_dir = base_dir
        self.config_file = f"{self.base_dir}/kcadm-jans.config"
        self.ctx = ctx

        for src in ["jans.api-openid-client.json", "jans.api-realm.json", "jans.api-user.json"]:
            with open(f"/app/templates/jans-saml/kc_jans_api/{src}") as f:
                tmpl = Template(f.read())

            with open(f"{self.base_dir}/{src}", "w") as f:
                f.write(tmpl.safe_substitute(ctx))

    @property
    def server_url(self):
        host = os.environ.get("CN_SAML_HTTP_HOST", "0.0.0.0")  # nosec: B104
        port = os.environ.get("CN_SAML_HTT_PORT", "8083")
        return f"http://{host}:{port}/kc"

    @property
    def kcadm_script(self):
        return "/opt/keycloak/bin/kcadm.sh"

    def login(self):
        out, err, code = exec_cmd(
            f"{self.kcadm_script} config credentials --server {self.server_url} --realm master --user {self.admin_username!r} --password {self.admin_password!r} --config {self.config_file}"
        )

        if code != 0:
            logger.warning(f"Unable to login to Keycloak; reason={err.decode()}")
            sys.exit(1)

    def _maybe_realm_exists(self):
        # check if realm exists
        out, err, code = exec_cmd(f"{self.kcadm_script} get realms/{self.ctx['jans_idp_realm']} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to get realm {self.ctx['jans_idp_realm']}; reason={err.decode()}")
            return False
        return True

    def create_realm(self):
        if self._maybe_realm_exists():
            return

        logger.info(f"Creating realm {self.ctx['jans_idp_realm']}")

        realm_config = f"{self.base_dir}/jans.api-realm.json"

        out, err, code = exec_cmd(f"{self.kcadm_script} create realms -f {realm_config} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to create realm {self.ctx['jans_idp_realm']} specified in {realm_config}; reason={err.decode()}")

    def _maybe_client_exists(self):
        client_exists = False

        # check if client exists
        out, err, code = exec_cmd(f"{self.kcadm_script} get clients --fields 'clientId' -r {self.ctx['jans_idp_realm']} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to list clients; reason={err.decode()}")
        else:
            for datum in json.loads(out.decode()):
                if datum["clientId"] == self.ctx["jans_idp_client_id"]:
                    client_exists = True
                    break
        return client_exists

    def create_client(self):
        if self._maybe_client_exists():
            return

        logger.info(f"Creating client {self.ctx['jans_idp_client_id']} in realm {self.ctx['jans_idp_realm']}")

        client_config = f"{self.base_dir}/jans.api-openid-client.json"

        out, err, code = exec_cmd(f"{self.kcadm_script} create clients -r {self.ctx['jans_idp_realm']} -f {client_config} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to create client specified in {client_config}; reason={err.decode()}")

    def _maybe_user_exists(self):
        user_exists = False

        out, err, code = exec_cmd(f"{self.kcadm_script} get users --fields 'username' -r {self.ctx['jans_idp_realm']} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to list users; reason={err.decode()}")
        else:
            for datum in json.loads(out.decode()):
                if datum["username"] == self.ctx["jans_idp_user_name"]:
                    user_exists = True
                    break
        return user_exists

    def _reset_user_password(self, username, new_password):
        out, err, code = exec_cmd(
            f"{self.kcadm_script} set-password -r {self.ctx['jans_idp_realm']} --username {username} --new-password {new_password} --config {self.config_file}"
        )

        if code != 0:
            logger.warning(f"Unable to re-set password for  user {username}; reason={err.decode()}")

    def _assign_user_roles(self, username):
        out, err, code = exec_cmd(
            f"{self.kcadm_script} add-roles -r {self.ctx['jans_idp_realm']} --uusername {username} --cclientid realm-management --rolename manage-identity-providers --rolename view-identity-providers --config {self.config_file}"
        )

        if code != 0:
            logger.warning(f"Unable to assign roles for  user {username}; reason={err.decode()}")

    def create_user(self):
        if self._maybe_user_exists():
            return

        logger.info(f"Creating user {self.ctx['jans_idp_user_name']} in realm {self.ctx['jans_idp_realm']}")

        user_config = f"{self.base_dir}/jans.api-user.json"

        out, err, code = exec_cmd(f"{self.kcadm_script} create users -r {self.ctx['jans_idp_realm']} -f {self.base_dir}/jans.api-user.json --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to create user specified in {user_config}; reason={err.decode()}")
        else:
            # re-set password
            self._reset_user_password(self.ctx["jans_idp_user_name"], self.ctx["jans_idp_user_password"])
            self._assign_user_roles(self.ctx["jans_idp_user_name"])


def main():
    manager = get_manager()

    creds_file = os.environ.get("CN_SAML_KC_ADMIN_CREDENTIALS_FILE", "/etc/jans/conf/kc_admin_creds")

    with open(creds_file) as f:
        creds = f.read().strip()
        admin_username, admin_password = base64.b64decode(creds).decode().strip().split(":")

    ctx = {
        "jans_idp_realm": "jans-api",
        "jans_idp_client_id": manager.config.get("jans_idp_client_id"),
        "jans_idp_client_secret": manager.secret.get("jans_idp_client_secret"),
        "jans_idp_user_name": "jans-api",
        "jans_idp_user_password": manager.secret.get("jans_idp_user_password"),
    }

    # with tempfile.TemporaryDirectory() as tmp_dir:
    base_dir = os.path.join(tempfile.gettempdir(), "kc_jans_api")
    os.makedirs(base_dir, exist_ok=True)

    with manager.lock.create_lock("saml-configure-kc"):
        logger.info("Configuring Keycloak (if required)")
        kc = KC(admin_username, admin_password, base_dir, ctx)
        kc.login()
        kc.create_realm()
        kc.create_client()
        kc.create_user()


if __name__ == "__main__":
    wait_for_keycloak()
    main()
