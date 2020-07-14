import copy

from ldap3.utils import dn as dnutils
from setup_app.pylib.ldif4.ldif import LDIFParser
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

        if rd[0] == 'o' and rd[1] == 'gluu':
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

                    if not 'gluu' in oc.lower() and len(oc_list) > 1:
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


