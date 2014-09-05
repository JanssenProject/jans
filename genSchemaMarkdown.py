#!/usr/bin/python

# MIT License
#
# Copyright 2014 Gluu, Inc.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software
# and associated documentation files (the Software), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.

# THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
# INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
# PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
# FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
# OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

import sys
import string
from ldif import LDIFParser, LDIFWriter

class SchemaParser(LDIFParser):

    def __init__(self, input, output):
        LDIFParser.__init__(self, input)
        objectclasses = {}
        attributes = {}


    def handle(self, dn, entry):
        print "# Schema"
        attributeTypes = entry['attributeTypes']
        objectclasses = entry['objectclasses']

        print "## Attributes"
        for attr in attributeTypes:
            desc = self.getDESC(attr)
            name = self.getName(attr)
            if desc != None:
                print " * __%s__ %s" % (name, desc)

        print "## Objectclasses "
        for oc in objectclasses:
            desc = self.getDESC(oc)
            if not desc:
                desc = ''
            name = self.getName(oc)
            print " * __%s__ %s" % (name, desc)
            for attr in self.getMays(oc):
                print "    * %s" % attr

    def getMays(self, oc):
        mays = oc.split('MAY')[1].split('(')[1].split(')')[0].split('$')
        return map(string.strip, mays)

    def getDESC(self, s):
        desc = None
        try:
            desc = s.split('DESC')[1].split("'")[1]
        except:
            pass
        return desc

    def getName(self,s):
        name = None
        try:
            name = s.split('NAME')[1].split("'")[1]
        except:
            pass
        return name

if __name__ == '__main__':
    input_ldif = './static/opendj/101-ox.ldif'
    output_md = "./ldap-schema-table.md"
    parser = SchemaParser(open(input_ldif, 'rb'), sys.stdout)
    parser.parse()

