#!/usr/bin/python
import os
import argparse
import sys
import subprocess
import json
import zipfile

cur_dir = os.path.dirname(os.path.realpath(__file__))

if not os.path.exists('setup.py'):
    print "This script should be run from /install/community-edition-setup/"
    sys.exit()
    
if not os.path.exists('/install/community-edition-setup/setup.properties.last'):
    print "setup.properties.last is missing can't continue"
    sys.exit()

f=open('setup.py').readlines()

for l in f:
    if l.startswith('from pyDes import *'):
        break
else:
    f.insert(1, 'from pyDes import *\n')
    with open('setup.py','w') as w:
        w.write(''.join(f))

parser = argparse.ArgumentParser()
parser.add_argument("-addshib", help="Install Shibboleth SAML IDP", action="store_true")
parser.add_argument("-addpassport", help="Install Passport", action="store_true")
args = parser.parse_args()

if  len(sys.argv)<2:
    parser.print_help()
    parser.exit(1)


oxVersion = 0
setup_properties_fn = '/install/community-edition-setup/setup.properties.last'
install_dir = '.'

#Determine setup version
with open(setup_properties_fn) as f:
    for l in f:
        ls = l.strip()
        if ls.startswith('oxVersion'):
            n = ls.find('=')
            oxVersion_setup = ls[n+1:].strip()

run_oxauth_war_fn = '/opt/gluu/jetty/oxauth/webapps/oxauth.war'

os.system('cp -f {} /opt/dist/gluu'.format(run_oxauth_war_fn))

#Determine gluu version
war_zip = zipfile.ZipFile(run_oxauth_war_fn, 'r')
menifest = war_zip.read('META-INF/MANIFEST.MF')

for l in menifest.splitlines():
    ls = l.strip()
    n = ls.find(':')

    if ls[:n].strip() == 'Implementation-Version':
        oxVersion_current = ls[n+1:].strip()
        gluu_version_list = oxVersion_current.split('.')
        if not gluu_version_list[-1].isdigit():
            gluu_version_list.pop(-1)

        gluu_version = '.'.join(gluu_version_list)

print "Current Gluu Version", gluu_version

ces_version_l = []
for ci in oxVersion_current.split('.'):
    if ci.lower() == 'final' or ci.lower().startswith('sp') or ci.lower().startswith('patch'):
        continue
    ces_version_l.append(ci)
    
ces_version = '.'.join(ces_version_l)

if os.path.exists('ces_current.back'):
    os.system('rm -r -f ces_current.back')

if os.path.exists('ces_current'):
    os.system('mv ces_current ces_current.back')

ces_url = 'https://github.com/GluuFederation/community-edition-setup/archive/version_{}.zip'.format(ces_version)

print "Downloading Community Edition Setup {}".format(ces_version)

os.system('wget -nv {} -O version_{}.zip'.format(ces_url, ces_version))
print "Extracting package"
os.system('unzip -o -qq version_{}.zip'.format(ces_version))
os.system('mv community-edition-setup-version_{} ces_current'.format(ces_version))

open('ces_current/__init__.py','w').close()

sys.path.append('ces_current')

from ces_current.setup import *
install_dir = 'ces_current'

setupObj = Setup(install_dir)

setupObj.setup = setupObj

setupObj.os_type, setupObj.os_version = setupObj.detect_os_type()
setupObj.os_initdaemon = setupObj.detect_initd()


if oxVersion != gluu_version:

    keep_keys = ['idp3_war', 'idp3_cml_keygenerator', 'idp3_dist_jar', 'idp3Folder',
                'idp3MetadataFolder', 'idp3MetadataCredentialsFolder', 'idp3LogsFolder',
                'idp3LibFolder', 'idp3ConfFolder', 'idp3ConfAuthnFolder', 
                'idp3CredentialsFolder', 'idp3WebappFolder', 'oxVersion',
                'templateFolder', 'outputFolder',
                'ldif_passport_config', 'ldif_passport', 'ldif_passport_clients',
                ]
    keep_dict = {}

    for k in keep_keys:
        v = getattr(setupObj, k)
        if v:
            keep_dict[k] = v

