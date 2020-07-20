#!/usr/bin/python3

import os
import argparse
import sys
import subprocess
import json
import zipfile
import ldap3

cur_dir = os.path.dirname(os.path.realpath(__file__))
ces_dir = os.path.join(cur_dir, 'ces_current')

parser = argparse.ArgumentParser()
parser.add_argument("-addshib", help="Install Shibboleth SAML IDP", action="store_true")
parser.add_argument("-addpassport", help="Install Passport", action="store_true")
parser.add_argument("-addoxd", help="Install Oxd Server", action="store_true")
parser.add_argument("-addcasa", help="Install Gluu Casa", action="store_true")
parser.add_argument("-addradius", help="Install Gluu Radius Server", action="store_true")
parser.add_argument("-addscim", help="Install Scim Server", action="store_true")
parser.add_argument("-addfido2", help="Install Fido2 Server", action="store_true")



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

gluu_version = '4.2.0'

print("Current Gluu Version", gluu_version)


if os.path.exists(ces_dir + '.back'):
    os.system('rm -r -f ' + ces_dir + '.back')

if os.path.exists(ces_dir):
    os.system('mv {0} {0}.back'.format(ces_dir))

ces_url = 'https://github.com/GluuFederation/community-edition-setup/archive/version_{}.zip'.format(gluu_version)

print("Downloading Community Edition Setup {}".format(gluu_version))

ces_fn = os.path.basename(ces_url)
ces_path = os.path.join(cur_dir, ces_fn)

os.system('wget -nv {} -O {}'.format(ces_url, ces_path))

ces_zip = zipfile.ZipFile(ces_path)
ces_folder = ces_zip.namelist()[0]
print("Extracting package")
os.system('unzip -o -qq {}'.format(ces_path))
os.system('mv {}/{} {}/ces_current'.format(cur_dir, ces_folder, cur_dir))

open(os.path.join(cur_dir, 'ces_current/__init__.py'),'w').close()


sys.path.append(ces_dir)

import ruamel.yaml
from ces_current.setup import *
from ces_current import setup

from ces_current.pylib.cbm import CBM
from ces_current.pylib.generate_properties import generate_properties
from ces_current.pylib.jproperties import Properties as JProperties
from ces_current.pylib.gluu_utils import read_properties_file
from ces_current.pylib.dbutils import get_ldap_conn, get_cbm_conn

class ProgressBar:
    def progress(self, ptype, msg, incr=True):
        print(msg)

setup_porperties = generate_properties(True)


setup.__dict__['ruamel'] = ruamel
setupObj = setup.Setup(ces_dir)
setupObj.initialize()
setupObj.pbar = ProgressBar()
setupObj.oxVersion = gluu_version 

setupObj.setup = setupObj

setupObj.os_type, setupObj.os_version = setupObj.detect_os_type()
setupObj.os_initdaemon = setupObj.detect_initd()
setupObj.templateRenderingDict['jetty_dist'] = max(glob.glob('/opt/jetty-*'))
for setup_key in setup_porperties:
    val = setup_porperties[setup_key]
    if isinstance(val, bytes):
        val = val.decode()
    setattr(setupObj, setup_key, val)

if not setupObj.ip:
    setupObj.ip = setupObj.detect_ip()

setupObj.log = os.path.join(setupObj.install_dir, 'post_setup.log')
setupObj.logError = os.path.join(setupObj.install_dir, 'post_setup_error.log')

print("Log Files:", setupObj.log, setupObj.logError)

setupObj.ldapCertFn = setupObj.opendj_cert_fn

# Determine persistence type
gluu_cb_prop_fn = setupObj.gluuCouchebaseProperties
gluu_prop = read_properties_file(setupObj.gluu_properties_fn)
persistence_type = gluu_prop['persistence.type']
setupObj.persistence_type = persistence_type

if persistence_type == 'hybrid':
    hybrid_prop = read_properties_file(setupObj.gluu_hybrid_roperties)    
    persistence_type = hybrid_prop['storage.default']

if persistence_type == 'couchbase':
    setupObj.cbm = get_cbm_conn()
else:
    setupObj.createLdapPw()
    ldap_conn = get_ldap_conn()

if os.path.exists(gluu_cb_prop_fn):
    gluu_cb_prop = read_properties_file(gluu_cb_prop_fn)

def get_oxAuthConfiguration_ldap():

    ldap_conn.search(
                search_base='o=gluu', 
                search_scope=ldap3.SUBTREE,
                search_filter='(objectClass=oxAuthConfiguration)',
                attributes=["oxAuthConfDynamic"]
                )

    dn = ldap_conn.response[0]['dn']
    oxAuthConfDynamic = json.loads(ldap_conn.response[0]['attributes']['oxAuthConfDynamic'][0])

    return dn, oxAuthConfDynamic


