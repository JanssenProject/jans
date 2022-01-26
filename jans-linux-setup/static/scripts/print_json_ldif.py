#!/usr/bin/python

import sys, base64
from ldif import LDIFParser, LDIFWriter

fn = None

try:
    fn = sys.argv[1]
except:
    pass

if not fn:
    print "Input ldif filename not found"
    sys.exit(2)

class MyLDIF(LDIFParser):
    def __init__(self,input,output):
        LDIFParser.__init__(self,input)
        self.writer = LDIFWriter(output)

    def handle(self, dn, entry):
        s = (len(dn)+4) * "="
        print "\n\n%s\ndn: %s\n%s" % (s, dn, s)
        for attr in entry.keys():
            print "\nattr: %s\n%s" % (attr, (len(attr)+6) * "-")
            for value in entry[attr]:
                print value
                     
parser = MyLDIF(open(fn, 'rb'), sys.stdout)
parser.parse()

