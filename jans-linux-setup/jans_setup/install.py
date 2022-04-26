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

from types import SimpleNamespace
from urllib import request
from urllib.parse import urljoin, urlparse
from pathlib import Path

ssl._create_default_https_context = ssl._create_unverified_context

SETUP_BRANCH = 'main'

app_globals = SimpleNamespace()
app_globals.jetty_dist_string = 'jetty-home'
app_globals.jetty_services = ['jans-auth', 'jans-config-api']
app_globals.package_dependencies = []

jans_dir = '/opt/jans'
app_dir = '/opt/dist/app'
jans_app_dir = '/opt/dist/jans'
scripts_dir = '/opt/dist/scripts'
jetty_home = '/opt/jans/jetty'
maven_base_url = 'https://maven.jans.io/maven/io/jans/'
jans_zip_file = os.path.join(jans_app_dir, 'jans.zip')
sqlalchemy_zip_file = os.path.join(jans_app_dir, 'sqlalchemy.zip')

package_installer = shutil.which('apt') or shutil.which('dnf') or shutil.which('yum') or shutil.which('zypper')


for d in (jans_dir, app_dir, jans_app_dir, scripts_dir):
    if not os.path.exists(d):
        os.makedirs(d)

parser = argparse.ArgumentParser(description="This script downloads Janssen Server components and fires setup")
parser.add_argument('-a', help=argparse.SUPPRESS, action='store_true')
parser.add_argument('-use-downloaded', help="Use already downloaded components", action='store_true')
parser.add_argument('-upgrade', help="Upgrade Janssen war and jar files", action='store_true')
parser.add_argument('-uninstall', help="Uninstall Jans server and removes all files", action='store_true')
parser.add_argument('--args', help="Arguments to be passed to setup.py")
parser.add_argument('-yes', help="No prompt", action='store_true')
parser.add_argument('--keep-downloads', help="Keep downloaded files (applicable for uninstallation only)", action='store_true')
parser.add_argument('--profile', help="Setup profile", choices=['jans', 'openbanking'], default='jans')
parser.add_argument('--jans-app-version', help="Version for Jannses applications")
parser.add_argument('--jans-build', help="Buid version for Janssen applications")
parser.add_argument('--setup-branch', help="Jannsen setup github branch", default='main')
parser.add_argument('--no-setup', help="Do not launch setup", action='store_true')
parser.add_argument('--no-jans-setup', help="Do not extract jans-setup", action='store_true')
parser.add_argument('--no-gcs', help="Do not download gcs", action='store_true')
parser.add_argument('-setup-dir', help="Setup directory", default=os.path.join(jans_dir, 'jans-setup'))
parser.add_argument('--force-download', help="Force downloading files", action='store_true')

if '-a' in sys.argv:
    parser.add_argument('--jetty-version', help="Jetty verison. For example 11.0.8")


def init_installer():

    if app_globals.argsp.jans_app_version:
        app_globals.app_versions['JANS_APP_VERSION'] = app_globals.argsp.jans_app_version

    if app_globals.argsp.jans_build:
        app_globals.app_versions['JANS_BUILD'] = app_globals.argsp.jans_build

    if app_globals.argsp.setup_branch:
        app_globals.app_versions['SETUP_BRANCH'] = app_globals.argsp.setup_branch

    if app_globals.argsp.profile == 'jans':
        app_globals.jetty_services += ['jans-fido2', 'jans-scim', 'jans-eleven']

def check_install_dependencies():

    try:
        from distutils import dist
    except:
        app_globals.package_dependencies.append('python3-distutils')

    try:
        import ldap3
    except:
        app_globals.package_dependencies.append('python3-ldap3')

    if app_globals.package_dependencies and not app_globals.argsp.yes:
        install_dist = input('Required package(s): {}. Install now? [Y/n] '.format(', '.join(app_globals.package_dependencies)))
        if install_dist.lower().startswith('n'):
            print("Can't continue...")
            sys.exit()

        os.system('{} install -y {}'.format(package_installer, ' '.join(app_globals.package_dependencies)))


