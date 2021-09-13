# Jans CE setup base utilities

import os
import sys
import time
import glob
import csv
import zipfile
import json
import datetime
import copy
import subprocess
import traceback
import re
import shutil
import multiprocessing
import ssl

from pathlib import Path
from collections import OrderedDict
from urllib.request import urlretrieve

# disable ssl certificate check
ssl._create_default_https_context = ssl._create_unverified_context

from setup_app import paths
from setup_app import static
from setup_app.config import Config
from setup_app.pylib.jproperties import Properties

# Note!!! This module should be imported after paths

cur_dir = Path(__file__).parent.as_posix()
ces_dir = Path(__file__).parent.parent.as_posix()
par_dir = Path(__file__).parent.parent.parent.as_posix()

snap = os.environ.get('SNAP','')
snap_common = snap_common_dir = os.environ.get('SNAP_COMMON','')

re_split_host = re.compile(r'[^,\s,;]+')

# Determine initdaemon
with open('/proc/1/status', 'r') as f:
    os_initdaemon = f.read().split()[1]

# Determine os_type and os_version
os_type, os_version = '', ''

os_release_fn = '/usr/lib/os-release'
if not os.path.exists(os_release_fn):
    os_release_fn = '/etc/os-release'

with open(os_release_fn) as f:
    reader = csv.reader(f, delimiter="=")
    for row in reader:
        if row:
            if row[0] == 'ID':
                os_type = row[1].lower()
                if os_type in  ('rhel', 'redhat'):
                    os_type = 'red'
                elif 'ubuntu-core' in os_type:
                    os_type = 'ubuntu'
            elif row[0] == 'VERSION_ID':
                os_version = row[1].split('.')[0]

if not (os_type and os_version):
    print("Can't determine OS type and OS version")
    sys.exit()

os_name = os_type + os_version
deb_sysd_clone = os_name in ('ubuntu18', 'ubuntu20', 'debian9', 'debian10')

# Determine service path
if (os_type in ('centos', 'red', 'fedora') and os_initdaemon == 'systemd') or deb_sysd_clone:
    service_path = shutil.which('systemctl')
elif os_type in ['debian', 'ubuntu']:
    service_path = '/usr/sbin/service'
else:
    service_path = '/sbin/service'

if os_type in ('centos', 'red', 'fedora'):
    clone_type = 'rpm'
    httpd_name = 'httpd'
else:
    clone_type = 'deb'
    httpd_name = 'apache2'

if snap:
    snapctl = shutil.which('snapctl')

# resources
current_file_max = int(open("/proc/sys/fs/file-max").read().strip())
current_mem_bytes = os.sysconf('SC_PAGE_SIZE') * os.sysconf('SC_PHYS_PAGES')
current_mem_size = round(current_mem_bytes / (1024.**3), 1) #in GB
current_number_of_cpu = multiprocessing.cpu_count()

disk_st = os.statvfs(snap_common if snap else '/')
current_free_disk_space = round(disk_st.f_bavail * disk_st.f_frsize / (1024 * 1024 *1024), 1)

class current_app:
    pass

def check_resources():

    if '--shell' in sys.argv or '-x' in sys.argv:
        return

    if current_file_max < static.file_max:
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



    if current_free_disk_space < static.suggested_free_disk_space:
        print(("{0}Warning: Available free disk space was determined to be {1} "
            "GB. This is less than the required disk space of {2} GB.{3}".format(
                                                        static.colors.WARNING,
                                                        current_free_disk_space,
                                                        static.suggested_free_disk_space,
                                                        static.colors.ENDC)))

        result = input("Proceed anyways? [Y|n] ")
        if result and result[0].lower() == 'n':
            sys.exit()


def determineApacheVersion(full=False):
    httpd_cmd = shutil.which(httpd_name)
    cmd = httpd_name + " -v | egrep '^Server version'"
    output = run(cmd, shell=True)
    apache_version_re = re.search('Apache/(\d).(\d).(\d)', output.strip())
    if apache_version_re:
        (major, minor, pathc) =  apache_version_re.groups()
        if full:
            return '.'.join((major, minor, pathc))
        return '.'.join((major, minor))

def get_os_package_list():
    package_list_fn = os.path.join(paths.DATA_DIR, 'package_list.json')
    with open(package_list_fn) as f:
        packages = json.load(f)
        return packages

def check_os_supported():
    return os_type + ' '+ os_version in get_os_package_list()


def logIt(msg, errorLog=False, fatal=False):
    log_fn = paths.LOG_ERROR_FILE if errorLog else paths.LOG_FILE
    with open(log_fn, 'a') as w:
        w.write('{} {}\n'.format(time.strftime('%X %x'), msg))
        if errorLog and 'NoneType: None' not in traceback.format_exc():
             w.write('{} {}\n'.format(time.strftime('%X %x'), traceback.format_exc()))

    if fatal:
        print("FATAL:", errorLog)
        print(traceback.format_exc())
        Config.dump(True)
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
    if snap and args[0] in [paths.cmd_chown]:
        return ''

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

    if get_stderr:
        return output, err

    return output


def determine_package(glob_pattern):
    logIt("Determining package for pattern: {}".format(glob_pattern))
    package_list = glob.glob(glob_pattern)
    if package_list:
        return max(package_list)


def readJsonFile(jsonFile, ordered=False):
    logIt("Loading json file: {}".format(jsonFile))
    object_pairs_hook = OrderedDict if ordered else None
    if os.path.exists(jsonFile):
        with open(jsonFile) as f:
            return json.load(f, object_pairs_hook=object_pairs_hook)


def find_script_names(ldif_file):
    name_list = []
    rec = re.compile('\%\(((?s).*)\)s')
    with open(ldif_file) as f:
        for l in f:
            if l.startswith('oxScript::'):
                result = rec.search(l)
                if result:
                    name_list.append(result.groups()[0])
                
    return name_list

def download(url, dst):
    pardir, fn = os.path.split(dst)
    if not os.path.exists(pardir):
        logIt("Creating driectory", pardir)
        os.makedirs(pardir)
    logIt("Downloading {} to {}".format(url, dst))
    download_tries = 1
    while download_tries < 4:
        try:
            result = urlretrieve(url, dst)
            f_size = result[1].get('Content-Length','0')
            logIt("Download size: {} bytes".format(f_size))
            time.sleep(0.1)
        except:
             logIt("Error downloading {}. Download will be re-tried once more".format(url))
             download_tries += 1
             time.sleep(1)
        else:
            break

