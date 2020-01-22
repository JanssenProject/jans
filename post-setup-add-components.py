#!/usr/bin/python
import os
import argparse
import sys
import subprocess
import json

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
parser.add_argument("-addradius", help="Install Radius", action="store_true")
args = parser.parse_args()

if  len(sys.argv)<2:
    parser.print_help()
    parser.exit(1)


from setup import *

setupObj = Setup('.')

setupObj.setup = setupObj
setupObj.load_properties('/install/community-edition-setup/setup.properties.last')

setupObj.log = os.path.join(setupObj.install_dir, 'post_setup.log')
setupObj.logError = os.path.join(setupObj.install_dir, 'post_setup_error.log')

attribDataTypes.startup('.')

setupObj.cbm = CBM(setupObj.couchbase_hostname, setupObj.couchebaseClusterAdmin, setupObj.ldapPass)


if not hasattr(setupObj, 'ldap_type'):
    setupObj.ldap_type = 'open_ldap'

if setupObj.ldap_type == 'opendj':
    setupObj.ldapCertFn = setupObj.opendj_cert_fn
else:
    setupObj.ldapCertFn = setupObj.openldapTLSCert

setupObj.ldapCertFn = setupObj.opendj_cert_fn

def installSaml():

    if setupObj.idp3_metadata[0] == '/':
        setupObj.idp3_metadata = setupObj.idp3_metadata[1:]

    metadata_file = os.path.join(setupObj.idp3MetadataFolder, setupObj.idp3_metadata)


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
    print "Shibboleth installation done"

def installRadius():

    radius_dir = '/opt/gluu/radius'
    logs_dir = os.path.join(radius_dir,'logs')
    radius_jar = os.path.join(setupObj.distGluuFolder, 'super-gluu-radius-server.jar')
    source_dir = os.path.join(setupObj.staticFolder, 'radius')
    conf_dir = os.path.join(setupObj.gluuBaseFolder, 'conf/radius/')
    radius_libs = os.path.join(setupObj.distGluuFolder, 'gluu-radius-libs.zip')
    ldif_file_server = os.path.join(setupObj.outputFolder, 'gluu_radius_server.ldif')

    if not os.path.exists(logs_dir):
        setupObj.run([setupObj.cmd_mkdir, '-p', logs_dir])

    setupObj.run(['unzip', '-n', '-q', radius_libs, '-d', radius_dir ])
    setupObj.copyFile(radius_jar, radius_dir)

    if setupObj.mappingLocations['default'] == 'ldap':
        schema_ldif = os.path.join(source_dir, 'schema/98-radius.ldif')
        setupObj.import_ldif_opendj([schema_ldif])
        setupObj.import_ldif_opendj([ldif_file_server])
    else:
        setupObj.import_ldif_couchebase([ldif_file_server])

    setupObj.createUser('radius', homeDir=radius_dir, shell='/bin/false')
    setupObj.addUserToGroup('gluu', 'radius')
    
    setupObj.copyFile(os.path.join(source_dir, 'etc/default/gluu-radius'), setupObj.osDefault)
    setupObj.copyFile(os.path.join(source_dir, 'etc/gluu/conf/radius/gluu-radius-logging.xml'), conf_dir)
    setupObj.copyFile(os.path.join(source_dir, 'scripts/gluu_common.py'), os.path.join(setupObj.gluuOptPythonFolder, 'libs'))

    setupObj.copyFile(os.path.join(source_dir, 'etc/init.d/gluu-radius'), '/etc/init.d')
    setupObj.run([setupObj.cmd_chmod, '+x', '/etc/init.d/gluu-radius'])
    
    if setupObj.os_type+setupObj.os_version == 'ubuntu16':
        setupObj.run(['update-rc.d', 'gluu-radius', 'defaults'])
    else:
        setupObj.copyFile(os.path.join(source_dir, 'systemd/gluu-radius.service'), '/usr/lib/systemd/system')
        setupObj.run([setupObj.systemctl, 'daemon-reload'])
    
    #create empty gluu-radius.private-key.pem
    gluu_radius_private_key_fn = os.path.join(setupObj.certFolder, 'gluu-radius.private-key.pem')
    setupObj.writeFile(gluu_radius_private_key_fn, '')
    
    setupObj.run([setupObj.cmd_chown, '-R', 'radius:gluu', radius_dir])
    setupObj.run([setupObj.cmd_chown, '-R', 'root:gluu', conf_dir])
    setupObj.run([setupObj.cmd_chown, 'root:gluu', os.path.join(setupObj.gluuOptPythonFolder, 'libs/gluu_common.py')])

    setupObj.enable_service_at_start('gluu-radius')

    setupObj.run([setupObj.cmd_chown, 'radius:gluu', os.path.join(setupObj.certFolder, 'gluu-radius.jks')])
    setupObj.run([setupObj.cmd_chown, 'radius:gluu', os.path.join(setupObj.certFolder, 'gluu-radius.private-key.pem')])

    setupObj.run([setupObj.cmd_chmod, '660', os.path.join(setupObj.certFolder, 'gluu-radius.jks')])
    setupObj.run([setupObj.cmd_chmod, '660', os.path.join(setupObj.certFolder, 'gluu-radius.private-key.pem')])

def installPassport():
    
    if os.path.exists('/opt/gluu/node/passport'):
        print "Passport is already installed on this system"
        sys.exit()

    print "Installing Passport ..."
    setupObj.generate_passport_configuration()
    setupObj.install_passport()
    print "Passport installation done"


if args.addshib:
    installSaml()

if args.addpassport:
    installPassport()

if args.addradius:
    installRadius()

print "Please exit container and restart Gluu Server"
