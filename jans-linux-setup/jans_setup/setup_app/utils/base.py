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
import tempfile

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
pylib_dir = os.path.join(ces_dir, 'pylib')

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
                elif ('sles' in os_type) or ('opensuse' in os_type):
                    os_type = 'suse'
            elif row[0] == 'VERSION_ID':
                os_version = row[1].split('.')[0]

if not (os_type and os_version):
    print("Can't determine OS type and OS version")
    sys.exit()

os_name = os_type + os_version
deb_sysd_clone = os_name.startswith(('ubuntu', 'debian'))

# Determine service path
if (os_type in ('centos', 'red', 'fedora', 'suse') and os_initdaemon == 'systemd') or deb_sysd_clone:
    service_path = shutil.which('systemctl')
elif os_type in ['debian', 'ubuntu']:
    service_path = '/usr/sbin/service'
else:
    service_path = '/sbin/service'

if os_type in ('centos', 'red', 'fedora'):
    clone_type = 'rpm'
    httpd_name = 'httpd'
elif os_type == 'suse':
    clone_type = 'rpm'
    httpd_name = 'apache2'
else:
    clone_type = 'deb'
    httpd_name = 'apache2'

def get_os_description():
    desc_dict = { 'suse': 'SUSE', 'red': 'RHEL', 'ubuntu': 'Ubuntu', 'deb': 'Debian', 'centos': 'CentOS', 'fedora': 'Fedora' }
    descs = desc_dict.get(os_type, os_type)
    descs += ' ' + os_version

    fipsl = subprocess.getoutput("sysctl crypto.fips_enabled").strip().split()

    if fipsl and fipsl[0] == 'crypto.fips_enabled' and fipsl[-1] == '1':
        descs += ' [FIPS]'
    if snap:
        descs += ' [SNAP]'

    return descs

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

def download(url, dst, verbose=False):
    pardir, fn = os.path.split(dst)
    if not os.path.exists(pardir):
        logIt("Creating driectory", pardir)
        os.makedirs(pardir)

    def mylog(logs):
        logIt(logs)
        if verbose:
            print(logs)

    mylog("Downloading {} to {}".format(url, dst))
    download_tries = 1
    while download_tries < 4:
        try:
            urlretrieve(url, dst)
            mylog("Download size: {} bytes".format(os.path.getsize(dst)))
            time.sleep(0.1)
        except:
             mylog("Error downloading {}. Download will be re-tried once more".format(url))
             download_tries += 1
             time.sleep(1)
        else:
            break

def extract_file(zip_file, source, target, ren=False):
    zip_obj = zipfile.ZipFile(zip_file, "r")
    for member in zip_obj.infolist():
        if not member.is_dir() and member.filename.endswith(source):
            if ren:
                target_p = Path(target)
            else:
                p = Path(member.filename)
                target_p = Path(target).joinpath(p.name)
                if not target_p.parent.exists():
                    target_p.parent.mkdir(parents=True)
            target_p.write_bytes(zip_obj.read(member))
            break
    zip_obj.close()


def extract_from_zip(zip_file, sub_dir, target_dir, remove_target_dir=False):
    zipobj = zipfile.ZipFile(zip_file, "r")
    parent_dir = zipobj.namelist()[0]

    if sub_dir and not sub_dir.endswith('/'):
        sub_dir += '/'

    parent_sub_dir = os.path.join(parent_dir, sub_dir)
    target_dir_path = Path(target_dir)

    if remove_target_dir and target_dir_path.exists():
        shutil.rmtree(target_dir_path)

    if not target_dir_path.exists():
        target_dir_path.mkdir(parents=True)

    for member in zipobj.infolist():
        if member.filename.startswith(parent_sub_dir):
            p = Path(member.filename)
            pr = p.relative_to(parent_sub_dir)
            target_fn = target_dir_path.joinpath(pr)
            if member.is_dir():
                if not target_fn.exists():
                    target_fn.mkdir(parents=True)
            else:
                if not target_fn.parent.exists():
                    target_fn.parent.mkdir(parents=True)
                target_fn.write_bytes(zipobj.read(member))
    zipobj.close()

def extract_subdir(zip_fn, sub_dir, target_dir, par_dir=None):
    if par_dir is None:
        zip_obj = zipfile.ZipFile(zip_fn, 'r')
        par_dir = zip_obj.namelist()[0]
        zip_obj.close()

    if not sub_dir.endswith('/'):
        sub_dir += '/'

    target_dir_path = Path(target_dir)

    if target_dir_path.exists():
        shutil.rmtree(target_dir_path)

    with tempfile.TemporaryDirectory() as unpack_dir:
        shutil.unpack_archive(zip_fn, unpack_dir, format='zip')
        shutil.copytree(os.path.join(unpack_dir, par_dir, sub_dir), target_dir)

current_app.app_info = readJsonFile(os.path.join(par_dir, 'app_info.json'))
current_app.jans_zip = os.path.join(Config.distFolder, 'jans/jans.zip')

def as_bool(val):
    return str(val).lower() in ('t', 'true', 'y', 'yes', 'on', 'ok', '1')
