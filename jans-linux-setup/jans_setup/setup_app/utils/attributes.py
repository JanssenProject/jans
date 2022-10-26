import os
import datetime
import zipfile
from setup_app import paths
from setup_app.utils import base

# Currently we implement only three data types: string, boolean, integer, datetime
syntaxType = {
                '1.3.6.1.4.1.1466.115.121.1.7': 'boolean',
                '1.3.6.1.4.1.1466.115.121.1.27': 'integer',
                '1.3.6.1.4.1.1466.115.121.1.24': 'datetime',
              }
# other syntaxes are treated as string

# This function was used to gather data types from opendj schema files
# collected types were dumped to opendj_types.json


class AttribDataTypes:

    listAttributes = ['member']
    attribTypes = {}

    def __init__(self):
        opendjTypesFn = os.path.join(paths.INSTALL_DIR, 'schema/opendj_types.json')
        self.attribTypes = base.readJsonFile(opendjTypesFn)

        for v in syntaxType.values():
            if not v in self.attribTypes:
                self.attribTypes[v] = []

        if 'json' not in self.attribTypes:
            self.attribTypes['json'] = []

        self.processJansSchema()

    def processJansSchema(self):

        jansSchemaFn = os.path.join(paths.INSTALL_DIR, 'schema/jans_schema.json')
        jansSchema = base.readJsonFile(jansSchemaFn)
        jansAtrribs = jansSchema['attributeTypes']

        for attrib in jansAtrribs:
            if attrib.get('json'):
                atype = 'json'
            elif  attrib['syntax'] in syntaxType:
                atype = syntaxType[attrib['syntax']]
            else:
                atype = 'string'
                
            for name in attrib['names']:
                self.attribTypes[atype].append(name)

        for obj_type in ['objectClasses', 'attributeTypes']:
            for obj in jansSchema[obj_type]:
                if obj.get('multivalued'):
                    for name in obj['names']:
                        if not name in self.listAttributes:
                            self.listAttributes.append(name)


    def getAttribDataType(self, attrib):
        for atype in self.attribTypes:
            if attrib in self.attribTypes[atype]:
                return atype

        return 'string'

    def getTypedValue(self, dtype, val):
        retVal = val
        
        if dtype == 'json':
            try:
                retVal = json.loads(val)
            except Exception as e:
                pass

        if dtype == 'integer':
            try:
                retVal = int(retVal)
            except:
                pass
        elif dtype == 'datetime':
            if not isinstance(val, datetime.datetime):

                if '.' in val:
                    date_format = '%Y%m%d%H%M%S.%fZ'
                else:
                    date_format = '%Y%m%d%H%M%SZ'

                if not val.lower().endswith('z'):
                    val += 'Z'
                
                val = datetime.datetime.strptime(val, date_format)

            retVal = val.strftime('%Y-%m-%dT%H:%M:%S.%f')

        elif dtype == 'boolean':
            if not isinstance(retVal, bool):
                if retVal.lower() in ('true', 'yes', '1', 'on'):
                    retVal = True
                else:
                    retVal = False

        return retVal

attribDataTypes = AttribDataTypes()
