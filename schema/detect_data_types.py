import os
import re
import json

dataTypes = {'string':[],'integer':[],'boolean':[]}

def collectDataTypes(jdir):
    for root, dirs, files in os.walk(jdir):
        path = root.split(os.sep)
        for f in files:
            fn=os.path.join(root, f)
            if fn.endswith('.java'):
                ff = open(fn).readlines()
                for i, l in enumerate(ff):
                    ls = l.strip()
                    if ls.startswith('@AttributeName') and 'name' in ls:
                        m = re.search('name\s*=\s*\"(\w*)\"', ls)
                        if m:
                            aname = m.groups()[0]
                            la = ff[i+1].strip()

                            dtype = None

                            if la.startswith('private'):
                                if 'string ' in la.lower():
                                    dtype = 'string'
                                elif 'boolean ' in la.lower():
                                    dtype = 'boolean'
                                    dataTypes['boolean'].append(aname)
                                elif 'integer ' in la:
                                    dtype = 'integer'

                                if dtype:
                                    if not aname in dataTypes[dtype]:
                                        dataTypes[dtype].append(aname)

collectDataTypes('/tmp/oxTrust-master')
collectDataTypes('/tmp/oxAuth-master')


print(json.dumps(dataTypes, indent=2))
