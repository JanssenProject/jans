#!/usr/bin/env python3
"""
manager.py - Script which acts as the user interface for schema management.

"""

import argparse
import json
import os

from schema_parser import LDAPSchemaParser
from generator import SchemaGenerator

localdir = os.path.dirname(os.path.abspath(__file__))


def generate(infile, schema_type=None, out_file=None):
    """Function generates the LDAP schema definitions from the JSON data

    Args:
        schema_type (str): The schema type to be generated (opendj)
    """
    fp = open(infile, 'r')
    json_text = fp.read()
    fp.close()
    gen = SchemaGenerator(json_text)
    if schema_type == 'opendj':
        schema_str = gen.generate_ldif()
    else:
        schema_str = gen.generate_schema()
    if out_file:
        with open(out_file, 'w') as w:
            w.write(schema_str)
    else:
        print(schema_str)


def autogenerate():
    """Function that generates the LDAP schemas for OpenDJ from the
    gluu_schema.json and custom_schema.json and puts them in their respective
    folders.
    """
    opendj_folder = os.path.join(os.path.dirname(localdir), 'static/opendj/')

    fp = open(os.path.join(localdir, 'gluu_schema.json'), 'r')
    gluu_json = fp.read()
    fp.close()
    gen = SchemaGenerator(gluu_json)
    with open(os.path.join(opendj_folder, '101-ox.ldif'), 'w') as f:
        f.write(gen.generate_ldif())

    fp = open(os.path.join(localdir, 'custom_schema.json'), 'r')
    custom_json = fp.read()
    fp.close()
    gen = SchemaGenerator(custom_json)
    with open(os.path.join(opendj_folder, '77-customAttributes.ldif'), 'w') \
            as f:
        f.write(gen.generate_ldif())


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
    schema_dict['oidMacros'] = definitions['oidMacros']
    print(json.dumps(schema_dict, indent=4, sort_keys=True))


def make_schema_docs():
    schema = os.path.join(localdir, 'gluu_schema.json')
    f = open(schema)
    json_string = f.read()
    f.close()
    data = json.loads(json_string)
    objClasses = data['objectClasses']
    attTypes = data['attributeTypes']
    docs = ''

    for obj_class in objClasses:
        docs += "\n\n## {}".format(" (or) ".join(obj_class['names']))
        if 'desc' in obj_class:
            docs += "\n_{}_".format(obj_class['desc'].encode('utf-8'))

        for obj_attr in obj_class['may']:
            attr_docs_added = False
            for attr_type in attTypes:
                if obj_attr in attr_type['names']:
                    docs += "\n* __{}__".format(" (or) ".join(attr_type['names']))
                    if 'desc' in attr_type:
                        docs += ":  {}".format(attr_type['desc'].encode('utf-8'))
                    attr_docs_added = True
                    break
            if not attr_docs_added:
                docs += "\n* __{}__".format(obj_attr)
    print(docs)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "action", help="the action you want to perform.",
        choices=["autogenerate", "generate", "makejson", "makedocs", "test"])
    parser.add_argument(
        "--type", help="the schema type you want to generate",
        choices=["opendj"])
    parser.add_argument(
        "--filename", help="the input file for various actions")
    args = parser.parse_args()

    if args.action == 'generate':
        if args.filename:
            generate(args.filename, args.type)
        else:
            print("No JSON Input. Specify a JSON file with --filename")
    elif args.action == 'test':
        run_tests()
    elif args.action == 'makejson':
        if args.filename:
            make_json(args.filename)
        else:
            print("No Schema Input. Specify schema file with --filename")
    elif args.action == 'autogenerate':
        autogenerate()
    elif args.action == 'makedocs':
        make_schema_docs()