def get_oxTrustConfiguration_ldap():
    ldap_conn.search(
                search_base='o=gluu',
                search_scope=ldap3.SUBTREE,
                search_filter='(objectClass=oxTrustConfiguration)',
                attributes=['oxTrustConfApplication']
                )
    dn = ldap_conn.response[0]['dn']
    oxTrustConfApplication = json.loads(ldap_conn.response[0]['attributes']['oxTrustConfApplication'][0])

    return dn, oxTrustConfApplication

def installSaml():

    if os.path.exists('/opt/shibboleth-idp'):
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

        ldap_conn.modify( 
                        dn,
                        {"oxTrustConfApplication": [ldap3.MODIFY_REPLACE, oxTrustConfApplication_js]}
                        )

        ldap_conn.modify(
                        'ou=configuration,o=gluu',
                        {"gluuSamlEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                        )

    else:
        bucket = gluu_cb_prop['bucket.default']
        
        n1ql = 'UPDATE `{}` USE KEYS "configuration_oxtrust" SET configGeneration=true'.format(bucket)
        setupObj.cbm.exec_query(n1ql)
        
        n1ql = 'UPDATE `{}` USE KEYS "configuration" SET gluuSamlEnabled=true'.format(bucket)
        setupObj.cbm.exec_query(n1ql)

    print("Shibboleth installation done")



def add2strlist(client_id, strlist):
    value2 = []
    for v in strlist.split(','):
        if v.strip() and v.strip() != 'None':
            value2.append(v.strip())
    value2.append(client_id)

    return  ','.join(value2)

def add_client2script(script_inum, client_id):
    dn = 'inum={},ou=scripts,o=gluu'.format(script_inum)

    if persistence_type == 'ldap':
        ldap_conn.search(search_base=dn, search_filter='(objectClass=*)', search_scope=ldap3.BASE, attributes=['oxConfigurationProperty'])
        
        for e in ldap_conn.response[0]['attributes'].get('oxConfigurationProperty', []):
            try:
                oxConfigurationProperty = json.loads(e)
            except:
                continue
            if isinstance(oxConfigurationProperty, dict) and oxConfigurationProperty.get('value1') == 'allowed_clients':
                if not client_id in oxConfigurationProperty['value2']:
                    oxConfigurationProperty['value2'] = add2strlist(client_id, oxConfigurationProperty['value2'])
                    oxConfigurationProperty_js = json.dumps(oxConfigurationProperty)
                    ldap_conn.modify(
                        dn,
                        {'oxConfigurationProperty': [ldap3.MODIFY_DELETE, e]}
                        )
                    ldap_conn.modify(
                        dn,
                        {'oxConfigurationProperty': [ldap3.MODIFY_ADD, oxConfigurationProperty_js]}
                        )
                    break

    else:
        n1ql = 'SELECT oxConfigurationProperty FROM `gluu` USE KEYS "scripts_{}"'.format(script_inum)
        result = setupObj.cbm.exec_query(n1ql)
        js = result.json()

        oxConfigurationProperties = js['results'][0]['oxConfigurationProperty']
        for i, oxconfigprop_str in enumerate(oxConfigurationProperties):
            oxconfigprop = json.loads(oxconfigprop_str)
            if oxconfigprop.get('value1') == 'allowed_clients' and not client_id in oxconfigprop['value2']:
                oxconfigprop['value2'] = self.add2strlist(client_id, oxconfigprop['value2'])
                oxConfigurationProperties[i] = json.dumps(oxconfigprop)
                break
        else:
            return

        n1ql = 'UPDATE `gluu` USE KEYS "scripts_{}" SET `oxConfigurationProperty`={}'.format(script_inum, json.dumps(oxConfigurationProperties))
        setupObj.cbm.exec_query(n1ql)   


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
        
        ldap_conn.modify(
                dn,
                {"oxTrustConfApplication": [ldap3.MODIFY_REPLACE, oxTrustConfApplication_js]}
                )

        ldap_conn.modify(
                'ou=configuration,o=gluu',
                {"gluuPassportEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                )

        
        for scr in scripts_enable:
            ldap_conn.modify(
                    'inum={},ou=scripts,o=gluu'.format(scr),
                    {"oxEnabled": [ldap3.MODIFY_REPLACE, 'true']}
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

    setupObj.run([setupObj.cmd_chown, 'root:gluu', setupObj.passport_rs_client_jks_fn])
    
    add_client2script('2DAF-F9A5', setupObj.passport_rp_client_id)
    add_client2script('2DAF-F995', setupObj.passport_rp_client_id)
    
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
    service_url = 'https://raw.githubusercontent.com/GluuFederation/community-edition-package/master/package/systemd/oxd-server.service'.format(gluu_version, service_file)
    setupObj.run(['wget', '-nv', service_url, '-O', os.path.join(oxd_tmp_dir, service_file)])

    oxd_server_sh_url = 'https://raw.githubusercontent.com/GluuFederation/oxd/version_{}/debian/oxd-server'.format(gluu_version)
    setupObj.run(['wget', '-nv', oxd_server_sh_url, '-O', os.path.join(oxd_tmp_dir, 'bin/oxd-server')])

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
    
    setupObj.templateRenderingDict['oxd_hostname'], setupObj.templateRenderingDict['oxd_port'] = setupObj.parse_url(setupObj.oxd_server_https)
    
    
    setupObj.renderTemplateInOut(
                    os.path.join(ces_dir, 'templates', casa_script_fn),
                    os.path.join(ces_dir, 'templates'),
                    os.path.join(ces_dir, 'output'),
                    )

    casa_ldif_fn = os.path.basename(setupObj.ldif_casa)
    casa_ldif_fp = os.path.join(ces_dir, 'output', casa_ldif_fn)

    setupObj.renderTemplateInOut(
                    os.path.join(ces_dir, 'templates', casa_ldif_fp),
                    os.path.join(ces_dir, 'templates'),
                    os.path.join(ces_dir, 'output'),
                    )


    if persistence_type == 'ldap':
        setupObj.import_ldif_template_opendj(casa_script_fp)
        setupObj.import_ldif_template_opendj(casa_ldif_fp)
        
    else:
        setupObj.import_ldif_couchebase(ldif_file_list=[casa_script_fp, casa_ldif_fp], bucket='gluu')

    if setupObj.installOxd:
        installOxd()
        setupObj.run_service_command('oxd-server', 'restart')

    setupObj.import_oxd_certificate()

    setupObj.calculate_selected_aplications_memory()
    setupObj.install_casa()


def installRadius():

    tmp_dir = os.path.join(setupObj.staticFolder, 'radius', 'templates')
    radius_ldif_fp = os.path.join(tmp_dir, 'gluu_radius_server.ldif')

    setupObj.renderTemplateInOut(
                    os.path.join(radius_ldif_fp),
                    tmp_dir,
                    os.path.join(ces_dir, 'output'),
                    )

    setupObj.install_gluu_radius()

    setupObj.run([setupObj.cmd_chown, 'radius:gluu', os.path.join(setupObj.certFolder, 'gluu-radius.private-key.pem')])
    setupObj.run([setupObj.cmd_chmod, '660', os.path.join(setupObj.certFolder, 'gluu-radius.private-key.pem')])

    dn, oxAuthConfiguration = get_oxAuthConfiguration_ldap()

    oxAuthConfiguration['openidScopeBackwardCompatibility'] = True
    oxAuthConfiguration['legacyIdTokenClaims'] = True
    oxAuthConfiguration_js = json.dumps(oxAuthConfiguration, indent=2)

    ldap_conn.modify(
            dn,
            {"oxAuthConfDynamic": [ldap3.MODIFY_REPLACE, oxAuthConfiguration_js]}
            )

    ldap_conn.modify(
            'ou=configuration,o=gluu',
            {"gluuRadiusEnabled": [ldap3.MODIFY_REPLACE, 'true']}
            )

    ldap_conn.modify(
            'inum=B8FD-4C11,ou=scripts,o=gluu',
            {"oxEnabled": [ldap3.MODIFY_REPLACE, 'true']}
            )


def installScim():
    if os.path.exists(os.path.join(setupObj.jetty_base, 'scim')):
        print("Scim Server is already installed on this system")
        return

    setupObj.installScimServer = True
    setupObj.calculate_selected_aplications_memory()

    print("Installing Scim Server")
    setupObj.install_scim_server()
    
    setupObj.renderTemplateInOut(
                os.path.join(ces_dir, 'templates/jetty/scim'),
                os.path.join(ces_dir, 'templates/jetty'),
                setupObj.osDefault,
                )

def installFido():
    if os.path.exists(os.path.join(setupObj.jetty_base, 'fido2')):
        print("Fido2 Server is already installed on this system")
        return

    setupObj.installFido2 = True
    setupObj.calculate_selected_aplications_memory()

    print("Installing Fido2")
    setupObj.install_fido2()

    setupObj.renderTemplateInOut(
                os.path.join(ces_dir, 'templates/jetty/fido2'),
                os.path.join(ces_dir, 'templates/jetty'),
                setupObj.osDefault,
                )

if args.addshib:
    installSaml()

if args.addpassport:
    installPassport()

if args.addoxd:
    installOxd()

if args.addcasa:
    installCasa()

if args.addradius:
    installRadius()

if args.addscim:
    installScim()

if args.addfido2:
    installFido()

if persistence_type == 'ldap':
    setupObj.deleteLdapPw()

print("Please exit container and restart Gluu Server")
