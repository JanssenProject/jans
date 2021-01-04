#!/usr/bin/python3

import sys
import os
import argparse
import zipfile
import shutil
import time
import ssl

from urllib.request import urlretrieve
from urllib.parse import urljoin


setup_package_name = 'master.zip'
maven_base_url = 'https://maven.jans.io/maven/io/jans/'

app_versions = {
  "JANS_APP_VERSION": "1.0.0",
  "JANS_BUILD": "-SNAPSHOT", 
  "JETTY_VERSION": "9.4.31.v20200723", 
  "AMAZON_CORRETTO_VERSION": "11.0.8.10.1", 
  "JYTHON_VERSION": "2.7.2",
  "OPENDJ_VERSION": "4.0.0.gluu",
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
parser.add_argument('-u', help="Use downloaded components", action='store_true')
parser.add_argument('-upgrade', help="Upgrade Janssen war and jar files", action='store_true')
parser.add_argument('--args', help="Arguments to be passed to setup.py")
argsp = parser.parse_args()


ssl._create_default_https_context = ssl._create_unverified_context

def download(url, target_fn):
    
    dst = os.path.join(app_dir, target_fn)
    pardir, fn = os.path.split(dst)
    if not os.path.exists(pardir):
        os.makedirs(pardir)
    print("Downloading", url, "to", dst)
    urlretrieve(url, dst)


setup_zip_file = os.path.join(jans_app_dir, 'jans-setup.zip')

if not argsp.u:
    setup_url = 'https://github.com/JanssenProject/jans-setup/archive/master.zip'
    download(setup_url, setup_zip_file)

    download('https://corretto.aws/downloads/resources/{0}/amazon-corretto-{0}-linux-x64.tar.gz'.format(app_versions['AMAZON_CORRETTO_VERSION']), os.path.join(app_dir, 'amazon-corretto-{0}-linux-x64.tar.gz'.format(app_versions['AMAZON_CORRETTO_VERSION'])))
    download('https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/{0}/jetty-distribution-{0}.tar.gz'.format(app_versions['JETTY_VERSION']), os.path.join(app_dir,'jetty-distribution-{0}.tar.gz'.format(app_versions['JETTY_VERSION'])))
    download('https://repo1.maven.org/maven2/org/python/jython-installer/{0}/jython-installer-{0}.jar'.format(app_versions['JYTHON_VERSION']), os.path.join(app_dir, 'jython-installer-{0}.jar'.format(app_versions['JYTHON_VERSION'])))
    download('https://ox.gluu.org/maven/org/gluufederation/opendj/opendj-server-legacy/{0}/opendj-server-legacy-{0}.zip'.format(app_versions['OPENDJ_VERSION']), os.path.join(app_dir, 'opendj-server-legacy-{0}.zip'.format(app_versions['OPENDJ_VERSION'])))
    download(urljoin(maven_base_url, 'jans-auth-server/{0}{1}/jans-auth-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-auth.war'))
    download(urljoin(maven_base_url, 'jans-auth-client/{0}{1}/jans-auth-client-{0}{1}-jar-with-dependencies.jar'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-auth-client-jar-with-dependencies.jar'))
    download(urljoin(maven_base_url, 'jans-config-api/{0}{1}/jans-config-api-{0}{1}-runner.jar'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-config-api-runner.jar'))
    download(urljoin(maven_base_url, 'jans-fido2-server/{0}{1}/jans-fido2-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-fido2.war'))
    download(urljoin(maven_base_url, 'jans-scim-server/{0}{1}/jans-scim-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD'])), os.path.join(jans_app_dir, 'jans-scim.war'))
    download('https://jenkins.jans.io/maven/io/jans/jans-eleven-server/{0}{1}/jans-eleven-server-{0}{1}.war'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD']), os.path.join(jans_app_dir, 'jans-eleven.war'))
    download('https://github.com/JanssenProject/jans-cli/archive/main.zip', os.path.join(jans_app_dir, 'jans-cli.zip'))


if argsp.upgrade:

    if not (os.path.exists('/opt/jans/jetty') and os.path.join('/opt/jans/jans-setup/setup_app') and ('/etc/jans/conf/jans.properties')):
        print("Jans server seems not installed")
        sys.exit()

    jetty_home = '/opt/jans/jetty'
    for service in ('jans-auth', 'jans-fido2', 'jans-scim', 'jans-eleven'):
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

else:
    if os.path.exists(setup_dir):
        shutil.move(setup_dir, setup_dir + '-back.' + time.ctime())

    print("Extracting jans-setup package")

    setup_zip = zipfile.ZipFile(setup_zip_file, "r")
    setup_par_dir = setup_zip.namelist()[0]

    for filename in setup_zip.namelist():
        setup_zip.extract(filename, jans_dir)

    shutil.move(os.path.join(jans_dir,setup_par_dir), setup_dir)

    download('https://raw.githubusercontent.com/JanssenProject/jans-config-api/master/src/main/resources/uma-rs-protect.json'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD']), os.path.join(setup_dir, 'setup_app/data/uma-rs-protect.json'))
    download('https://raw.githubusercontent.com/JanssenProject/jans-config-api/master/docs/jans-config-api-swagger.yaml'.format(app_versions['JANS_APP_VERSION'], app_versions['JANS_BUILD']), os.path.join(setup_dir, 'setup_app/data/jans-config-api-swagger.yaml'))

    print("Launcing Janssen Setup")
    setup_cmd = 'python3 {}/setup.py'.format(setup_dir)

    if argsp.args:
        setup_cmd += ' ' + argsp.args

    os.system(setup_cmd)
