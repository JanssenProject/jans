import os
import glob
import shutil

from setup_app.config.config import Config
from setup_app.utils.setup_utils import SetupUtils

class RadiusInstaller(SetupUtils):

    def __init__(self):
        self.service_path = self.detect_service_path()


    def install_base(self):

        if not self.gluu_radius_client_id:
            self.gluu_radius_client_id = '1701.'  + str(uuid.uuid4())

        source_dir = os.path.join(self.staticFolder, 'radius')
        conf_dir = os.path.join(self.gluuBaseFolder, 'conf/radius/')
        self.createDirs(conf_dir)

        self.radius_jwt_pass = self.getPW()
        radius_jwt_pass = self.obscure(self.radius_jwt_pass)
        radius_jks_fn = os.path.join(self.certFolder, 'gluu-radius.jks')
        
        self.raidus_client_jwks = self.gen_openid_jwks_jks_keys(radius_jks_fn, self.radius_jwt_pass)

        raidus_client_jwks = ''.join(self.raidus_client_jwks).replace('\'','').replace(',,',',').replace('{,','{')
        
        raidus_client_jwks = json.loads(raidus_client_jwks)
        
        self.templateRenderingDict['radius_jwt_pass'] = radius_jwt_pass


        raidus_client_jwks_json = json.dumps(raidus_client_jwks, indent=2)
        
        self.templateRenderingDict['gluu_ro_client_base64_jwks'] = base64.encodestring(raidus_client_jwks_json.encode('utf-8')).decode('utf-8').replace(' ','').replace('\n','')

        for k in raidus_client_jwks['keys']:
            if k.get('alg') == 'RS512':
                self.templateRenderingDict['radius_jwt_keyId'] = k['kid']
        
        self.gluu_ro_pw = self.getPW()
        self.gluu_ro_encoded_pw = self.obscure(self.gluu_ro_pw)

        scripts_dir = os.path.join(source_dir,'scripts')

        for scriptFile, scriptName in ( ('super_gluu_ro_session.py', 'super_gluu_ro_session_script'),
                            ('super_gluu_ro.py','super_gluu_ro_script'),
                          ):
            
            scriptFilePath = os.path.join(scripts_dir, scriptFile)
            base64ScriptFile = self.generate_base64_file(scriptFilePath, 1)
            self.templateRenderingDict[scriptName] = base64ScriptFile

        for tmp_ in ('gluu_radius_base.ldif', 'gluu_radius_clients.ldif', 'gluu_radius_server.ldif'):
            tmp_fn = os.path.join(source_dir, 'templates', tmp_)
            self.renderTemplateInOut(tmp_fn, os.path.join(source_dir, 'templates'), self.outputFolder)
        
        self.renderTemplateInOut('gluu-radius.properties', os.path.join(source_dir, 'etc/gluu/conf/radius/'), conf_dir)


        ldif_file_clients = os.path.join(self.outputFolder, 'gluu_radius_clients.ldif')
        ldif_file_base = os.path.join(self.outputFolder, 'gluu_radius_base.ldif')

        if self.mappingLocations['default'] == 'ldap':
            self.import_ldif_opendj([ldif_file_base, ldif_file_clients])
        else:
            self.import_ldif_couchebase([ldif_file_base, ldif_file_clients])

        if self.installGluuRadius:
            self.install_gluu_radius()


    def install(self):

        self.pbar.progress("radius", "Installing Gluu components: Radius", False)
        
        radius_libs = os.path.join(self.distGluuFolder, 'gluu-radius-libs.zip')
        radius_jar = os.path.join(self.distGluuFolder, 'super-gluu-radius-server.jar')
        conf_dir = os.path.join(self.gluuBaseFolder, 'conf/radius/')
        ldif_file_server = os.path.join(self.outputFolder, 'gluu_radius_server.ldif')
        source_dir = os.path.join(self.staticFolder, 'radius')
        logs_dir = os.path.join(self.radius_dir,'logs')

        if not os.path.exists(logs_dir):
            self.run([self.cmd_mkdir, '-p', logs_dir])

        self.run(['unzip', '-n', '-q', radius_libs, '-d', self.radius_dir ])
        self.copyFile(radius_jar, self.radius_dir)

        if self.mappingLocations['default'] == 'ldap':
            schema_ldif = os.path.join(source_dir, 'schema/98-radius.ldif')
            self.import_ldif_opendj([schema_ldif])
            self.import_ldif_opendj([ldif_file_server])
        else:
            self.import_ldif_couchebase([ldif_file_server])
        
        self.copyFile(os.path.join(source_dir, 'etc/default/gluu-radius'), self.osDefault)
        self.copyFile(os.path.join(source_dir, 'etc/gluu/conf/radius/gluu-radius-logging.xml'), conf_dir)
        self.copyFile(os.path.join(source_dir, 'scripts/gluu_common.py'), os.path.join(self.gluuOptPythonFolder, 'libs'))

        
        self.copyFile(os.path.join(source_dir, 'etc/init.d/gluu-radius'), '/etc/init.d')
        self.run([self.cmd_chmod, '+x', '/etc/init.d/gluu-radius'])
        
        if self.os_type+self.os_version == 'ubuntu16':
            self.run(['update-rc.d', 'gluu-radius', 'defaults'])
        else:
            self.copyFile(os.path.join(source_dir, 'systemd/gluu-radius.service'), '/usr/lib/systemd/system')
            self.run([self.systemctl, 'daemon-reload'])
        
        #create empty gluu-radius.private-key.pem
        gluu_radius_private_key_fn = os.path.join(self.certFolder, 'gluu-radius.private-key.pem')
        self.writeFile(gluu_radius_private_key_fn, '')
        
        self.run([self.cmd_chown, '-R', 'radius:gluu', self.radius_dir])
        self.run([self.cmd_chown, '-R', 'root:gluu', conf_dir])
        self.run([self.cmd_chown, 'root:gluu', os.path.join(self.gluuOptPythonFolder, 'libs/gluu_common.py')])

        self.run([self.cmd_chown, 'radius:gluu', os.path.join(self.certFolder, 'gluu-radius.jks')])
        self.run([self.cmd_chown, 'radius:gluu', os.path.join(self.certFolder, 'gluu-radius.private-key.pem')])

        self.run([self.cmd_chmod, '755', self.radius_dir])
        self.run([self.cmd_chmod, '660', os.path.join(self.certFolder, 'gluu-radius.jks')])
        self.run([self.cmd_chmod, '660', os.path.join(self.certFolder, 'gluu-radius.private-key.pem')])

        self.enable_service_at_start('gluu-radius')

    def enable(self):
        self.enable_service_at_start('opendj')
    
    def start(self):
        self.run([self.service_path, 'opendj', 'start'])
        
    def stop(self):
        self.run([self.service_path, 'opendj', 'stop'])
