import os

if not os.path.exists('setup.py'):
    print "This script should be run from /install/community-edition-setup/"
    sys.exit()
    
if not os.path.exists('/install/community-edition-setup/setup.properties.last'):
    print "setup.properties.last is missing can't continue"
    sys.exit()

f=open('setup.py').readlines()

for l in f:
    if l.startswith('from pyDes import *'):
        break
else:
    f.insert(30, 'from pyDes import *\n')
    with open('setup.py','w') as w:
        w.write(''.join(f))

from setup import *

installObject = Setup( os.path.dirname(os.path.realpath(__file__)))



installObject.load_properties('setup.properties.last')

if installObject.ldap_type == 'opendj':
    installObject.createLdapPw()


installObject.encode_test_passwords()
installObject.generate_passport_configuration()
installObject.generate_scim_configuration()

installObject.prepare_base64_extension_scripts()

installObject.render_templates()
installObject.render_test_templates()
installObject.loadTestData()

if installObject.ldap_type == 'opendj':
    installObject.deleteLdapPw()
