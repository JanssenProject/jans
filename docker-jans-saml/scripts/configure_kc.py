import base64
import os
import json
import logging.config
import sys
import tempfile
from string import Template

import backoff
from sqlalchemy import create_engine
from sqlalchemy import MetaData
from sqlalchemy.sql import text
from sqlalchemy.exc import OperationalError
from jans.pycloudlib import get_manager
from jans.pycloudlib.utils import exec_cmd
from jans.pycloudlib.utils import as_boolean
from jans.pycloudlib.wait import get_wait_max_time
from jans.pycloudlib.wait import get_wait_interval

from healthcheck import run_healthcheck
from settings import LOGGING_CONFIG
from utils import get_kc_db_password

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

    def render_templates(self, templates=None):
        templates = templates or []

        for src in templates:
            with open(f"/app/templates/jans-saml/kc_jans_api/{src}") as f:
                tmpl = Template(f.read())

            with open(f"{self.base_dir}/{src}", "w") as f:
                f.write(tmpl.safe_substitute(self.ctx))

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

    def maybe_realm_exists(self):
        # check if realm exists
        out, err, code = exec_cmd(f"{self.kcadm_script} get realms/{self.ctx['jans_idp_realm']} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to get realm {self.ctx['jans_idp_realm']}; reason={err.decode()}")
            return False
        return True

    def create_realm(self):
        if self.maybe_realm_exists():
            return

        logger.info(f"Creating realm {self.ctx['jans_idp_realm']}")

        realm_config = f"{self.base_dir}/jans.api-realm.json"

        out, err, code = exec_cmd(f"{self.kcadm_script} create realms -f {realm_config} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to create realm {self.ctx['jans_idp_realm']} specified in {realm_config}; reason={err.decode()}")

    def maybe_client_exists(self):
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
        if self.maybe_client_exists():
            return

        logger.info(f"Creating client {self.ctx['jans_idp_client_id']} in realm {self.ctx['jans_idp_realm']}")

        client_config = f"{self.base_dir}/jans.api-openid-client.json"

        out, err, code = exec_cmd(f"{self.kcadm_script} create clients -r {self.ctx['jans_idp_realm']} -f {client_config} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to create client specified in {client_config}; reason={err.decode()}")

    def maybe_user_exists(self):
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
        if self.maybe_user_exists():
            return

        logger.info(f"Creating user {self.ctx['jans_idp_user_name']} in realm {self.ctx['jans_idp_realm']}")

        user_config = f"{self.base_dir}/jans.api-user.json"

        out, err, code = exec_cmd(f"{self.kcadm_script} create users -r {self.ctx['jans_idp_realm']} -f {user_config} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to create user specified in {user_config}; reason={err.decode()}")
        else:
            # re-set password
            self._reset_user_password(self.ctx["jans_idp_user_name"], self.ctx["jans_idp_user_password"])
            self._assign_user_roles(self.ctx["jans_idp_user_name"])

    def _get_flow(self):
        flow = {}

        out, err, code = exec_cmd(f"{self.kcadm_script} get authentication/flows -r {self.ctx['jans_idp_realm']} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to list authentication flows; reason={err.decode()}")
        else:
            with open(f"{self.base_dir}/jans.browser-auth-flow.json") as f:
                alias = json.loads(f.read())["alias"]

            for datum in json.loads(out.decode()):
                if datum["alias"] == alias:
                    flow = datum
                    break
        return flow

    def get_or_create_flow(self):
        flow = self._get_flow()

        if flow:
            return flow

        with open(f"{self.base_dir}/jans.browser-auth-flow.json") as f:
            alias = json.loads(f.read())["alias"]

        logger.info(f"Creating flow {alias!r} in realm {self.ctx['jans_idp_realm']}")

        flow_config = f"{self.base_dir}/jans.browser-auth-flow.json"

        out, err, code = exec_cmd(f"{self.kcadm_script} create authentication/flows -r {self.ctx['jans_idp_realm']} -f {flow_config} --config {self.config_file}")

        if code != 0:
            logger.warning(f"Unable to create flow specified in {flow_config}; reason={err.decode()}")
            return {}

        # double check flow
        return self._get_flow()

    def create_flow_executions(self, flow):
        def _create_execution(config_fn, flow, authenticator):
            execution_id = ""
            executions = [
                execution for execution in flow["authenticationExecutions"]
                if execution["authenticator"] == authenticator
            ]
            if not executions:
                out, err, code = exec_cmd(f"{self.kcadm_script} create authentication/executions -r {self.ctx['jans_idp_realm']} -f {config_fn} --config {self.config_file}")
                if code != 0:
                    logger.warning(f"Unable to create execution specified in {config_fn}; reason={err.decode()}")
                else:
                    execution_id = err.decode().strip().split()[-1].strip("'").strip('"')
            return execution_id

        # create required executions
        _create_execution(f"{self.base_dir}/jans.execution-auth-cookie.json", flow, "auth-cookie")

        if execution_id := _create_execution(f"{self.base_dir}/jans.execution-auth-jans.json", flow, "kc-jans-authn"):
            config_fn = f"{self.base_dir}/jans.execution-config-jans.json"
            out, err, code = exec_cmd(f"{self.kcadm_script} create authentication/executions/{execution_id}/config -r {self.ctx['jans_idp_realm']} -f {config_fn} --config {self.config_file}")
            if code != 0:
                logger.warning(f"Unable to create execution config specified in {config_fn}; reason={err.decode()}")

    def grant_xa_transaction_privilege(self):
        recovery_enabled = as_boolean(os.environ.get("QUARKUS_TRANSACTION_MANAGER_ENABLE_RECOVERY", "false"))
        db_vendor = os.environ.get("KC_DB", "mysql")

        if recovery_enabled and db_vendor == "mysql":
            mysql_kc = MysqlKeycloak()

            if not mysql_kc.check_xa_recover_admin():
                mysql_kc.grant_xa_recover_admin()


