import os
import glob
import random
import string
import uuid

from setup_app import paths
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller

class OxauthInstaller(JettyInstaller):

    def __init__(self):
        self.service_name = 'oxauth'
        self.pbar_text = "Installing oxauth"
        self.oxauth_war = 'https://ox.gluu.org/maven/org/gluu/oxauth-server/%s/oxauth-server-%s.war' % (Config.oxVersion, Config.oxVersion)
        self.oxauth_rp_war = 'https://ox.gluu.org/maven/org/gluu/oxauth-rp/%s/oxauth-rp-%s.war' % (Config.oxVersion, Config.oxVersion)
        self.oxAuthRPWar = 'oxauth-rp.war'

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.outputFolder, self.service_name)

        self.ldif_config = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_clients = os.path.join(self.output_folder, 'clients.ldif')
        self.oxauth_config_json = os.path.join(self.output_folder, 'oxauth-config.json')
        self.oxauth_static_conf_json = os.path.join(self.templates_folder, 'oxauth-static-conf.json')
        self.oxauth_error_json = os.path.join(self.templates_folder, 'oxauth-errors.json')
        self.oxauth_openid_jwks_fn = os.path.join(self.output_folder, 'oxauth-keys.json')
        self.oxauth_openid_jks_fn = os.path.join(Config.certFolder, 'oxauth-keys.jks')

    def install(self):
        self.logIt("Copying oxauth.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name,  'webapps')
        src_war = os.path.join(Config.distGluuFolder, 'oxauth.war')
        self.copyFile(src_war, jettyServiceWebapps)
        self.generate_configuration()
        self.render_import_templates()
        self.enable()

    def generate_configuration(self):
        if not Config.get('oxauth_openid_jks_pass'):
            Config.oxauth_openid_jks_pass = self.getPW()
        
        if not Config.get('oxauth_client_id'):
            Config.oxauth_client_id = '1001.'+ str(uuid.uuid4())
        
        if not Config.get('oxauthClient_pw'):
            Config.oxauthClient_pw = self.getPW()
            Config.oxauthClient_encoded_pw = self.obscure(Config.oxauthClient_pw)
            
        self.logIt("Generating oxauth openid keys", pbar='gluu')
        sig_keys = 'RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512'
        enc_keys = 'RSA1_5 RSA-OAEP'
        jwks = self.gen_openid_jwks_jks_keys(self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, key_expiration=2, key_algs=sig_keys, enc_keys=enc_keys)
        self.write_openid_keys(self.oxauth_openid_jwks_fn, jwks)

    def render_import_templates(self):
        # make variables of this class accesible from Config
        Config.templateRenderingDict.update(self.__dict__)
        
        self.renderTemplateInOut(self.oxauth_config_json, self.templates_folder, self.output_folder)

        Config.templateRenderingDict['oxauth_config_base64'] = self.generate_base64_ldap_file(self.oxauth_config_json)
        Config.templateRenderingDict['oxauth_static_conf_base64'] = self.generate_base64_ldap_file(self.oxauth_static_conf_json)
        Config.templateRenderingDict['oxauth_error_base64'] = self.generate_base64_ldap_file(self.oxauth_error_json)
        Config.templateRenderingDict['oxauth_openid_key_base64'] = self.generate_base64_ldap_file(self.oxauth_openid_jwks_fn)

        self.renderTemplateInOut(self.ldif_config, self.templates_folder, self.output_folder)
        self.renderTemplateInOut(self.ldif_clients, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config, self.ldif_clients])


    def install_oxauth_rp(self):
        Config.pbar.progress("oxauthrp", "Installing OxAuthRP", False)

        distOxAuthRpPath = os.path.join(Config.distGluuFolder, self.oxAuthRPWar)

        self.logIt("Copying oxauth-rp.war into jetty webapps folder...")

        jettyServiceName = 'oxauth-rp'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName])

        jettyServiceWebapps = os.path.join(self.jetty_base, jettyServiceName, 'webapps')
        src_war = os.path.join(Config.distGluuFolder, self.oxAuthRPWar)
        self.copyFile(src_war, jettyServiceWebapps)

        self.enable('oxauth-rp')

    def genRandomString(self, N):
        return ''.join(random.SystemRandom().choice(string.ascii_lowercase
                                                    + string.ascii_uppercase
                                                    + string.digits) for _ in range(N))
    def make_oxauth_salt(self):
        Config.pbar.progress("gluu", "Making oxauth salt")
        Config.pairwiseCalculationKey = self.genRandomString(random.randint(20,30))
        Config.pairwiseCalculationSalt = self.genRandomString(random.randint(20,30))

    def download_files(self, oxauth_rp=False):
        if not oxauth_rp:
            Config.pbar.progress('oxauth', "Downloading oxAuth war file")
            self.run([paths.cmd_wget, self.oxauth_war, '--no-verbose', '--retry-connrefused', '--tries=10', '-O', os.path.join(Config.distGluuFolder, 'oxauth.war')])
        
        if Config.installOxAuthRP:
            # oxAuth RP is not part of CE package. We need to download it if needed
            distOxAuthRpPath = os.path.join(Config.distGluuFolder, 'oxauth-rp.war')
            if not os.path.exists(distOxAuthRpPath):
                Config.pbar.progress('oxauth', "Downloading oxAuth RP war file", False)
                self.run([paths.cmd_wget, self.oxauth_rp_war, '--no-verbose', '--retry-connrefused', '--tries=10', '-O', os.path.join(Config.distGluuFolder,  'oxauth-rp.war')])

    def copy_static(self):
        self.copyFile(
                os.path.join(Config.install_dir, 'static/auth/lib/duo_web.py'),
                os.path.join(Config.gluuOptPythonFolder, 'libs' )
            )
        
        for conf_fn in ('duo_creds.json', 'gplus_client_secrets.json', 'super_gluu_creds.json',
                        'vericloud_gluu_creds.json', 'cert_creds.json', 'otp_configuration.json'):
            
            src_fn = os.path.join(Config.install_dir, 'static/auth/conf', conf_fn)
            self.copyFile(src_fn, Config.certFolder)
