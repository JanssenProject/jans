#!/usr/bin/python3

import sys
import os
import argparse
import zipfile
import shutil
import time
import ssl
import json

from urllib import request
from urllib.parse import urljoin


setup_package_name = 'master.zip'
maven_base_url = 'https://maven.jans.io/maven/io/jans/'

app_versions = {
  "JANS_APP_VERSION": "1.0.0",
  "JANS_BUILD": "-SNAPSHOT", 
  "JETTY_VERSION": "9.4.31.v20200723", 
  "AMAZON_CORRETTO_VERSION": "11.0.8.10.1", 
  "JYTHON_VERSION": "2.7.2",
  "OPENDJ_VERSION": "4.4.10",
  "SETUP_BRANCH": "master",
}

jans_dir = '/opt/jans'
app_dir = '/opt/dist/app'
jans_app_dir = '/opt/dist/jans'
scripts_dir = '/opt/dist/scripts'
setup_dir = os.path.join(jans_dir, 'jans-setup')

for d in (jans_dir, app_dir, jans_app_dir, scripts_dir):
    if not os.path.exists(d):
        os.makedirs(d)

parser = argparse.ArgumentParser(description="This script downloads Janssen Server components and fires setup")
parser.add_argument('-u', help="Use already downloaded components", action='store_true')
parser.add_argument('-upgrade', help="Upgrade Janssen war and jar files", action='store_true')
parser.add_argument('-uninstall', help="Uninstall Jans server and removes all files", action='store_true')
parser.add_argument('--args', help="Arguments to be passed to setup.py")
parser.add_argument('-n', help="No prompt", action='store_true')
parser.add_argument('--keep-downloads', help="Keep downloaded files (applicable for uninstallation only)", action='store_true')

argsp = parser.parse_args()

ssl._create_default_https_context = ssl._create_unverified_context

try:
    from distutils import dist
except:
    if not argsp.n:
        install_dist = input('python3-disutils package is needed. Install now? [Y/n] ')
        if install_dist.lower().startswith('n'):
            print("Can't continue...")
            sys.exit()
    os.system('apt install -y python3-distutils')


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
        gcs_download_url = 'http://162.243.99.240/icrby8xcvbcv/spanner/gcs.tgz'
        tmp_dir = '/tmp/' + os.urandom(5).hex()
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


setup_zip_file = os.path.join(jans_app_dir, 'jans-setup.zip')

if not (argsp.u or argsp.uninstall):
    setup_url = 'https://github.com/JanssenProject/jans-setup/archive/master.zip'
    download(setup_url, setup_zip_file)

    download('https://corretto.aws/downloads/resources/{0}/amazon-corretto-{0}-linux-x64.tar.gz'.format(app_versions['AMAZON_CORRETTO_VERSION']), os.path.join(app_dir, 'amazon-corretto-{0}-linux-x64.tar.gz'.format(app_versions['AMAZON_CORRETTO_VERSION'])))
    download('https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/{0}/jetty-distribution-{0}.tar.gz'.format(app_versions['JETTY_VERSION']), os.path.join(app_dir,'jetty-distribution-{0}.tar.gz'.format(app_versions['JETTY_VERSION'])))
    download('https://repo1.maven.org/maven2/org/python/jython-installer/{0}/jython-installer-{0}.jar'.format(app_versions['JYTHON_VERSION']), os.path.join(app_dir, 'jython-installer-{0}.jar'.format(app_versions['JYTHON_VERSION'])))
    download('https://ox.gluu.org/maven/org/gluufederation/opendj/opendj-server-legacy/{0}/opendj-server-legacy-{0}.zip'.format(app_versions['OPENDJ_VERSION']), os.path.join(app_dir, 'opendj-server-legacy-{0}.zip'.format(app_versions['OPENDJ_VERSION'])))
    download(urljoin(maven_base_url, 'jans-auth-server/{0}{1}/jans-auth-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-auth.war'))
    download(urljoin(maven_base_url, 'jans-auth-client/{0}{1}/jans-auth-client-{0}{1}-jar-with-dependencies.jar'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-auth-client-jar-with-dependencies.jar'))
    download(urljoin(maven_base_url, 'jans-config-api-server/{0}{1}/jans-config-api-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-config-api.war'))
    download(urljoin(maven_base_url, 'jans-fido2-server/{0}{1}/jans-fido2-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-fido2.war'))
    download(urljoin(maven_base_url, 'jans-scim-server/{0}{1}/jans-scim-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-scim.war'))
    download('https://jenkins.jans.io/maven/io/jans/jans-eleven-server/{0}{1}/jans-eleven-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD']), os.path.join(jans_app_dir, 'jans-eleven.war'))
    download('https://api.github.com/repos/JanssenProject/jans-cli/tarball/main', os.path.join(jans_app_dir, 'jans-cli.tgz'))
    download('https://github.com/sqlalchemy/sqlalchemy/archive/rel_1_3_23.zip', os.path.join(jans_app_dir, 'sqlalchemy.zip'))
    download('https://www.apple.com/certificateauthority/Apple_WebAuthn_Root_CA.pem', os.path.join(app_dir, 'Apple_WebAuthn_Root_CA.pem'))

