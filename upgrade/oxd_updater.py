import os
import time
import glob
import shutil
import json
import xml.etree.ElementTree as ET
import string
import yaml
import re


def get_by_attrib(elements, attrib, value=True):
    for element in elements:
        if element.get('name') == attrib:
            if value:
                return element.get('value')
            return element
    if value:
        return ''

def json_load_byteified(file_handle):
    return _byteify(
        json.load(open(file_handle), object_hook=_byteify),
        ignore_dicts=True
    )


def _byteify(data, ignore_dicts = False):
    # if this is a unicode string, return its string representation
    if isinstance(data, unicode):
        return data.encode('utf-8')
    # if this is a list of values, return list of byteified values
    if isinstance(data, list):
        return [ _byteify(item, ignore_dicts=True) for item in data ]
    # if this is a dictionary, return dictionary of byteified keys and values
    # but only if we haven't already byteified it
    if isinstance(data, dict) and not ignore_dicts:
        return {
            _byteify(key, ignore_dicts=True): _byteify(value, ignore_dicts=True)
            for key, value in data.iteritems()
        }
    # if it's anything else, return it in its original form
    return data


oxd_base_dir = '/opt/oxd-server'
oxd_data_dir = os.path.join(oxd_base_dir, 'conf')
oxd_conf_dir = '/etc/oxd/oxd-server' if os.path.exists('etc/oxd/oxd-server') else '/opt/oxd-server/conf'

oxd_data_backup_dir = os.path.join(oxd_base_dir, 'json_data_backup')

oxd_conf_json_fn = os.path.join(oxd_conf_dir, 'oxd-conf.json')
oxd_default_site_config_json_fn = os.path.join(oxd_conf_dir, 'oxd-default-site-config.json')
log4j_xml_fn = os.path.join(oxd_conf_dir, 'log4j.xml')



oxd4_server_yaml_fn = os.path.join(oxd_conf_dir, 'oxd-server.yml')

shutil.copyfile(oxd4_server_yaml_fn, oxd4_server_yaml_fn + '._backup_'+time.ctime().replace(' ','_'))


oxd_conf_json = json_load_byteified(oxd_conf_json_fn)
oxd_default_site_config_json = json_load_byteified(oxd_default_site_config_json_fn)

log4j_xml_tree = tree = ET.parse(log4j_xml_fn)
log4j_xml_root = log4j_xml_tree.getroot()

oxd4_server_yaml = yaml.safe_load(open(oxd4_server_yaml_fn).read())


for key in oxd_default_site_config_json:
    if key in oxd4_server_yaml['defaultSiteConfig']:
        oxd4_server_yaml['defaultSiteConfig'][key] = oxd_default_site_config_json[key]



for key in oxd_conf_json:
    if key in oxd4_server_yaml:
        oxd4_server_yaml[key] = oxd_conf_json[key]


xml_appenders = log4j_xml_root.findall('appender')



file_attrib = get_by_attrib(xml_appenders, 'FILE', False)

params =  file_attrib.findall('param')

currentLogFilename = get_by_attrib(params, 'File')
log_fp, log_e = os.path.splitext(currentLogFilename)

oxd4_server_yaml['logging']['appenders'][1]['currentLogFilename'] = currentLogFilename

DatePattern = log_file_attrib = get_by_attrib(params, 'DatePattern')

archivedLogFilenamePattern =  log_fp +'-%d{'+ DatePattern.replace("'.'",'') + '}-%i.log.gz'

oxd4_server_yaml['logging']['appenders'][1]['archivedLogFilenamePattern'] = archivedLogFilenamePattern

categories = log4j_xml_root.findall('category') 
org_xdi_attrib = get_by_attrib(categories,  'org.xdi', False)
org_xdi = org_xdi_attrib.find('priority').get('value')
oxd4_server_yaml['logging']['loggers']['org.xdi'] = org_xdi


root = log4j_xml_root.find('root')

priority = root.find('priority').get('value')

oxd4_server_yaml['logging']['level'] = priority


if not os.path.exists(oxd_data_backup_dir):
    os.mkdir(oxd_data_backup_dir)

json_files = glob.glob(os.path.join(oxd_data_dir,'*.json'))

json_files.remove(os.path.join(oxd_data_dir,'oxd-conf.json'))
json_files.remove(os.path.join(oxd_data_dir,'oxd-default-site-config.json'))

for json_file in json_files:
    shutil.move(json_file, oxd_data_backup_dir)

oxd4_server_yaml['migration_source_folder_path'] = oxd_data_backup_dir

yaml_temp = open('oxd-server.yml.temp').read()

sub_vars = re.findall('\{\{(.*?)\}\}', yaml_temp)

for sv in sub_vars:
    sv_pattern = sv.split(':')
    m = oxd4_server_yaml
    for p in sv_pattern:
        if '|' in p:
            p,n=p.split('|')
            m = m[p][int(n)]
        else:
            m = m[p]

    if type(True) == type(m):
        m = str(m).lower()
    if not m:
        m="''"
    k = '{{'+sv+'}}'
    #print sv, m
    yaml_temp = yaml_temp.replace(k,str(m))
    

with open(oxd4_server_yaml_fn,'w') as W:
    W.write(yaml_temp)

