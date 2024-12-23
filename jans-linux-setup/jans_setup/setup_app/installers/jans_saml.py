import os
import glob
import shutil
import time
import socket
import tempfile
import uuid
import json

from setup_app import paths
from setup_app.utils import base
from setup_app.utils.package_utils import packageUtils
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app.utils.ldif_utils import create_client_ldif

# Config
Config.idp_config_http_port = '8083'
Config.jans_idp_enabled = 'true'
Config.jans_idp_realm = 'jans'
Config.jans_idp_client_id = f'jans-{uuid.uuid4()}'
Config.jans_idp_client_secret = os.urandom(10).hex()
Config.jans_idp_grant_type = 'PASSWORD'
Config.jans_idp_user_name = 'jans'
Config.jans_idp_user_password = os.urandom(10).hex()
Config.jans_idp_idp_root_dir = os.path.join(Config.jansOptFolder, 'idp')
Config.jans_idp_ignore_validation = 'true'
Config.jans_idp_idp_metadata_file = 'idp-metadata.xml'
Config.kc_db_provider = 'postgresql'
Config.kc_db_username = 'kcdbuser'
Config.kc_db_password = 'kcdbuserpassword'
Config.kc_jdbc_url = 'jdbc:postgresql:kcdbuser:kcdbuserpassword@//localhost:1122/kc_service'

