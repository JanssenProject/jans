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

# This program is used to generate the documentation for the Gluu Server schema found on
# http://www.gluu.org/docs/reference/ldap/schema

import sys
import string
from ldif import LDIFParser, LDIFWriter

class SchemaParser(LDIFParser):

    def __init__(self, input):
        LDIFParser.__init__(self, input)
        self.objectclasses = {}
        self.attributes = {}

    def __repr__(self):
        s = ''
        oc_list = self.objectclasses.keys()
        oc_list.sort()
        for name in oc_list:
            desc = self.objectclasses[name][0]
            attrs = self.objectclasses[name][1]
            s = s + "### Objectclass %s\n" % name
            s  = s + " * __Description__ %s\n" % desc
            for attr in attrs:
                attrDesc = ''
                if self.attributes.has_key(attr):
                    attrDesc = self.attributes[attr]
                s = s + " * __%s__ %s\n" % (attr, attrDesc)
            s = s + "\n"
        return s

    def handle(self, dn, entry):
        attributeTypes = entry['attributeTypes']
        objectclasses = entry['objectclasses']

        for attr in attributeTypes:
            desc = self.getDESC(attr)
            name_list = self.getName(attr)
            for name in name_list:
                self.attributes[name] = desc

        for oc in objectclasses:
            name = self.getName(oc)[0]  # Assumes OC has one name
            desc = self.getDESC(oc)
            if not desc:
                desc = ''
            mays = self.getMays(oc)
            self.objectclasses[name] = (desc, mays)

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
        name_list = None
        try:
            name_string = s.split('NAME')[1].split("'")[1].strip()
            name_list = name_string.split(" ")
        except:
            pass
        return name_list

if __name__ == '__main__':
    input_ldif = './static/opendj/101-ox.ldif'
    output_md = "./ldap-schema-table.md"
    parser = SchemaParser(open(input_ldif, 'rb'))
    parser.parse()
    print parser
