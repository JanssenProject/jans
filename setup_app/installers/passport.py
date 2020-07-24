import os
import glob
import json

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.config import Config
from setup_app.utils import base
from setup_app.installers.node import NodeInstaller


class PassportInstaller(NodeInstaller):

    def __init__(self):
        self.service_name = 'passport'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installPassport'
        self.register_progess()

        passport_version = Config.oxVersion.replace('-SNAPSHOT','').replace('.Final','')
        self.source_files = [
                (os.path.join(Config.distGluuFolder, 'passport.tgz'), 'https://ox.gluu.org/npm/passport/passport-{}.tgz'.format(passport_version)),
                (os.path.join(Config.distGluuFolder, 'passport-node_modules.tar.gz'), 'https://ox.gluu.org/npm/passport/passport-version_{}-node_modules.tar.gz'.format(passport_version))
                ]

        self.gluu_passport_base = os.path.join(self.node_base, 'passport')
        self.passport_initd_script = os.path.join(Config.install_dir, 'static/system/initd/passport')
        self.passport_config = os.path.join(Config.configFolder, 'passport-config.json')

        self.passport_templates_folder = os.path.join(Config.templateFolder, 'passport')

        self.ldif_scripts_fn = os.path.join(Config.outputFolder, 'passport/scripts.ldif')
        self.passport_oxtrust_config_fn = os.path.join(Config.outputFolder, 'passport/passport_oxtrust_config.son')
        self.passport_central_config_json = os.path.join(Config.outputFolder, 'passport/passport-central-config.json')
        self.ldif_passport_config = os.path.join(Config.outputFolder, 'passport/oxpassport-config.ldif')
        self.ldif_passport = os.path.join(Config.outputFolder, 'passport/passport.ldif')
        self.ldif_passport_clients = os.path.join(Config.outputFolder, 'passport/passport_clients.ldif')

        self.passport_rs_client_jks_fn = os.path.join(Config.certFolder, 'passport-rs.jks')
        self.passport_rp_client_jks_fn = os.path.join(Config.certFolder, 'passport-rp.jks')
        self.passport_rp_client_cert_fn = os.path.join(Config.certFolder, 'passport-rp.pem')
        self.passportSpTLSCACert = os.path.join(Config.certFolder, 'passport-sp.pem')
        self.passportSpTLSCert = os.path.join(Config.certFolder, 'passport-sp.crt')
        self.passportSpTLSKey = os.path.join(Config.certFolder, 'passport-sp.key')
        self.passportSpJksFn = os.path.join(Config.certFolder, 'passport-sp.jks')


    def install(self):
        self.logIt("Installing passport", pbar=self.service_name)
        self.logIt("Preparing passport service base folders")
        self.run([paths.cmd_mkdir, '-p', self.gluu_passport_base])

        # Extract package
        try:
            self.logIt("Extracting {} into {}".format(self.source_files[0][0], self.gluu_passport_base))
            self.run([paths.cmd_tar, '--strip', '1', '-xzf', self.source_files[0][0], '-C', self.gluu_passport_base, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive {}".format(self.source_files[0][0]))

        modules_target_dir = os.path.join(self.gluu_passport_base, 'node_modules')
        modules_source_dir = os.path.dirname(self.source_files[1][0])
        self.run([paths.cmd_mkdir, '-p', modules_target_dir])

        node_modules_list = glob.glob(os.path.join(modules_source_dir,  'passport*node_modules*'))

        if node_modules_list:
            passport_modules_archive = max(node_modules_list)
            self.logIt("Extracting passport node modules")
            self.run([paths.cmd_tar, '--strip', '1', '-xzf', passport_modules_archive, '-C', modules_target_dir, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        else:
            # Install dependencies
            try: 
                self.logIt("Running npm install in %s" % self.gluu_passport_base)
                nodeEnv = os.environ.copy()
                nodeEnv['PATH'] = ':'.join((os.path.join(Config.node_home, 'bin'), nodeEnv['PATH']))
                cmd_npm = os.path.join(Config.node_home, 'bin', 'npm')
                self.run([cmd_npm, 'install', '-P'], self.gluu_passport_base, nodeEnv, True)
            except:
                self.logIt("Error encountered running npm install in {}".format(self.gluu_passport_base))

        # Copy init.d script
        self.copyFile(self.passport_initd_script, Config.gluuOptSystemFolder)
        self.run([paths.cmd_chmod, '-R', "755", os.path.join(Config.gluuOptSystemFolder, 'passport')])

        # Install passport system service script
        self.installNodeService('passport')

        self.run([paths.cmd_chown, '-R', 'node:node', self.gluu_passport_base])

        # enable service at startup
        self.enable()

    def installed(self):
        return os.path.exists(self.gluu_passport_base)

    def generate_configuration(self):

        self.logIt("Generating Passport configuration")
        if not Config.get('passportSpKeyPass'):
            Config.passportSpKeyPass = self.getPW()
            Config.passportSpJksPass = self.getPW()

        if not Config.get('passport_rp_client_cert_alg'):
            Config.passport_rp_client_cert_alg = 'RS512'

        if not Config.get('passport_rp_client_jks_pass'):
            Config.passport_rp_client_jks_pass = 'secret'

        if not Config.get('passport_rs_client_jks_pass'):
            Config.passport_rs_client_jks_pass = self.getPW()
            Config.passport_rs_client_jks_pass_encoded = self.obscure(Config.passport_rs_client_jks_pass)

        client_var_id_list = (
                    ('passport_rs_client_id', '1501.'),
                    ('passport_rp_client_id', '1502.'),
                    ('passport_rp_ii_client_id', '1503.'),
                    )

        self.check_clients(client_var_id_list)
        self.check_clients([('passport_resource_id', '1504.')], resource=True)

        # backup existing files
        for f in glob.glob(os.path.join(Config.certFolder, 'passport-*')):
            if not f.endswith('~'):
                self.backupFile(f, move=True)

        # create certificates
        self.gen_cert('passport-sp', Config.passportSpKeyPass, 'ldap', Config.ldap_hostname)

        # set owner and mode of certificate files
        cert_files = glob.glob(os.path.join(Config.certFolder, 'passport*'))
        for fn in cert_files:
            self.run([paths.cmd_chmod, '500', fn])
            self.run([paths.cmd_chown, 'root:gluu', fn])

        Config.passport_rs_client_jwks = self.gen_openid_jwks_jks_keys(self.passport_rs_client_jks_fn, Config.passport_rs_client_jks_pass)
        Config.templateRenderingDict['passport_rs_client_base64_jwks'] = self.generate_base64_string(Config.passport_rs_client_jwks, 1)

        Config.passport_rp_client_jwks = self.gen_openid_jwks_jks_keys(self.passport_rp_client_jks_fn, Config.passport_rp_client_jks_pass)
        Config.templateRenderingDict['passport_rp_client_base64_jwks'] = self.generate_base64_string(Config.passport_rp_client_jwks, 1)

        self.logIt("Preparing Passport OpenID RP certificate...")

        passport_rp_client_jwks_json = json.loads(''.join(Config.passport_rp_client_jwks))

        for jwks_key in passport_rp_client_jwks_json["keys"]:
            if jwks_key["alg"]  == Config.passport_rp_client_cert_alg:
                Config.passport_rp_client_cert_alias = jwks_key["kid"]
                break

        self.export_openid_key(self.passport_rp_client_jks_fn, Config.passport_rp_client_jks_pass, Config.passport_rp_client_cert_alias, self.passport_rp_client_cert_fn)


    def render_import_templates(self):

        self.logIt("Rendering Passport templates")
        output_folder = os.path.join(Config.outputFolder,'passport')
        self.renderTemplateInOut(self.passport_config, self.passport_templates_folder, Config.configFolder)
        self.renderTemplateInOut(self.passport_central_config_json, self.passport_templates_folder, output_folder)

        Config.templateRenderingDict['passport_central_config_base64'] = self.generate_base64_ldap_file(self.passport_central_config_json)

        scripts_template = os.path.join(self.passport_templates_folder, os.path.basename(self.ldif_scripts_fn))
        extensions = base.find_script_names(scripts_template)
        self.prepare_base64_extension_scripts(extensions=extensions)

        for tmp in (
                    self.passport_oxtrust_config_fn,
                    self.ldif_scripts_fn,
                    self.passport_config,
                    self.ldif_passport,
                    self.ldif_passport_clients,
                    self.ldif_passport_config,
                    ):
            self.renderTemplateInOut(tmp, self.passport_templates_folder, output_folder)

        ldif_files = (self.ldif_scripts_fn, self.ldif_passport, self.ldif_passport_config, self.ldif_passport_clients)

        self.dbUtils.import_ldif(ldif_files)


    def update_backend(self):

        self.dbUtils.enable_service('gluuPassportEnabled')

        for inum in ['2FDB-CF02', 'D40C-1CA4', '2DAF-F9A5']:
            self.dbUtils.enable_script(inum)

        passport_oxtrust_config = base.readJsonFile(self.passport_oxtrust_config_fn)

        self.dbUtils.set_oxTrustConfApplication(passport_oxtrust_config)
        self.dbUtils.set_configuration('gluuPassportEnabled', 'true')
        self.dbUtils.add_client2script('2DAF-F9A5', Config.passport_rp_client_id)
        self.dbUtils.add_client2script('2DAF-F995', Config.passport_rp_client_id)


    def create_folders(self):
        # Create logs folder
        self.run([paths.cmd_mkdir, '-p', os.path.join(self.gluu_passport_base, 'logs')])

        #create empty log file unless exists
        log_file = os.path.join(self.gluu_passport_base, 'logs/start.log')
        if not os.path.exists(log_file):
            self.writeFile(log_file, '')
