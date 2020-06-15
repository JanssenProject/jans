import uuid
import inspect

from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.db_utils import dbUtils
from setup_app.utils.progress import gluuProgress

class BaseInstaller:
    needdb = True
    dbUtils = dbUtils

    def register_progess(self):
        gluuProgress.register(self)

    def start_installation(self):
        if not hasattr(self, 'pbar_text'):
            pbar_text = "Installing" + self.service_name.title()
        else:
            pbar_text = self.pbar_text
        self.logIt(pbar_text, pbar=self.service_name)
        if self.needdb:
            self.dbUtils.bind()

        # execute for each installer
        if Config.downloadWars:
            self.download_files()

        self.create_user()
        self.create_folders()

        self.install()
        self.copy_static()
        self.generate_configuration()
        
        # before rendering templates, let's push variables of this class to Config.templateRenderingDict
        self.update_rendering_dict()

        self.render_import_templates()
        self.update_backend()

    def update_rendering_dict(self):
        mydict = {}
        for obj_name, obj in inspect.getmembers(self):
            if obj_name in ('dbUtils',):
                continue
            if not obj_name.startswith('__') and (not callable(obj)):
                mydict[obj_name] = obj

        Config.templateRenderingDict.update(mydict)


    def check_clients(self, client_var_id_list, resource=False):
        field_name, ou = ('oxId', 'resources') if resource else ('inum', 'clients')

        for client_var_name, client_id_prefix in client_var_id_list:
            if not Config.get(client_var_name):
                result = self.dbUtils.search('ou={},o=gluu'.format(ou), '({}={}*)'.format(field_name, client_id_prefix))
                if result:
                    setattr(Config, client_var_name, result[field_name])
                    self.logIt("{} was found in backend as {}".format(client_var_name, result[field_name]))
                else:
                    setattr(Config, client_var_name, client_id_prefix + str(uuid.uuid4()))

    def check_resources(self, client_var_id_list):
        for client_var_name, client_id_prefix in client_var_id_list:
            if not Config.get(client_var_name):
                result = self.dbUtils.search('ou=clients,o=gluu', '(inum={}*)'.format(client_id_prefix))
                if result:
                    setattr(Config, client_var_name, result['inum'])
                    self.logIt("{} was found in backend as {}".format(client_var_name, result['inum']))
                else:
                    client_id = client_id_prefix + str(uuid.uuid4())
                    self.logIt("{} is created as {}".format(client_var_name, client_id))
                    setattr(Config, client_var_name, client_id)

    def run_service_command(self, operation, service):
        if not service:
            service = self.service_name
        try:
            if (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or (base.os_name in ('ubuntu18','debian9','debian10')):
                self.run([base.service_path, operation, service], None, None, True)
            else:
                self.run([base.service_path, service, operation], None, None, True)
        except:
            self.logIt("Error running operation {} for service {}".format(operation, service), True)

    def enable(self, service=None):
        self.run_service_command('enable', service)

    def stop(self, service=None):
        self.run_service_command('stop', service)

    def start(self, service=None):
        self.run_service_command('start', service)

    def restart(self, service=None):
        self.stop(service)
        self.start(service)

    def reload_daemon(self, service=None):
        if not service:
            service = self.service_name
        if (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or (base.os_name in ('ubuntu18','debian9','debian10')):
            self.run([base.service_path, 'daemon-reload'])
        elif base.os_name == 'ubuntu16':
            self.run([paths.cmd_update_rc, service, 'defaults'])

    def generate_configuration(self):
        pass

    def render_import_templates(self):
        pass

    def update_backend(self):
        pass

    def download_files(self):
        pass

    def create_user(self):
        pass

    def create_folders(self):
        pass
    
    def copy_static(self):
        pass
