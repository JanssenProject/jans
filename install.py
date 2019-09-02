#!/usr/bin/python

import re
import glob
import sys
import os
import argparse
import time

run_time = time.strftime("%Y-%m-%d_%H-%M-%S")
ces_dir = '/install/community-edition-setup'

parser = argparse.ArgumentParser(description="This script extracts community-edition-setup package and runs setup.py without arguments")
parser.add_argument('-o', help="download latest package from github and override current community-edition-setup", action='store_true')
parser.add_argument('--args', help="Arguments to be passed to setup.py")

argsp = parser.parse_args()
    
if argsp.o:
    for cep in glob.glob('/opt/dist/gluu/community-edition-setup*.zip'):
        os.remove(cep)
    if os.path.exists(ces_dir):
        back_dir = ces_dir+'.back.'+run_time
        print "Backing up", ces_dir, "to", back_dir
        os.rename(ces_dir, back_dir)


if not os.path.exists(ces_dir):

    ces_list = glob.glob('/opt/dist/gluu/community-edition-setup*.zip')

    if not ces_list:
        if not argsp.o:
            print "community-edition-setup package was not found"
            dl = raw_input("Download from github? (Y/n) ")
        else:
            dl = 'y'
        
        if not dl.strip() or dl.lower()[0]=='y':
            print "Downloading..."
            os.system('wget -nv https://github.com/GluuFederation/community-edition-setup/archive/master.zip -O /opt/dist/gluu/community-edition-setup-master.zip')
            ces_list = glob.glob('/opt/dist/gluu/community-edition-setup*.zip')
        else:
            print "Exiting..."
            sys.exit()

    ces = max(ces_list)

    if not os.path.exists('/install'):
        os.mkdir('/install')

    print "Extracting community-edition-setup package"
    os.system('unzip -o -q {0} -d /install'.format(ces))
    ces_dir_list = glob.glob('/install/community-edition-setup*')
    
    for d in ces_dir_list[:]:
        if '.back.' in d:
            ces_dir_list.remove(d)

    if not ces_dir_list:
        print "community-edition-setup package was not extracted properly. Exiting."

    ces_cur_dir = max(ces_dir_list)
    os.rename(ces_cur_dir, ces_dir)

cmd = '/install/community-edition-setup/setup.py'
if argsp.args:
    cmd += ' ' + argsp.args
    
print "Executing command", cmd
os.system(cmd)
