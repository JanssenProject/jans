import traceback

from setup_app.utils.base import logIt, run, os_type, os_version, \
    os_initdaemon, service_path, clone_type
from setup_app.config import Config


class BaseInstaller:

    def start_installation(self):
        Config.pbar.progress(self.service_name, self.pbar_text)
        # execute for each installer
        if Config.downloadWars:
            self.download_files()
        self.create_user()
        self.create_folders()

        self.install()

    def run_service_command(self, operation):
        try:
            if (clone_type == 'rpm' and os_initdaemon == 'systemd') or (os_type + os_version in ('ubuntu18','debian9','debian10')):
                run([service_path, operation, self.service_name], None, None, True)
            else:
                run([service_path, self.service_name, operation], None, None, True)
        except:
            logIt("Error starting service '%s'" % operation)
            logIt(traceback.format_exc(), True)

    def enable(self):
        self.run_service_command('enable')

    def stop(self):
        self.run_service_command('stop')

    def start(self):
        self.run_service_command('start')
    
    def download_files(self):
        pass

    def create_user(self):
        pass

    def create_folders(self):
        pass
