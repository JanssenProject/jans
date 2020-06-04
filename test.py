from setup_app import paths
from setup_app import static
from setup_app.utils import base
import copy
import json

from setup_app.config import Config
Config.install_dir = '/mnt/data/projects/gluu/community-edition-setup'


from setup_app.utils import base
from setup_app.utils.cbm import CBM




parser = base.myLdifParser('/tmp/configuration.ldif')
parser.parse()

keyx, entry = parser.entries[0]

key, document = base.get_document_from_entry(keyx, entry)

n1ql = 'UPSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s)' % ('gluus', key, json.dumps(document))
cbm=CBM('c1.gluu.org', 'admin', 'Top!Secret-20')

result = cbm.exec_query(n1ql)
