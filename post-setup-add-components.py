#!/usr/bin/python3

import os
import argparse
import sys
import subprocess
import json
import zipfile

cur_dir = os.path.dirname(os.path.realpath(__file__))
ces_dir = os.path.join(cur_dir, 'ces_current')

parser = argparse.ArgumentParser()
parser.add_argument("-addshib", help="Install Shibboleth SAML IDP", action="store_true")
parser.add_argument("-addpassport", help="Install Passport", action="store_true")
parser.add_argument("-addoxd", help="Install Oxd Server", action="store_true")
parser.add_argument("-addcasa", help="Install Gluu Casa", action="store_true")

args = parser.parse_args()

if  len(sys.argv)<2:
    parser.print_help()
    parser.exit(1)

def read_properties_file(prop_fn):
    jprop = JProperties()
    with open(prop_fn) as f:
        jprop.load(f)

    return jprop

run_oxauth_war_fn = '/opt/gluu/jetty/oxauth/webapps/oxauth.war'

#Determine gluu version
war_zip = zipfile.ZipFile(run_oxauth_war_fn, 'r')
menifest = war_zip.read('META-INF/MANIFEST.MF')

for l in menifest.splitlines():
    ls = l.strip().decode('utf-8')
    n = ls.find(':')

    if ls[:n].strip() == 'Implementation-Version':
        oxVersion_current = ls[n+1:].strip()
        gluu_version_list = oxVersion_current.split('.')
        if not gluu_version_list[-1].isdigit():
            gluu_version_list.pop(-1)

        gluu_version = '.'.join(gluu_version_list)

print("Current Gluu Version", gluu_version)

"""
if os.path.exists(ces_dir + '.back'):
    os.system('rm -r -f ' + ces_dir + '.back')

if os.path.exists(ces_dir):
    os.system('mv {0} {0}.back'.format(ces_dir))

ces_url = 'https://github.com/GluuFederation/community-edition-setup/archive/version_{}.zip'.format(gluu_version)
print("Downloading Community Edition Setup {}".format(gluu_version))

os.system('wget -nv {} -O {}/version_{}.zip'.format(ces_url, cur_dir, gluu_version))
print("Extracting package")
os.system('unzip -o -qq {}/version_{}.zip'.format(cur_dir, gluu_version))
os.system('mv {}/community-edition-setup-version_{} {}/ces_current'.format(cur_dir, gluu_version, cur_dir))
os.system('wget -nv https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/pylib/generate_properties.py -O {}'.format(os.path.join(ces_dir, 'pylib', 'generate_properties.py')))
"""
open(os.path.join(cur_dir, 'ces_current/__init__.py'),'w').close()

sys.path.append(ces_dir)

from ces_current.setup import *
from ces_current.pylib.cbm import CBM
from ces_current.pylib.generate_properties import generate_properties
from ces_current.pylib.jproperties import Properties as JProperties
from ces_current.pylib.gluu_utils import read_properties_file

setup_porperties = generate_properties(True)

setupObj = Setup(ces_dir)
setupObj.initialize()

setupObj.setup = setupObj

setupObj.os_type, setupObj.os_version = setupObj.detect_os_type()
setupObj.os_initdaemon = setupObj.detect_initd()

for setup_key in setup_porperties:
    setattr(setupObj, setup_key, setup_porperties[setup_key])

setupObj.log = os.path.join(setupObj.install_dir, 'post_setup.log')
setupObj.logError = os.path.join(setupObj.install_dir, 'post_setup_error.log')

print("Log Files:", setupObj.log, setupObj.logError)

setupObj.ldapCertFn = setupObj.opendj_cert_fn

# Determine persistence type
gluu_cb_prop_fn = '/etc/gluu/conf/gluu-couchbase.properties'
gluu_prop = read_properties_file(setupObj.gluu_properties_fn)
persistence_type = gluu_prop['persistence.type']
setupObj.persistence_type = persistence_type

if persistence_type == 'hybrid':
    hybrid_prop = read_properties_file(setupObj.gluu_hybrid_roperties)    
    persistence_type = hybrid_prop['storage.default']
