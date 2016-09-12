# Script to convert OpenDJ LDIF Schema file to a OpenLDAP *.schema file


import re

org_oid = '1.3.6.1.4.1.12345'  # dummy
reserved = org_oid + '.0'
published = org_oid + '.1'

# The OID strcturing is copied from the OpenLDAP Project
# Reference: http://www.openldap.org/faq/data/cache/197.html
syntax_oid = published + '.1'
match_oid = published + '.2'
attr_oid = published + '.3'
objc_oid = published + '.4'


def convert(ldif_file):
    ldif = open(ldif_file, 'r')
    output = ''

    attrs = 0
    objclasses = 0

    for line in ldif:
        if re.match('^dn:', line) or re.match('^objectClass:', line) or \
                re.match('^cn:', line):
            continue

        if re.match('^#', line):  # comments copied as such
            output += line
        elif re.match('^\s*$', line):  # empty lines copied as such
            output += line
        elif re.match('^\s\s', line):  # change the space indendation to tabs
            output += re.sub('^\s\s', '\t', line)
        elif re.match('^\s', line):
            output += re.sub('^\s', '\t', line)
        # Change the keyword for attributetype
        elif re.match('^attributeTypes:\s', line, re.IGNORECASE):
            line = re.sub('^attributeTypes:', '\nattributetype', line, 1,
                          re.IGNORECASE)
            oid = attr_oid + '.' + str(attrs+1)
            output += re.sub('[a-z]+-oid', oid, line, 1, re.IGNORECASE)
            attrs += 1
        # Change the keyword for objectclass
        elif re.match('^objectClasses:\s', line, re.IGNORECASE):
            line = re.sub('^objectClasses:', '\nobjectclass', line, 1,
                          re.IGNORECASE)
            oid = objc_oid + '.' + str(objclasses+1)
            output += re.sub('[a-z]+-oid', oid, line, 1, re.IGNORECASE)
            objclasses += 1
        else:
            print "Unknown Case: {}".format(line)

    print "AttributeTypes = %d" % attrs
    print "Object Classes = %d" % objclasses
    print output

if __name__ == '__main__':
    input_ldif = './static/opendj/101-ox.ldif'
    convert(input_ldif)
