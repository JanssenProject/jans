import sys
import time
import os

from setup_app import static
from setup_app.config import Config
from threading import Thread

# credit https://github.com/verigak/progress/blob/master/progress/spinner.py
phases = ['◑', '◒', '◐', '◓']

phases = ['◷', '◶', '◵', '◴']
phases = ['⎺', '⎻', '⎼', '⎽', '⎼', '⎻']
phases = ['⣾', '⣷', '⣯', '⣟', '⡿', '⢿', '⣻', '⣽']
finished_char = '✓'
try:
    with open(os.devnull, 'w') as w:
        w.write(phases[0])
except:
    phases = ('-', '\\', '|', '/')
    finished_char = '@'


class ShowProgress(Thread):
    def __init__(self, services, queue, timeout=15):
        Thread.__init__(self)
        self.services = services
        self.service_names = [s['name'] for s in services]
        self.queue = queue
        self.end_time = time.time() + timeout * 60

    def get_max_len(self):
        max_len = 0
        for s in self.services:
            if len(s['name']) > max_len:
                max_len = len(s['name'])
        max_len += 1
        return max_len

    def run(self):
        number_of_services = len(self.services)
        current_service = 0
        phase_counter = 0
        data = {}
        msg = ''
        last_completed = 0
        max_len = self.get_max_len()

        while True:
            if time.time() > self.end_time:
                print("Timed out. Ending up process.")
                break
            if not self.queue.empty():
                data = self.queue.get()

            if data.get('current'):
                for si, s in enumerate(self.services):
                    if s['name'] == data['current']:
                        current_service = si
                        s['msg'] = data.get('msg','')
                        last_completed = si
                        break
                else:
                    if data['current'] == static.COMPLETED:
                        current_service = 99
                    # this means service was not registered before, do it now
                    elif not data['current'] in self.service_names:
                        self.services.insert(last_completed + 1, {
                                'name': data['current'],
                                'app_type': static.AppType.APPLICATION,
                                'install_type': static.InstallOption.OPTONAL,
                                'msg': data.get('msg','')
                                })
                        number_of_services += 1
                        max_len = self.get_max_len()

            for i, service in enumerate(self.services):
                spin_char = ' '
                if i == current_service:
                    spin_char = phases[phase_counter%len(phases)]
                elif i < current_service:
                    spin_char = '\033[92m{}\033[0m'.format(finished_char)

                sys.stdout.write(spin_char + ' ' + service['name'].ljust(max_len) + ' ' + service.get('msg','') +'\n')
                
            if current_service < number_of_services:
                for _ in range(number_of_services):
                    sys.stdout.write("\x1b[1A\x1b[2K")
            
            else:
                sys.stdout.write(data.get('msg',"Installation completed"))
                print()
                break

            time.sleep(0.15)

            phase_counter += 1


class JansProgress:

    services = []
    queue = None
    
    def register(self, installer):

        progress_entry = {
                    'name': installer.service_name,
                    'app_type': installer.app_type,
                    'install_type': installer.install_type,
                    'object': installer,
                    'install_var': installer.install_var,
                    }

        self.services.append(progress_entry)

    def before_start(self):
        for service in self.services[:]:
            if service['name'] != 'post-setup':
                if Config.installed_instance:
                    if not service['install_var'] in Config.addPostSetupService:
                        self.services.remove(service)
                else:
                    if not Config.get(service['install_var']):
                        self.services.remove(service)

    def start(self):
        if self.queue:
            th = ShowProgress(self.services, self.queue)
            th.setDaemon(True)
            th.start()

    def progress(self, service_name, msg='', incr=False):
        if self.queue:
            self.queue.put({'current': service_name, 'msg': msg})
        elif service_name != static.COMPLETED:
            print("Process {}: {}".format(service_name, msg))
        
jansProgress = JansProgress()
