# Gluu CE setup utilities

import os
import platform
import zipfile
import json
import datetime
import copy
import csv

from jproperties import Properties
from ldif3.ldif3 import LDIFParser
from attribute_data_types import ATTRUBUTEDATATYPES
from ldap3.utils import dn as dnutils

cur_dir = os.path.dirname(os.path.realpath(__file__))
ces_dir = os.path.split(cur_dir)[0]

attribDataTypes = ATTRUBUTEDATATYPES()

listAttrib = ['member']

supportes_os_types = {
                'centos': ['7', '8'],
                'red': ['7', '8'], 
                'fedora': [], 
                'ubuntu': ['18', '20'],
                'debian': ['9', '10']
                }

class colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    DANGER = '\033[31m'

class myLdifParser(LDIFParser):
    def __init__(self, ldif_file):
        self.ldif_file = ldif_file
        self.entries = []

    def parse(self):
        with open(self.ldif_file, 'rb') as f:
            parser = LDIFParser(f)
            for dn, entry in parser.parse():
                for e in entry:
                    for i, v in enumerate(entry[e][:]):
                        if isinstance(v, bytes):
                            entry[e][i] = v.decode('utf-8')
                self.entries.append((dn, entry))


def prepare_multivalued_list():
    gluu_schema_fn = os.path.join(ces_dir, 'schema/gluu_schema.json')
    gluu_schema = json.load(open(gluu_schema_fn))

    for obj_type in ['objectClasses', 'attributeTypes']:
        for obj in gluu_schema[obj_type]:
            if obj.get('multivalued'):
                for name in obj['names']:
                    listAttrib.append(name)


def get_os_type():
    os_type, os_version = '', ''
    with open("/etc/os-release") as f:
        reader = csv.reader(f, delimiter="=")
        for row in reader:
            if row:
                if row[0] == 'ID':
                    os_type = row[1].lower()
                    if os_type == 'rhel':
                        os_type = 'redhat'
                elif row[0] == 'VERSION_ID':
                    os_version = row[1].split('.')[0]
    return os_type, os_version

def read_properties_file(fn):
    retDict = {}
    p = Properties()
    if os.path.exists(fn):
        with open(fn, 'rb') as f:
            p.load(f, 'utf-8')

        for k in p.keys():
            retDict[str(k)] = str(p[k].data)
            
    return retDict

def get_key_shortcuter_rules():
    ox_auth_war_file = '/opt/dist/gluu/oxauth.war'
    oxauth_zf = zipfile.ZipFile(ox_auth_war_file)

    for file_info in oxauth_zf.infolist():
        if 'oxcore-persistence-core' in file_info.filename:
            oxcore_persistence_core_path = file_info.filename
            break

    oxcore_persistence_core_content = oxauth_zf.read(oxcore_persistence_core_path)
    oxcore_persistence_core_io = io.StringIO(oxcore_persistence_core_content)
    oxcore_persistence_core_zf = zipfile.ZipFile(oxcore_persistence_core_io)
    key_shortcuter_rules_str = oxcore_persistence_core_zf.read('key-shortcuter-rules.json')
    key_shortcuter_rules = json.loads(key_shortcuter_rules_str)

    return key_shortcuter_rules

def get_mapped_entry(entry):
    rEntry = copy.deepcopy(entry)
    
    for key in list(rEntry.keys()):
        mapped_key = key
        if key in key_shortcuter_rules['exclusions']:
            mapped_key = key_shortcuter_rules['exclusions'][key]
        else:
            for map_key in key_shortcuter_rules['replaces']:
                if map_key in mapped_key:
                    mapped_key = mapped_key.replace(map_key, key_shortcuter_rules['replaces'][map_key])
                
        if mapped_key != key:
            mapped_key = mapped_key[0].lower() + mapped_key[1:]
            rEntry[mapped_key] = rEntry.pop(key)

    for key in list(rEntry.keys()):
        if key in key_shortcuter_rules['exclusions']:
            continue
        for prefix in key_shortcuter_rules['prefixes']:
            if key.startswith(prefix):
                mapped_key = key.replace(prefix, '',1)
                mapped_key = mapped_key[0].lower() + mapped_key[1:]
                rEntry[mapped_key] = rEntry.pop(key)
                break


    return rEntry

def getTypedValue(dtype, val):
    retVal = val
    
    if dtype == 'json':
        try:
            retVal = json.loads(val)
        except Exception as e:
            pass

    if dtype == 'integer':
        try:
            retVal = int(retVal)
        except:
            pass
    elif dtype == 'datetime':
        if '.' in val:
            date_format = '%Y%m%d%H%M%S.%fZ'
        else:
            date_format = '%Y%m%d%H%M%SZ'
        
        if not val.lower().endswith('z'):
            val += 'Z'

        dt = datetime.datetime.strptime(val, date_format)
        retVal = dt.isoformat()

    elif dtype == 'boolean':
        if retVal.lower() in ('true', 'yes', '1', 'on'):
            retVal = True
        else:
            retVal = False

    return retVal


def get_key_from(dn):
    dns = []
    for rd in dnutils.parse_dn(dn):

        if rd[0] == 'o' and rd[1] == 'gluu':
            continue
        dns.append(rd[1])

    dns.reverse()
    key = '_'.join(dns)

    if not key:
        key = '_'

    return key


def get_documents_from_ldif(ldif_file):
    parser = myLdifParser(ldif_file)
    parser.parse()
    documents = []

    if not hasattr(attribDataTypes, 'attribTypes'):
        attribDataTypes.startup(ces_dir)
        prepare_multivalued_list()

    for dn, entry in parser.entries:
        if len(entry) > 2:
            key = get_key_from(dn)
            entry['dn'] = dn
            for k in copy.deepcopy(entry):
                if len(entry[k]) == 1:
                    if not k in listAttrib:
                        entry[k] = entry[k][0]

            for k in entry:
                dtype = attribDataTypes.getAttribDataType(k)
                if dtype != 'string':
                    if type(entry[k]) == type([]):
                        for i in range(len(entry[k])):
                            entry[k][i] = getTypedValue(dtype, entry[k][i])
                            if entry[k][i] == 'true':
                                entry[k][i] = True
                            elif entry[k][i] == 'false':
                                entry[k][i] = False
                    else:
                        entry[k] = getTypedValue(dtype, entry[k])

                if k == 'objectClass':
                    entry[k].remove('top')
                    oc_list = entry[k]

                    for oc in oc_list[:]:
                        if 'Custom' in oc and len(oc_list) > 1:
                            oc_list.remove(oc)

                        if not 'gluu' in oc.lower() and len(oc_list) > 1:
                            oc_list.remove(oc)

                    entry[k] = oc_list[0]

            #mapped_entry = get_mapped_entry(entry)
            documents.append((key, entry))

    return documents


