import sys
import os
import json
from collections import OrderedDict
from setup_app.pylib.ldif4.ldif import LDIFWriter


with open(sys.argv[1]) as f:
    data = json.load(f, object_pairs_hook=OrderedDict)


stdout = os.fdopen(sys.stdout.fileno(), "wb", closefd=False)

ldif_writer = LDIFWriter(stdout, cols=10000)
for entry in data:
    dn = entry.pop('dn')
    for e in entry:
        
        if isinstance(entry[e], dict):
            ne = []
            for v in entry[e]['v']:
                ne.append(v)
            entry[e] = ne
        else:
            entry[e] = [str(entry[e])]
    entry['objectClass'].insert(0, 'top')
    ldif_writer.unparse(dn, entry)

stdout.flush()
