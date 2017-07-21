#!/usr/bin/python

import subprocess
from getpass import *

def get_line(s):
    cmd = ['/bin/grep', s, "clients.ldif"]
    result = subprocess.check_output(cmd).strip()
    return result

d = {}
dn = get_line('dn')
d['base'] = ",".join(dn.split(',')[1:])
d['scopes']= get_line('oxAuthScope')

print
d['inum'] = raw_input("Enter client id: ")
raw_secret = raw_input("Enter client secret: ")
d['callback'] = raw_input("Enter redirect uri: ")
d['displayName'] = raw_input("Enter client display name: ")
print 

cmd = ['/opt/gluu/bin/encode.py', raw_secret]
d['secret'] = subprocess.check_output(cmd).strip()

ldif = """dn: inum=%(inum)s,%(base)s
objectClass: top
objectClass: oxAuthClient
displayName: %(displayName)s
inum: %(inum)s
oxAuthClientSecret: %(secret)s
oxAuthAppType: web
oxAuthResponseType: code
oxAuthGrantType: authorization_code
oxAuthGrantType: refresh_token
oxAuthRedirectURI: %(callback)s
oxAuthTokenEndpointAuthMethod: client_secret_basic
oxAuthIdTokenSignedResponseAlg: HS256
oxAuthTrustedClient: true
oxAuthSubjectType: public
oxPersistClientAuthorizations: false
oxAuthLogoutSessionRequired: true
%(scopes)s

"""

fn = "new_client_%s.ldif" % d['inum']
f = open(fn, 'w')
f.write(ldif % d)
f.close()
print "Wrote file %s" %fn

add_it = raw_input("Add entry to ldap? [Y|n] ") or 'y'
if add_it[0].lower() == 'y': 
    pw = getpass("Enter LDAP directory manager password: ")
    cmd = ['/opt/opendj/bin/ldapmodify',
       '-h', 
       'localhost', 
       '-p', 
       '1636', 
       '-Z', 
       '-X', 
       '-D', 
       'cn=directory manager,o=gluu', 
       '-w', 
       pw, 
       '-a', 
       '-f', 
       fn]
    result = subprocess.check_output(cmd)
    if result.find('ADD operation successful') > 0: 
        print "Added client %s" % d['inum']