def extract_subdir(zip_fn, sub_dir, target_dir, zipf=None):
    zip_obj = zipfile.ZipFile(zip_fn, "r")
    par_dir = zip_obj.namelist()[0]

    if not sub_dir.endswith('/'):
        sub_dir += '/'

    if zipf:
        target_zip_obj = zipfile.ZipFile(zipf, "w")

    ssub_dir = os.path.join(par_dir, sub_dir)
    target_dir_path = Path(target_dir)

    if target_dir_path.exists():
        shutil.rmtree(target_dir_path)

    target_dir_path.mkdir(parents=True)

    for member in zip_obj.infolist():
        if member.filename.startswith(ssub_dir):
            p = Path(member.filename)
            pr = p.relative_to(ssub_dir)
            target_fn = target_dir_path.joinpath(pr)
            if member.is_dir():
                if zipf:
                    z_dirn = target_fn.as_posix()
                    if not z_dirn.endswith('/'):
                        z_dirn += '/'
                    zinfodir = zipfile.ZipInfo(filename=z_dirn)
                    zinfodir.external_attr=0x16
                    target_zip_obj.writestr(zinfodir, '')
                elif not target_fn.exists():
                    target_fn.mkdir(parents=True)
            else:
                if zipf:
                    target_zip_obj.writestr(target_fn.as_posix(), zip_obj.read(member))
                else:
                    if not target_fn.parent.exists():
                        target_fn.parent.mkdir(parents=True)
                    target_fn.write_bytes(zip_obj.read(member))
    if zipf:
        target_zip_obj.close()

    zip_obj.close()

def extract_file(zip_fn, source, target, ren=False):
    zip_obj = zipfile.ZipFile(zip_fn, "r")
    for member in zip_obj.infolist():
        if not member.is_dir() and member.filename.endswith(source):
            if ren:
                target_p = Path(target)
            else:
                p = Path(member.filename)
                target_p = Path(target).joinpath(p.name)
                if not target_p.parent.exists():
                    target_p.parent.mkdir(parents=True)
            target_p.write_bytes(zip_obj.read(member))
            break
    zip_obj.close()


def download(url, target_fn):
    dst = target_fn if target_fn.startswith('/') else os.path.join(app_dir, target_fn)
    pardir, fn = os.path.split(dst)
    if not os.path.exists(pardir):
        os.makedirs(pardir)
    print("Downloading", url, "to", dst)
    request.urlretrieve(url, dst)


def download_gcs():
    if not os.path.exists(os.path.join(app_dir, 'gcs')):
        print("Downloading Spanner modules")
        gcs_download_url = 'https://ox.gluu.org/icrby8xcvbcv/spanner/gcs.tgz'
        tmp_dir = os.path.join(app_dir, 'gcs-' + os.urandom(5).hex())
        target_fn = os.path.join(tmp_dir, 'gcs.tgz')
        download(gcs_download_url, target_fn)
        shutil.unpack_archive(target_fn, app_dir)

        req = request.urlopen('https://pypi.org/pypi/grpcio/1.37.0/json')
        data_s = req.read()
        data = json.loads(data_s)

        pyversion = 'cp{0}{1}'.format(sys.version_info.major, sys.version_info.minor)

        package = {}

        for package_ in data['urls']:

            if package_['python_version'] == pyversion and 'manylinux' in package_['filename'] and package_['filename'].endswith('x86_64.whl'):
                if package_['upload_time'] > package.get('upload_time',''):
                    package = package_

        if package.get('url'):
            target_whl_fn = os.path.join(tmp_dir, os.path.basename(package['url']))
            download(package['url'], target_whl_fn)
            whl_zip = zipfile.ZipFile(target_whl_fn)

            for member in  whl_zip.filelist:
                fn = os.path.basename(member.filename)
                if fn.startswith('cygrpc.cpython') and fn.endswith('x86_64-linux-gnu.so'):
                    whl_zip.extract(member, os.path.join(app_dir, 'gcs'))

            whl_zip.close()

        shutil.rmtree(tmp_dir)


