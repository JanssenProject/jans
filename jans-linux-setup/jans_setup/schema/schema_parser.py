#!/usr/bin/env python3

"""Module containing the functions to parse the LDAP schema files.
"""

import sys
import re
import logging
from pathlib import Path
my_path = Path(__file__)
sys.path.append(my_path.parent.parent.joinpath('jans_setup/pylib').as_posix())
from schema import ObjectClass, AttributeType

class LDAPSchemaParser(object):
    def __init__(self, filename):
        self.filename = filename
        self.objClasses = []
        self.attrTypes = []
        self.macros = {}

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

    def __getMacroDefinitions(self, text):
        """Parses a block of text and built a dict of the OID macros as they
        are definied in the schema file"""
        lines = text.split('\n')
        macros = {}
        for line in lines:
            if re.match('^objectIdentifier', line):
                keyword, macro, definition = line.split()
                macros[macro] = definition
        return macros

    def __parseSchema(self, expand_oid_macros=False):
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
                self.macros = self.__getMacroDefinitions(block)
            elif re.match('^objectclass', block):
                block = block.replace('\n', ' ').replace('\t', ' ').strip()
                obj = ObjectClass(block[obj_len:])
                # Extra parsing to get the X-ORIGIN values as the python-ldap
                # parser isn't parsing the value for objectClasses
                if 'X-ORIGIN' in block:
                    waste, originstr = block.split('X-ORIGIN')
                    parts = originstr.strip().split('\'')
                    obj.x_origin = parts[1]
                self.objClasses.append(obj)
            elif re.match('^attributetype', block):
                block = block.replace('\n', ' ').replace('\t', ' ').strip()
                att = AttributeType(block[att_len:])
                self.attrTypes.append(att)

        if expand_oid_macros:
            error_msg = "You requested for the expansion of OID macros." \
                + " The definition for macro `{}` was  not found." \
                + " Storing it without expansion."
            for obj in self.objClasses:
                if ':' in obj.oid:
                    macro, index = obj.oid.split(':')
                    try:
                        obj.oid = oid_macros[macro] + '.' + index
                    except KeyError as e:
                        logging.warning(error_msg, macro)
                        logging.debug(e, exc_info=True)

            for att in self.attrTypes:
                if ':' in att.oid:
                    macro, index = att.oid.split(':')
                    try:
                        att.oid = oid_macros[macro] + '.' + index
                    except KeyError as e:
                        logging.warning(error_msg, macro)
                        logging.debug(e, exc_info=True)

    def __parseLDIF(self):
        """Parser for .ldif files, like the one from OpenDJ"""
        pass

    def parse(self, expand_oid_macros=False):
        """Function to parse the LDAP Schema File and collect the information

        Args:
            expand_oid_macros (bool) - Set to true of you want to expand
                OpenLDAP style OID macros to be expanded to dot seperated int
                format. Default False, just copies the OID text.

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
                        ...],
                'oidMacros': {
                        'macro_1': 'OID definition',
                        'macro_2': 'OID definition',
                        ...
                    }
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
                'attributeTypes': self.attrTypes,
                'oidMacros': self.macros}
