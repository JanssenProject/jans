#!/usr/bin/python3
from __future__ import print_function

import re
import zipfile
import glob
import sys
import os
import json
import argparse

try:
    from urllib.request import urlopen
except:
    from urllib2 import urlopen

def get_latest_commit(service):
    if service == 'jans-auth':
        service = 'jans-auth-server'
    url = f'https://api.github.com/repos/JanssenProject/jans/commits?path={service}&per_page=1'
    try:
        f = urlopen(url)
        content = f.read()
        commits = json.loads(content.decode('utf-8'))
        return commits[0]['sha']
    except Exception as e:
        return f"ERROR: Unable to retreive latest commit - {e}"

def get_war_info(war_fn):
    retDict = {'title':'', 'version':'', 'build':'', 'buildDate':'', 'branch':''}
    war_zip = zipfile.ZipFile(war_fn,"r")
    menifest = war_zip.read('META-INF/MANIFEST.MF')

    for l in menifest.splitlines():
        ls = l.strip().decode('utf-8')
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
        if f.filename.startswith('META-INF/maven/org.jans') and f.filename.endswith('pom.properties'):
            pom_prop = war_zip.read(f.filename)
            for l in pom_prop.splitlines():
                build_date = re.findall("\w{3}\s\w{3}\s{1,2}\w{1,2}\s\w{2}:\w{2}:\w{2}\s[+\w]{3}\s\w{4}", l.decode('utf-8'))
                if build_date:
                    retDict['buildDate'] =  build_date[0]
                    break

    return retDict

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument("--show-latest-commit", help="Gets latest commit from githbub", action='store_true')
    parser.add_argument("-target", help="Target directory", default='/opt/jans/jetty/*/webapps')
    parser.add_argument("--json", help="Print output in json format", action='store_true')
    parser.add_argument("-artifact", help="Display only this artifact")
    args = parser.parse_args()


    t_path = args.target
    if args.artifact:
        t_path = t_path.replace('*', args.artifact)

    target = os.path.join(t_path, '*.war')

    if args.json:
        output = []

    for war_fn in glob.glob(target):
        info = get_war_info(war_fn)
        service = os.path.basename(war_fn).split('.')[0]

        if not args.json:
            for si in ('title', 'version', 'buildDate', 'build'):
                print("{0}: {1}".format(si.title(), info[si]))

        if args.show_latest_commit:
            latest_commit = get_latest_commit(service)
            if not 'ERROR:' in latest_commit and info['build'] != latest_commit:
                compare_build = f'diff: https://github.com/JanssenProject/jans/compare/{latest_commit}...{info["build"]}'
            else:
                compare_build = ''
            if not args.json:
                print("Latest Commit:", latest_commit, compare_build)
            else:
                info["Latest Commit"] = latest_commit

        if args.json:
            info["file"] = war_fn
            output.append(info)
        else:
            print()

    if args.json:
        if args.artifact:
            output = output[0]
        print(json.dumps(output))
