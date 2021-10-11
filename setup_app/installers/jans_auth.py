import os
import glob
import random
import string
import uuid
import shutil
from urllib.parse import urlparse

from setup_app import paths
from setup_app.utils import base
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.static import AppType, InstallOption

class JansAuthInstaller(JettyInstaller):

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-auth'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installOxAuth'
        self.register_progess()

        self.source_files = [
                    (os.path.join(Config.distJansFolder, 'jans-auth.war'), 'https://ox.gluu.org/maven/org/gluu/oxauth-server/%s/oxauth-server-%s.war' % (Config.oxVersion, Config.oxVersion)),
                    (os.path.join(Config.distJansFolder, 'jans-auth-rp.war'), 'https://ox.gluu.org/maven/org/gluu/jans-auth-rp/%s/jans-auth-rp-%s.war' % (Config.oxVersion, Config.oxVersion))
                    ]

        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.outputFolder, self.service_name)

        self.ldif_config = os.path.join(self.output_folder, 'configuration.ldif')
        self.oxauth_config_json = os.path.join(self.output_folder, 'jans-auth-config.json')
        self.oxauth_static_conf_json = os.path.join(self.templates_folder, 'jans-auth-static-conf.json')
        self.oxauth_error_json = os.path.join(self.templates_folder, 'jans-auth-errors.json')
        self.oxauth_openid_jwks_fn = os.path.join(self.output_folder, 'jans-auth-keys.json')
        self.oxauth_openid_jks_fn = os.path.join(Config.certFolder, 'jans-auth-keys.jks')
        self.ldif_people = os.path.join(self.output_folder, 'people.ldif')
        self.ldif_groups = os.path.join(self.output_folder, 'groups.ldif')

    def install(self):
        self.logIt("Copying auth.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)
        self.war_for_jetty10(os.path.join(jettyServiceWebapps, os.path.basename(self.source_files[0][0])))

        self.enable()

    def generate_configuration(self):
        if not Config.get('oxauth_openid_jks_pass'):
            Config.oxauth_openid_jks_pass = self.getPW()

        if not Config.get('admin_inum'):
            Config.admin_inum = str(uuid.uuid4())

        if Config.profile == 'jans':
            Config.encoded_admin_password = self.ldap_encode(Config.admin_password)

        self.logIt("Generating OAuth openid keys", pbar=self.service_name)
        sig_keys = 'RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512'
        enc_keys = 'RSA1_5 RSA-OAEP'
        jwks = self.gen_openid_jwks_jks_keys(self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, key_expiration=2, key_algs=sig_keys, enc_keys=enc_keys)
        self.write_openid_keys(self.oxauth_openid_jwks_fn, jwks)

        if Config.get('use_external_key'):
            self.import_openbanking_key()

    def render_import_templates(self):

        templates = [self.oxauth_config_json]
        if Config.profile == 'jans':
            templates += [self.ldif_people, self.ldif_groups]

        for tmp in templates:
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)

        Config.templateRenderingDict['oxauth_config_base64'] = self.generate_base64_ldap_file(self.oxauth_config_json)
        Config.templateRenderingDict['oxauth_static_conf_base64'] = self.generate_base64_ldap_file(self.oxauth_static_conf_json)
        Config.templateRenderingDict['oxauth_error_base64'] = self.generate_base64_ldap_file(self.oxauth_error_json)
        Config.templateRenderingDict['oxauth_openid_key_base64'] = self.generate_base64_ldap_file(self.oxauth_openid_jwks_fn)

        self.ldif_scripts = os.path.join(Config.outputFolder, 'scripts.ldif')
        self.renderTemplateInOut(self.ldif_scripts, Config.templateFolder, Config.outputFolder)
        self.renderTemplateInOut(self.ldif_config, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config, self.ldif_scripts])
        if Config.profile == 'jans':
            self.dbUtils.import_ldif([self.ldif_people, self.ldif_groups])
        if Config.profile == 'openbanking':
            self.import_openbanking_certificate()

    def install_oxauth_rp(self):
        self.download_files(downloads=[self.source_files[1][0]])

        Config.pbar.progress(self.service_name, "Installing OxAuthRP", False)

        self.logIt("Copying jans-auth-rp.war into jetty webapps folder...")

        jettyServiceName = 'jans-auth-rp'
        self.installJettyService(self.jetty_app_configuration[jettyServiceName])

        jettyServiceWebapps = os.path.join(self.jetty_base, jettyServiceName, 'webapps')
        self.copyFile(self.source_files[1][0], jettyServiceWebapps)

        self.enable('jans-auth-rp')

    def genRandomString(self, N):
        return ''.join(random.SystemRandom().choice(string.ascii_lowercase
                                                    + string.ascii_uppercase
                                                    + string.digits) for _ in range(N))
    def make_salt(self, enforce=False):
        if not Config.get('pairwiseCalculationKey') or enforce:
            Config.pairwiseCalculationKey = self.genRandomString(random.randint(20,30))
        if not Config.get('pairwiseCalculationSalt') or enforce:
            Config.pairwiseCalculationSalt = self.genRandomString(random.randint(20,30))

    def copy_static(self):
        self.copyFile(
                os.path.join(Config.install_dir, 'static/auth/lib/duo_web.py'),
                os.path.join(Config.jansOptPythonFolder, 'libs' )
            )

        for conf_fn in ('duo_creds.json', 'gplus_client_secrets.json', 'super_gluu_creds.json',
                        'vericloud_jans_creds.json', 'cert_creds.json', 'otp_configuration.json'):

            src_fn = os.path.join(Config.install_dir, 'static/auth/conf', conf_fn)
            self.copyFile(src_fn, Config.certFolder)

    def import_openbanking_certificate(self):
        self.logIt("Importing openbanking ssl certificate")
        oxauth_config_json = base.readJsonFile(self.oxauth_config_json)
        jwksUri = oxauth_config_json['jwksUri']
        o = urlparse(jwksUri)
        jwks_addr = o.netloc
        ssl_cmd = shutil.which('openssl')
        random_crt_fn = os.path.join(output_folder, '{}.crt'.format(os.urandom(3).hex()))
        cmd = "echo -n | {} s_client -connect {}:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > {}".format(ssl_cmd, jwks_addr, random_crt_fn)
        self.run(cmd, shell=True)
        alias = jwks_addr.replace('.', '_')

        self.run([Config.cmd_keytool, '-import', '-trustcacerts', '-keystore', 
                            Config.defaultTrustStoreFN, '-storepass', 'changeit', 
                            '-noprompt', '-alias', alias, '-file', random_crt_fn])

        #os.remove(random_crt_fn)

    def import_openbanking_key(self):
        if os.path.isfile(Config.ob_key_fn) and os.path.isfile(Config.ob_cert_fn):
            self.gen_keystore('obsigning', self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, Config.ob_key_fn, Config.ob_cert_fn, Config.ob_alias)


    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))
