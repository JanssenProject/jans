#!/usr/bin/python

import re
import glob
import sys
import os
import argparse
import time
import zipfile
from urlparse import urljoin


run_time = time.strftime("%Y-%m-%d_%H-%M-%S")
ces_dir = '/install/community-edition-setup'

parser = argparse.ArgumentParser(description="This script extracts community-edition-setup package and runs setup.py without arguments")
parser.add_argument('-o', help="download latest package from github and override current community-edition-setup", action='store_true')
parser.add_argument('--args', help="Arguments to be passed to setup.py")
parser.add_argument('-b', help="Github branch name, e.g. version_4.0.b4")

argsp = parser.parse_args()
    
if argsp.o:
    for cep in glob.glob('/opt/dist/gluu/community-edition-setup*.zip'):
        os.remove(cep)
    if os.path.exists(ces_dir):
        back_dir = ces_dir+'.back.'+run_time
        print "Backing up", ces_dir, "to", back_dir
        os.rename(ces_dir, back_dir)

github_base_url = 'https://github.com/GluuFederation/community-edition-setup/archive/'
arhchive_name = 'master.zip'


if argsp.b:
    arhchive_name = argsp.b+'.zip'

download_link = urljoin(github_base_url, arhchive_name)


def get_path_list(path):
    folders = []
    while 1:
        path, folder = os.path.split(path)

        if folder != '':
            folders.append(folder)
        else:
            if path != '':
                folders.append(path)

            break

    folders.reverse()

    return folders


if 1:
    ces_list = glob.glob('/opt/dist/gluu/community-edition-setup*.zip')

    if not ces_list:
        if not argsp.o:
            print "community-edition-setup package was not found"
            dl = raw_input("Download from github? (Y/n) ")
        else:
            dl = 'y'
        
        if not dl.strip() or dl.lower()[0]=='y':
            print "Downloading ", download_link
            os.system('wget -nv {0} -O /opt/dist/gluu/{1}'.format(download_link, arhchive_name))
            ces_list = [os.path.join('/opt/dist/gluu', arhchive_name)]
        else:
            print "Exiting..."
            sys.exit()

    ces = max(ces_list)


    print "Extracting community-edition-setup package"


    zf = zipfile.ZipFile(ces)

    namelist = zf.namelist()

    parent_dir = namelist[0]
    zf.close()
    
    
    print "Extracting community-edition-setup package"
    os.system('unzip -o -q {0} -d /install'.format(ces))

    source_dir = os.path.join('/install',parent_dir)
    
    if not os.path.exists(source_dir):
        sys.exit("Unzip failed. Exting")
    

    cmd = 'cp -r -f {}* /install/community-edition-setup'.format(source_dir)
    os.system(cmd)
    os.system('rm -r -f '+source_dir)


cmd = '/install/community-edition-setup/setup.py'

if argsp.args:
    cmd += ' ' + argsp.args
    
print "Executing command", cmd
os.system(cmd)