class JansSamlInstaller(BaseInstaller, SetupUtils):

    install_var = 'install_jans_saml'

    source_files = [
        (os.path.join(Config.dist_jans_dir, 'jans-scim-model.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-scim-model/{0}/jans-scim-model-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_app_dir, 'keycloak.zip'), 'https://github.com/keycloak/keycloak/releases/download/{0}/keycloak-{0}.zip'.format(base.current_app.app_info['KC_VERSION'])),
        (os.path.join(Config.dist_jans_dir, 'kc-saml-plugin.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-config-api/plugins/kc-saml-plugin/{0}/kc-saml-plugin-{0}-distribution.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'kc-jans-scheduler-deps.zip'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-scheduler/{0}/kc-jans-scheduler-{0}-deps.zip').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'kc-jans-scheduler.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-scheduler/{0}/kc-jans-scheduler-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'kc-jans-spi.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-spi/{0}/kc-jans-spi-{0}.jar').format(base.current_app.app_info['jans_version'])),
        (os.path.join(Config.dist_jans_dir, 'kc-jans-spi-deps.zip'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/kc-jans-spi/{0}/kc-jans-spi-{0}-deps.zip').format(base.current_app.app_info['jans_version'])),
            ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-saml'
        self.needdb = True
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.systemd_units = ['kc']
        self.register_progess()

        self.saml_enabled = True
        self.config_generation = True
        self.ignore_validation = True
        self.idp_root_dir = os.path.join(Config.opt_dir, 'idp/configs/')

        # sample config
        self.idp_config_id = 'keycloak'
        self.idp_config_root_dir = os.path.join(self.idp_root_dir, self.idp_config_id)
        self.idp_config_enabled = 'true'

        self.idp_config_data_dir = os.path.join(Config.opt_dir, self.idp_config_id)
        self.idp_config_log_dir = os.path.join(self.idp_config_data_dir, 'logs')
        self.idp_config_providers_dir = os.path.join(self.idp_config_data_dir, 'providers')
        self.output_folder = os.path.join(Config.output_dir, self.service_name)
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.ldif_config_fn = os.path.join(self.output_folder, 'configuration.ldif')
        self.config_json_fn = os.path.join(self.templates_folder, 'jans-saml-config.json')
        self.idp_config_fn = os.path.join(self.templates_folder, 'keycloak.conf')
        self.idp_quarkus_config_fn = os.path.join(self.templates_folder, 'quarkus.properties')
        self.clients_json_fn = os.path.join(self.templates_folder, 'clients.json')

        Config.jans_idp_idp_metadata_root_dir = os.path.join(self.idp_config_root_dir, 'idp/metadata')
        Config.jans_idp_idp_metadata_temp_dir = os.path.join(self.idp_config_root_dir, 'idp/temp_metadata')
        Config.jans_idp_sp_metadata_root_dir = os.path.join(self.idp_config_root_dir, 'sp/metadata')
        Config.jans_idp_sp_metadata_temp_dir = os.path.join(self.idp_config_root_dir, 'sp/temp_metadata')

        Config.scheduler_dir = os.path.join(Config.opt_dir, 'kc-scheduler')

        Config.idp_config_hostname = Config.hostname
        Config.keycloak_hostname = Config.hostname

        self.kc_admin_realm = 'master'
        self.kc_admin_username = 'admin'

    def install(self):
        """installation steps"""
        self.create_clients()
        self.install_keycloak()
        self.install_keycloak_scheduler()

    def render_import_templates(self):
        self.logIt("Preparing base64 encodings configuration files")

        self.renderTemplateInOut(self.config_json_fn, self.templates_folder, self.output_folder, pystring=True)
        Config.templateRenderingDict['saml_dynamic_conf_base64'] = self.generate_base64_ldap_file(
                os.path.join(self.output_folder,os.path.basename(self.config_json_fn))
            )
        self.renderTemplateInOut(self.ldif_config_fn, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config_fn])


    def create_folders(self):
        for saml_dir in (self.idp_root_dir, self.idp_config_root_dir, self.idp_config_data_dir,
                        self.idp_config_log_dir, self.idp_config_providers_dir,
                        Config.jans_idp_idp_metadata_temp_dir, Config.jans_idp_idp_metadata_root_dir,
                        Config.jans_idp_sp_metadata_root_dir, Config.jans_idp_sp_metadata_temp_dir,
                ):
            self.createDirs(saml_dir)

        self.chown(self.idp_root_dir, Config.jetty_user, Config.jetty_group, recursive=True)
        self.run([paths.cmd_chmod, '0760', saml_dir])

    def create_clients(self):
        clients_data = base.readJsonFile(self.clients_json_fn)
        client_ldif_fns = []
        for client_info in clients_data:
                check_client = self.check_clients([(client_info['client_var'], client_info['client_prefix'])])
                if check_client.get(client_info['client_prefix']) == -1:
                    scopes = client_info['scopes_dns']
                    for scope_id in client_info['scopes_ids']:
                        scope_info = self.dbUtils.search('ou=scopes,o=jans', search_filter=f'(&(objectClass=jansScope)(jansId={scope_id}))')
                        if scope_info:
                            scopes.append(scope_info['dn'])
                    client_id = getattr(Config, client_info['client_var'])
                    client_ldif_fn = os.path.join(self.output_folder, f'clients-{client_id}.ldif')
                    client_ldif_fns.append(client_ldif_fn)
                    encoded_pw_var = '_'.join(client_info["client_var"].split('_')[:-1])+'_encoded_pw'
                    if client_info['redirect_uri']:
                        for i, redirect_uri in enumerate(client_info['redirect_uri']):
                            client_info['redirect_uri'][i] = self.fomatWithDict(redirect_uri, Config.__dict__)
                    create_client_ldif(
                        ldif_fn=client_ldif_fn,
                        client_id=client_id,
                        description=client_info['description'],
                        display_name=client_info['display_name'],
                        encoded_pw=getattr(Config, encoded_pw_var),
                        scopes=scopes,
                        redirect_uri=client_info['redirect_uri'] ,
                        grant_types=client_info['grant_types'],
                        authorization_methods=client_info['authorization_methods'],
                        application_type=client_info['application_type'],
                        response_types=client_info['response_types'],
                        trusted_client=client_info['trusted_client']
                        )

        self.dbUtils.import_ldif(client_ldif_fns)

    def install_keycloak(self):
        self.logIt("Installing KC", pbar=self.service_name)
        base.unpack_zip(self.source_files[1][0], self.idp_config_data_dir, with_par_dir=False)

        # retreive auth config
        _, jans_auth_config = self.dbUtils.get_jans_auth_conf_dynamic()
        Config.templateRenderingDict['jans_auth_token_endpoint'] = jans_auth_config['tokenEndpoint']

        self.update_rendering_dict()

        self.renderTemplateInOut(self.idp_config_fn, self.templates_folder, os.path.join(self.idp_config_data_dir, 'conf'))
        self.renderTemplateInOut(self.idp_quarkus_config_fn, self.templates_folder, os.path.join(self.idp_config_data_dir, 'conf'))
        self.chown(self.idp_config_data_dir, Config.jetty_user, Config.jetty_group, recursive=True)


    def service_post_setup(self):
        self.deploy_jans_keycloak_providers()
        self.config_api_idp_plugin_config()


    def deploy_jans_keycloak_providers(self):
        self.copyFile(self.source_files[0][0], self.idp_config_providers_dir)
        self.copyFile(self.source_files[5][0], self.idp_config_providers_dir)
        base.unpack_zip(self.source_files[6][0], self.idp_config_providers_dir)


    def config_api_idp_plugin_config(self):

        # Render templates
        self.update_rendering_dict()
        jans_api_tmp_dir = os.path.join(self.templates_folder, 'kc_jans_api')
        jans_api_output_dir = os.path.join(self.output_folder, 'kc_jans_api')

        jans_api_openid_client_fn = 'jans.api-openid-client.json'
        jans_api_realm_fn = 'jans.api-realm.json'
        jans_api_user_fn = 'jans.api-user.json'
        jans_browser_auth_flow_fn = 'jans.browser-auth-flow.json'
        jans_execution_config_jans_fn = 'jans.execution-config-jans.json'
        jans_userstorage_provider_component_fn = 'jans.userstorage-provider-component.json'
        jans_disable_verify_profile_fn = 'jans.disable-required-action-verify-profile.json'
        jans_update_authenticator_config_fn = 'jans.update-authenticator-config.json'

        for tmp_fn in (jans_api_openid_client_fn, jans_api_realm_fn, jans_api_user_fn, jans_browser_auth_flow_fn, jans_disable_verify_profile_fn, jans_update_authenticator_config_fn):
            self.renderTemplateInOut(os.path.join(jans_api_tmp_dir, tmp_fn), jans_api_tmp_dir, jans_api_output_dir, pystring=True)

        self.logIt("Starting KC for config api idp plugin configurations")
        self.start()
        #wait a while for KC to start
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        for i in range(30):
            self.logIt("Wait 5 seconds to KC started")
            time.sleep(5)
            try:
                self.logIt("Connecting KC")
                s.connect(('localhost', int(Config.idp_config_http_port)))
                self.logIt("Successfully connected to KC")
                break
            except Exception:
                self.logIt("KC not ready")
        else:
            self.logIt("KC did not start in 150 seconds. Giving up configuration", errorLog=True, fatal=True)

        kcadm_cmd = '/opt/keycloak/bin/kcadm.sh'
        kcm_server_url = f'http://localhost:{Config.idp_config_http_port}/kc'
        env = {'JAVA_HOME': Config.jre_home}

        with tempfile.TemporaryDirectory() as tmp_dir:
            kc_tmp_config = os.path.join(tmp_dir, 'kcadm-jans.config')
            self.run([kcadm_cmd, 'config', 'credentials', '--server', kcm_server_url, '--realm', self.kc_admin_realm, '--user', self.kc_admin_username, '--password', 'admin', '--config', kc_tmp_config], env=env)

            self.run([kcadm_cmd, 'config', 'credentials', '--server', kcm_server_url, '--realm', self.kc_admin_realm, '--user', self.kc_admin_username, '--password', Config.admin_password, '--config', kc_tmp_config], env=env)

            # Change default password
            self.run([kcadm_cmd, 'set-password', '-r', self.kc_admin_realm, '--username', self.kc_admin_username, '--new-password', Config.admin_password, '--config', kc_tmp_config], env=env)

            # create realm
            self.run([kcadm_cmd, 'create', 'realms', '-f', os.path.join(jans_api_output_dir, jans_api_realm_fn),'--config', kc_tmp_config], env=env)

            # get realm id
            realm_result = self.run([kcadm_cmd, 'get', f'realms/{Config.jans_idp_realm}', '--fields', 'id', '--config', kc_tmp_config], env=env)
            realm_data = json.loads(realm_result)
            Config.jans_idp_realm_id = realm_data['id']

            # disable keycloak required action verify_profile
            self.run([kcadm_cmd, 'update', 'authentication/required-actions/VERIFY_PROFILE', '-r', Config.jans_idp_realm,'-f', os.path.join(jans_api_output_dir, jans_disable_verify_profile_fn),'--config', kc_tmp_config], env=env)

            # create client
            self.run([kcadm_cmd, 'create', 'clients', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_api_openid_client_fn), '--config', kc_tmp_config], env=env)

            # create user and change password
            self.run([kcadm_cmd, 'create', 'users', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_api_user_fn),'--config', kc_tmp_config], env=env)
            self.run([kcadm_cmd, 'set-password', '-r', Config.jans_idp_realm, '--username', Config.jans_idp_user_name, '--new-password', Config.jans_idp_user_password, '--config', kc_tmp_config], env=env)

            # assign roles to jans-api-user
            self.run([kcadm_cmd, 'add-roles', '-r', Config.jans_idp_realm, '--uusername', Config.jans_idp_user_name, '--cclientid', 'realm-management', '--rolename', 'manage-identity-providers', '--rolename', 'view-identity-providers', '--rolename', 'query-realms', '--rolename', 'view-realm', '--rolename', 'view-clients', '--rolename', 'manage-clients', '--rolename', 'query-clients', '--rolename', 'query-users', '--rolename', 'view-users', '--config', kc_tmp_config], env=env)

            # Create authentication flow in the jans-api realm used for saml clients
            _, result = self.run([kcadm_cmd, 'create', 'authentication/flows', '-r',  Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_browser_auth_flow_fn), '--config', kc_tmp_config], env=env, get_stderr=True)
            Config.templateRenderingDict['jans_browser_auth_flow_id'] = result.strip().split()[-1].strip("'").strip('"')

            jans_execution_auth_cookie_fn = 'jans.execution-auth-cookie.json'
            jans_execution_auth_jans_fn = 'jans.execution-auth-jans.json'

            for tmp_fn in (jans_execution_auth_cookie_fn, jans_execution_auth_jans_fn):
                self.renderTemplateInOut(os.path.join(jans_api_tmp_dir, tmp_fn), jans_api_tmp_dir, jans_api_output_dir, pystring=True)

            # Add execution steps to the flow created in the jansapi realm
            self.run([kcadm_cmd, 'create', 'authentication/executions', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_execution_auth_cookie_fn), '--config', kc_tmp_config], env=env)
            _, result = self.run([kcadm_cmd, 'create', 'authentication/executions', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_execution_auth_jans_fn), '--config', kc_tmp_config], env=env, get_stderr=True)
            jans_execution_auth_jans_id = result.strip().split()[-1].strip("'").strip('"')
            self.renderTemplateInOut(os.path.join(jans_api_tmp_dir, jans_execution_config_jans_fn), jans_api_tmp_dir, jans_api_output_dir, pystring=True)

            # Configure the jans auth execution step in realm jans-api
            self.run([kcadm_cmd, 'create', f'authentication/executions/{jans_execution_auth_jans_id}/config', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_execution_config_jans_fn), '--config', kc_tmp_config], env=env)

            # create userstorage provider component
            self.renderTemplateInOut(os.path.join(jans_api_tmp_dir, jans_userstorage_provider_component_fn), jans_api_tmp_dir, jans_api_output_dir, pystring=True)
            self.run([kcadm_cmd, 'create', 'components', '-r', Config.jans_idp_realm, '-f', os.path.join(jans_api_output_dir, jans_userstorage_provider_component_fn), '--config', kc_tmp_config], env=env)

            # turn off update profile for Review Profile
            result, _ = self.run([kcadm_cmd, 'get', 'authentication/flows/first%20broker%20login/executions', '-r', 'jans', '--config', kc_tmp_config], env=env, get_stderr=True)
            data = json.loads(result)
            for entry in data:
                if entry['displayName'] == 'Review Profile':
                    entry_auth_config_s, _ = self.run([kcadm_cmd, 'get', f'authentication/executions/{entry["id"]}', '-r', 'jans', '--config', kc_tmp_config], env=env, get_stderr=True)
                    entry_auth_config = json.loads(entry_auth_config_s)
                    self.run([kcadm_cmd, 'update', f'authentication/config/{entry_auth_config["authenticatorConfig"]}', '-f', os.path.join(jans_api_output_dir, jans_update_authenticator_config_fn),  '-r', 'jans', '--config', kc_tmp_config], env=env, get_stderr=True)
                    break

    def install_keycloak_scheduler(self):

        scheduler_templates_dir = os.path.join(self.templates_folder, 'kc-scheduler')

        # create directories
        for _ in ('bin', 'conf', 'lib', 'logs'):
            self.createDirs(os.path.join(Config.scheduler_dir, _))

        #unpack libs
        base.unpack_zip(self.source_files[3][0], os.path.join(Config.scheduler_dir, 'lib'))
        for s_config in ('config.properties', 'logback.xml'):
            base.extract_file(base.current_app.jans_zip, f'jans-keycloak-integration/job-scheduler/src/main/resources/{s_config}.sample', os.path.join(Config.scheduler_dir, 'conf'))
            os.rename(os.path.join(Config.scheduler_dir, 'conf', f'{s_config}.sample'), os.path.join(Config.scheduler_dir, 'conf', s_config))

        self.copyFile(self.source_files[4][0], os.path.join(Config.scheduler_dir, 'lib'))

        # configuration rendering identifiers
        _, jans_auth_config = self.dbUtils.get_jans_auth_conf_dynamic()
        self.check_clients([('kc_scheduler_api_client_id', '2102.')])

        rendering_dict = {
                'api_url': f'https://{Config.hostname}/jans-config-api',
                'token_endpoint': jans_auth_config['tokenEndpoint'],
                'client_id': Config.kc_scheduler_api_client_id,
                'client_secret': Config.kc_scheduler_api_client_pw,
                'scopes': '',
                'auth_method': 'basic',
                'keycloak_admin_url': f'https://{Config.idp_config_hostname}/kc',
                'keycloak_admin_realm': self.kc_admin_realm,
                'keycloak_admin_username': self.kc_admin_username,
                'keycloak_admin_password': Config.admin_password,
                'keycloak_client_id': 'admin-cli',
            }

        config_properties_fn = os.path.join(Config.scheduler_dir, 'conf','config.properties')
        config_properties_s = self.render_template(config_properties_fn, pystring=True, rendering_dict=rendering_dict)
        self.writeFile(config_properties_fn, config_properties_s, backup=False)
        self.chown(Config.scheduler_dir, Config.jetty_user, Config.jetty_group, recursive=True)

        # render start script
        self.renderTemplateInOut(
            os.path.join(scheduler_templates_dir, 'kc-scheduler.sh'),
            scheduler_templates_dir,
            os.path.join(Config.scheduler_dir, 'bin')
            )

        self.run([paths.cmd_chmod, '+x', os.path.join(Config.scheduler_dir, 'bin/kc-scheduler.sh')])

        # render contab entry and restart cron service
        self.renderTemplateInOut(
            os.path.join(scheduler_templates_dir, 'kc-scheduler-cron'),
            scheduler_templates_dir,
            '/etc/cron.d'
            )

        if not Config.installed_instance:
            self.restart(base.cron_service)

    def installed(self):
        return os.path.exists(self.idp_config_data_dir)

    def service_post_install_tasks(self):
        base.current_app.ConfigApiInstaller.install_plugin('kc-saml')