if persistence_type == 'couchbase':
    gluu_cb_prop = read_properties_file(setupObj.gluuCouchebaseProperties)
    cb_serevr = gluu_cb_prop['servers'].split(',')[0].strip()
    cb_admin = gluu_cb_prop['auth.userName']
    encoded_cb_password = gluu_cb_prop['auth.userPassword']
    cb_passwd = os.popen('/opt/gluu/bin/encode.py -D ' + encoded_cb_password).read().strip()
    
    from ces_current.pylib.cbm import CBM
    setupObj.cbm = CBM(cb_serevr, cb_admin, cb_passwd)

else:
    setupObj.createLdapPw()

    ox_ldap_prop = read_properties_file('/etc/gluu/conf/gluu-ldap.properties')

    bindDN = ox_ldap_prop['bindDN']
    bindPassword_e = ox_ldap_prop['bindPassword']
    cmd = '/opt/gluu/bin/encode.py -D ' + bindPassword_e    
    bindPassword = os.popen(cmd).read().strip()
    ldap_host_port = ox_ldap_prop['servers'].split(',')[0].strip()

    ldap.set_option(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_ALLOW)
    ldap_conn = ldap.initialize('ldaps://'+ldap_host_port)
    ldap_conn.simple_bind_s(bindDN, bindPassword)


def get_oxTrustConfiguration_ldap():
    result = ldap_conn.search_s(
                        'o=gluu',
                        ldap.SCOPE_SUBTREE,
                        '(objectClass=oxTrustConfiguration)',
                        ['oxTrustConfApplication']
                        )
    dn = result[0][0]
    oxTrustConfApplication = json.loads(result[0][1]['oxTrustConfApplication'][0])

    return dn, oxTrustConfApplication

def installSaml():

    if os.path.exists('/opt//shibboleth-idp'):
        print("SAML is already installed on this system")
        return

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
        print("Shibboleth is already installed on this system")
        sys.exit()

    print("Installing Shibboleth ...")
    setupObj.oxTrustConfigGeneration = "true"



    if setupObj.persistence_type == 'couchbase':
        if 'user' in setupObj.getMappingType('couchbase'):
            setupObj.renderTemplateInOut(
                            os.path.join(ces_dir, 'templates', setupObj.data_source_properties),
                            os.path.join(ces_dir, 'templates'),
                            os.path.join(ces_dir, 'output'),
                            )

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

    if persistence_type == 'ldap':

        dn, oxTrustConfApplication = get_oxTrustConfiguration_ldap()

        oxTrustConfApplication['configGeneration'] = True
        oxTrustConfApplication_js = json.dumps(oxTrustConfApplication, indent=2)

        ldap_conn.modify_s(
                        dn,
                        [( ldap.MOD_REPLACE, 'oxTrustConfApplication',  oxTrustConfApplication_js)]
                    )
        ldap_conn.modify_s('ou=configuration,o=gluu', [( ldap.MOD_REPLACE, 'gluuSamlEnabled',  'true')])

    else:
        bucket = gluu_cb_prop['bucket.default']
        
        n1ql = 'UPDATE `{}` USE KEYS "configuration_oxtrust" SET configGeneration=true'.format(bucket)
        setupObj.cbm.exec_query(n1ql)
        
        n1ql = 'UPDATE `{}` USE KEYS "configuration" SET gluuSamlEnabled=true'.format(bucket)
        setupObj.cbm.exec_query(n1ql)

    print("Shibboleth installation done")


    
