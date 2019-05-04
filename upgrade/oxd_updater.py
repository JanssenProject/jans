#!/usr/bin/python

import os
import sys
import time
import glob
import shutil
import json
import xml.etree.ElementTree as ET
import string
import re
import platform
import time

os_commands = {

    'debian': {

            '9': [
                'echo "deb https://repo.gluu.org/debian/ stretch-stable main" > /etc/apt/sources.list.d/gluu-repo.list',
                ],

            '8': [
                'echo "deb https://repo.gluu.org/debian/ stable main" > /etc/apt/sources.list.d/gluu-repo.list',
                ],

        },

    'ubuntu': {
            '16': [
                'echo "deb https://repo.gluu.org/ubuntu/ xenial-devel main" > /etc/apt/sources.list.d/gluu-repo.list',
                ],
            '14': [
                'echo "deb https://repo.gluu.org/ubuntu/ trusty-devel main" > /etc/apt/sources.list.d/gluu-repo-devel.list'
                ],
            },

    'centos': {
            '6': [
                'wget https://repo.gluu.org/centos/Gluu-centos-testing.repo -O /etc/yum.repos.d/Gluu.repo'
                #change this
                ],

            '7': [
                 'wget https://repo.gluu.org/centos/Gluu-centos-7-testing.repo -O /etc/yum.repos.d/Gluu.repo' #testing
                 #'wget https://repo.gluu.org/centos/Gluu-centos7.repo -O /etc/yum.repos.d/Gluu.repo'
                ],
            },
    'red': {
            '6': [
                'wget https://repo.gluu.org/rhel/Gluu-rhel6.repo -O /etc/yum.repos.d/Gluu.repo'
                ],
            '7': [
                'wget https://repo.gluu.org/rhel/Gluu-rhel7.repo -O /etc/yum.repos.d/Gluu.repo'
                ],
        },

    }


def detect_os_type():
    try:
        p = platform.linux_distribution()
        os_type = p[0].split()[0].lower()
        os_version = p[1].split('.')[0]
        return os_type, os_version
    except:
        sys.exit('OS type could not be determined, exiting.')

os_type = detect_os_type()

print "Detected OS Type", " ".join(os_type)

try:
    commands = os_commands[os_type[0]][os_type[1]]
except:
    sys.exit('Unsupported Operating System, exiting.')


if os_type[0] in ('ubuntu', 'debian'):
    package_type = 'deb'
    install_command = 'apt-get install -y {0}'
elif os_type[0] in ('centos', 'red'):
    package_type = 'rpm'
    install_command = 'yum install -y {0}'

detected_oxd = 'oxd-server'


stop_gluu_command = {
        'ubuntu16': 'service {0} stop',
        'ubuntu14': 'service {0} stop',
        'centos6': 'service {0} stop',
        'centos7': 'service {0} stop',
    }


os_type_str = ''.join(os_type)

commands.insert(0, stop_gluu_command[os_type_str].format(detected_oxd))


if package_type == 'deb':
    commands += [
            'curl https://repo.gluu.org/debian/gluu-apt.key | apt-key add -',
            'apt-get update',
            'apt-get install -y oxd-server-4.0',
        ]

elif package_type == 'rpm':
    commands += [
            'yum install -y epel-release',
            'wget https://repo.gluu.org/rhel/RPM-GPG-KEY-GLUU -O /etc/pki/rpm-gpg/RPM-GPG-KEY-GLUU',
            'rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-GLUU',
            'yum clean all',
            'yum install -y oxd-server-4.0',
        ]

add_commands = []

try:
    import pip
except:
    
    add_commands.append(install_command.format('python-pip'))
try:
    import yaml
except:
    add_commands.append(install_command.format('python-yaml'))

if add_commands:
    commands += add_commands

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
        tmp_ = {}
        for key, value in data.iteritems():
            tmp_[_byteify(key, ignore_dicts=True)] = _byteify(value, ignore_dicts=True)

        return tmp_

    # if it's anything else, return it in its original form
    return data

current_version = '4.0'
oxd_base_dir = '/opt/oxd-server'
oxd_data_dir = os.path.join(oxd_base_dir, 'conf')
oxd_conf_dir = '/etc/oxd/oxd-server' if os.path.exists('/etc/oxd/oxd-server') else '/opt/oxd-server/conf'

oxd_backup_dir = os.path.join('/var/oxd-backup')
oxd_data_backup_dir = os.path.join(oxd_backup_dir, 'json_data_backup')

if not os.path.exists(oxd_backup_dir):
    os.mkdir(oxd_backup_dir)

if not os.path.exists(oxd_data_backup_dir):
    os.mkdir(oxd_data_backup_dir)

oxd_conf_json_fn = os.path.join(oxd_conf_dir, 'oxd-conf.json')
oxd_default_site_config_json_fn = os.path.join(oxd_conf_dir, 'oxd-default-site-config.json')
log4j_xml_fn = os.path.join(oxd_conf_dir, 'log4j.xml')

