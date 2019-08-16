#!/usr/bin/python

import re
import glob
import sys
import os

ces_dir = '/install/community-edition-setup'

if not os.path.exists(ces_dir):

    ces_list = glob.glob('/opt/dist/gluu/community-edition-setup*.zip')

    if not ces_list:
        print "community-edition-setup package was not found"
        dl = raw_input("Download from github? (Y/n) ")
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
    os.system('unzip -q {0} -d /install'.format(ces))
    ces_dir_list = glob.glob('/install/community-edition-setup*')

    if not ces_dir_list:
        print "community-edition-setup package was not extracted properly. Exiting."

    ces_cur_dir = max(ces_dir_list)
    os.rename(ces_cur_dir, ces_dir)

os.system('/install/community-edition-setup/setup.py')
