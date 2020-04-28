import os
import json
import glob

# Currently we implement only three data types: string, boolean, integer, datetime
syntaxType = {
                '1.3.6.1.4.1.1466.115.121.1.7': 'boolean',
                '1.3.6.1.4.1.1466.115.121.1.27': 'integer',
                '1.3.6.1.4.1.1466.115.121.1.24': 'datetime',
              }
# other syntaxes are treated as string

# This function was used to gather data types from opendj schema files
# collected types were dumped to opendj_types.json


class ATTRUBUTEDATATYPES:

    def __init__(self, installDir=None):
        if installDir:
            self.startup(installDir)

    def startup(self, installDir):
        self.installDir = installDir
        opendjTypesFn = os.path.join(self.installDir, 'schema/opendj_types.json')
        self.attribTypes = json.load(open(opendjTypesFn))

        for v in syntaxType.values():
            if not v in self.attribTypes:
                self.attribTypes[v] = []

        if 'json' not in self.attribTypes:
            self.attribTypes['json'] = []

        
        self.processGluuSchema()

    def processGluuSchema(self):

        gluuSchemaFn = os.path.join(self.installDir, 'schema/gluu_schema.json')
        gluuSchema = json.load(open(gluuSchemaFn))
        gluuAtrribs = gluuSchema['attributeTypes']

        for attrib in gluuAtrribs:
            if attrib.get('json'):
                atype = 'json'
            elif  attrib['syntax'] in syntaxType:
                atype = syntaxType[attrib['syntax']]
            else:
                atype = 'string'
                
            for name in attrib['names']:
                self.attribTypes[atype].append(name)

    def getAttribDataType(self, attrib):
        for atype in self.attribTypes:
            if attrib in self.attribTypes[atype]:
                return atype

        return 'string'
