#!/usr/bin/env python3

import os
import sys
import json
import zipfile
import argparse
import hashlib
from pathlib import Path
from collections import OrderedDict

parser = argparse.ArgumentParser(description="Create .gama project archieve from directory")
parser.add_argument('directory', help="Directory containing agama project files")
parser.add_argument('-exclude-extension', help="Exclude files having this extension", action='append')
parser.add_argument('-exclude-file', help="Exclude files having this name", action='append')
parser.add_argument('-exclude-dir', help="Exclude files in this directory", action='append')
parser.add_argument('-agama-archieve-name', help="Output file name of project archieve. If excluded archieve name will be created based on directory")
argsp = parser.parse_args()


cur_dir = os.path.dirname(os.path.realpath(__file__))
if not argsp.directory.startswith('/'):
    argsp.directory = os.path.join(cur_dir, argsp.directory)

agama_path = Path(argsp.directory)
exclude_extensions = argsp.exclude_extension or []
exclude_dirs = argsp.exclude_dir or []
exclude_files = argsp.exclude_file or []


for i, eext in enumerate(exclude_extensions):
    if not eext.startswith('.'):
        exclude_extensions[i] = '.' + eext

for i, edir in enumerate(exclude_dirs):
    if not edir.startswith('/'):
        exclude_dirs[i] = os.path.join(argsp.directory, edir)

for i, efn in enumerate(exclude_files):
    if not efn.startswith('/'):
        exclude_files[i] = os.path.join(argsp.directory, efn)


if not agama_path.is_dir():
    print("{} is not a directory".format(agama_path))
    sys.exit()

if argsp.agama_archieve_name:
    agama_zip_fn = argsp.agama_archieve_name
else:
    agama_zip_fn = agama_path.name

if not agama_zip_fn.endswith('.gama'):
    agama_zip_fn += '.gama'

file_list = []
mandatory_dirs = {'web': False, 'code': False}

for fpath in agama_path.glob("**/*"):
    add = True
    relative_path = fpath.relative_to(agama_path)
    if fpath.is_file() and fpath.suffix in exclude_extensions:
        print("Not zipping", fpath)
        continue
    if fpath.is_dir():
        if fpath.parts[-1] in mandatory_dirs:
            mandatory_dirs[fpath.parts[-1]] = True

        if fpath.as_posix().startswith(tuple(exclude_dirs)) or fpath.as_posix().startswith(tuple(exclude_files)):
            print("Not zipping", fpath)
            add = False

    if add:
        file_list.append((fpath, relative_path))

# check if directories code and web exists
flow_files = []
flows = []

for fpath, rpath in file_list:
   if fpath.is_file() and rpath.suffix == '.flow' and rpath.parts[0] == 'code':
        flow_files.append((fpath, rpath))

for mdir in mandatory_dirs:
    if not mandatory_dirs[mdir]:
        print("Directory {} does not exist".format(mdir))
        sys.exit()


if not flow_files:
    print("No .flow files found")
    sys.exit()

def strip_quotation(s):
    if s:
        if s[0] == '"':
            return s.strip('"')
        elif s[0] == "'":
            return s.strip("'")

    return s

def get_path(path_s, l):
    if path_s in l:
        ns = l.find(path_s)
        rpath = strip_quotation(l[ns+len(path_s):].strip().split()[0])
        return rpath

for fpath, rpath in flow_files:
    #print("Examining", rpath)
    flow_section = False
    check_base_path = None
    for l in fpath.open().read().splitlines():
        ls = l.rstrip().split()

        if ls and ls[0] == 'Flow':
            flow_section = True
            flow = get_path('Flow', l)
            flows.append(flow)
            continue

        if flow_section and not ls:
            flow_section = False

        if flow_section:
            base_path = get_path('Basepath', l)
            if base_path:
                check_base_path = agama_path.joinpath('web', base_path)
                if not check_base_path.is_dir():
                    print("Basepath {} not found".format(check_base_path))
                    sys.exit()

        if check_base_path:
            rrf_path = get_path('RRF', l)
            if rrf_path:
                check_rrf_path = check_base_path.joinpath(check_base_path, rrf_path)
                if not check_rrf_path.is_file():
                    print("RRF {} not found".format(check_rrf_path))
                    sys.exit()


# check configs in project.json
project_metadata_fn = agama_path.joinpath('project.json')
if project_metadata_fn.exists():
    try:
        project_metadata = json.loads(project_metadata_fn.open().read(), object_pairs_hook=OrderedDict)
    except Exception as e:
        print("Can't decode file {}. Error is: {}".format(project_metadata_fn, e))

    if 'configs' in project_metadata:
        for cflow in project_metadata['configs']:
            if not cflow in flows:
                print("Config {} in project.json does not match any flow".format(cflow))
                sys.exit()
        for flow in flows:
            if flow in project_metadata['configs']:
                if not isinstance(project_metadata['configs'][flow], dict):
                    print("Config of flow {} should be an object".format(flow))
                    sys.exit()
    if 'noDirectLaunch' in project_metadata:
        for ndflow in project_metadata['noDirectLaunch']:
            if not ndflow in flows:
                print("noDirectLaunch {} in project.json does not match any flow".format(ndflow))
                sys.exit()
else:
    project_metadata = OrderedDict()
    file_list.append((project_metadata_fn, 'project.json'))


if not 'projectName' in project_metadata:
    while True:
        project_name = input("Please enter project name: ")
        project_name_s = project_name.strip()
        if project_name_s:
            break
    project_metadata_new = OrderedDict()
    project_metadata_new['projectName'] = project_name_s
    for key in project_metadata:
        project_metadata_new[key] = project_metadata[key]

    project_metadata_fn.open('w').write(json.dumps(project_metadata_new, indent=2))


agama_zip_obj = zipfile.ZipFile(agama_zip_fn, 'w')

for fpath, rpath in file_list:
    agama_zip_obj.write(filename=fpath, arcname=rpath)

agama_zip_obj.close()

with open(agama_zip_fn, 'rb') as f:
    data = f.read()
    sha256hash = hashlib.sha256(data).hexdigest()


sha256sum_fn = agama_zip_fn + '.sha256sum'
with open(sha256sum_fn, 'w') as w:
    w.write(sha256hash)

print("All seems good. Archieve {} and signature file {} were created".format(agama_zip_fn, sha256sum_fn))



