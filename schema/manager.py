#!/usr/bin/env python
"""
manager.py - Script which acts as the user interface for schema management.

"""

import argparse
import json

from schema_parser import LDAPSchemaParser


def generate(schema_type=None):
    """Function generates the LDAP schema definitions from the JSON data

    Args:
        schmea_type (str): The schema type to be generated (openldap, opendj)
    """
    # TODO
    pass


def run_tests():
    """Function that runs the unit tests of the scripts in this package.
    """
    # TODO
    pass


def make_json(filename):
    """Function that parses the input schema file and generates JSON.
    """
    parser = LDAPSchemaParser(filename)
    definitions = parser.parse()
    schema_dict = {}
    objectclasses = []
    attributetypes = []
    for obj in definitions['objectClasses']:
        obcl = {}
        props = ['oid', 'names', 'desc', 'must', 'may', 'sup', 'x_origin']
        for prop in props:
            if hasattr(obj, prop):
                if getattr(obj, prop):
                    obcl[prop] = getattr(obj, prop)
        # obcl['obsolete'] = obj.obsolete
        if obj.kind == 0:
            obcl['kind'] = 'STRUCTURAL'
        elif obj.kind == 1:
            obcl['kind'] = 'ABSTRACT'
        elif obj.kind == 2:
            obcl['kind'] = 'AUXILIARY'
        objectclasses.append(obcl)

    for att in definitions['attributeTypes']:
        attype = {}
        props = ['oid', 'names', 'desc', 'equality', 'substr', 'ordering',
                 'syntax', 'x_origin']
        for prop in props:
            if hasattr(att, prop):
                if getattr(att, prop):
                    attype[prop] = getattr(att, prop)
        # attype['no_user_mod'] = att.no_user_mod
        # attype['single_value'] = att.single_value
        # attype['obsolete'] = att.obsolete
        attributetypes.append(attype)

    schema_dict['objectClasses'] = objectclasses
    schema_dict['attributeTypes'] = attributetypes
    print json.dumps(schema_dict, indent=4, sort_keys=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "action", help="the action you want to perform.",
        choices=["generate", "makejson", "test"])
    parser.add_argument(
        "--type", help="the schema type you want to generate",
        choices=["openldap", "opendj"])
    parser.add_argument(
        "--filename", help="the schema file to generate JSON."
        " Required for argument {makejson}")
    args = parser.parse_args()

    if args.action == 'generate':
        generate(args.type)
    elif args.action == 'test':
        run_tests()
    elif args.action == 'makejson':
        if args.filename:
            make_json(args.filename)
        else:
            print "No Schema Input. Specify schema file with --file <filename>"