def installPassport():
    
    if os.path.exists('/opt/gluu/node/passport'):
        print("Passport is already installed on this system")
        return

    # we need to find a way to determine node version used by current passport
    """
    node_url = 'https://nodejs.org/dist/v{0}/node-v{0}-linux-x64.tar.xz'.format(setupObj.node_version)
    nod_archive_fn = os.path.basename(node_url)

    print("Downloading {}".format(nod_archive_fn))
    setupObj.run(['wget', '-nv', node_url, '-O', os.path.join(setupObj.distAppFolder, nod_archive_fn)])
    cur_node_dir = os.readlink('/opt/node')
    setupObj.run(['unlink', '/opt/node'])
    setupObj.run(['mv', cur_node_dir, cur_node_dir+'.back'])

    print("Installing", nod_archive_fn)
    setupObj.installNode()
    """

    passport_url = 'https://ox.gluu.org/npm/passport/passport-{}.tgz'.format(gluu_version)
    passport_modules_url = 'https://ox.gluu.org/npm/passport/passport-version_{}-node_modules.tar.gz'.format(gluu_version)
    passport_fn = os.path.basename(passport_url)
    passport_modules_fn = os.path.basename(passport_modules_url)

    print("Downloading {}".format(passport_fn))
    setupObj.run(['wget', '-nv', passport_url, '-O', os.path.join(setupObj.distGluuFolder, 'passport.tgz')])

    print("Downloading {}".format(passport_modules_fn))
    setupObj.run(['wget', '-nv', passport_modules_url, '-O', os.path.join(setupObj.distGluuFolder, 'passport-node_modules.tar.gz')])

    if setupObj.os_initdaemon == 'systemd':
        passport_syatemd_url = 'https://raw.githubusercontent.com/GluuFederation/community-edition-package/master/package/systemd/passport.service'
        passport_syatemd_fn = os.path.basename(passport_syatemd_url)
        
        print("Downloading {}".format(passport_syatemd_fn))
        setupObj.run(['wget', '-nv', passport_syatemd_url, '-O', '/usr/lib/systemd/system/passport.service'])

    setupObj.installPassport = True
    setupObj.calculate_selected_aplications_memory()
    
    
    setupObj.renderTemplateInOut(
                    os.path.join(ces_dir, 'templates/node/passport'),
                    os.path.join(ces_dir, 'templates/node'),
                    os.path.join(ces_dir, 'output/node')
                    )


    print("Installing Passport ...")

    if not os.path.exists(os.path.join(setupObj.configFolder, 'passport-inbound-idp-initiated.json')) and os.path.exists('ces_current/templates/passport-inbound-idp-initiated.json'):
        setupObj.run(['cp', 'ces_current/templates/passport-inbound-idp-initiated.json', setupObj.configFolder])

    proc = subprocess.Popen('echo "" | /opt/jre/bin/keytool -list -v -keystore /etc/certs/passport-rp.jks', shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    
    setupObj.generate_passport_configuration()

    passport_oxtrust_config = json.loads(
        setupObj.readFile(setupObj.passport_oxtrust_config_fn)
        )

    setupObj.install_passport()
    
    scripts_enable = ['2FDB-CF02', 'D40C-1CA4', '2DAF-F9A5']
    
    if persistence_type == 'ldap':

        dn, oxTrustConfApplication = get_oxTrustConfiguration_ldap()

        for k in passport_oxtrust_config:
            oxTrustConfApplication[k] = passport_oxtrust_config[k]
        
        oxTrustConfApplication_js = json.dumps(oxTrustConfApplication, indent=2)
        ldap_conn.modify_s(
                        dn,
                        [( ldap.MOD_REPLACE, 'oxTrustConfApplication',  oxTrustConfApplication_js)]
                    )
        ldap_conn.modify_s('ou=configuration,o=gluu', [( ldap.MOD_REPLACE, 'gluuPassportEnabled',  'true')])
        
        for scr in scripts_enable:
            ldap_conn.modify_s(
                        'inum={},ou=scripts,o=gluu'.format(scr),
                        [( ldap.MOD_REPLACE, 'oxEnabled',  'true')]
                    )
        
    else:
        bucket = gluu_cb_prop['bucket.default']

        n1ql = 'UPDATE `{}` USE KEYS "configuration" SET gluuPassportEnabled=true'.format(bucket)
        setupObj.cbm.exec_query(n1ql)
        
        for k in passport_oxtrust_config:
            n1ql = 'UPDATE `{}` USE KEYS "configuration_oxtrust" SET {}="{}"'.format(bucket, k, passport_oxtrust_config[k])
            setupObj.cbm.exec_query(n1ql)
    
        for scr in scripts_enable:
            n1ql = 'UPDATE `{}` USE KEYS "scripts_{}" SET oxEnabled=true'.format(bucket, scr)
            setupObj.cbm.exec_query(n1ql)
    
    print("Passport installation done")

def installOxd():
    
    if os.path.exists('/opt/oxd-server'):
        print("Oxd server was already installed")
        return
    
    print("Installing Oxd Server")

    oxd_url = 'https://ox.gluu.org/maven/org/gluu/oxd-server/{0}/oxd-server-{0}-distribution.zip'.format(oxVersion_current)

    print("Downloading {} and preparing package".format(os.path.basename(oxd_url)))
    oxd_zip_fn = '/tmp/oxd-server.zip'
    oxd_tmp_dir = '/tmp/oxd-server'

    setupObj.run(['wget', '-nv', oxd_url, '-O', oxd_zip_fn])
    setupObj.run(['unzip', '-qqo', '/tmp/oxd-server.zip', '-d', oxd_tmp_dir])
    setupObj.run(['mkdir', os.path.join(oxd_tmp_dir,'data')])

    if setupObj.os_type + setupObj.os_version in ('ubuntu18','debian9'):
        default_url = 'https://raw.githubusercontent.com/GluuFederation/oxd/version_{}/debian/oxd-server-default'.format(gluu_version)
        setupObj.run(['wget', '-nv', default_url, '-O', os.path.join(oxd_tmp_dir, 'oxd-server-default')])

    service_file = 'oxd-server.init.d' if setupObj.os_type + setupObj.os_version in ('ubuntu18','debian9') else 'oxd-server.service'
    service_url = 'https://raw.githubusercontent.com/GluuFederation/oxd/version_{}/debian/{}.file'.format(gluu_version, service_file)
    setupObj.run(['wget', '-nv', service_url, '-O', os.path.join(oxd_tmp_dir, service_file)])

    oxd_server_sh_url = 'https://raw.githubusercontent.com/GluuFederation/oxd/version_{}/debian/oxd-server.sh'.format(gluu_version)
    setupObj.run(['wget', '-nv', oxd_server_sh_url, '-O', os.path.join(oxd_tmp_dir, 'bin/oxd-server.sh')])

    setupObj.run(['tar', '-zcf', os.path.join(setupObj.distGluuFolder, 'oxd-server.tgz'), 'oxd-server'], cwd='/tmp')

    setupObj.oxd_package = os.path.join(setupObj.distGluuFolder, 'oxd-server.tgz')
    setupObj.install_oxd()

def installCasa():

    if os.path.exists('/opt/gluu/jetty/casa'):
        print("Casa is already installed on this system")
        return

    print("Installing Gluu Casa")


    setupObj.promptForCasaInstallation(promptForCasa='y')
    if not setupObj.installCasa:
        print("Casa installation cancelled")

    setupObj.prepare_base64_extension_scripts()

    casa_script_fn = os.path.basename(setupObj.ldif_scripts_casa)
    casa_script_fp = os.path.join(ces_dir, 'output', casa_script_fn)
    
    setupObj.renderTemplateInOut(
                    os.path.join(ces_dir, 'templates', casa_script_fn),
                    os.path.join(ces_dir, 'templates'),
                    os.path.join(ces_dir, 'output'),
                    )

    if persistence_type == 'ldap':
        setupObj.import_ldif_template_opendj(casa_script_fp)
        
    else:
        setupObj.import_ldif_couchebase(ldif_file_list=[casa_script_fp], bucket='gluu')

    if setupObj.installOxd:
        installOxd()
        setupObj.run_service_command('oxd-server', 'restart')

    setupObj.import_oxd_certificate()

    setupObj.renderTemplateInOut(
                    os.path.join(ces_dir, 'templates/casa.json'),
                    os.path.join(ces_dir, 'templates'),
                    os.path.join(ces_dir, 'output'),
                    )
    setupObj.calculate_selected_aplications_memory()
    setupObj.install_casa()

if args.addshib:
    installSaml()

if args.addpassport:
    installPassport()

if args.addoxd:
    installOxd()

if args.addcasa:
    installCasa()

if persistence_type == 'ldap':
    setupObj.deleteLdapPw()

print("Please exit container and restart Gluu Server")
