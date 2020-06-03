from setup_app.utils import base
from setup_app.config import Config
from setup_app.utils.db_utils import dbUtils

class BaseInstaller:
    needdb = True

    def start_installation(self):
        self.dbUtils = dbUtils
        self.logIt(self.pbar_text, pbar=self.service_name)
        if self.needdb and not self.dbUtils.ready:
            try:
                self.dbUtils.bind()
            except:
                pass

        # execute for each installer
        if Config.downloadWars:
            self.download_files()

        self.create_user()
        self.create_folders()

        self.install()
        self.copy_static()
        self.render_import_templates()

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

    def render_import_templates(self):
        pass


    def enable(self, service=None):
        self.run_service_command('enable', service)

    def stop(self, service=None):
        self.run_service_command('stop', service)

    def start(self, service=None):
        self.run_service_command('start', service)

    def reload_daemon(self, service=None):
        if not service:
            service = self.service_name
        if (base.clone_type == 'rpm' and base.os_initdaemon == 'systemd') or (base.os_name in ('ubuntu18','debian9','debian10')):
            self.run([base.service_path, 'daemon-reload'])
        elif base.os_name == 'ubuntu16':
            self.run([paths.cmd_update_rc, service, 'defaults'])

    def download_files(self):
        pass

    def create_user(self):
        pass

    def create_folders(self):
        pass
    
    def copy_static(self):
        pass