conf_yaml_template = 'oxd-server.yml.temp'

oxd4_server_yaml_fn = os.path.join(oxd_conf_dir, 'oxd-server.yml')


update_required = False

if os.path.exists(oxd_conf_json_fn):
    update_required = True

if os.path.exists(oxd_default_site_config_json_fn):
    update_required = True



if update_required:

    print """A previous version of oxd-server detected. If you continue,
the current version will be replaced by the latest version available,
and your config/data will be migrated to the new version.
"""
    ask = "Do you want to migrate data to oxd-server-{0}? [y|N]: ".format(current_version)

    answer = raw_input(ask)
    if not answer or answer.lower()[0] != 'y':
        sys.exit("Migration cancelled, exiting.")

    if os_type[0] in ('ubuntu','debian'):
        commands.insert(1,'apt-get purge -y oxd-server')
    elif os_type[0] in ('centos','red'):
        commands.insert(1,'yum remove -y oxd-server')

if update_required:

    oxd_conf_json = json_load_byteified(oxd_conf_json_fn)

    current_dbFileLocation = '/opt/oxd-server/data/oxd_db'
    
    if oxd_conf_json.get('storage_configuration') and oxd_conf_json['storage_configuration'].get('dbFileLocation'):
        current_dbFileLocation = oxd_conf_json['storage_configuration']['dbFileLocation']

    current_dbFile = current_dbFileLocation+'.mv.db'

    commands.append('wget https://raw.githubusercontent.com/GluuFederation/oxd/version_4.0/upgrade/oxd-server.yml.temp  -O oxd-server.yml.temp')


    for b_file in ( 
                    oxd_conf_json_fn,
                    oxd_default_site_config_json_fn,
                    log4j_xml_fn,
                    oxd4_server_yaml_fn,
                    current_dbFile,
                    ):

        if os.path.exists(b_file):
            shutil.copy2(b_file, oxd_backup_dir)

    json_files = glob.glob(os.path.join(oxd_conf_dir,'*.json'))

    json_files.remove(os.path.join(oxd_conf_dir,'oxd-conf.json'))
    json_files.remove(os.path.join(oxd_conf_dir,'oxd-default-site-config.json'))

    for json_file in json_files:
        shutil.move(json_file, oxd_data_backup_dir)


print "About to execute following commands:"
print '\n'.join(commands)

ask = "Do you want to continue [y|N]: "

answer = raw_input(ask)
if not answer or answer.lower()[0] != 'y':
    sys.exit("Migration cancelled, exiting.")

for cmd in commands:
    print "Executing", cmd
    os.system(cmd)

import yaml

if update_required:

    oxd_conf_json_back_fn = os.path.join(oxd_backup_dir, 'oxd-conf.json')
    oxd_default_site_config_json_back_fn = os.path.join(oxd_backup_dir, 'oxd-default-site-config.json')
    log4j_xml_back_fn = os.path.join(oxd_backup_dir, 'log4j.xml')

    oxd_default_site_config_json = json_load_byteified(oxd_default_site_config_json_back_fn)


    if not oxd_default_site_config_json['contacts']:
        oxd_default_site_config_json['contacts'] = []
    else:
        if not type(oxd_default_site_config_json['contacts']) == type([]):
            oxd_default_site_config_json['contacts'] = [oxd_default_site_config_json['contacts']]

    log4j_xml_tree = tree = ET.parse(log4j_xml_back_fn)
    log4j_xml_root = log4j_xml_tree.getroot()

    oxd4_server_yaml_fn = '/opt/oxd-server/conf/oxd-server.yml'

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
    org_xdi_attrib = get_by_attrib(categories,  'org.gluu', False)

    if not org_xdi_attrib:
        org_xdi_attrib = get_by_attrib(categories,  'org.xdi', False)


    org_xdi = org_xdi_attrib.find('priority').get('value')
    oxd4_server_yaml['logging']['loggers']['org.gluu'] = org_xdi

    root = log4j_xml_root.find('root')
    priority = root.find('priority').get('value')

    oxd4_server_yaml['logging']['level'] = priority
    oxd4_server_yaml['migration_source_folder_path'] = oxd_data_backup_dir

    yaml_temp = open(conf_yaml_template).read()

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
        
        if (type(m) != type([]) and not m):
            m="''"
        k = '{{'+sv+'}}'
        #print sv, m
        yaml_temp = yaml_temp.replace(k,str(m))


    with open(oxd4_server_yaml_fn,'w') as W:
        W.write(yaml_temp)


    db_fn_backup = os.path.join(oxd_backup_dir,'oxd_db.mv.db')

    if os.path.exists(db_fn_backup):
        shutil.copy2(db_fn_backup, '/opt/oxd-server/data/')

    os.system('chown jetty:jetty ' + oxd_backup_dir)

    print "Migration is finished. Please restart oxd-server"