jetty_home = '/opt/jans/jetty'
jetty_services = ('jans-auth', 'jans-fido2', 'jans-scim', 'jans-eleven')

def check_installation():
    if not (os.path.exists(jetty_home) and os.path.join('/opt/jans/jans-setup/setup_app') and ('/etc/jans/conf/jans.properties')):
        print("Jans server seems not installed")
        sys.exit()

if argsp.upgrade:

    check_installation()

    for service in jetty_services:
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

elif argsp.uninstall:
    check_installation()
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

    print("Stopping OpenDj Server")
    os.system('/opt/opendj/bin/stop-ds')
    for service in jetty_services:
        if os.path.exists(os.path.join(jetty_home, service)):
            default_fn = os.path.join('/etc/default/', service)
            if os.path.exists(default_fn):
                print("Removing", default_fn)
                os.remove(default_fn)
            print("Stopping", service)
            os.system('systemctl stop ' + service)

    remove_list = ['/etc/certs', '/etc/jans', '/opt/jans', '/opt/amazon-corretto*', '/opt/jre', '/opt/jetty*', '/opt/jython*', '/opt/opendj']
    if not argsp.keep_downloads:
        remove_list.append('/opt/dist')

    for p in remove_list:
        cmd = 'rm -r -f ' + p
        print("Executing", cmd)
        os.system('rm -r -f ' + p)

else:
    if os.path.exists(setup_dir):
        shutil.move(setup_dir, setup_dir + '-back.' + time.ctime())

    print("Extracting jans-setup package")

    setup_zip = zipfile.ZipFile(setup_zip_file, "r")
    setup_par_dir = setup_zip.namelist()[0]

    for filename in setup_zip.namelist():
        setup_zip.extract(filename, jans_dir)

    shutil.move(os.path.join(jans_dir,setup_par_dir), setup_dir)

    sqlalchemy_zfn = os.path.join(jans_app_dir, 'sqlalchemy.zip')
    sqlalchemy_zip = zipfile.ZipFile(sqlalchemy_zfn, "r")
    sqlalchemy_par_dir = sqlalchemy_zip.namelist()[0]
    tmp_dir = os.path.join('/tmp', os.urandom(2).hex())
    sqlalchemy_zip.extractall(tmp_dir)
    shutil.copytree(
            os.path.join(tmp_dir, sqlalchemy_par_dir, 'lib/sqlalchemy'), 
            os.path.join(setup_dir, 'setup_app/pylib/sqlalchemy')
            )
    shutil.rmtree(tmp_dir)

    download_gcs()

    download('https://raw.githubusercontent.com/JanssenProject/jans-config-api/master/docs/jans-config-api-swagger.yaml', os.path.join(setup_dir, 'setup_app/data/jans-config-api-swagger.yaml'))
    download('https://raw.githubusercontent.com/JanssenProject/jans-scim/master/server/src/main/resources/jans-scim-openapi.yaml'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD']), os.path.join(setup_dir, 'setup_app/data/jans-scim-openapi.yaml'))
    print("Launching Janssen Setup")
    setup_cmd = 'python3 {}/setup.py'.format(setup_dir)

    if argsp.args:
        setup_cmd += ' ' + argsp.args

    os.system(setup_cmd)
