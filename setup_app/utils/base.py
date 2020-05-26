# Gluu CE setup base utilities

import os
import sys
import time
import platform
import zipfile
import json
import datetime
import copy
import subprocess
import traceback
import re
import shutil
import multiprocessing

from setup_app import paths
from setup_app import static
from setup_app.pylib.jproperties import Properties
from setup_app.pylib.ldif3.ldif3 import LDIFParser
from setup_app.utils.attribute_data_types import ATTRUBUTEDATATYPES

# Note!!! This module should be imported after paths

cur_dir = os.path.dirname(os.path.realpath(__file__))
ces_dir = os.path.split(cur_dir)[0]

# Determine initdaemon
with open('/proc/1/status', 'r') as f:
    os_initdaemon = f.read().split()[1]

# Determine os_type and os_version
p = platform.linux_distribution()
os_type = p[0].split()[0].lower()
os_version = p[1].split('.')[0]
os_name = os_type + os_version

if os_type == 'debian':
    os.environ['LC_ALL'] = 'C'

# Determine service path
if (os_type in ('centos', 'red', 'fedora') and os_initdaemon == 'systemd') or (os_type + os_version in ('ubuntu18','debian9','debian10')):
    service_path = shutil.which('systemctl')
elif self.os_type in ['debian', 'ubuntu']:
    service_path = '/usr/sbin/service'
else:
    service_path = '/sbin/service'

if os_type in ('centos', 'red', 'fedora'):
    clone_type = 'rpm'
    httpd_name = 'httpd'
else:
    clone_type = 'deb'
    httpd_name = 'apache2'

# resources
file_max = int(open("/proc/sys/fs/file-max").read().strip())
current_mem_bytes = os.sysconf('SC_PAGE_SIZE') * os.sysconf('SC_PHYS_PAGES')
current_mem_size = round(current_mem_bytes / (1024.**3), 1) #in GB
current_number_of_cpu = multiprocessing.cpu_count()
disk_st = os.statvfs('/')
available_disk_space = disk_st.f_bavail * disk_st.f_frsize / (1024 * 1024 *1024)

def check_resources():

    if file_max < static.file_max:
        print(("{0}Maximum number of files that can be opened on this computer is "
                  "less than 64000. Please increase number of file-max on the "
                  "host system and re-run setup.py{1}".format(static.colors.DANGER,
                                                                static.colors.ENDC)))
        sys.exit(1)

    if current_mem_size < static.suggested_mem_size:
        print(("{0}Warning: RAM size was determined to be {1:0.1f} GB. This is less "
               "than the suggested RAM size of {2} GB.{3}").format(static.colors.WARNING,
                                                        current_mem_size, 
                                                        static.suggested_mem_size,
                                                        static.colors.ENDC))


        result = input("Proceed anyways? [Y|n] ")
        if result and result[0].lower() == 'n':
            sys.exit()

    if current_number_of_cpu < static.suggested_number_of_cpu:

        print(("{0}Warning: Available CPU Units found was {1}. "
            "This is less than the required amount of {2} CPU Units.{3}".format(
                                                        static.colors.WARNING,
                                                        current_number_of_cpu, 
                                                        static.suggested_number_of_cpu,
                                                        static.colors.ENDC)))

        result = input("Proceed anyways? [Y|n] ")
        if result and result[0].lower() == 'n':
            sys.exit()



    if available_disk_space < static.suggested_free_disk_space:
        print(("{0}Warning: Available free disk space was determined to be {1:0.1f} "
            "GB. This is less than the required disk space of {2} GB.{3}".format(
                                                        static.colors.WARNING,
                                                        available_disk_space,
                                                        static.suggested_free_disk_space,
                                                        static.colors.ENDC)))

        result = input("Proceed anyways? [Y|n] ")
        if result and result[0].lower() == 'n':
            sys.exit()






attribDataTypes = ATTRUBUTEDATATYPES()

listAttrib = ['member']



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

