import os
import glob
import random
import string
import uuid
import shutil
import json
import tempfile
import zipfile
import re

from urllib.parse import urlparse

from setup_app import paths
from setup_app.utils import base
from setup_app.config import Config
from setup_app.installers.jetty import JettyInstaller
from setup_app.static import AppType, InstallOption, SetupProfiles

class JansAuthInstaller(JettyInstaller):

    source_files = [
                    (os.path.join(Config.dist_jans_dir, 'jans-auth.war'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-auth-server/{0}/jans-auth-server-{0}.war'.format(base.current_app.app_info['ox_version']))),
                    (os.path.join(Config.dist_jans_dir, 'jans-auth-client-jar-with-dependencies.jar'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-auth-client/{0}/jans-auth-client-{0}-jar-with-dependencies.jar'.format(base.current_app.app_info['ox_version'])))
                    ]

    source_fips_files = [
                    (os.path.join(Config.dist_jans_dir, 'jans-auth-fips.war'), "http://192.168.64.4/jans/jans-auth-server-fips.war"),
                    (os.path.join(Config.dist_jans_dir, 'jans-auth-client-jar-without-provider-dependencies.jar'), "http://192.168.64.4/jans/jans-auth-client-jar-without-provider-dependencies.jar")
                    ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'jans-auth'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'installOxAuth'
        self.register_progess()

        self.jetty_service_webapps = os.path.join(self.jetty_base, self.service_name, 'webapps')
        self.templates_folder = os.path.join(Config.templateFolder, self.service_name)
        self.output_folder = os.path.join(Config.output_dir, self.service_name)

        self.ldif_config = os.path.join(self.output_folder, 'configuration.ldif')
        self.ldif_role_scope_mappings = os.path.join(self.output_folder, 'role-scope-mappings.ldif')
        self.oxauth_config_json = os.path.join(self.output_folder, 'jans-auth-config.json')
        self.oxauth_static_conf_json = os.path.join(self.templates_folder, 'jans-auth-static-conf.json')
        self.oxauth_error_json = os.path.join(self.templates_folder, 'jans-auth-errors.json')
        self.oxauth_openid_jwks_fn = os.path.join(self.output_folder, 'jans-auth-keys.json')
#        self.oxauth_openid_jks_fn = os.path.join(Config.certFolder, 'jans-auth-keys.p12')
        self.oxauth_openid_jks_fn = os.path.join(Config.certFolder, self.get_keystore_fn('oxauth-keys'))
        self.ldif_people = os.path.join(self.output_folder, 'people.ldif')
        self.ldif_groups = os.path.join(self.output_folder, 'groups.ldif')
        self.agama_root = os.path.join(self.jetty_base, self.service_name, 'agama')

        if Config.profile == SetupProfiles.OPENBANKING:
            Config.enable_ob_auth_script = '0' if base.argsp.disable_ob_auth_script else '1'
            Config.jwks_uri = base.argsp.jwks_uri

    def pre_install(self):
        if Config.profile == SetupProfiles.DISA_STIG:
            self.extract_bcfips_jars()
            Config.init_ext()
            
            self.logIt("Config.opendj_truststore_format = %s" % Config.opendj_truststore_format)            

    def install(self):
        self.init_key_gen()
    
        self.logIt("Copying auth.war into jetty webapps folder...")

        self.installJettyService(self.jetty_app_configuration[self.service_name], True)
        self.copyFile(self.source_files[0][0], self.jetty_service_webapps)
        
        self.setup_agama()
        self.enable()

    def generate_configuration(self):
        if not Config.get('oxauth_openid_jks_pass'):
            Config.oxauth_openid_jks_pass = self.getPW()

        if not Config.get('admin_inum'):
            Config.admin_inum = str(uuid.uuid4())

        Config.encoded_admin_password = self.ldap_encode(Config.admin_password)

        self.logIt("Generating OAuth openid keys", pbar=self.service_name)
        sig_keys = 'RS256 RS384 RS512 ES256 ES256K ES384 ES512 PS256 PS384 PS512'
        enc_keys = 'RSA1_5 RSA-OAEP ECDH-ES'
    
        #jwks = self.gen_openid_jwks_jks_keys(self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, key_expiration=2, key_algs=sig_keys, enc_keys=enc_keys)
        jwks = self.gen_openid_data_store_keys(self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, key_expiration=2, key_algs=sig_keys, enc_keys=enc_keys)
        self.write_openid_keys(self.oxauth_openid_jwks_fn, jwks)

        if Config.get('use_external_key'):
            self.import_openbanking_key()


    def get_config_api_scopes(self):
        data = base.current_app.ConfigApiInstaller.read_config_api_swagger()
        scope_list = []

        for epath in data['paths']:
            for m in data['paths'][epath]:
                if 'security' in data['paths'][epath][m]:
                    scope_items = [item['oauth2'] for item in data['paths'][epath][m]['security']]
                    for scopes in scope_items:
                        scope_list += scopes

        return scope_list


    def role_scope_mappings(self):

        role_scope_mappings_fn = os.path.join(self.templates_folder, 'role-scope-mappings.json')
        role_mapping = base.readJsonFile(role_scope_mappings_fn)

        scope_list = self.get_config_api_scopes()

        for api_role in role_mapping['rolePermissionMapping']:
            if api_role['role'] == 'api-admin':
                break

        for scope in scope_list:
            if scope not in api_role['permissions']:
                api_role['permissions'].append(scope)

        Config.templateRenderingDict['role_scope_mappings'] = json.dumps(role_mapping)


    def render_import_templates(self):

        self.role_scope_mappings()

        Config.templateRenderingDict['person_custom_object_class_list'] = '[]' if Config.mapping_locations['default'] == 'rdbm' else '["jansCustomPerson", "jansPerson"]'

        templates = [self.oxauth_config_json, self.ldif_people, self.ldif_groups]

        for tmp in templates:
            self.renderTemplateInOut(tmp, self.templates_folder, self.output_folder)

        if Config.profile == SetupProfiles.OPENBANKING:
            base.extract_file(
                base.current_app.jans_zip,
                'jans-linux-setup/jans_setup/static/extension/introspection/introspection_role_based_scope.py',
                os.path.join(Config.extensionFolder, 'introspection/')
                )

        self.prepare_base64_extension_scripts()

        Config.templateRenderingDict['oxauth_config_base64'] = self.generate_base64_ldap_file(self.oxauth_config_json)
        Config.templateRenderingDict['oxauth_static_conf_base64'] = self.generate_base64_ldap_file(self.oxauth_static_conf_json)
        Config.templateRenderingDict['oxauth_error_base64'] = self.generate_base64_ldap_file(self.oxauth_error_json)
        Config.templateRenderingDict['oxauth_openid_key_base64'] = self.generate_base64_ldap_file(self.oxauth_openid_jwks_fn)

        self.ldif_scripts = os.path.join(Config.output_dir, 'scripts.ldif')
        self.renderTemplateInOut(self.ldif_scripts, Config.templateFolder, Config.output_dir)
        for temp in (self.ldif_config, self.ldif_role_scope_mappings):
            self.renderTemplateInOut(temp, self.templates_folder, self.output_folder)

        self.dbUtils.import_ldif([self.ldif_config, self.ldif_scripts, self.ldif_role_scope_mappings, self.ldif_people, self.ldif_groups])

        if Config.profile == SetupProfiles.OPENBANKING:
            self.import_openbanking_certificate()

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
        for conf_fn in ('duo_creds.json', 'gplus_client_secrets.json', 'super_gluu_creds.json',
                        'vericloud_jans_creds.json', 'cert_creds.json', 'otp_configuration.json'):

            src_fn = os.path.join(Config.install_dir, 'static/auth/conf', conf_fn)
            self.copyFile(src_fn, Config.certFolder)

    def import_openbanking_certificate(self):
        self.logIt("Importing openbanking ssl certificate")
        oxauth_config_json = base.readJsonFile(self.oxauth_config_json)
        jwks_uri = oxauth_config_json['jwksUri']
        o = urlparse(jwks_uri)
        jwks_addr = o.netloc
        open_banking_cert = self.get_server_certificate(jwks_addr)
        alias = jwks_addr.replace('.', '_')

        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_fn = os.path.join(tmp_dir, jwks_addr+'.crt')
            self.writeFile(tmp_fn, open_banking_cert)
            self.run([Config.cmd_keytool, '-import', '-trustcacerts', '-keystore', 
                      Config.default_trust_store_fn, '-storepass', 'changeit', 
                      '-noprompt', '-alias', alias, '-file', tmp_fn])


    def import_openbanking_key(self):
        if not os.path.isfile(Config.ob_cert_fn):
            self.download_ob_cert(Config.ob_cert_fn)

        if os.path.isfile(Config.ob_key_fn) and os.path.isfile(Config.ob_cert_fn):
            self.import_key_cert_into_keystore('obsigning', self.oxauth_openid_jks_fn, Config.oxauth_openid_jks_pass, Config.ob_key_fn, Config.ob_cert_fn, Config.ob_alias)

    def setup_agama(self):
        self.createDirs(self.agama_root)
        for adir in ('fl', 'ftl', 'scripts'):
            self.createDirs(os.path.join(self.agama_root, adir))
        base.extract_from_zip(base.current_app.jans_zip, 'agama/misc', self.agama_root)
        self.chown(self.agama_root, Config.jetty_user, Config.jetty_group, recursive=True)

        tmp_dir = os.path.join(Config.templateFolder, 'jetty')
        src_xml = os.path.join(tmp_dir, 'agama_web_resources.xml')
        self.renderTemplateInOut(src_xml, tmp_dir, self.jetty_service_webapps)

        self.chown(os.path.join(self.jetty_service_webapps, os.path.basename(src_xml)), Config.jetty_user, Config.jetty_group)

    def extract_bcfips_jars(self):
#        Config.bc_fips_jar;
#        Config.bcpkix_fips_jar;
#        self.source_files[2][0];

        war_zip = zipfile.ZipFile(self.source_fips_files[0][0], "r")
        for fn in war_zip.namelist():
            if re.search('bc-fips-(.*?).jar$', fn) or re.search('bcpkix-fips-(.*?).jar$', fn):
                file_name = os.path.basename(fn)
                target_fn = os.path.join(Config.dist_app_dir, file_name)
#                print("Extracting", fn, "to", target_fn)
                file_content = war_zip.read(fn)
                with open(target_fn, 'wb') as w:
                    w.write(file_content)
        war_zip.close()

#    self.copyFile(self.source_files[0][0], self.jetty_service_webapps)

#    if argsp.profile == 'DISA-STIG':
#        war_zip = zipfile.ZipFile(oxauth_war_fn, "r")
#        for fn in war_zip.namelist():
#            if re.search('bc-fips-(.*?).jar$', fn) or re.search('bcpkix-fips-(.*?).jar$', fn):
#                file_name = os.path.basename(fn)
#                target_fn = os.path.join(app_dir, file_name)
#                print("Extracting", fn, "to", target_fn)
#                file_content = war_zip.read(fn)
#                with open(target_fn, 'wb') as w:
#                    w.write(file_content)
#        war_zip.close()

#    Config.profile == SetupProfiles.OPENBANKING
        
            
            
#   dist_app_dir = os.path.join(distFolder, 'app')
#    dist_jans_dir = os.path.join(distFolder, 'jans')
        
        
#            self.bc_fips_jar = max(glob.glob(os.path.join(self.distAppFolder, 'bc-fips-*.jar')))
#            self.bcpkix_fips_jar = max(glob.glob(os.path.join(self.distAppFolder, 'bcpkix-fips-*.jar')))

    def init_key_gen(self):
#        #Download jans-auth-client-jar-with-dependencies
#        if not os.path.exists(Config.non_setup_properties['jans_auth_client_jar_fn']):
#            jans_auth_client_jar_url = os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-auth-client/{0}/jans-auth-client-{0}-jar-with-dependencies.jar').format(base.current_app.app_info['ox_version'])
#            self.logIt("Downloading {}".format(os.path.basename(jans_auth_client_jar_url)))
#            base.download(jans_auth_client_jar_url, Config.non_setup_properties['jans_auth_client_jar_fn'])

        self.logIt("Determining key generator path")
        jans_auth_client_jar_zf = zipfile.ZipFile(Config.non_setup_properties['jans_auth_client_jar_fn'])

        for f in jans_auth_client_jar_zf.namelist():
            if os.path.basename(f) == 'KeyGenerator.class':
                p, e = os.path.splitext(f)
                Config.non_setup_properties['key_gen_path'] = p.replace(os.path.sep, '.')
            elif os.path.basename(f) == 'KeyExporter.class':
                p, e = os.path.splitext(f)
                Config.non_setup_properties['key_export_path'] = p.replace(os.path.sep, '.')

        if (not 'key_gen_path' in Config.non_setup_properties) or (not 'key_export_path' in Config.non_setup_properties):
            self.logIt("Can't determine key generator and/or key exporter path form {}".format(Config.non_setup_properties['jans_auth_client_jar_fn']), True, True)
        else:
            self.logIt("Key generator path was determined as {}".format(Config.non_setup_properties['key_export_path']))
