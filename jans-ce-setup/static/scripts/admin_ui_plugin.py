import sys
import os
import time
import glob
import json
import shutil
from collections import OrderedDict

mydir = os.getcwd()
dirs = glob.glob('/opt/jans/jans-setup/output/gluu-admin-ui*')
run_cmd = '/bin/su node -c "PATH=$PATH:/opt/jre/bin:/opt/node/bin {}"'

for d in dirs:
    if os.path.exists(os.path.join(d, '.env')):
        uid_admin_dir = d
        break
else:
    print("Admin UI installation directory not found.")
    sys.exit()

os.chdir(uid_admin_dir)
plugin_json_fn = os.path.join(uid_admin_dir, 'plugins.config.json')

def read_plugins():
    with open(plugin_json_fn) as f:
        plugins = json.load(f, object_pairs_hook=OrderedDict)
    return plugins

plugins = read_plugins()

def print_plugins():
    print("Available Plugins")
    for i, p in enumerate(plugins):
        e = '\033[92m*\033[0m' if p.get('enabled') else ' '
        print('{} {} {}'.format(e, i+1, p.get('title') or p.get('key')))
    print()


def exec_command(cmd):
    print("\033[1mExecuting {}\033[0m".format(cmd))
    os.system(run_cmd.format(cmd))


def build_copy():
    exec_command('npm run build:prod')
    admin_dir = '/var/www/html/admin'
    if os.path.exists(admin_dir):
        os.rename(admin_dir, admin_dir + '.' + time.ctime())

    print("Copying admin ui files to apache directory")
    shutil.copytree(os.path.join(uid_admin_dir, 'dist'), admin_dir)


while True:
    print_plugins()
    user_input = input('Add/Remove/Finish/Quit [a/r/f/q]: ')
    if user_input:
        choice = user_input.lower()[0]
        if choice == 'q':
           print("Exiting without modification.")
           break

        elif choice == 'f':
            build_copy()
            break

        elif choice == 'r':
            plugin_number = input('Enter plugin number to remove :')
            if plugin_number.isdigit() and int(plugin_number) <= len(plugins):
                pn = int(plugin_number) - 1
                for i, p in enumerate(plugins):
                    if i == pn:
                        exec_command('npm run plugin:remove {}'.format(p['key']))
                        plugins = read_plugins()
                        break
        elif choice == 'a':
            plugin_fn = input('Enter path of plugin: ')
            if plugin_fn.lower().endswith('.zip'):
                exec_command('npm run plugin:add {}'.format(plugin_fn))
                plugins = read_plugins()
            else:
                print("Can't find \033[31m{}\033[0m".format(plugin_metadata_fn))
        print()
