#!/usr/bin/python3

import sys
import os
import argparse
import zipfile
import shutil
import time
import ssl
import json
import re
import json
import tempfile

from urllib import request
from urllib.parse import urljoin, urlparse
from pathlib import Path

ssl._create_default_https_context = ssl._create_unverified_context

SETUP_BRANCH = 'main'


package_dependencies = []
jans_dir = '/opt/jans'
jans_app_dir = '/opt/dist/jans'
maven_base_url = 'https://maven.jans.io/maven/io/jans/'
jetty_home = '/opt/jans/jetty'
jans_zip_file = os.path.join(jans_app_dir, 'jans.zip')

package_installer = shutil.which('apt') or shutil.which('dnf') or shutil.which('yum') or shutil.which('zypper')

if not os.path.exists(jans_app_dir):
    os.makedirs(jans_app_dir)

parser = argparse.ArgumentParser(description="This script downloads Janssen Server components and fires setup")
parser.add_argument('-a', help=argparse.SUPPRESS, action='store_true')
parser.add_argument('-use-downloaded', help="Use already downloaded components", action='store_true')
parser.add_argument('-upgrade', help="Upgrade Janssen war and jar files", action='store_true')
parser.add_argument('-uninstall', help="Uninstall Jans server and removes all files", action='store_true')
parser.add_argument('--args', help="Arguments to be passed to setup.py")
parser.add_argument('-yes', help="No prompt", action='store_true')
parser.add_argument('--keep-downloads', help="Keep downloaded files (applicable for uninstallation only)", action='store_true')
parser.add_argument('--profile', help="Setup profile", choices=['jans', 'openbanking', 'disa-stig'], default='jans')
parser.add_argument('-download-exit', help="Downloads files and exits", action='store_true')
parser.add_argument('--setup-branch', help="Jannsen setup github branch", default="main")
parser.add_argument('--setup-dir', help="Setup directory", default=os.path.join(jans_dir, 'jans-setup'))
parser.add_argument('-force-download', help="Force downloading files", action='store_true')

argsp = parser.parse_args()


bacup_ext = '-back.' + time.ctime()

def check_install_dependencies():

    try:
        from distutils import dist
    except ImportError:
        package_dependencies.append('python3-distutils')

    try:
        import ldap3
    except ImportError:
        package_dependencies.append('python3-ldap3')

    try:
        import psycopg2
    except ImportError:
        package_dependencies.append('python3-psycopg2')

    if package_dependencies and not argsp.yes:
        install_dist = input('Required package(s): {}. Install now? [Y/n] '.format(', '.join(package_dependencies)))
        if install_dist.lower().startswith('n'):
            print("Can't continue...")
            sys.exit()

    if package_dependencies:
        os.system('{} install -y {}'.format(package_installer, ' '.join(package_dependencies)))


def download_jans_acrhieve():
    jans_acrhieve_url = 'https://github.com/JanssenProject/jans/archive/refs/heads/{}.zip'.format(argsp.setup_branch)
    print("Downloading {} as {}".format(jans_acrhieve_url, jans_zip_file))
    request.urlretrieve(jans_acrhieve_url, jans_zip_file)



def check_installation():
    if not (os.path.exists(jetty_home) and os.path.exists('/etc/jans/conf/jans.properties')):
        print("Jans server seems not installed")
        sys.exit()


def profile_setup():
    print("Preparing Setup for profile {}".format(argsp.profile))

    profile_dir = os.path.join(argsp.setup_dir, argsp.profile)
    replace_dirs = []
    if not os.path.exists(profile_dir):
        print("Profile directory {} does not exist. Exiting ...".format(profile_dir))
    replace_dirs_fn = os.path.join(profile_dir, '.profiledirs')

    if os.path.exists(replace_dirs_fn):
        with open(replace_dirs_fn) as f:
            fcontent = f.read()
        for l in fcontent.splitlines():
            ls = l.strip()
            if ls:
                replace_dirs.append(ls)

    replaced_dirs = []
    for pdir in replace_dirs:
        source_dir = os.path.join(profile_dir, pdir)
        target_dir = os.path.join(argsp.setup_dir, pdir)
        replaced_dirs.append(source_dir)
        if os.path.exists(source_dir) and os.path.exists(target_dir):
            shutil.rmtree(target_dir)
            shutil.copytree(source_dir, target_dir)

    for root, dirs, files in os.walk(profile_dir):
        if root.startswith(tuple(replaced_dirs)):
            continue
        if files:
            target_dir = Path(argsp.setup_dir).joinpath(Path(root).relative_to(Path(profile_dir)))
            for f in files:
                if f in ['.profiledirs']:
                    continue
                source_file = os.path.join(root, f)
                print("Copying", source_file, target_dir)
                shutil.copy(source_file, target_dir)



