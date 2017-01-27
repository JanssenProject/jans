#!/usr/bin/env python

"""Module containing the functions to parse the LDAP schema files.
"""

import re

from ldap.schema.models import ObjectClass, AttributeType


class LDAPSchemaParser(object):
    def __init__(self, filename):
        self.filename = filename
        self.objClasses = []
        self.attrTypes = []

    def __parseOIDMacros(self, text):
        """Parses a block of text and build a dict containing OID macros with
        their dot seperated integer notations
        """
        lines = text.split('\n')
        macros = {}
        for line in lines:
            if re.match('^objectIdentifier', line):
                keyword, macro, oid = line.split()
                if ':' in oid:
                    parent, index = oid.split(':')
                    parent_oid = macros[parent]
                    oid = parent_oid + '.' + index
                macros[macro] = oid
        return macros

    def __parseSchema(self):
        """Parser for .schema files, like the one from OpenLDAP"""
        obj_str = 'objectclass'
        obj_len = len(obj_str)
        att_str = 'attributetype'
        att_len = len(att_str)
        oid_macros = {}

        f = open(self.filename, 'r')
        blocks = f.read().split('\n\n')

        for block in blocks:
            block = block.strip()
            if 'objectIdentifier' in block:
                oid_macros = self.__parseOIDMacros(block)
            elif re.match('^objectclass', block):
                block = block.replace('\n', ' ').replace('\t', ' ').strip()
                obj = ObjectClass(block[obj_len:])
                self.objClasses.append(obj)
            elif re.match('^attributetype', block):
                block = block.replace('\n', ' ').replace('\t', ' ').strip()
                att = AttributeType(block[att_len:])
                self.attrTypes.append(att)

        for obj in self.objClasses:
            if ':' in obj.oid:
                macro, index = obj.oid.split(':')
                obj.oid = oid_macros[macro] + '.' + index

        for att in self.attrTypes:
            if ':' in att.oid:
                macro, index = att.oid.split(':')
                att.oid = oid_macros[macro] + '.' + index

    def __parseLDIF(self):
        """Parser for .ldif files, like the one from OpenDJ"""
        pass

    def parse(self):
        """Function to parse the LDAP Schema File and collect the information

        Returns:
            dict: A dictionary containing all the AttributeType and ObjectClass
            definitions in the format below:

                {
                 'ojectClasses': [
                        <obj of type ldap.schema.models.ObjectClass>,
                        <obj of type ldap.schema.models.ObjectClass>,
                        ...],
                 'attributeTypes': [
                        <obj of type ldap.schema.models.AttributeType>,
                        <obj of type ldap.schema.models.AttributeType>,
                        ...]
                }
        """
        with open(self.filename, 'r') as ldapfile:
            for line in ldapfile:
                if 'objectClasses: ' in line or 'attributeTypes: ' in line:
                    self.__parseLDIF()
                    break
                elif 'objectclass ' in line or 'attributetype ' in line:
                    self.__parseSchema()
                    break
        return {'objectClasses': self.objClasses,
                'attributeTypes': self.attrTypes}