setupObj.load_properties('/install/community-edition-setup/setup.properties.last')

if oxVersion != gluu_version:
    for k in keep_dict:
        setattr(setupObj, k, keep_dict[k])


setupObj.githubBranchName = oxVersion

setupObj.log = os.path.join(setupObj.install_dir, 'post_setup.log')
setupObj.logError = os.path.join(setupObj.install_dir, 'post_setup_error.log')

print "Log Files:", setupObj.log, setupObj.logError

attribDataTypes.startup(install_dir)

if oxVersion == gluu_version:
    setupObj.cbm = CBM(setupObj.couchbase_hostname, setupObj.couchebaseClusterAdmin, setupObj.ldapPass)


if not hasattr(setupObj, 'ldap_type'):
    setupObj.ldap_type = 'open_ldap'

if setupObj.ldap_type == 'opendj':
    setupObj.ldapCertFn = setupObj.opendj_cert_fn
else:
    setupObj.ldapCertFn = setupObj.openldapTLSCert

setupObj.ldapCertFn = setupObj.opendj_cert_fn

def installSaml():

    setupObj.run(['cp', '-f', os.path.join(setupObj.gluuOptFolder, 'jetty/identity/webapps/identity.war'), 
                setupObj.distGluuFolder])

    if not os.path.exists(setupObj.idp3Folder):
        os.mkdir(setupObj.idp3Folder)

    if setupObj.idp3_metadata[0] == '/':
        setupObj.idp3_metadata = setupObj.idp3_metadata[1:]

    metadata_file = os.path.join(setupObj.idp3MetadataFolder, setupObj.idp3_metadata)

    setupObj.run(['cp', '-f', './ces_current/templates/jetty.conf.tmpfiles.d',
                            setupObj.templateFolder])

    if os.path.exists(metadata_file):
        print "Shibboleth is already installed on this system"
        sys.exit()

    print "Installing Shibboleth ..."
    setupObj.oxTrustConfigGeneration = "true"

    if not setupObj.application_max_ram:
        setupObj.application_max_ram = setupObj.getPrompt("Enter maximum RAM for applications in MB", '3072')

    if not setupObj.hostname:
        setupObj.hostname = setupObj.getPrompt("Hostname", '')

    if not setupObj.orgName:
        setupObj.orgName = setupObj.getPrompt("Organization Name", '')

    if not setupObj.shibJksPass:
        setupObj.shibJksPass = setupObj.getPW()
        setupObj.gen_cert('shibIDP', setupObj.shibJksPass, 'jetty')


    setupObj.calculate_selected_aplications_memory()
    realIdp3Folder = os.path.realpath(setupObj.idp3Folder)
    setupObj.run([setupObj.cmd_chown, '-R', 'jetty:jetty', realIdp3Folder])
    realIdp3BinFolder = "%s/bin" % realIdp3Folder

    if os.path.exists(realIdp3BinFolder):
        setupObj.run(['find', realIdp3BinFolder, '-name', '*.sh', '-exec', 'chmod', "755", '{}',  ';'])
    
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3Folder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3MetadataFolder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3MetadataCredentialsFolder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3LogsFolder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3LibFolder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3ConfFolder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3ConfAuthnFolder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3CredentialsFolder])
    setupObj.run([setupObj.cmd_mkdir, '-p', setupObj.idp3WebappFolder])
    

    setupObj.run(['/usr/bin/wget', setupObj.idp3_war, '--no-verbose', '-c', '--retry-connrefused', '--tries=10', '-O', '%s/idp.war' % setupObj.distGluuFolder])
    setupObj.run(['/usr/bin/wget', setupObj.idp3_cml_keygenerator, '--no-verbose', '-c', '--retry-connrefused', '--tries=10', '-O', setupObj.distGluuFolder + '/idp3_cml_keygenerator.jar'])
    setupObj.run(['/usr/bin/wget', setupObj.idp3_dist_jar, '--no-verbose', '-c', '--retry-connrefused', '--tries=10', '-O', setupObj.distGluuFolder + '/shibboleth-idp.jar'])
    setupObj.installSaml = True
    setupObj.install_saml()
    
    setupObj.run([setupObj.cmd_chown, '-h', 'root:gluu', '/etc/certs/idp-signing.crt'])
    setupObj.run([setupObj.cmd_chown, '-h', 'root:gluu', '/etc/certs/idp-signing.key'])

    metadata = open(metadata_file).read()
    metadata = metadata.replace('md:ArtifactResolutionService', 'ArtifactResolutionService')
    with open(metadata_file,'w') as F:
        F.write(metadata)
    
    setupObj.run([setupObj.cmd_chown, '-R', 'jetty:jetty', setupObj.idp3Folder])
    if not os.path.exists('/var/run/jetty'):
        os.mkdir('/var/run/jetty')
    setupObj.run([setupObj.cmd_chown, '-R', 'jetty:jetty', '/var/run/jetty'])
    setupObj.enable_service_at_start('idp')

    print "Shibboleth installation done"


    