class MysqlKeycloak:
    def __init__(self):
        host = os.environ.get("KC_DB_URL_HOST", "localhost")
        port = os.environ.get("KC_DB_URL_PORT", "3306")
        database = os.environ.get("KC_DB_URL_DATABASE", "keycloak")
        self.user = os.environ.get("KC_DB_USERNAME", "keycloak")
        password = get_kc_db_password()

        self.engine = create_engine(
            f"mysql+pymysql://{self.user}:{password}@{host}:{port}/{database}",
            pool_pre_ping=True,
            hide_parameters=True,
        )

        metadata = MetaData(bind=self.engine)
        metadata.reflect()

    @property
    def xa_grant_name(self):
        return "XA_RECOVER_ADMIN"

    def check_xa_recover_admin(self):
        granted = False

        with self.engine.connect() as conn:
            query = text("SHOW GRANTS FOR :username")
            for grant in conn.execute(query, username=self.user):
                if self.xa_grant_name in grant[0]:
                    granted = True
                    break

        # privilege not granted
        return granted

    def grant_xa_recover_admin(self):
        with self.engine.begin() as conn:
            logger.info(
                "Transaction recovery is enabled via QUARKUS_TRANSACTION_MANAGER_ENABLE_RECOVERY environment variable "
                f"and KC_DB is set to 'mysql'; trying to grant required privilege {self.xa_grant_name} to {self.user!r} user ..."
            )

            query = text("GRANT :grant_name ON *.* TO :username@'%';")

            try:
                conn.execute(query, grant_name=self.xa_grant_name, username=self.user)
            except OperationalError as exc:
                logger.warning(f"Unable to grant {self.xa_grant_name} privilege to {self.user!r} user; reason={exc.orig.args[1]}")

                if exc.orig.args[0] == 1227:
                    manual_query = f"""GRANT {self.xa_grant_name} ON *.* TO '{self.user}'@'%'; FLUSH PRIVILEGES;"""
                    # access denied, may need to switch user or run the query in mysql client manually
                    logger.warning(
                        f"Got insufficient permission, please try using user with {self.xa_grant_name} privilege "
                        f"and running the following query manually via MySQL client: {manual_query!r}"
                    )


def main():
    manager = get_manager()

    creds_file = os.environ.get("CN_SAML_KC_ADMIN_CREDENTIALS_FILE", "/etc/jans/conf/kc_admin_creds")

    with open(creds_file) as f:
        creds = f.read().strip()
        admin_username, admin_password = base64.b64decode(creds).decode().strip().split(":")

    ctx = {
        "jans_idp_realm": "jans",
        "jans_idp_client_id": manager.config.get("jans_idp_client_id"),
        "jans_idp_client_secret": manager.secret.get("jans_idp_client_secret"),
        "jans_idp_user_name": "jans",
        "jans_idp_user_password": manager.secret.get("jans_idp_user_password"),
        "kc_saml_openid_client_id": manager.config.get("kc_saml_openid_client_id"),
        "kc_saml_openid_client_pw": manager.secret.get("kc_saml_openid_client_pw"),
        "hostname": manager.config.get("hostname"),
    }

    base_dir = os.path.join(tempfile.gettempdir(), "kc_jans_api")
    os.makedirs(base_dir, exist_ok=True)

    with manager.lock.create_lock("saml-configure-kc"):
        logger.info("Configuring Keycloak (if required)")
        kc = KC(admin_username, admin_password, base_dir, ctx)
        kc.login()

        kc.render_templates(templates=[
            "jans.api-openid-client.json",
            "jans.api-realm.json",
            "jans.api-user.json",
            "jans.browser-auth-flow.json",
        ])
        kc.create_realm()
        kc.create_client()
        kc.create_user()

        if flow := kc.get_or_create_flow():
            kc.ctx["jans_browser_auth_flow_id"] = flow["id"]

            kc.render_templates(templates=[
                "jans.execution-auth-cookie.json",
                "jans.execution-auth-jans.json",
                "jans.execution-config-jans.json",
            ])

            kc.create_flow_executions(flow)

        # grant privilege (if required)
        kc.grant_xa_transaction_privilege()


if __name__ == "__main__":
    wait_for_keycloak()
    main()