def download_files():

    setup_url = 'https://github.com/JanssenProject/jans/archive/refs/heads/{}.zip'.format(app_globals.app_versions['SETUP_BRANCH'])
    download(setup_url, jans_zip_file)

    download('https://corretto.aws/downloads/resources/{0}/amazon-corretto-{0}-linux-x64.tar.gz'.format(app_globals.app_versions['AMAZON_CORRETTO_VERSION']), os.path.join(app_dir, 'amazon-corretto-{0}-linux-x64.tar.gz'.format(app_globals.app_versions['AMAZON_CORRETTO_VERSION'])))
    download('https://repo1.maven.org/maven2/org/eclipse/jetty/{1}/{0}/{1}-{0}.tar.gz'.format(app_globals.app_versions['JETTY_VERSION'], app_globals.jetty_dist_string), os.path.join(app_dir,'{1}-{0}.tar.gz'.format(app_globals.app_versions['JETTY_VERSION'], app_globals.jetty_dist_string)))
    download('https://maven.gluu.org/maven/org/gluufederation/jython-installer/{0}/jython-installer-{0}.jar'.format(app_globals.app_versions['JYTHON_VERSION']), os.path.join(app_dir, 'jython-installer-{0}.jar'.format(app_globals.app_versions['JYTHON_VERSION'])))
    download(urljoin(maven_base_url, 'jans-auth-server/{0}{1}/jans-auth-server-{0}{1}.war'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-auth.war'))
    download(urljoin(maven_base_url, 'jans-auth-client/{0}{1}/jans-auth-client-{0}{1}-jar-with-dependencies.jar'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-auth-client-jar-with-dependencies.jar'))
    download(urljoin(maven_base_url, 'jans-config-api-server/{0}{1}/jans-config-api-server-{0}{1}.war'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-config-api.war'))
    download('https://github.com/sqlalchemy/sqlalchemy/archive/rel_1_3_23.zip', sqlalchemy_zip_file)
    download(urljoin(maven_base_url, 'jans-config-api/plugins/scim-plugin/{0}{1}/scim-plugin-{0}{1}-distribution.jar'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'scim-plugin.jar'))
    download(urljoin(maven_base_url, 'jans-config-api/plugins/user-mgt-plugin/{0}{1}/user-mgt-plugin-{0}{1}-distribution.jar'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'user-mgt-plugin.jar'))
    download('https://ox.gluu.org/icrby8xcvbcv/cli-swagger/jca_swagger_client.zip', os.path.join(jans_app_dir, 'jca-swagger-client.zip'))
    download('https://ox.gluu.org/icrby8xcvbcv/cli-swagger/scim_swagger_client.zip', os.path.join(jans_app_dir, 'scim-swagger-client.zip'))
    download('https://raw.githubusercontent.com/GluuFederation/gluu-snap/master/facter/facter', os.path.join(jans_app_dir, 'facter'))
    download('https://github.com/jpadilla/pyjwt/archive/refs/tags/2.3.0.zip', os.path.join(app_dir, 'pyjwt.zip'))

    if app_globals.argsp.profile == 'jans':
        download('https://maven.gluu.org/maven/org/gluufederation/opendj/opendj-server-legacy/{0}/opendj-server-legacy-{0}.zip'.format(app_globals.app_versions['OPENDJ_VERSION']), os.path.join(app_dir, 'opendj-server-legacy-{0}.zip'.format(app_globals.app_versions['OPENDJ_VERSION'])))
        download(urljoin(maven_base_url, 'jans-fido2-server/{0}{1}/jans-fido2-server-{0}{1}.war'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-fido2.war'))
        download(urljoin(maven_base_url, 'jans-scim-server/{0}{1}/jans-scim-server-{0}{1}.war'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-scim.war'))
        download('https://jenkins.jans.io/maven/io/jans/jans-eleven-server/{0}{1}/jans-eleven-server-{0}{1}.war'.format(app_globals.app_versions['JANS_APP_VERSION'], app_globals.app_versions['JANS_BUILD']), os.path.join(jans_app_dir, 'jans-eleven.war'))
        download('https://www.apple.com/certificateauthority/Apple_WebAuthn_Root_CA.pem', os.path.join(app_dir, 'Apple_WebAuthn_Root_CA.pem'))


def check_installation():
    if not (os.path.exists(jetty_home) and os.path.exists('/etc/jans/conf/jans.properties')):
        print("Jans server seems not installed")
        sys.exit()


def profile_setup():
    print("Preparing Setup for profile {}".format(app_globals.argsp.profile))
    profile_dir = os.path.join(app_globals.argsp.setup_dir, app_globals.argsp.profile)
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
        target_dir = os.path.join(app_globals.argsp.setup_dir, pdir)
        replaced_dirs.append(source_dir)
        if os.path.exists(source_dir) and os.path.exists(target_dir):
            shutil.rmtree(target_dir)
            copy_target = os.path.join(app_globals.argsp.setup_dir, os.path.sep.join(os.path.split(pdir)[:-1]))
            print(target_dir)
            shutil.copytree(source_dir, target_dir)

    for root, dirs, files in os.walk(profile_dir):
        if root.startswith(tuple(replaced_dirs)):
            continue
        if files:
            target_dir = Path(app_globals.argsp.setup_dir).joinpath(Path(root).relative_to(Path(profile_dir)))
            for f in files:
                if f in ['.profiledirs']:
                    continue
                source_file = os.path.join(root, f)
                print("Copying", source_file, target_dir)
                shutil.copy(source_file, target_dir)

def extract_setup():
    if os.path.exists(app_globals.argsp.setup_dir):
        shutil.move(app_globals.argsp.setup_dir, app_globals.argsp.setup_dir + '-back.' + time.ctime())
    print("Extracting jans-setup package")
    extract_subdir(jans_zip_file, 'jans-linux-setup/jans_setup', app_globals.argsp.setup_dir)
    extract_subdir(sqlalchemy_zip_file, 'lib/sqlalchemy', os.path.join(app_globals.argsp.setup_dir, 'setup_app/pylib/sqlalchemy'))

    target_setup = os.path.join(app_globals.argsp.setup_dir, 'setup.py')
    if not os.path.exists(target_setup):
        os.symlink(os.path.join(app_globals.argsp.setup_dir, 'jans_setup.py'), target_setup)

    o = urlparse(maven_base_url)
    app_globals.app_versions['JANS_MAVEN'] = o._replace(path='').geturl()
    app_info_fn = os.path.join(app_globals.argsp.setup_dir, 'app_info.json')
    with open(app_info_fn, 'w') as w:
        json.dump(app_globals.app_versions, w, indent=2)

def extract_yaml_files():
    extract_file(jans_zip_file, 'jans-config-api/docs/jans-config-api-swagger.yaml', os.path.join(app_globals.argsp.setup_dir, 'setup_app/data'))
    extract_file(jans_zip_file, 'jans-scim/server/src/main/resources/jans-scim-openapi.yaml', os.path.join(app_globals.argsp.setup_dir, 'setup_app/data'))
    extract_file(jans_zip_file, 'jans-config-api/server/src/main/resources/log4j2.xml', jans_app_dir)

def prepare_jans_cli_package():
    print("Preparing jans-cli package")
    extract_subdir(jans_zip_file, 'jans-cli', 'jans-cli', os.path.join(jans_app_dir, 'jans-cli.zip'))

def uninstall_jans():
    check_installation()
    if not app_globals.argsp.yes:
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
    for service in app_globals.jetty_services:
        if os.path.exists(os.path.join(jetty_home, service)):
            default_fn = os.path.join('/etc/default/', service)
            if os.path.exists(default_fn):
                print("Removing", default_fn)
                os.remove(default_fn)
            print("Stopping", service)
            os.system('systemctl stop ' + service)

    if app_globals.argsp.profile == 'jans':
        print("Stopping OpenDj Server")
        os.system('/opt/opendj/bin/stop-ds')

    remove_list = ['/etc/certs', '/etc/jans', '/opt/jans', '/opt/amazon-corretto*', '/opt/jre', '/opt/node*', '/opt/jetty*', '/opt/jython*']
    if app_globals.argsp.profile == 'jans':
        remove_list.append('/opt/opendj')
    if not app_globals.argsp.keep_downloads:
        remove_list.append('/opt/dist')

    for p in remove_list:
        cmd = 'rm -r -f ' + p
        print("Executing", cmd)
        os.system('rm -r -f ' + p)


def upgrade():
    check_installation()

    for service in app_globals.jetty_services:
        source_fn = os.path.join('/opt/dist/jans', service +'.war')
        target_fn = os.path.join(jetty_home, service, 'webapps', service +'.war' )
        if os.path.exists(target_fn):
            print("Copying", source_fn, "as", target_fn)
            shutil.move(target_fn, target_fn+'-back.' + time.ctime())
            shutil.copy(source_fn, target_fn)
            print("Restarting", service)
            os.system('systemctl restart ' + service)

    jans_config_api_fn = '/opt/jans/config-api/jans-config-api-runner.jar'
    if os.path.exists(jans_config_api_fn):
        shutil.move(jans_config_api_fn, jans_config_api_fn + '-back.' + time.ctime())
        source_fn = '/opt/dist/jans/jans-config-api-runner.jar'
        print("Copying", source_fn, "as", jans_config_api_fn)
        shutil.copy(source_fn, jans_config_api_fn)
        print("Restarting jans-config-api")
        os.system('systemctl restart jans-config-api')

def do_install():
    if not app_globals.argsp.no_jans_setup:
        extract_setup()

    extract_yaml_files()

    if app_globals.argsp.profile == 'jans':
        if not app_globals.argsp.no_gcs:
            download_gcs()
    else:
        profile_setup()

    if not app_globals.argsp.no_setup:
        print("Launching Janssen Setup")

        setup_cmd = 'python3 {}/setup.py'.format(app_globals.argsp.setup_dir)

        if app_globals.argsp.args:
            setup_cmd += ' ' + app_globals.argsp.args

        os.system(setup_cmd)

def get_app_info():
    app_versions_url = 'https://raw.githubusercontent.com/JanssenProject/jans/{}/jans-linux-setup/jans_setup/app_info.json'.format(app_globals.argsp.setup_branch)
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_fn = os.path.join(tmp_dir, os.path.basename(app_versions_url))
        download(app_versions_url, tmp_fn)
        with open(tmp_fn) as f:
            app_globals.app_versions = json.load(f)


def main():

    app_globals.argsp = parser.parse_known_args()[0]
    if not app_globals.argsp.uninstall:
        get_app_info()
        init_installer()
        check_install_dependencies()

    if not (app_globals.argsp.use_downloaded or app_globals.argsp.uninstall):
        download_files()

    if app_globals.argsp.upgrade:
        upgrade()
    elif app_globals.argsp.uninstall:
        uninstall_jans()
    else:
        do_install()


if __name__ == "__main__":
    main()