def installPassport():
    
    if os.path.exists('/opt/gluu/node/passport'):
        print "Passport is already installed on this system"
        sys.exit()

    if oxVersion != gluu_version:

        node_url = 'https://nodejs.org/dist/v{0}/node-v{0}-linux-x64.tar.xz'.format(setupObj.node_version)
        nod_archive_fn = os.path.basename(node_url)

        print "Downloading {}".format(nod_archive_fn)
        setupObj.run(['wget', '-nv', node_url, '-O', os.path.join(setupObj.distAppFolder, nod_archive_fn)])
        cur_node_dir = os.readlink('/opt/node')
        setupObj.run(['unlink', '/opt/node'])
        setupObj.run(['mv', cur_node_dir, cur_node_dir+'.back'])

        print "Installing", nod_archive_fn
        setupObj.installNode()

        passport_url = 'https://ox.gluu.org/npm/passport/passport-{}.tgz'.format(gluu_version)
        passport_modules_url = 'https://ox.gluu.org/npm/passport/passport-version_{}-node_modules.tar.gz'.format(gluu_version)
        passport_fn = os.path.basename(passport_url)
        passport_modules_fn = os.path.basename(passport_modules_url)

        print "Downloading {}".format(passport_fn)
        setupObj.run(['wget', '-nv', passport_url, '-O', os.path.join(setupObj.distGluuFolder, 'passport.tgz')])

        print "Downloading {}".format(passport_modules_fn)
        setupObj.run(['wget', '-nv', passport_modules_url, '-O', os.path.join(setupObj.distGluuFolder, 'passport-node_modules.tar.gz')])

        if setupObj.os_initdaemon == 'systemd':
            passport_syatemd_url = 'https://raw.githubusercontent.com/GluuFederation/community-edition-package/master/package/systemd/passport.service'
            passport_syatemd_fn = os.path.basename(passport_syatemd_url)
            
            print "Downloading {}".format(passport_syatemd_fn)
            setupObj.run(['wget', '-nv', passport_syatemd_url, '-O', '/usr/lib/systemd/system/passport.service'])



    setupObj.installPassport = True
    setupObj.calculate_selected_aplications_memory()
    
    setupObj.renderTemplateInOut(
                    os.path.join(cur_dir, 'ces_current/templates/node/passport'),
                    os.path.join(cur_dir, 'ces_current/templates/node'),
                    os.path.join(cur_dir, 'ces_current/output/node')
                    )


    print "Installing Passport ..."

    if not os.path.exists(os.path.join(setupObj.configFolder, 'passport-inbound-idp-initiated.json')) and os.path.exists('ces_current/templates/passport-inbound-idp-initiated.json'):
        setupObj.run(['cp', 'ces_current/templates/passport-inbound-idp-initiated.json', setupObj.configFolder])

    proc = subprocess.Popen('echo "" | /opt/jre/bin/keytool -list -v -keystore /etc/certs/passport-rp.jks', shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    alias_l=''
    
    setupObj.generate_passport_configuration()
    setupObj.install_passport()
    
    
    print "Passport installation done"


if args.addshib:
    installSaml()

if args.addpassport:
    installPassport()

print "Please exit container and restart Gluu Server"
