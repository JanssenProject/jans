#!/usr/bin/env python3
"""
A Module containing the classes which generate schema files from JSON.
"""

import json


def cmp_to_key(mycmp):
    'Convert a cmp= function into a key= function'
    class K:
        def __init__(self, obj, *args):
            self.obj = obj
        def __lt__(self, other):
            return mycmp(self.obj, other.obj) < 0
        def __gt__(self, other):
            return mycmp(self.obj, other.obj) > 0
        def __eq__(self, other):
            return mycmp(self.obj, other.obj) == 0
        def __le__(self, other):
            return mycmp(self.obj, other.obj) <= 0
        def __ge__(self, other):
            return mycmp(self.obj, other.obj) >= 0
        def __ne__(self, other):
            return mycmp(self.obj, other.obj) != 0
    return K

class SchemaGenerator(object):
    def __init__(self, jsontext, header=None):
        self.data = json.loads(jsontext)
        self.header = header
        self.macroMap = {}
        self.macroMapIndex = {}
        if self.data['oidMacros']:
            self.__mapMacros()

    def __mapMacros(self):
        if not self.data['oidMacros']:
            return

        macros = self.data['oidMacros']
        # Find the root
        for mac in macros:
            if '.' in macros[mac]:
                self.macroMap[mac] = macros[mac]
                break

        if not self.macroMap:
            return

        while len(macros) != len(self.macroMap):
            for mac in macros:
                if ':' not in macros[mac]:
                    continue
                oid = macros[mac]
                parent, index = oid.split(':')
                if parent in self.macroMap:
                    self.macroMap[mac] = self.macroMap[parent] + '.' + index
                    self.macroMapIndex[mac] = 1

    def __compare_defs(self, m1, m2):
        n1 = int(m1[1].split(':')[1])
        n2 = int(m2[1].split(':')[1])
        return n1 - n2

    def __get_macro_order(self, macros, parent):
        children = [(k, v) for k, v in list(macros.items()) if parent in v]
        items = [parent]
        for k, v in sorted(children, key=cmp_to_key(self.__compare_defs)):
            items.extend(self.__get_macro_order(macros, k))
        return items

    def generate_schema(self):
        """Function that generates the schema and returns it as a string"""
        self.outString = ''
        self.outString += self.header if self.header else ""
        if len(self.outString):
            self.outString += "\n"
        if len(self.data['oidMacros']) > 0:
            macros = self.data['oidMacros']
            root = ''
            for definition in macros:
                if '.' in macros[definition]:
                    root = definition
                    break
            order = self.__get_macro_order(macros, root)

            for oid in order:
                self.outString += "objectIdentifier {:15} {}\n".format(
                        oid, macros[oid])
            self.outString += '\n'

        for attr in self.data['attributeTypes']:
            attr_str = "attributetype ( {} NAME ".format(attr['oid'])
            if len(attr['names']) > 1:
                namestring = ''
                for name in attr['names']:
                    namestring += "'{}' ".format(name)
                attr_str += "( {})".format(namestring)
            elif len(attr['names']) == 1:
                attr_str += "'{}'".format(attr['names'][0])
            else:
                print("Invalid attribute data. Doesn't define a name", attr)
            if 'desc' in attr:
                attr_str += "\n\tDESC '{}'".format(attr['desc'])
            if 'equality' in attr:
                attr_str += "\n\tEQUALITY {}".format(attr['equality'])
            if 'substr' in attr:
                attr_str += "\n\tSUBSTR {}".format(attr['substr'])
            if 'syntax' in attr:
                attr_str += "\n\tSYNTAX {}".format(attr['syntax'])
            if 'ordering' in attr:
                attr_str += "\n\tORDERING {}".format(attr['ordering'])
            if 'x_origin' in attr:
                attr_str += "\n\tX-ORIGIN '{}'".format(attr['x_origin'])
            attr_str += " )\n\n"

            self.outString += attr_str

        for obc in self.data['objectClasses']:
            obc_str = "objectclass ( {} NAME ".format(obc['oid'])
            if len(obc['names']) > 1:
                namestring = ''
                for name in obc['names']:
                    namestring += "'{}' ".format(name)
                obc_str += "( {})".format(namestring)
            elif len(obc['names']) == 1:
                obc_str += "'{}'".format(obc['names'][0])
            else:
                print("Invalid objectclass data. Doesn't define a name", obc)
            if 'desc' in obc:
                obc_str += "\n\tDESC '{}'".format(obc['desc'])
            if 'sup' in obc:
                sup = " $ ".join(obc['sup'])
                obc_str += "\n\tSUP ( {} )".format(sup)
            obc_str += "\n\t{}".format(obc['kind'])
            if 'must' in obc:
                must = " $ ".join(obc['must'])
                obc_str += "\n\tMUST ( {} )".format(must)
            if 'may' in obc:
                may = " $ ".join(obc['may'])
                obc_str += "\n\tMAY ( {} )".format(may)
            if 'x_origin' in obc:
                obc_str += "\n\tX-ORIGIN '{}'".format(obc['x_origin'])
            obc_str += " )\n\n"

            self.outString += obc_str

        return self.outString.strip()

    def _getOID(self, model):
        oid = model['oid']

        if oid.replace('.','').isdigit():
            return oid

        oid = self.macroMap[oid] + '.' + str(self.macroMapIndex[oid])
        self.macroMapIndex[model['oid']] += 1

        return oid

    def generate_ldif(self):
        """Function which generates the OpenDJ LDIF format schema string."""
        self.outString = ''
        self.outString += self.header if self.header else ""
        if len(self.outString):
            self.outString += "\n"
        self.outString += "dn: cn=schema\nobjectClass: top\nobjectClass: " \
            + "ldapSubentry\nobjectClass: subschema\ncn: schema\n"

        for attr in self.data['attributeTypes']:
            attr_str = "attributeTypes: ( {} NAME ".format(self._getOID(attr))
            if len(attr['names']) > 1:
                namestring = ''
                for name in attr['names']:
                    namestring += "'{}' ".format(name)
                attr_str += "( {})".format(namestring)
            elif len(attr['names']) == 1:
                attr_str += "'{}'".format(attr['names'][0])
            else:
                print("Invalid attribute data. Doesn't define a name", attr)
            if 'desc' in attr:
                attr_str += "\n  DESC '{}'".format(attr['desc'])
            if 'equality' in attr:
                attr_str += "\n  EQUALITY {}".format(attr['equality'])
            if 'substr' in attr:
                attr_str += "\n  SUBSTR {}".format(attr['substr'])
            if 'syntax' in attr:
                attr_str += "\n  SYNTAX {}".format(attr['syntax'])
            if 'ordering' in attr:
                attr_str += "\n  ORDERING {}".format(attr['ordering'])
            if 'x_origin' in attr:
                attr_str += "\n  X-ORIGIN '{}'".format(attr['x_origin'])
            attr_str += " )\n"

            self.outString += attr_str

        for obc in self.data['objectClasses']:
            obc_str = "objectClasses: ( {} NAME ".format(self._getOID(obc))
            if len(obc['names']) > 1:
                namestring = ''
                for name in obc['names']:
                    namestring += "'{}' ".format(name)
                obc_str += "( {})".format(namestring)
            elif len(obc['names']) == 1:
                obc_str += "'{}'".format(obc['names'][0])
            else:
                print("Invalid objectclass data. Doesn't define a name", obc)
            if 'desc' in obc:
                obc_str += "\n  DESC '{}'".format(obc['desc'])
            if 'sup' in obc:
                sup = " $ ".join(obc['sup'])
                obc_str += "\n  SUP ( {} )".format(sup)
            obc_str += "\n  {}".format(obc['kind'])
            if 'must' in obc:
                must = " $ ".join(obc['must'])
                obc_str += "\n  MUST ( {} )".format(must)
            if 'may' in obc:
                may = " $ ".join(obc['may'])
                obc_str += "\n  MAY ( {} )".format(may)
            if 'x_origin' in obc:
                obc_str += "\n  X-ORIGIN '{}'".format(obc['x_origin'])
            obc_str += " )\n"

            self.outString += obc_str

        # Remove excess spaces and a new line at the end of the file
        self.outString = self.outString.strip() + '\n\n'
        return self.outString
