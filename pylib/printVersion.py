#!/usr/bin/python

import re
import zipfile
import glob
import urllib
import sys
import os
import ssl
import json
import argparse

ssl._create_default_https_context = ssl._create_unverified_context

repos = {
        'identity': 'oxTrust',
        'oxauth': 'oxAuth',
        'idp': 'oxShibboleth',
        }


def get_latest_commit(service, branch):
    url = 'https://api.github.com/repos/GluuFederation/{0}/commits/{1}'.format(repos[service], branch)
    try:
        f = urllib.urlopen(url)
        commits = json.loads(f.read())
        return commits['sha']
    except:
        return "ERROR: Unable to retreive latest commit"

def get_war_info(war_fn):
    retDict = {'title':'', 'version':'', 'build':'', 'build date':'', 'branch':''}
    war_zip = zipfile.ZipFile(war_fn,"r")
    menifest = war_zip.read('META-INF/MANIFEST.MF')

    for l in menifest.split('\n'):
        ls = l.strip()
        for key in retDict:
            if ls.startswith('Implementation-{0}'.format(key.title())):
                retDict[key] = ls.split(':')[1].strip()
                break
        if ls.startswith('Build-Branch:'):
            branch = ls.split(':')[1].strip()
            if '/' in branch:
                branch = branch.split('/')[1]

            retDict['branch'] = branch
            

    for f in war_zip.filelist:
        if f.filename.startswith('META-INF/maven/org.gluu') and f.filename.endswith('pom.properties'):
            pom_prop = war_zip.read(f.filename)
            for l in pom_prop.split('\n'):
                build_date = re.findall("\w{3}\s\w{3}\s{1,2}\w{1,2}\s\w{2}:\w{2}:\w{2}\s[+\w]{3}\s\w{4}", l)
                if build_date:
                    retDict['build date'] =  build_date[0]
                    break

    return retDict

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument("--show-latest-commit", help="Gets latest commit from githbub", action='store_true')
    args = parser.parse_args()

    for war_fn in glob.glob('/opt/gluu/jetty/*/webapps/*.war'):
        info = get_war_info(war_fn)
        service = os.path.basename(war_fn).split('.')[0]
        
        for si in ('title', 'version', 'build date', 'build'):
            print "{0}: {1}".format(si.title(), info[si])
        
        if args.show_latest_commit and (service in repos):
            latest_commit = get_latest_commit(service, info['branch'])
            if not 'ERROR:' in latest_commit:
                compare_build = 'diff: https://github.com/GluuFederation/{0}/compare/{1}...{2}'.format(repos[service], info['build'], latest_commit) 
            else:
                compare_build = ''
            
            print "Latest Commit:", latest_commit, compare_build
        
        print
