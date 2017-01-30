#!/usr/bin/env python
"""
A Module containing the classes which generate schema files from JSON.
"""

import json


class SchemaGenerator(object):
    def __init__(self, jsontext, header=None):
        self.data = json.loads(jsontext)
        self.outString = header if header else u''

    def generate_schema(self):
        """Function that generates the schema and returns it as a string"""
        if len(self.outString):
            self.outString += u"\n"
        for attr in self.data['attributeTypes']:
            attr_str = u"attributetype ( {} NAME ".format(attr['oid'])
            if len(attr['names']) > 1:
                namestring = u''
                for name in attr['names']:
                    namestring += u"'{}' ".format(name)
                attr_str += u"( {})".format(namestring)
            elif len(attr['names']) == 1:
                attr_str += u"'{}'".format(attr['names'][0])
            else:
                print "Invalid attribute data. Doesn't define a name"
            if 'desc' in attr:
                attr_str += u"\n\tDESC '{}'".format(attr['desc'])
            if 'equality' in attr:
                attr_str += u"\n\tEQUALITY {}".format(attr['equality'])
            if 'substr' in attr:
                attr_str += u"\n\tSUBSTR {}".format(attr['substr'])
            if 'syntax' in attr:
                attr_str += u"\n\tSYNTAX {}".format(attr['syntax'])
            if 'ordering' in attr:
                attr_str += u"\n\tORDERING {}".format(attr['ordering'])
            if 'x_origin' in attr:
                attr_str += u"\n\tX-ORIGIN '{}'".format(attr['x_origin'])
            attr_str += u" )\n\n"

            self.outString += attr_str

        for obc in self.data['objectClasses']:
            obc_str = u"objectclass ( {} NAME ".format(obc['oid'])
            if len(obc['names']) > 1:
                namestring = ''
                for name in obc['names']:
                    namestring += u"'{}' ".format(name)
                obc_str += u"( {})".format(namestring)
            elif len(obc['names']) == 1:
                obc_str += u"'{}'".format(obc['names'][0])
            else:
                print "Invalid objectclass data. Doesn't define a name"
            if 'desc' in obc:
                obc_str += u"\n\tDESC '{}'".format(obc['desc'])
            if 'sup' in obc:
                sup = u" $ ".join(obc['sup'])
                obc_str += u"\n\tSUP ( {} )".format(sup)
            obc_str += u"\n\t{}".format(obc['kind'])
            if 'must' in obc:
                must = u" $ ".join(obc['must'])
                obc_str += "\n\tMUST ( {} )".format(must)
            if 'may' in obc:
                may = u" $ ".join(obc['may'])
                obc_str += "\n\tMAY ( {} )".format(may)
            if 'x_origin' in obc:
                obc_str += u"\n\tX-ORIGIN '{}'".format(obc['x_origin'])
            obc_str += u" )\n\n"

            self.outString += obc_str

        return self.outString.strip()

    def generate_ldif(self):
        """Function which generates the OpenDJ LDIF format schema string."""
        self.outString += u"dn: cn=schema\nobjectClass: top\nobjectClass: " \
            + u"ldapSubentry\nobjectClass: subschema\ncn: schema\n"

        for attr in self.data['attributeTypes']:
            attr_str = u"attributeTypes: ( {} NAME ".format(attr['oid'])
            if len(attr['names']) > 1:
                namestring = u''
                for name in attr['names']:
                    namestring += u"'{}' ".format(name)
                attr_str += u"( {})".format(namestring)
            elif len(attr['names']) == 1:
                attr_str += u"'{}'".format(attr['names'][0])
            else:
                print "Invalid attribute data. Doesn't define a name"
            if 'desc' in attr:
                attr_str += u"\n  DESC '{}'".format(attr['desc'])
            if 'equality' in attr:
                attr_str += u"\n  EQUALITY {}".format(attr['equality'])
            if 'substr' in attr:
                attr_str += u"\n  SUBSTR {}".format(attr['substr'])
            if 'syntax' in attr:
                attr_str += u"\n  SYNTAX {}".format(attr['syntax'])
            if 'ordering' in attr:
                attr_str += u"\n  ORDERING {}".format(attr['ordering'])
            if 'x_origin' in attr:
                attr_str += u"\n  X-ORIGIN '{}'".format(attr['x_origin'])
            attr_str += u" )\n"

            self.outString += attr_str

        for obc in self.data['objectClasses']:
            obc_str = u"objectClasses: ( {} NAME ".format(obc['oid'])
            if len(obc['names']) > 1:
                namestring = ''
                for name in obc['names']:
                    namestring += u"'{}' ".format(name)
                obc_str += u"( {})".format(namestring)
            elif len(obc['names']) == 1:
                obc_str += u"'{}'".format(obc['names'][0])
            else:
                print "Invalid objectclass data. Doesn't define a name"
            if 'desc' in obc:
                obc_str += u"\n  DESC '{}'".format(obc['desc'])
            if 'sup' in obc:
                sup = u" $ ".join(obc['sup'])
                obc_str += u"\n  SUP ( {} )".format(sup)
            obc_str += u"\n  {}".format(obc['kind'])
            if 'must' in obc:
                must = u" $ ".join(obc['must'])
                obc_str += "\n  MUST ( {} )".format(must)
            if 'may' in obc:
                may = u" $ ".join(obc['may'])
                obc_str += "\n  MAY ( {} )".format(may)
            if 'x_origin' in obc:
                obc_str += u"\n  X-ORIGIN '{}'".format(obc['x_origin'])
            obc_str += u" )\n"

            self.outString += obc_str

        return self.outString.strip()
