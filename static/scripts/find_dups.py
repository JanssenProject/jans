#!/usr/bin/python
"""Script to find the duplicate attributes in a LDIF file"""

import sys
from ldif import LDIFParser

fn = None
attr = None

try:
    fn = sys.argv[1]
    attr = sys.argv[2]
except:
    pass

if not fn:
    print "Input ldif filename not found"
    sys.exit(2)

if not attr:
    print "Target attr not found"
    sys.exit(2)

class MyLDIF(LDIFParser):
    def __init__(self,input,output):
        LDIFParser.__init__(self,input)
        self.attrs = {}

    def handle(self, dn, entry):
        if entry.has_key(attr):
            values = entry[attr]
            if not len(values):
                return
            id = dn
            if entry.has_key('uid'):
                id = entry['uid'][0]
            for value in values:
                if self.attrs.has_key(value):
                    current_list = self.attrs[value]
                    current_list.append(id)
                    self.attrs[value] = current_list
                else:
                    self.attrs[value] = [id]

    def get_attrMap(self):
        return self.attrs

parser = MyLDIF(open(fn, 'rb'), sys.stdout)
parser.parse()
dups = []
attrMap = parser.get_attrMap()
for value in attrMap.keys():
    values = attrMap[value]
    if len(attrMap[value]) > 1:
        print "%s,%s" % (value, ",".join(values))
