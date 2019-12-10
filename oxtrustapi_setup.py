# Please place this script in the same directory as setup.py

import os
import ldap
import xml.etree.ElementTree as ET
import shutil
import glob
from setup import Setup
from pylib.cbm import CBM

ldap.set_option(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_ALLOW)

scripts_location = 'ldap'

setupObject = Setup(os.path.dirname(os.path.realpath(__file__)))
setupObject.os_initdaemon = setupObject.detect_initd()
setupObject.os_type, setupObject.os_version = setupObject.detect_os_type()

if os.path.exists(setupObject.gluu_hybrid_roperties):
    for l in open(setupObject.gluu_hybrid_roperties):
        ls = l.strip()
        if ls.startswith('storage.default'):
            n = ls.find(':')
            scripts_location = ls[n+1:].strip()
elif os.path.exists(setupObject.gluuCouchebaseProperties):
    scripts_location = 'couchbase'

identtiy_xml_fn = '/opt/gluu/jetty/identity/webapps/identity.xml'

tree = ET.parse(identtiy_xml_fn)
root = tree.getroot()

oxtrust_api_server_version = '4.0.rc2'
oxtrust_api_server_url = 'https://ox.gluu.org/maven/org/gluu/oxtrust-api-server/{0}/oxtrust-api-server-{0}.jar'.format(oxtrust_api_server_version)
oxtrust_api_server_path = '/opt/gluu/jetty/identity/custom/libs/oxtrust-api-server.jar'

print "Downloading oxtrust-api-server-{}.jar".format(oxtrust_api_server_version)
os.system('wget -nv {} -O {}'.format(oxtrust_api_server_url, oxtrust_api_server_path))
os.system('chown jetty:jetty {}'.format(oxtrust_api_server_path))

for child in root:
    if child.attrib.get('name') == 'extraClasspath':
        if 'oxtrust-api-server.jar' in child.text:
            break
else:
    print "Adding oxtrust-api-server-{}.jar to identity extraClasspath".format(oxtrust_api_server_version)
    
    with open(identtiy_xml_fn) as f:
        identtiy_xml = f.readlines()

    n = None
    for i, l in enumerate(identtiy_xml):
        if l.strip() == '</Configure>':
            n = i -1
            break

    if n and n > 0:
        identtiy_xml.insert(n, '<Set name="extraClasspath">{}</Set>\n'.format(oxtrust_api_server_path))

        with open(identtiy_xml_fn+'~','w') as w:
            w.write(''.join(identtiy_xml))

        backups = glob.glob(identtiy_xml_fn+'.bak.*')
        bn = len(backups)+1
        shutil.copyfile(identtiy_xml_fn, identtiy_xml_fn+'.bak.'+str(bn))
        shutil.copyfile(identtiy_xml_fn+'~', identtiy_xml_fn)
        os.remove(identtiy_xml_fn+'~')

if scripts_location == 'ldap':
    # Obtain ldap binddn, server and password
    for l in open(setupObject.ox_ldap_properties):
        if l.startswith('bindPassword'):
            crypted_passwd = l.split(':')[1].strip()
            ldap_password = os.popen('/opt/gluu/bin/encode.py -D {}'.format(crypted_passwd)).read().strip()
        elif l.startswith('servers'):
            ls = l.strip()
            n = ls.find(':')
            s = ls[n+1:].strip()
            servers_s = s.split(',')
            ldap_server = servers_s[0].strip()
        elif l.startswith('bindDN'):
            ldap_binddn = l.split(':')[1].strip()

    # Enable custom script oxtrust_api_access_policy
    ldap_conn = ldap.initialize('ldaps://{}'.format(ldap_server))
    ldap_conn.simple_bind_s(ldap_binddn, ldap_password)

    basedn = 'inum=OO11-BAFE,ou=scripts,o=gluu'
    result = ldap_conn.search_s(basedn, ldap.SCOPE_BASE,  attrlist=['oxEnabled'])

    if result and result[0][1]['oxEnabled'][0].lower() != 'true':
        print "Enabling custom script oxtrust_api_access_policy"
        ldap_conn.modify_s(basedn, [(ldap.MOD_REPLACE, 'oxEnabled',  'true')])

else:
    # Obtain couchbase credidentals
    for l in open(setupObject.gluuCouchebaseProperties):
        ls = l.strip()
        n = ls.find(':')
        if ls.startswith('servers'):
            server = ls[n+1:].strip().split(',')[0].strip()
        elif ls.startswith('auth.userName'):
            userName = ls[n+1:].strip()
        elif ls.startswith('auth.userPassword'):
            userPasswordEnc = ls[n+1:].strip()
            userPassword = os.popen('/opt/gluu/bin/encode.py -D {}'.format(userPasswordEnc)).read().strip()
    
    cbm = CBM(server, userName, userPassword)
    result = cbm.exec_query('UPDATE `gluu` USE KEYS "scripts_OO11-BAFE" SET `oxEnabled`=true')

print "Restarting identity, this will take a while"
setupObject.run_service_command('identity', 'restart')
