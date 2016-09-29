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

attrs = 0
objclasses = 0


def convert(in_file, out_file):
    global attrs, objclasses
    ldif = open(in_file, 'r')
    schema = open(out_file, 'w')
    output = ''

    for line in ldif:
        if re.match('^dn:', line) or re.match('^objectClass:', line) or \
                re.match('^cn:', line):
            continue

        # empty lines and the comments are copied as such
        if re.match('^#', line) or re.match('^\s*$', line):
            pass
        elif re.match('^\s\s', line):  # change the space indendation to tabs
            line = re.sub('^\s\s', '\t', line)
        elif re.match('^\s', line):
            line = re.sub('^\s', '\t', line)
        # Change the keyword for attributetype
        elif re.match('^attributeTypes:\s', line, re.IGNORECASE):
            line = re.sub('^attributeTypes:', '\nattributetype', line, 1,
                          re.IGNORECASE)
            oid = attr_oid + '.' + str(attrs+1)
            line = re.sub('[\w]+-oid', oid, line, 1, re.IGNORECASE)
            attrs += 1
        # Change the keyword for objectclass
        elif re.match('^objectClasses:\s', line, re.IGNORECASE):
            line = re.sub('^objectClasses:', '\nobjectclass', line, 1,
                          re.IGNORECASE)
            oid = objc_oid + '.' + str(objclasses+1)
            line = re.sub('[\w]+-oid', oid, line, 1, re.IGNORECASE)
            objclasses += 1
        else:
            print "Unknown line starting: {}".format(line)

        output += line

    # print "AttributeTypes = %d" % attrs
    # print "Object Classes = %d" % objclasses
    schema.write(output)
    schema.close()
    ldif.close()


if __name__ == '__main__':
    ox101 = './static/opendj/101-ox.ldif'
    gluu_schema = './static/openldap/gluu.schema'
    convert(ox101, gluu_schema)

    custom77 = './static/opendj/101-ox.ldif'
    custom_schema = './static/openldap/custom.schema'
    convert(custom77, custom_schema)