def determineApacheVersion():
    httpd_cmd = shutil.which(httpd_name)
    cmd = "/usr/sbin/%s -v | egrep '^Server version'" % httpd_name
    output = run(cmd, shell=True)
    apache_version_re = re.search('Apache/(\d).(\d).(\d)', output.strip())
    if apache_version_re:
        (major, minor, pathc) =  apache_version_re.groups()
        return '.'.join((major, minor))

def get_os_package_list():
    package_list_fn = os.path.join(paths.DATA_DIR, 'package_list.json')
    with open(package_list_fn) as f:
        packages = json.load(f)
        return packages

def check_os_supported():
    return os_type + ' '+ os_version in get_os_package_list()

def prepare_multivalued_list():
    gluu_schema_fn = os.path.join(ces_dir, 'schema/gluu_schema.json')
    gluu_schema = json.load(open(gluu_schema_fn))

    for obj_type in ['objectClasses', 'attributeTypes']:
        for obj in gluu_schema[obj_type]:
            if obj.get('multivalued'):
                for name in obj['names']:
                    listAttrib.append(name)


def logIt(msg, errorLog=False, fatal=False):
    log_fn = paths.LOG_ERROR_FILE if errorLog else paths.LOG_FILE
    with open(log_fn, 'a') as w:
        w.write('{} {}\n'.format(time.strftime('%X %x'), msg))

    if fatal:
        print("FATAL:", errorLog)
        sys.exit(1)

def logOSChanges(text):
    with open(paths.LOG_OS_CHANGES_FILE, 'a') as w:
        w.write(text+"\n")

def read_properties_file(fn):
    retDict = {}
    p = Properties()
    if os.path.exists(fn):
        with open(fn, 'rb') as f:
            p.load(f, 'utf-8')

        for k in p.keys():
            retDict[str(k)] = str(p[k].data)
            
    return retDict

def get_clean_args(args):
    argsc = args[:]

    for a in ('-R', '-h', '-p'):
        if a in argsc:
            argsc.remove(a)

    if '-m' in argsc:
        m = argsc.index('-m')
        argsc.pop(m)
        
    return argsc
        
# args = command + args, i.e. ['ls', '-ltr']
def run(args, cwd=None, env=None, useWait=False, shell=False, get_stderr=False):
    output = ''
    log_arg = ' '.join(args) if type(args) is list else args
    logIt('Running: %s' % log_arg)
    
    if args[0] == paths.cmd_chown:
        argsc = get_clean_args(args)
        if not argsc[2].startswith('/opt'):
            logOSChanges('Making owner of %s to %s' % (', '.join(argsc[2:]), argsc[1]))
    elif args[0] == paths.cmd_chmod:
        argsc = get_clean_args(args)
        if not argsc[2].startswith('/opt'):
            logOSChanges('Setting permission of %s to %s' % (', '.join(argsc[2:]), argsc[1]))
    elif args[0] == paths.cmd_chgrp:
        argsc = get_clean_args(args)
        if not argsc[2].startswith('/opt'):
            logOSChanges('Making group of %s to %s' % (', '.join(argsc[2:]), argsc[1]))
    elif args[0] == paths.cmd_mkdir:
        argsc = get_clean_args(args)
        if not (argsc[1].startswith('/opt') or argsc[1].startswith('.')):
            logOSChanges('Creating directory %s' % (', '.join(argsc[1:])))

    try:
        p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd, env=env, shell=shell)
        if useWait:
            code = p.wait()
            logIt('Run: %s with result code: %d' % (' '.join(args), code) )
        else:
            output, err = p.communicate()
            output = output.decode('utf-8')
            err = err.decode('utf-8')

            if output:
                logIt(output)
            if err:
                logIt(err, True)
    except:
        logIt("Error running command : %s" % " ".join(args), True)
        logIt(traceback.format_exc(), True)

    if get_stderr:
        return output, err

    return output



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


    
