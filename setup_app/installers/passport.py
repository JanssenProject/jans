import os
import glob
import uuid
import json

from setup_app import paths
from setup_app.config import Config
from setup_app.utils import base
from setup_app.installers.node import NodeInstaller
from setup_app.utils.ldap_utils import LDAPUtils


class PassportInstaller(NodeInstaller):

    def __init__(self):
        super().__init__()
        self.service_name = 'passport'
        self.pbar_text = "Installing Passport"

        self.gluu_passport_base = os.path.join(self.node_base, 'passport')
        self.passport_oxtrust_config_fn = os.path.join(Config.outputFolder, 'passport_oxtrust_config.son')
        self.passport_central_config_json = os.path.join(Config.outputFolder, 'passport-central-config.json')
        self.passport_config = os.path.join(Config.configFolder, 'passport-config.json')
        self.ldif_passport_config = os.path.join(Config.outputFolder, 'oxpassport-config.ldif')
        self.ldif_passport = os.path.join(Config.outputFolder, 'passport.ldif')
        self.ldif_passport_clients = os.path.join(Config.outputFolder, 'passport_clients.ldif')
        self.passport_rs_client_jks_fn = os.path.join(Config.certFolder, 'passport-rs.jks')
        self.passport_rp_client_jks_fn = os.path.join(Config.certFolder, 'passport-rp.jks')
        self.passport_rp_client_cert_fn = os.path.join(Config.certFolder, 'passport-rp.pem')
        self.passport_initd_script = os.path.join(Config.install_dir, 'static/system/initd/passport')
        self.passportSpTLSCACert = os.path.join(Config.certFolder, 'passport-sp.pem')
        self.passportSpTLSCert = os.path.join(Config.certFolder, 'passport-sp.crt')
        self.passportSpTLSKey = os.path.join(Config.certFolder, 'passport-sp.key')
        self.passportSpJksFn = os.path.join(Config.certFolder, 'passport-sp.jks')

        self.ldapUtils = LDAPUtils()

    def install(self):

        self.generate_configuration()

        self.gen_cert('passport-sp', Config.passportSpKeyPass, 'ldap', Config.ldap_hostname)

        Config.passport_rs_client_jwks = self.gen_openid_jwks_jks_keys(self.passport_rs_client_jks_fn, Config.passport_rs_client_jks_pass)
        Config.templateRenderingDict['passport_rs_client_base64_jwks'] = self.generate_base64_string(Config.passport_rs_client_jwks, 1)

        Config.passport_rp_client_jwks = self.gen_openid_jwks_jks_keys(self.passport_rp_client_jks_fn, Config.passport_rp_client_jks_pass)
        Config.templateRenderingDict['passport_rp_client_base64_jwks'] = self.generate_base64_string(Config.passport_rp_client_jwks, 1)

        self.logIt("Rendering Passport templates")
        self.renderTemplate(self.passport_central_config_json)
        Config.templateRenderingDict['passport_central_config_base64'] = self.generate_base64_ldap_file(self.passport_central_config_json)
        self.renderTemplate(self.ldif_passport_config)
        self.renderTemplate(self.ldif_passport)
        self.renderTemplate(self.ldif_passport_clients)

        if Config.mappingLocations['default'] == 'ldap':
            self.ldapUtils.import_ldif([self.ldif_passport, self.ldif_passport_config, self.ldif_passport_clients])
        else:
            #TODO: implement for couchbase ???
            self.import_ldif_couchebase([self.ldif_passport, self.ldif_passport_config, self.ldif_passport_clients])


        self.logIt("Preparing passport service base folders")
        self.run([paths.cmd_mkdir, '-p', self.gluu_passport_base])

        # Extract package
        passportArchive = 'passport.tgz'
        try:
            self.logIt("Extracting {} into {}".format(passportArchive, self.gluu_passport_base))
            self.run([paths.cmd_tar, '--strip', '1', '-xzf', os.path.join(Config.distGluuFolder, passportArchive), '-C', self.gluu_passport_base, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive {}".format(passportArchive))
            self.logIt(traceback.format_exc(), True)
        
        passport_modules_archive = os.path.join(Config.distGluuFolder, 'passport-{}-node_modules.tar.gz'.format(Config.githubBranchName))
        modules_target_dir = os.path.join(self.gluu_passport_base, 'node_modules')
        self.run([paths.cmd_mkdir, '-p', modules_target_dir])

        if os.path.exists(passport_modules_archive):
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
                self.logIt(traceback.format_exc(), True)

        # Create logs folder
        self.run([paths.cmd_mkdir, '-p', os.path.join(self.gluu_passport_base, 'server/logs')])
        
        #create empty log file
        log_file = os.path.join(self.gluu_passport_base, 'server/logs/start.log')
        self.writeFile(log_file, '')

        self.run([paths.cmd_chown, '-R', 'node:node', self.gluu_passport_base])

        self.logIt("Preparing Passport OpenID RP certificate...")

        passport_rp_client_jwks_json = json.loads(''.join(Config.passport_rp_client_jwks))
        
        for jwks_key in passport_rp_client_jwks_json["keys"]:
            if jwks_key["alg"]  == Config.passport_rp_client_cert_alg:
                Config.passport_rp_client_cert_alias = jwks_key["kid"]
                break

        self.export_openid_key(self.passport_rp_client_jks_fn, Config.passport_rp_client_jks_pass, Config.passport_rp_client_cert_alias, self.passport_rp_client_cert_fn)
        self.renderTemplateInOut(self.passport_config, Config.templateFolder, Config.configFolder)

        self.update_ldap()
        # Install passport system service script
        self.installNodeService('passport')

        # Copy init.d script
        self.copyFile(self.passport_initd_script, Config.gluuOptSystemFolder)
        self.run([paths.cmd_chmod, '-R', "755", "%s/passport" % Config.gluuOptSystemFolder])

        # enable service at startup
        self.enable()

    def generate_configuration(self):
        self.logIt("Generating Passport configuration")
        
        if not(hasattr(Config, 'passportSpKeyPass') and getattr(Config, 'passportSpKeyPass')):
            Config.passportSpKeyPass = self.getPW()
            Config.passportSpJksPass = self.getPW()

        if not(hasattr(Config, 'passport_rp_client_cert_alg') and getattr(Config, 'passport_rp_client_cert_alg')):
            Config.passport_rp_client_cert_alg = 'RS512'
        
        if not(hasattr(Config, 'passport_rp_client_jks_pass') and getattr(Config, 'passport_rp_client_jks_pass')):
            Config.passport_rp_client_jks_pass = 'secret'

        if not(hasattr(Config, 'passport_rs_client_jks_pass') and getattr(Config, 'passport_rs_client_jks_pass')):
            Config.passport_rs_client_jks_pass = self.getPW()

        if not(hasattr(Config, 'passport_rs_client_jks_pass_encoded') and  getattr(Config, 'passport_rs_client_jks_pass_encoded')):
            Config.passport_rs_client_jks_pass_encoded = self.obscure(Config.passport_rs_client_jks_pass)

        if not(hasattr(Config, 'passport_rs_client_id') and getattr(Config, 'passport_rs_client_id')):
            Config.passport_rs_client_id = '1501.{}'.format(uuid.uuid4())

        if not(hasattr(Config, 'passport_rp_client_id') and getattr(Config, 'passport_rp_client_id')):
            Config.passport_rp_client_id = '1502.{}'.format(uuid.uuid4())

        if not(hasattr(Config, 'passport_rp_ii_client_id') and getattr(Config, 'passport_rp_ii_client_id')):
            Config.passport_rp_ii_client_id = '1503.{}'.format(uuid.uuid4())

        if not(hasattr(Config, 'passport_resource_id') and getattr(Config, 'passport_resource_id')):
            Config.passport_resource_id = '1504.{}'.format(uuid.uuid4())


        Config.non_setup_properties.update(self.__dict__)
        self.renderTemplate(self.passport_oxtrust_config_fn)

    def update_ldap(self):

        for inum in ['2FDB-CF02', 'D40C-1CA4', '2DAF-F9A5']:
            self.ldapUtils.enable_script(inum)

        passport_oxtrust_config = base.readJsonFile(self.passport_oxtrust_config_fn)

        self.ldapUtils.set_oxTrustConfApplication(passport_oxtrust_config)
        self.ldapUtils.set_configuration('gluuPassportEnabled', 'true')
        self.ldapUtils.add_client2script('2DAF-F9A5', Config.passport_rp_client_id)
        self.ldapUtils.add_client2script('2DAF-F995', Config.passport_rp_client_id)
