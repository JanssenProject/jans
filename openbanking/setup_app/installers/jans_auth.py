import os
import glob
import random
import string
import uuid
import json
import ssl

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

        self.json_scripts = os.path.join(Config.outputFolder, 'scripts.json')
        self.json_config = os.path.join(self.output_folder, 'configuration.json')
        self.oxauth_config_json = os.path.join(self.output_folder, 'jans-auth-config.json')
        self.oxauth_static_conf_json = os.path.join(self.templates_folder, 'jans-auth-static-conf.json')
        self.oxauth_error_json = os.path.join(self.templates_folder, 'jans-auth-errors.json')
        self.oxauth_openid_jwks_fn = os.path.join(self.output_folder, 'jans-auth-keys.json')
        self.oxauth_openid_jks_fn = os.path.join(Config.certFolder, 'jans-auth-keys.jks')


    def install(self):
        self.logIt("Copying auth.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)

        jettyServiceWebapps = os.path.join(self.jetty_base, self.service_name,  'webapps')
        self.copyFile(self.source_files[0][0], jettyServiceWebapps)
        self.enable()
        self.import_openbanking_certificate()

    def generate_configuration(self):
        if not Config.get('oxauth_openid_jks_pass'):
            Config.oxauth_openid_jks_pass = self.getPW()

        if not Config.get('admin_inum'):
            Config.admin_inum = str(uuid.uuid4())

        self.logIt("Generating OAuth openid keys", pbar=self.service_name)
        sig_keys = 'RS256 RS384 RS512 ES256 ES384 ES512 PS256 PS384 PS512'
        enc_keys = 'RSA1_5 RSA-OAEP'
        jwks = self.gen_openid_jwks_jks_keys(self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, key_expiration=2, key_algs=sig_keys, enc_keys=enc_keys)
        self.write_openid_keys(self.oxauth_openid_jwks_fn, jwks)

        if Config.use_external_key:
            self.import_openbanking_key()

    def render_import_templates(self):

        for tmp in (self.oxauth_config_json,):
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)

        Config.templateRenderingDict['oxauth_config'] = json.dumps(self.readFile(self.oxauth_config_json))
        Config.templateRenderingDict['oxauth_static_conf'] = json.dumps(self.readFile(self.oxauth_static_conf_json))
        Config.templateRenderingDict['oxauth_error'] = json.dumps(self.readFile(self.oxauth_error_json))
        Config.templateRenderingDict['oxauth_openid_key'] = json.dumps(self.readFile(self.oxauth_openid_jwks_fn))

        self.renderTemplateInOut(self.json_scripts, Config.templateFolder, Config.outputFolder)
        self.renderTemplateInOut(self.json_config, self.templates_folder, self.output_folder)

        self.dbUtils.import_templates([self.json_config, self.json_scripts])

    def genRandomString(self, N):
        return ''.join(random.SystemRandom().choice(string.ascii_lowercase
                                                    + string.ascii_uppercase
                                                    + string.digits) for _ in range(N))

    def make_salt(self, enforce=False):
        if not Config.get('pairwiseCalculationKey') or enforce:
            Config.pairwiseCalculationKey = self.genRandomString(random.randint(20,30))
        if not Config.get('pairwiseCalculationSalt') or enforce:
            Config.pairwiseCalculationSalt = self.genRandomString(random.randint(20,30))


    def import_openbanking_certificate(self):
        self.logIt("Importing openbanking ssl certificate")
        openbank_pem ='-----BEGIN CERTIFICATE-----\nMIIFWjCCBEKgAwIBAgIEWcYGxDANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJH\nQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFBy\nZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMjAxMjAzMTIxNTMwWhcNMjIwMTAz\nMTI0NTMwWjBXMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxHzAd\nBgNVBAsTFk9wZW4gQmFua2luZyBEaXJlY3RvcnkxETAPBgNVBAMTCGtleXN0b3Jl\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAumffFM6wwcouMUJ9+a0T\n2pvsW4VPpHvpWexk9nLAD2biltTiAueHnZvPjaDeO4wsIArgZ01crbfbmxr8K3x9\nsV2EtqEUBoLCI6/dLbSHnn+wY6CfkjOnjMjIcCJxI5qlLASI0/slLr3Kqm/5p5yn\nvtpAFlmHXsvhYHjM6yLLivQtq/OAAXd7OyeFXJxy7GRPxP27YvOrc9jwoxnlHDZQ\nQwOqinPETiThS57AHfcp5a1+6ZZEhup0CM0I21IPtRf1r77z5m717WdTkCn8hEFh\nRPDOx9PkXnhWHq3nox8Gk5k20Giz8UpOf8PpmsBkH2P8wsEsozWz00gUJdbgJZKI\nOwIDAQABo4ICMDCCAiwwKgYDVR0RBCMwIYIfa2V5c3RvcmUub3BlbmJhbmtpbmd0\nZXN0Lm9yZy51azAOBgNVHQ8BAf8EBAMCB4AwIAYDVR0lAQH/BBYwFAYIKwYBBQUH\nAwEGCCsGAQUFBwMCMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoG\nCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsG\nAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMg\nYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0\naW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQw\nbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5j\nb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3Bw\nX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0\naXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0\n/gASBoFceXluP1AwHQYDVR0OBBYEFCwcAwYGFvrwK0AS91JNOXD6G5QSMA0GCSqG\nSIb3DQEBCwUAA4IBAQBowG2r0iPpEIo2+3mBGiWtOOGuptgr831OSD+SzejRYS1L\nAcrYYLrQelfe8wIUxA1MWElNtIJI3/Z12BO/0biv5ycT9oLS+ieqsyYTnKsYulYn\nosz4Kv40SE4vB8nVRw1le9mrdujHMX3vDOu0UrU+QbZ7yD0he42ckKPNhvGZWWcK\nmHpumKINHI17idIbvlZ43QNXU8PytR9uFyoIdJ7vomb8krXQYx6Nji0oRp6sKOOo\nU+e/HoQKYmaqSd0LB5nIkrThpVxYRqM57KTcjJbvrmIBlvZXT54WTqBtnbUdBVyy\n2J+g21uuPG5PAmZ48g85zLFP8k6H7aZAlXBSdiBg\n-----END CERTIFICATE-----'
        random_crt_fn = '/tmp/{}.crt'.format(os.urandom(3).hex())
        self.writeFile(random_crt_fn, openbank_pem)
        alias = 'keystore_openbankingtest_org_uk'

        self.run([Config.cmd_keytool, '-import', '-trustcacerts', '-keystore', 
                            Config.defaultTrustStoreFN, '-storepass', 'changeit', 
                            '-noprompt', '-alias', alias, '-file', random_crt_fn])

        os.remove(random_crt_fn)

    def import_openbanking_key(self):
        if os.path.isfile(Config.ob_key_fn) and os.path.isfile(Config.ob_cert_fn):
            self.gen_keystore('obsigning', self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, Config.ob_key_fn, Config.ob_cert_fn, Config.ob_alias)


    def installed(self):
        return os.path.exists(os.path.join(Config.jetty_base, self.service_name, 'start.ini'))
