import os
import glob
import json
import base64

from setup_app import paths
from setup_app.static import AppType, InstallOption
from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller

class RadiusInstaller(BaseInstaller, SetupUtils):

    def __init__(self):
        self.service_name = 'gluu-radius'
        self.pbar_text = "Installing Radius Base"
        self.app_type = AppType.SERVICE
        self.install_var = 'installGluuRadius'
        self.install_type = InstallOption.MONDATORY
        self.register_progess()

        self.source_files = [
                (os.path.join(Config.distGluuFolder, 'super-gluu-radius-server.jar'), 'https://ox.gluu.org/maven/org/gluu/super-gluu-radius-server/{0}/super-gluu-radius-server-{0}.jar'.format(Config.oxVersion)),
                (os.path.join(Config.distGluuFolder, 'gluu-radius-libs.zip'),  'https://ox.gluu.org/maven/org/gluu/super-gluu-radius-server/{0}/super-gluu-radius-server-{0}-distribution.zip'.format(Config.oxVersion))
                ]

        self.radius_dir = self.radius_dir = os.path.join(Config.gluuOptFolder, 'radius')
        self.source_dir = os.path.join(Config.staticFolder, 'radius')
        self.conf_dir = os.path.join(Config.gluuBaseFolder, 'conf/radius/')
        self.templates_folder = os.path.join(Config.templateFolder, 'radius')
        self.output_folder = os.path.join(Config.outputFolder, 'radius')

        self.config_generated = False

    def install(self):

        oxauth_updates = {
                        'oxauth_legacyIdTokenClaims': True, 
                        'oxauth_openidScopeBackwardCompatibility': True
                        }
        self.dbUtils.set_oxAuthConfDynamic(oxauth_updates)



    def render_import_templates(self):
        
        scripts_dir = os.path.join(self.source_dir,'scripts')
        for scriptFile, scriptName in ( ('super_gluu_ro_session.py', 'super_gluu_ro_session_script'),
                            ('super_gluu_ro.py','super_gluu_ro_script'),
                          ):
            scriptFilePath = os.path.join(scripts_dir, scriptFile)
            base64ScriptFile = self.generate_base64_file(scriptFilePath, 1)
            Config.templateRenderingDict[scriptName] = base64ScriptFile

        
        for template in ('gluu_radius_base.ldif', 'gluu_radius_clients.ldif'):
            tmp_fn = os.path.join(self.source_dir, 'templates', template)
            self.renderTemplateInOut(tmp_fn, self.templates_folder, self.output_folder)
        
        self.renderTemplateInOut(
                    'gluu-radius.properties', 
                    os.path.join(self.source_dir, 'etc/gluu/conf/radius/'), 
                    self.conf_dir)

        ldif_file_base = os.path.join(self.output_folder, 'gluu_radius_base.ldif')
        ldif_file_clients = os.path.join(self.output_folder, 'gluu_radius_clients.ldif')

        self.dbUtils.import_ldif([ldif_file_base, ldif_file_clients])

    def update_backend(self):
        self.dbUtils.enable_service('gluuRadiusEnabled')

    def install_gluu_radius(self):
        self.check_for_download()
        Config.pbar.progress(self.service_name, "Installing Radius Server", False)

        self.dbUtils.bind()

        result = self.dbUtils.search('ou=clients,o=gluu', '(inum=1701.*)')

        if result:
            Config.gluu_radius_client_id = result['inum']
            Config.gluu_ro_encoded_pw = result['oxAuthClientSecret']
        else:
            self.logIt("Can't find gluu_radius_client_id in database", True, True)

        radius_libs = self.source_files[1][0]
        radius_jar = self.source_files[0][0]
        ldif_file_server = os.path.join(self.output_folder, 'gluu_radius_server.ldif')
        logs_dir = os.path.join(self.radius_dir,'logs')

        self.renderTemplateInOut(ldif_file_server, self.templates_folder, self.output_folder)


        if not os.path.exists(logs_dir):
            self.run([paths.cmd_mkdir, '-p', logs_dir])

        self.run(['unzip', '-n', '-q', radius_libs, '-d', self.radius_dir ])
        self.copyFile(radius_jar, self.radius_dir)

        if Config.mappingLocations['default'] == 'ldap':
            schema_ldif = os.path.join(self.source_dir, 'schema/98-radius.ldif')
            self.dbUtils.import_schema(schema_ldif)
            self.dbUtils.ldap_conn.rebind()
            
        self.dbUtils.import_ldif([ldif_file_server])
        
        self.copyFile(os.path.join(self.source_dir, 'etc/default/gluu-radius'), Config.osDefault)
        self.copyFile(os.path.join(self.source_dir, 'etc/gluu/conf/radius/gluu-radius-logging.xml'), self.conf_dir)
        self.copyFile(os.path.join(self.source_dir, 'scripts/gluu_common.py'), os.path.join(Config.gluuOptPythonFolder, 'libs'))

        if not base.snap:
            self.copyFile(os.path.join(self.source_dir, 'etc/init.d/gluu-radius'), '/etc/init.d')
            self.run([paths.cmd_chmod, '+x', '/etc/init.d/gluu-radius'])
        
            if base.os_name != 'ubuntu16':
                self.copyFile(os.path.join(self.source_dir, 'systemd/gluu-radius.service'), '/usr/lib/systemd/system')

        #create empty gluu-radius.private-key.pem
        gluu_radius_private_key_fn = os.path.join(Config.certFolder, 'gluu-radius.private-key.pem')
        self.writeFile(gluu_radius_private_key_fn, '')
        
        self.run([paths.cmd_chown, '-R', 'radius:gluu', self.radius_dir])
        self.run([paths.cmd_chown, '-R', 'root:gluu', self.conf_dir])
        self.run([paths.cmd_chown, 'root:gluu', os.path.join(Config.gluuOptPythonFolder, 'libs/gluu_common.py')])
        self.run([paths.cmd_chown, 'radius:gluu', os.path.join(Config.certFolder, 'gluu-radius.jks')])
        self.run([paths.cmd_chown, 'radius:gluu', os.path.join(Config.certFolder, 'gluu-radius.private-key.pem')])

        self.run([paths.cmd_chmod, '755', self.radius_dir])
        self.run([paths.cmd_chmod, '755', self.conf_dir])
        self.run([paths.cmd_chmod, '660', os.path.join(Config.certFolder, 'gluu-radius.jks')])
        self.run([paths.cmd_chmod, '660', os.path.join(Config.certFolder, 'gluu-radius.private-key.pem')])

        self.dbUtils.enable_script('5866-4202')
        self.dbUtils.enable_script('B8FD-4C11')
        
        self.reload_daemon()
        self.enable()

    def create_user(self):
        self.createUser('radius', homeDir=self.radius_dir, shell='/bin/false')
        self.addUserToGroup('gluu', 'radius')

    def create_folders(self):
        self.createDirs(self.conf_dir)


    def generate_configuration(self):

        self.check_clients([('gluu_radius_client_id', '1701.')])

        if not Config.get('gluu_ro_encoded_pw'):
            Config.gluu_ro_pw = self.getPW()
            Config.gluu_ro_encoded_pw = self.obscure(Config.gluu_ro_pw)

        if not Config.get('radius_jwt_pass'):
            Config.radius_jwt_pass = self.getPW()

        radius_jwt_pass = self.obscure(Config.radius_jwt_pass)
        radius_jks_fn = os.path.join(Config.certFolder, 'gluu-radius.jks')
        
        raidus_client_jwks = self.gen_openid_jwks_jks_keys(radius_jks_fn, Config.radius_jwt_pass)
        raidus_client_jwks = ''.join(raidus_client_jwks).replace('\'','').replace(',,',',').replace('{,','{')
        raidus_client_jwks = json.loads(raidus_client_jwks)
        Config.templateRenderingDict['radius_jwt_pass'] = radius_jwt_pass
        raidus_client_jwks_json = json.dumps(raidus_client_jwks, indent=2)
        Config.templateRenderingDict['gluu_ro_client_base64_jwks'] = base64.encodestring(raidus_client_jwks_json.encode('utf-8')).decode('utf-8').replace(' ','').replace('\n','')

        for k in raidus_client_jwks['keys']:
            if k.get('alg') == 'RS512':
                Config.templateRenderingDict['radius_jwt_keyId'] = k['kid']
        

    def installed(self):
        return os.path.exists(self.radius_dir)
