import os
import glob


from setup_app.utils.base import httpd_name, clone_type, \
    os_initdaemon, os_type, determineApacheVersion

from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller
from setup_app import paths


class PassportInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        super().__init__()
        self.service_name = 'passport'


    def install(self):
        self.logIt("Installing Passport...")

        self.passport_rs_client_jwks = self.gen_openid_jwks_jks_keys(self.passport_rs_client_jks_fn, self.passport_rs_client_jks_pass)
        self.templateRenderingDict['passport_rs_client_base64_jwks'] = self.generate_base64_string(self.passport_rs_client_jwks, 1)

        self.passport_rp_client_jwks = self.gen_openid_jwks_jks_keys(self.passport_rp_client_jks_fn, self.passport_rp_client_jks_pass)
        self.templateRenderingDict['passport_rp_client_base64_jwks'] = self.generate_base64_string(self.passport_rp_client_jwks, 1)


        self.logIt("Rendering Passport templates")
        self.renderTemplate(self.passport_central_config_json)
        self.templateRenderingDict['passport_central_config_base64'] = self.generate_base64_ldap_file(self.passport_central_config_json)
        self.renderTemplate(self.ldif_passport_config)
        self.renderTemplate(self.ldif_passport)
        self.renderTemplate(self.ldif_passport_clients)

        if self.mappingLocations['default'] == 'ldap':
            self.import_ldif_opendj([self.ldif_passport, self.ldif_passport_config, self.ldif_passport_clients])
        else:
            self.import_ldif_couchebase([self.ldif_passport, self.ldif_passport_config, self.ldif_passport_clients])


        self.logIt("Preparing passport service base folders")
        self.run([self.cmd_mkdir, '-p', self.gluu_passport_base])

        # Extract package
        passportArchive = 'passport.tgz'
        try:
            self.logIt("Extracting %s into %s" % (passportArchive, self.gluu_passport_base))
            self.run(['tar', '--strip', '1', '-xzf', '%s/%s' % (self.distGluuFolder, passportArchive), '-C', self.gluu_passport_base, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        except:
            self.logIt("Error encountered while extracting archive %s" % passportArchive)
            self.logIt(traceback.format_exc(), True)
        
        passport_modules_archive = os.path.join(self.distGluuFolder, 'passport-%s-node_modules.tar.gz' % self.githubBranchName)
        modules_target_dir = os.path.join(self.gluu_passport_base, 'node_modules')
        self.run([self.cmd_mkdir, '-p', modules_target_dir])

        if os.path.exists(passport_modules_archive):
            self.logIt("Extracting passport node modules")
            self.run(['tar', '--strip', '1', '-xzf', passport_modules_archive, '-C', modules_target_dir, '--no-xattrs', '--no-same-owner', '--no-same-permissions'])
        else:
            # Install dependencies
            try: 
                self.logIt("Running npm install in %s" % self.gluu_passport_base)

                nodeEnv = os.environ.copy()
                nodeEnv['PATH'] = '%s/bin:' % self.node_home + nodeEnv['PATH']

                self.run(['npm', 'install', '-P'], self.gluu_passport_base, nodeEnv, True)
            except:
                self.logIt("Error encountered running npm install in %s" % self.gluu_passport_base)
                self.logIt(traceback.format_exc(), True)

        # Create logs folder
        self.run([self.cmd_mkdir, '-p', '%s/server/logs' % self.gluu_passport_base])
        
        #create empty log file
        log_file = os.path.join(self.gluu_passport_base, 'server/logs/start.log')
        open(log_file,'w')

        self.run([self.cmd_chown, '-R', 'node:node', self.gluu_passport_base])

        self.logIt("Preparing Passport OpenID RP certificate...")

        passport_rp_client_jwks_json = json.loads(''.join(self.passport_rp_client_jwks))
        
        for jwks_key in passport_rp_client_jwks_json["keys"]:
            if jwks_key["alg"]  == self.passport_rp_client_cert_alg:
                self.passport_rp_client_cert_alias = jwks_key["kid"]
                break

        self.export_openid_key(self.passport_rp_client_jks_fn, self.passport_rp_client_jks_pass, self.passport_rp_client_cert_alias, self.passport_rp_client_cert_fn)
        self.renderTemplateInOut(self.passport_config, self.templateFolder, self.configFolder)


        # Install passport system service script
        self.installNodeService('passport')

        # enable service at startup
        self.enable_service_at_start('passport')

    def generate_configuration(self):
        self.passport_rs_client_jks_pass = self.getPW()
        self.passport_rs_client_jks_pass_encoded = self.obscure(self.passport_rs_client_jks_pass)

        if not self.passport_rs_client_id:
            self.passport_rs_client_id = '1501.' + str(uuid.uuid4())
        if not self.passport_rp_client_id:
            self.passport_rp_client_id = '1502.' + str(uuid.uuid4())
        if not self.passport_rp_ii_client_id:
            self.passport_rp_ii_client_id = '1503.'  + str(uuid.uuid4())
        if not self.passport_resource_id:
            self.passport_resource_id = '1504.'  + str(uuid.uuid4())

        self.renderTemplate(self.passport_oxtrust_config_fn)
