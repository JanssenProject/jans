#!/usr/bin/python3

import site
import re
import glob
import sys
import os
import subprocess
import argparse
import time
import zipfile
import shutil
import distutils
import requests

from urllib.parse import urljoin


run_time = time.strftime("%Y-%m-%d_%H-%M-%S")
ces_dir = '/install/community-edition-setup'

parser = argparse.ArgumentParser(description="This script extracts community-edition-setup package and runs setup.py without arguments")
parser.add_argument('-o', help="download latest package from github and override current community-edition-setup", action='store_true')
parser.add_argument('--args', help="Arguments to be passed to setup.py")
parser.add_argument('-b', help="Github branch name, e.g. version_4.0.b4")

argsp = parser.parse_args()

npyscreen_package = '/opt/dist/app/npyscreen-master.zip'

if argsp.o:
    for cep in glob.glob('/opt/dist/jans/community-edition-setup*.zip'):
        os.remove(cep)
    if os.path.exists(ces_dir):
        back_dir = ces_dir+'.back.'+run_time
        print("Backing up", ces_dir, "to", back_dir)
        os.rename(ces_dir, back_dir)



github_base_url = 'https://github.com/JanssenProject/jans-setup/archive/'
arhchive_name = 'master.zip'


if argsp.b:
    arhchive_name = argsp.b+'.zip'

download_link = urljoin(github_base_url, arhchive_name)

ces_list = glob.glob('/opt/dist/jans/community-edition-setup*.zip')

if not ces_list:
    if not argsp.o:
        print("community-edition-setup package was not found")
        dl = input("Download from github? (Y/n) ")
    else:
        dl = 'y'
    
    if not dl.strip() or dl.lower()[0]=='y':
        print("Downloading ", download_link)
        result = requests.get(download_link, allow_redirects=True)
        with open('/opt/dist/jans/community-edition-setup.zip', 'wb') as w:
            w.write(result.content)
        ces_list = [os.path.join('/opt/dist/jans', arhchive_name)]
    else:
        print("Exiting...")
        sys.exit()

ces = max(ces_list)

ces_zip = zipfile.ZipFile(ces)
parent_dir = ces_zip.filelist[0].filename
target_dir = '/tmp/ces_tmp'
ces_zip.extractall(target_dir)
        


if not os.path.exists(ces_dir):
    os.makedirs(ces_dir)

print("Extracting community-edition-setup package")

source_dir = os.path.join(target_dir, parent_dir)
ces_zip.close()

if not os.path.exists(source_dir):
    sys.exit("Unzip failed. Exting")

cmd = 'cp -r -f {}* /install/community-edition-setup'.format(source_dir)
os.system(cmd)
os.system('rm -r -f '+source_dir)

shutil.rmtree(target_dir)

os.chmod('/install/community-edition-setup/setup.py', 33261)

post_setup = '/install/community-edition-setup/post-setup-add-components.py'
if os.path.exists(post_setup):
    os.chmod(post_setup, 33261)

if argsp.o:
    npy_download_link = 'https://github.com/npcole/npyscreen/archive/master.zip'
    result = requests.get(npy_download_link, allow_redirects=True)
    with open(npyscreen_package, 'wb') as w:
        w.write(result.content)

if os.path.exists(npyscreen_package):
    site_libdir = site.getsitepackages()[0]
    dest_dir = os.path.join(site_libdir, 'npyscreen')

    if not os.path.exists(dest_dir):
        print("Extracting npyscreen to", dest_dir)
        npyzip = zipfile.ZipFile(npyscreen_package)
        parent_dir = npyzip.filelist[0].filename
        target_dir = '/tmp/npyscreen_tmp'
        npyzip.extractall(target_dir)
        npyzip.close()
        
        shutil.copytree(
            os.path.join(target_dir, parent_dir, 'npyscreen'),
            dest_dir
            )

        shutil.rmtree(target_dir)

