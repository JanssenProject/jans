#!/usr/bin/python3

# Please place this script in the same directory as setup.py

import os
import xml.etree.ElementTree as ET
import shutil
import glob
import ldap3

from setup import Setup
from pylib.dbutils import get_ldap_conn, get_cbm_conn

scripts_location = 'ldap'

setupObject = Setup(os.path.dirname(os.path.realpath(__file__)))
setupObject.os_initdaemon = setupObject.detect_initd()
setupObject.os_type, setupObject.os_version = setupObject.detect_os_type()

if os.path.exists(setupObject.jans_hybrid_roperties):
    for l in open(setupObject.jans_hybrid_roperties):
        ls = l.strip()
        if ls.startswith('storage.default'):
            n = ls.find(':')
            scripts_location = ls[n+1:].strip()
elif os.path.exists(setupObject.jansCouchebaseProperties):
    scripts_location = 'couchbase'

identtiy_xml_fn = '/opt/jans/jetty/identity/webapps/identity.xml'

tree = ET.parse(identtiy_xml_fn)
root = tree.getroot()

oxtrust_api_server_version = '5.0.0-SNAPSHOT'
oxtrust_api_server_url = 'https://ox.gluu.org/maven/org/gluu/oxtrust-api-server/{0}/oxtrust-api-server-{0}.jar'.format(oxtrust_api_server_version)
oxtrust_api_server_path = '/opt/jans/jetty/identity/custom/libs/oxtrust-api-server.jar'

print("Downloading oxtrust-api-server-{}.jar".format(oxtrust_api_server_version))
os.system('wget -nv {} -O {}'.format(oxtrust_api_server_url, oxtrust_api_server_path))
os.system('chown jetty:jetty {}'.format(oxtrust_api_server_path))

for child in root:
    if child.attrib.get('name') == 'extraClasspath':
        if 'oxtrust-api-server.jar' in child.text:
            break
else:
    print("Adding oxtrust-api-server-{}.jar to identity extraClasspath".format(oxtrust_api_server_version))
    
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

print("Enabling custom script oxtrust_api_access_policy")

if scripts_location == 'ldap':
    ldap_conn = get_ldap_conn()

    ldap_conn.search(
                    search_base='inum=OO11-BAFE,ou=scripts,o=jans', 
                    search_scope=ldap3.BASE,
                    search_filter='(objectclass=*)',
                    attributes=["jansEnabled"]
                    )

    if ldap_conn.response:
        ldap_conn.modify(
                        ldap_conn.response[0]['dn'], 
                        {"jansEnabled": [ldap3.MODIFY_REPLACE, 'true']}
                        )

else:
    cbm = get_cbm_conn()
    result = cbm.exec_query('UPDATE `jans` USE KEYS "scripts_OO11-BAFE" SET `jansEnabled`=true')

print("Restarting identity, this will take a while")
setupObject.run_service_command('identity', 'restart')
