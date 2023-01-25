import copy
import os
import json

from collections import OrderedDict

from ldap3.utils import dn as dnutils
from setup_app.pylib.ldif4.ldif import LDIFParser, LDIFWriter
from setup_app.pylib.schema import AttributeType, ObjectClass
from setup_app.utils.attributes import attribDataTypes
from setup_app.config import Config


class myLdifParser(LDIFParser):
    def __init__(self, ldif_file):
        self.ldif_file = ldif_file
        self.entries = []

    def parse(self):
        with open(self.ldif_file, 'rb') as f:
            parser = LDIFParser(f)
            for dn, entry in parser.parse():
                for e in entry:
                    for i, v in enumerate(entry[e][:]):
                        if isinstance(v, bytes):
                            entry[e][i] = v.decode('utf-8')
                self.entries.append((dn, entry))


def get_key_from(dn):
    dns = []
    for rd in dnutils.parse_dn(dn):

        if rd[0] == 'o' and rd[1] == 'jans':
            continue
        dns.append(rd[1])

    dns.reverse()
    key = '_'.join(dns)

    if not key:
        key = '_'

    return key


def get_document_from_entry(dn, entry):

    document = copy.deepcopy(entry)

    if len(document) > 2:
        key = get_key_from(dn)
        document['dn'] = dn
        for k in document:
            if len(document[k]) == 1:
                if not k in attribDataTypes.listAttributes:
                    document[k] = document[k][0]

        for k in document:
            dtype = attribDataTypes.getAttribDataType(k)
            if dtype != 'string':
                if type(document[k]) == type([]):
                    for i in range(len(document[k])):
                        document[k][i] = attribDataTypes.getTypedValue(dtype, document[k][i])
                        if document[k][i] == 'true':
                            document[k][i] = True
                        elif document[k][i] == 'false':
                            document[k][i] = False
                else:
                    document[k] = attribDataTypes.getTypedValue(dtype, document[k])

            if k == 'objectClass':
                document[k].remove('top')
                oc_list = document[k]

                for oc in oc_list[:]:
                    if 'Custom' in oc and len(oc_list) > 1:
                        oc_list.remove(oc)

                    if not 'jans' in oc.lower() and len(oc_list) > 1:
                        oc_list.remove(oc)

                document[k] = oc_list[0]

        return key, document

def get_documents_from_ldif(ldif_file):
    parser = myLdifParser(ldif_file)
    parser.parse()
    documents = []

    for dn, entry in parser.entries:
        key, document = get_document_from_entry(dn, entry)

        documents.append((key, document))

    return documents


def schema2json(schema_file, out_dir=None):

    ldif_parser = myLdifParser(schema_file)
    ldif_parser.parse()

    jans_schema = OrderedDict((('attributeTypes',[]), ('objectClasses',[])))

    if 'attributeTypes' in ldif_parser.entries[0][1]:
        for attr_str in ldif_parser.entries[0][1]['attributeTypes']:
            attr_type = AttributeType(attr_str)

            attr_dict = {
                      "desc": attr_type.tokens['DESC'][0],
                      "equality": attr_type.tokens['EQUALITY'][0],
                      "names": list(attr_type.tokens['NAME']),
                      "multivalued": False,
                      "oid": attr_type.oid,
                      "syntax": attr_type.tokens['SYNTAX'][0],
                      "x_origin": attr_type.tokens['X-ORIGIN'][0]
                    }

            if 'X-RDBM-ADD' in attr_type.tokens and attr_type.tokens['X-RDBM-ADD'][0]:
                attr_dict['sql'] = {'add_table': attr_type.tokens['X-RDBM-ADD'][0]}

            jans_schema['attributeTypes'].append(attr_dict)

    if 'objectClasses' in ldif_parser.entries[0][1]:
        for objcls_str in ldif_parser.entries[0][1]['objectClasses']:
            objcls_type = ObjectClass(objcls_str)

            obj_class_dict = {
                      "kind": "AUXILIARY",
                      "may": list(objcls_type.tokens['MAY']),
                      "names": list(objcls_type.tokens['NAME']),
                      "oid": objcls_type.oid,
                      "sup": list(objcls_type.tokens['SUP']),
                      "x_origin": objcls_type.tokens['X-ORIGIN'][0]
                    }

            if 'X-RDBM-IGNORE' in objcls_type.tokens:
                if objcls_type.tokens['X-RDBM-IGNORE'][0].lower() == 'true':
                    obj_class_dict['sql'] = {"ignore": True}

            jans_schema['objectClasses'].append(obj_class_dict)

    path, fn = os.path.split(schema_file)
    if not out_dir:
        out_dir = path

    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    name, ext = os.path.splitext(fn)
    out_file = os.path.join(out_dir, name + '.json')

    schema_str = json.dumps(jans_schema, indent=2)
    with open(out_file, 'w') as w:
        w.write(schema_str)

def create_client_ldif(ldif_fn, client_id, encoded_pw, scopes, redirect_uri, trusted_client='false'):
    clients_ldif_fd = open(ldif_fn, 'wb')
    ldif_clients_writer = LDIFWriter(clients_ldif_fd, cols=1000)
    client_dn = 'inum={},ou=clients,o=jans'.format(client_id)

    ldif_clients_writer.unparse(
        client_dn, {
        'objectClass': ['top', 'jansClnt'],
        'del': ['false'],
        'displayName': ['Jans Config Api Client'],
        'inum': [client_id],
        'jansAccessTknAsJwt': ['false'],
        'jansAccessTknSigAlg': ['RS256'],
        'jansAppTyp': ['web'],
        'jansAttrs': ['{"tlsClientAuthSubjectDn":"","runIntrospectionScriptBeforeJwtCreation":false,"keepClientAuthorizationAfterExpiration":false,"allowSpontaneousScopes":false,"spontaneousScopes":[],"spontaneousScopeScriptDns":[],"backchannelLogoutUri":[],"backchannelLogoutSessionRequired":false,"additionalAudience":[],"postAuthnScripts":[],"consentGatheringScripts":[],"introspectionScripts":[],"rptClaimsScripts":[]}'],
        'jansClntSecret': [encoded_pw],
        'jansDisabled': ['false'],
        'jansGrantTyp': ['authorization_code', 'refresh_token', 'client_credentials'],
        'jansIdTknSignedRespAlg': ['RS256'],
        'jansInclClaimsInIdTkn': ['false'],
        'jansLogoutSessRequired': ['false'],
        'jansPersistClntAuthzs': ['true'],
        'jansRespTyp': ['code'],
        'jansRptAsJwt': ['false'],
        'jansScope': scopes,
        'jansSubjectTyp': ['pairwise'],
        'jansTknEndpointAuthMethod': ['client_secret_basic'],
        'jansTrustedClnt': [trusted_client],
        'jansRedirectURI': redirect_uri
        })

    clients_ldif_fd.close()
