import os
import xml.etree.ElementTree as ET
import shutil
import glob

identtiy_xml_fn = '/opt/gluu/jetty/identity/webapps/identity.xml'

tree = ET.parse(identtiy_xml_fn)
root = tree.getroot()

oxtrust_api_server_version = '4.0.rc2'
oxtrust_api_server_url = 'https://ox.gluu.org/maven/org/gluu/oxtrust-api-server/{0}/oxtrust-api-server-{0}.jar'.format(oxtrust_api_server_version)
oxtrust_api_server_path = '/opt/gluu/jetty/identity/custom/libs/oxtrust-api-server.jar'


os.system('wget {} -O {}'.format(oxtrust_api_server_url, oxtrust_api_server_path))
os.system('chown jetty:jetty {}'.format(oxtrust_api_server_path))

for child in root:
    if child.attrib.get('name') == 'extraClasspath':
        if 'oxtrust-api-server.jar' in child.text:
            break
else:
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

#TODO: 
# 1) enable oxtrust_api_access_policy
# 2) restart identity
