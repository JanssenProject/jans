#!/usr/bin/env python

import os
import glob
import sys
from ldap.schema import AttributeType, ObjectClass
from ldif import LDIFParser


if len(sys.argv) < 2:
    print "OpenLDAP to OpenDJ Schema Converter"
    print "Usage: python {} <schema file> or <schema dir>".format(sys.argv[0])
    sys.exit()

if not os.path.exists(sys.argv[1]):
    print "{} not found".format(sys.argv[1])
    sys.exit()

if os.path.isfile(sys.argv[1]):
    file_list = [ sys.argv[1] ]
else:
    file_list = glob.glob( sys.argv[1] + '/*.schema')
    if not file_list:
        print "Schema files were not found under {}".format(sys.argv[1])
        sys.exit()


cur_dir=os.path.dirname(os.path.realpath(__file__))

c=101

if not os.path.exists('opendj_schema'):
    os.mkdir('opendj_schema')


for fn in file_list:

    f = open(fn)
    entry_finished = True
    new_entry= []
    new_object = []

    attributes = []
    objectclasses = []

    for l in f:
        if l.lower().startswith('attributetype') or l.lower().startswith('objectclass'):
            entry_finished = False
            objs = ' '.join(new_entry)
            if objs.lower().startswith('attributetype'):
                attributes.append(AttributeType(objs[14:]))
            elif objs.lower().startswith('objectclass'):
                objectclasses.append(ObjectClass(objs[12:]))
            new_entry = []

        if not entry_finished:
            if not l.startswith('#'):
                ls = l.strip()
                if ls:
                    new_entry.append(ls)

    spath, sfile = os.path.split(fn)
    fname, fext = os.path.splitext(sfile)

    opendj_fn = 'opendj_schema/{}-{}.ldif'.format(c,fname)

    with open(opendj_fn, 'w') as f:
        f.write('dn: cn=schema\nobjectClass: top\nobjectClass: ldapSubentry\nobjectClass: subschema\n')
        for atyp in  attributes:
            f.write('attributeTypes: {}\n'.format(atyp.__str__()))
        
        for ocls in objectclasses:
            f.write('objectClasses: {}\n'.format(ocls.__str__()))
    print "Opendj schema ldif file {} was written.".format(opendj_fn)
    
    c += 1
