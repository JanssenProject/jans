#!/usr/bin/python3

import os
import sys
from datetime import datetime
import ldap3
import argparse

if not os.path.exists('ldif.py'):
    print("Please download https://raw.githubusercontent.com/abilian/ldif/master/ldif.py to current directory.")
    sys.exit()

from ldif import LDIFWriter

parser = argparse.ArgumentParser(description='Removes user entries that have been auto-enrolled by means of passport. This script allows to remove or deactivate entries corresponding to users that have not logged-in after a specified date.')

required = parser.add_argument_group('required arguments')

parser.add_argument('-remove', help="Remove people otherwise mark as inactive", action='store_true')
parser.add_argument('--provider-only', help="Remove people only for specified provider ID as registered in passport's provider form. Example: Github")
parser.add_argument('--log-file', help="Log file", default='remove.log')
required.add_argument('--last-logon', help="Remove people whose last logon time is earlier than the provided value. Example: 2016-04-27",required=True )

argsp = parser.parse_args()

try:
    last_logon = datetime.strptime(argsp.last_logon, '%Y-%m-%d').strftime('%Y%m%d%H%M%S.000Z')
except:
    print("Invalid date")
    sys.exit()


def read_properties_file(fn):
    retDict = {}
    with open(fn) as f:
        for l in f:
            ls = l.strip()
            if ls and ls[0] != '#':
                nc = ls.find(':')
                ne = ls.find('=')
                n = 0
                if nc > 0 and ne < 0:
                    n = nc
                elif ne > 0 and nc < 0:
                    n = ne
                elif ne < nc:
                    n = ne
                elif nc < ne:
                    n = nc
                if n > 0:
                    k = ls[:n].strip()
                    v = ls[n+1:].strip()
                    v = v.replace('\\=','=').replace("\\'","'").replace('\\"','"')
                    retDict[k] = v

    return retDict

def decode_password(encpw):
    return os.popen('/opt/gluu/bin/encode.py -D ' + encpw).read().strip()


ldap_prop_fn = '/etc/gluu/conf/gluu-ldap.properties'

if not os.path.exists(ldap_prop_fn):
    print(ldap_prop_fn, "is not found")

ldap_prop = read_properties_file(ldap_prop_fn)

search_filter = '(&(oxExternalUid=passport-*)(!(memberOf=inum=60B7,ou=groups,o=gluu))(oxLastLogonTime<={}))'.format(last_logon)
server = ldap3.Server("ldaps://{}".format(ldap_prop['servers'].split(',')[0].strip()))
conn = ldap3.Connection(server, user=ldap_prop['bindDN'], password=decode_password(ldap_prop['bindPassword']))
conn.bind()

conn.search(search_base='ou=people,o=gluu', search_filter=search_filter, search_scope=ldap3.LEVEL, attributes=['oxExternalUid', 'oxLastLogonTime', 'gluuStatus'])
result = conn.response

log = open(argsp.log_file, 'ab')
ldif_writer = LDIFWriter(log)

for e in result:
    for oxExternalUid in e['attributes']['oxExternalUid']:
        oxExternalUid_sp = oxExternalUid[9:].split(':')
        if not oxExternalUid_sp[-1]:
            continue

        if argsp.provider_only:
            loc_prov = oxExternalUid_sp[1] if oxExternalUid_sp[0] == 'saml' else oxExternalUid_sp[0]
            if loc_prov == argsp.provider_only:
                break
        else:
            break
    else:
        continue

    if argsp.remove:
        conn.search(search_base=e['dn'], search_filter='(objectClass=*)', search_scope=ldap3.SUBTREE, attributes=['*'])
        conn.response.reverse()
        for se in conn.response:
            conn.delete(se['dn']) 
            log.write('#DELETING\n'.encode())
            ldif_writer.unparse(se['dn'], dict(se['attributes']))
            conn.delete(se['dn'])
    else:
        if e['attributes']['gluuStatus'][0] == 'active':
            log.write('INACTIVATING {}\n'.format(e['dn']).encode())
            conn.modify(e['dn'],   {'gluuStatus': [ldap3.MODIFY_REPLACE, 'inactive'],
                                    'oxTrustActive': [ldap3.MODIFY_REPLACE, 'false'],
                                    })

log.close()
