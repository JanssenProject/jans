import ldap3
import json
import sys
import os
import argparse

from collections import OrderedDict

parser = argparse.ArgumentParser(description="Configuration script")
parser.add_argument('-f', help="Don't prompt", action='store_true')
parser.add_argument('l', help="Application name", choices=['jans-auth', 'jans-fido2', 'jans-scim'])
parser.add_argument('-g', help="Get property value")
parser.add_argument('-s', help="Set property value")
parser.add_argument('-d', help="Delete property value")


argsp = parser.parse_args()


if not (argsp.d or argsp.s or argsp.g):
    print("Nothing to do.")
    sys.exit()

if argsp.s:
    if not ':' in argsp.s:
        print("Specify -s property:value")
        sys.exit()
    n = argsp.s.find(':')
    confvar = argsp.s[:n].strip()
    confval = argsp.s[n+1:].strip()
else:
    confvar = argsp.g or argsp.d

if not argsp.f:
    if argsp.d:
        q = "This script will delete {} from cofiguration of {}. Continune (y/N)? ".format(argsp.d, argsp.l)
    elif argsp.s:        
        q = "This script will set value of {} to {} from cofiguration of {}. Continune (y/N)? ".format(confvar, confval, argsp.l)
    if not argsp.g:
        confirm = input(q)
        if not confirm or confirm[0].lower() != 'y':
            print("Bye ...")
            sys.exit()

gluu_ldap_prop_fn = '/etc/jans/conf/jans-ldap.properties'

for l in open(gluu_ldap_prop_fn):
    if l.startswith('bindPassword'):
        n = l.find(':')
        passwd_enc = l[n+1:].strip()
        ldap_admin_pw = os.popen('/opt/jans/bin/encode.py -D ' + passwd_enc).read().strip()
        break
else:
    print("Can't find ldap admin password")


server = ldap3.Server("ldaps://localhost:1636", use_ssl=True)
conn = ldap3.Connection(server, user="cn=directory manager", password=ldap_admin_pw)
conn.bind()

app_base_dn = 'ou={},ou=configuration,o=jans'.format(argsp.l)
conn.search(search_base=app_base_dn, search_scope=ldap3.BASE, search_filter='(objectclass=*)', attributes=['jansConfDyn'])

if conn.response and 'jansConfDyn' in conn.response[0]['attributes']:
    jansConfDyn = json.loads(conn.response[0]['attributes']['jansConfDyn'][0], object_pairs_hook=OrderedDict)
else:
    print("Can't load jansConfDyn")
    sys.exit()

if not confvar in jansConfDyn:
    print("jansConfDyn does not have property {}".format(confvar))
    sys.exit()

print("Current value of property {} is {}".format(confvar, jansConfDyn[confvar]))

if (argsp.d or argsp.s):
    if argsp.d:
        jansConfDyn.pop(confvar)
    else:
        jansConfDyn[confvar] = confval

    conn.modify(app_base_dn, {'jansConfDyn': [ldap3.MODIFY_REPLACE, json.dumps(jansConfDyn, indent=2)]})

    print("Property {} of {} was set to {}".format(confvar, argsp.l, confval))
