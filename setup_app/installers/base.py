import traceback

from setup_app.utils.base import logIt, run, os_type, os_version, os_name, \
    os_initdaemon, service_path, clone_type
from setup_app.config import Config


class BaseInstaller:

    def start_installation(self):
        self.logIt(self.pbar_text, pbar=self.service_name)
        # execute for each installer
        if Config.downloadWars:
            self.download_files()
        self.create_user()
        self.create_folders()

        self.install()
        self.copy_static()

    def run_service_command(self, operation, service):
        if not service:
            service = self.service_name
        try:
            if (clone_type == 'rpm' and os_initdaemon == 'systemd') or (os_name in ('ubuntu18','debian9','debian10')):
                run([service_path, operation, service], None, None, True)
            else:
                run([service_path, service, operation], None, None, True)
        except:
            logIt("Error running operation {} for service {}".format(operation, service))
            logIt(traceback.format_exc(), True)

    def enable(self, service=None):
        self.run_service_command('enable', service)

    def stop(self, service=None):
        self.run_service_command('stop', service)

    def start(self, service=None):       
        self.run_service_command('start', service)
    
    def reload_daemon(self):
        if (clone_type == 'rpm' and os_initdaemon == 'systemd') or (os_name in ('ubuntu18','debian9','debian10')):
            run([service_path, 'daemon-reload'])

    def download_files(self):
        pass

    def create_user(self):
        pass

    def create_folders(self):
        pass
    
    def copy_static(self):
        pass