def extract_setup():
    if os.path.exists(argsp.setup_dir):
        shutil.move(argsp.setup_dir, argsp.setup_dir + bacup_ext)

    print("Extracting jans-setup package")
    jans_zip = zipfile.ZipFile(jans_zip_file)
    parent_dir = jans_zip.filelist[0].orig_filename

    unpack_dir = os.path.join(jans_app_dir, os.urandom(4).hex())
    shutil.unpack_archive(jans_zip_file, unpack_dir)
    shutil.copytree(os.path.join(unpack_dir, parent_dir, 'jans-linux-setup/jans_setup'), argsp.setup_dir)
    jans_zip.close()
    shutil.rmtree(unpack_dir)

    target_setup = os.path.join(argsp.setup_dir, 'setup.py')
    if not os.path.exists(target_setup):
        os.symlink(os.path.join(argsp.setup_dir, 'jans_setup.py'), target_setup)

    o = urlparse(maven_base_url)
    app_info_fn = os.path.join(argsp.setup_dir, 'app_info.json')
    with open(app_info_fn) as f:
        app_info = json.load(f)

    app_info['JANS_MAVEN'] = o._replace(path='').geturl()

    with open(app_info_fn, 'w') as w:
        json.dump(app_info, w, indent=2)

    with open(os.path.join(argsp.setup_dir, 'profile'), 'w') as w:
        w.write(argsp.profile)

def uninstall_jans():
    check_installation()
    if not argsp.yes:
        print('\033[31m')
        print("This process is irreversible.")
        print("You will lose all data related to Janssen Server.")
        print('\033[0m')
        print()
        while True:
            print('\033[31m \033[1m')
            response = input("Are you sure to uninstall Janssen Server? [yes/N] ")
            print('\033[0m')
            if response.lower() in ('yes', 'n', 'no'):
                if not response.lower() == 'yes':
                    sys.exit()
                else:
                    break
            else:
                print("Please type \033[1m yes \033[0m to uninstall")

    print("Uninstalling Jannsen Server...")
    for service in os.listdir(jetty_home):
        if os.path.exists(os.path.join(jetty_home, service)):
            default_fn = os.path.join('/etc/default/', service)
            if os.path.exists(default_fn):
                print("Removing", default_fn)
                os.remove(default_fn)
            print("Stopping", service)
            os.system('systemctl stop ' + service)

    if argsp.profile == 'jans':
        print("Stopping OpenDj Server")
        os.system('/opt/opendj/bin/stop-ds')

    remove_list = ['/etc/certs', '/etc/jans', '/opt/jans', '/opt/amazon-corretto*', '/opt/jre', '/opt/node*', '/opt/jetty*', '/opt/jython*']
    if argsp.profile == 'jans':
        remove_list.append('/opt/opendj')
    if not argsp.keep_downloads:
        remove_list.append('/opt/dist')

    for p in remove_list:
        cmd = 'rm -r -f ' + p
        print("Executing", cmd)
        os.system('rm -r -f ' + p)

    apache_conf_fn_list = []

    if shutil.which('zypper'):
        apache_conf_fn_list = ['/etc/apache2/vhosts.d/_https_jans.conf']
    elif shutil.which('yum') or shutil.which('dnf'):
        apache_conf_fn_list = ['/etc/httpd/conf.d/https_jans.conf']
    elif shutil.which('apt'):
        apache_conf_fn_list = ['/etc/apache2/sites-enabled/https_jans.conf', '/etc/apache2/sites-available/https_jans.conf']

    for fn in apache_conf_fn_list:
        if os.path.exists(fn):
            print("Removing", fn)
            os.unlink(fn)

def upgrade():
    check_installation()

    for service in os.listdir(jetty_home):
        source_fn = os.path.join('/opt/dist/jans', service +'.war')
        target_fn = os.path.join(jetty_home, service, 'webapps', service +'.war' )
        if os.path.exists(target_fn):
            print("Copying", source_fn, "as", target_fn)
            shutil.move(target_fn, target_fn + bacup_ext)
            shutil.copy(source_fn, target_fn)
            print("Restarting", service)
            os.system('systemctl restart ' + service)

    jans_config_api_fn = '/opt/jans/config-api/jans-config-api-runner.jar'
    if os.path.exists(jans_config_api_fn):
        shutil.move(jans_config_api_fn, jans_config_api_fn + bacup_ext)
        source_fn = '/opt/dist/jans/jans-config-api-runner.jar'
        print("Copying", source_fn, "as", jans_config_api_fn)
        shutil.copy(source_fn, jans_config_api_fn)
        print("Restarting jans-config-api")
        os.system('systemctl restart jans-config-api')

def do_install():

    extract_setup()

    if argsp.profile != 'jans':
        profile_setup()


    print("Launching Janssen Setup")

    setup_cmd = '{} {}/setup.py'.format(sys.executable, argsp.setup_dir)
    setup_args = argsp.args or ''
    if argsp.force_download:
        setup_args += ' --force-download'

    if argsp.use_downloaded:
        setup_args += ' --use-downloaded'

    if argsp.download_exit:
        setup_args += ' --download-exit'

    if setup_args:
        setup_cmd += ' ' + setup_args

    print("Executing", setup_cmd)
    os.system(setup_cmd)

def main():

    if not argsp.uninstall or argsp.download_exit:
        check_install_dependencies()

    if not (argsp.use_downloaded or argsp.uninstall):
        download_jans_acrhieve()

    if argsp.upgrade:
        upgrade()
    elif argsp.uninstall:
        uninstall_jans()
    else:
        do_install()


if __name__ == "__main__":
    main()
