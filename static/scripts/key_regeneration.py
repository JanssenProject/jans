# Please place this script in the same directory as setup.py

import os
import shutil
import glob
import json
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET

defaul_storage = 'ldap'

conf_dir = '/etc/gluu/conf'
gluu_hybrid_roperties_fn = os.path.join(conf_dir, 'gluu-hybrid.properties')
gluu_couchbase_roperties_fn = os.path.join(conf_dir, 'gluu-couchbase.properties')
gluu_ldap_roperties_fn = os.path.join(conf_dir, 'gluu-ldap.properties')
ox_ldap_roperties_fn = os.path.join(conf_dir, 'ox-ldap.properties')

keystore_fn = 'oxauth-keys.jks'
oxauth_keys_json_fn = 'oxauth-keys.json'

if os.path.exists('/etc/yum.repos.d/'):
    package_type = 'rpm'
elif os.path.exists('/etc/apt/sources.list'):
    package_type = 'deb'

def backup_file(fn):
    if os.path.exists(fn):
        file_list = glob.glob(fn+'.*')
        n = len(file_list) + 1
        shutil.move(fn, fn+'.'+str(n))

def run_command(args):
    if type(args) == type([]):
        cmd = ' '.join(args)
    else:
        cmd = args
    print "Executing command", cmd
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
    result = p.communicate()
    return result

missing_packages = []

try:
    import ldap
except:
    missing_packages.append('python-ldap')

if missing_packages:
    packages_str = ' '.join(missing_packages)
    result = raw_input("Missing package(s): {0}. Install now? (Y|n): ".format(packages_str))
    if result.strip() and result.strip().lower()[0] == 'n':
        print "Can't continue without installing these packages. Exiting ..."
        sys.exit(False)

    if package_type == 'rpm':
        cmd = 'yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm'
        os.system(cmd)
        cmd = 'yum clean all'
        os.system(cmd)
        cmd = "yum install -y {0}".format(packages_str)
    else:
        os.system('apt-get update')
        cmd = "apt-get install -y {0}".format(packages_str)

    print "Installing package(s) with command: "+ cmd
    os.system(cmd)


if missing_packages:
    python_ = sys.executable
    os.execl(python_, python_, * sys.argv)


ldap.set_option(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_ALLOW)

backup_file(keystore_fn)
backup_file(oxauth_keys_json_fn)

if os.path.exists(gluu_hybrid_roperties_fn):

    for l in open(gluu_hybrid_roperties_fn):
        ls = l.strip()
        if ls.startswith('storage.default'):
            n = ls.find(':')
            defaul_storage = ls[n+1:].strip()

elif os.path.exists(gluu_couchbase_roperties_fn):
    defaul_storage = 'couchbase'

print "Obtaining keystore passwrod and determining algorithms"

if defaul_storage == 'ldap':
    prop_fn = gluu_ldap_roperties_fn if os.path.exists(gluu_ldap_roperties_fn) else ox_ldap_roperties_fn
    # Obtain ldap binddn, server and password
    for l in open(prop_fn):
        if l.startswith('bindPassword'):
            crypted_passwd = l.split(':')[1].strip()
            ldap_password = os.popen('/opt/gluu/bin/encode.py -D {}'.format(crypted_passwd)).read().strip()
        elif l.startswith('servers'):
            ls = l.strip()
            n = ls.find(':')
            s = ls[n+1:].strip()
            servers_s = s.split(',')
            ldap_server = servers_s[0].strip()
        elif l.startswith('bindDN'):
            ldap_binddn = l.split(':')[1].strip()

    # Enable custom script oxtrust_api_access_policy
    ldap_conn = ldap.initialize('ldaps://{}'.format(ldap_server))
    ldap_conn.simple_bind_s(ldap_binddn, ldap_password)

    result = ldap_conn.search_s('o=gluu', ldap.SCOPE_SUBTREE, '(objectClass=oxAuthConfiguration)', ['oxAuthConfDynamic','oxAuthConfWebKeys', 'oxRevision'])

    dn = result[0][0]

    oxAuthConfDynamic = json.loads(result[0][1]['oxAuthConfDynamic'][0])
    keyStoreSecret = oxAuthConfDynamic['keyStoreSecret']
    try:
        oxAuthConfWebKeys = json.loads(result[0][1]['oxAuthConfWebKeys'][0])
    except:
        oxAuthConfWebKeys = None
    oxRevision = 1
    try:
        oxRevision = result[0][1]['oxRevision'][0]
    except:
        pass
else:
    # Obtain couchbase credidentals
    for l in open(gluu_couchbase_roperties_fn):
        ls = l.strip()
        n = ls.find(':')
        if ls.startswith('servers'):
            server = ls[n+1:].strip().split(',')[0].strip()
        elif ls.startswith('auth.userName'):
            userName = ls[n+1:].strip()
        elif ls.startswith('auth.userPassword'):
            userPasswordEnc = ls[n+1:].strip()
            userPassword = os.popen('/opt/gluu/bin/encode.py -D {}'.format(userPasswordEnc)).read().strip()

    from pylib.cbm import CBM

    cbm = CBM(server, userName, userPassword)
    result = cbm.exec_query('select * from gluu USE KEYS "configuration_oxauth"')

    if result.ok:
        configuration_oxauth = result.json()
        keyStoreSecret = configuration_oxauth['results'][0]['gluu']['oxAuthConfDynamic']['keyStoreSecret']
        oxAuthConfWebKeys = configuration_oxauth['results'][0]['gluu']['oxAuthConfWebKeys']
        oxRevision = configuration_oxauth['results'][0]['gluu']['oxRevision']
    else:
        print "Couchbase server responded unexpectedly", result.text

if oxAuthConfWebKeys:
    #Determine current algs
    key_algs = [ wkey['alg'] for wkey in oxAuthConfWebKeys['keys'] ]
else:
    print "Keys does not exist on the server, using defaults"
    key_algs = ['RS256', 'RS384', 'RS512', 'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512', 'RSA1_5', 'RSA-OAEP']

oxRevision = int(oxRevision) + 1

print "Creating oxauth-keys.jks"
# Create oxauth-keys.jks
args = ['/opt/jre/bin/keytool', '-genkey',
        '-alias', 'dummy',
        '-keystore', keystore_fn,
        '-storepass', keyStoreSecret,
        '-keypass', keyStoreSecret,
        '-dname', '"CN=oxAuth CA Certificates"'
        ]

output = run_command(args)


print "Determining oxauth key generator path"
# Determine oxauth key generator path
try:
    oxauth_client_jar_fn = max(list(glob.iglob('/home/jetty/lib/oxauth-client-*.jar')))
except:
    print "Can't find oxauth-client jar file. Exiting..."
    sys.exit(False)


oxauth_client_jar_zf = zipfile.ZipFile(oxauth_client_jar_fn)
for fn in oxauth_client_jar_zf.namelist():
    if fn.endswith('KeyGenerator.class'):
        fp, ext = os.path.splitext(fn)
        key_gen_path = fp.replace('/','.')
        break
else:
    print "Can't determine oxauth-client KeyGenerator path. Exiting..."
    sys.exit(False)

print "Determining version and vendor_id"
#Determine version and vendor_id
war_zip = zipfile.ZipFile('/opt/gluu/jetty/oxauth/webapps/oxauth.war', 'r')
menifest = war_zip.read('META-INF/MANIFEST.MF')

for l in menifest.splitlines():
    ls = l.strip()
    n = ls.find(':')
    if ls.startswith('Implementation-Version:'):
        gluu_ver = ls[n+1:].strip()
    elif ls.startswith('Implementation-Vendor-Id:'):
        vendor_id = ls[n+1:].strip()

vendor = vendor_id.split('.')[-1]

print "Downloading oxauth-client with dependencies"
# Download oxauth-client with dependencies
oxauth_client_url = 'https://ox.gluu.org/maven/org/{0}/oxauth-client/{1}/oxauth-client-{1}-jar-with-dependencies.jar'.format(vendor, gluu_ver)
oxauth_client_fn = os.path.basename(oxauth_client_url)
oxauth_client_fn_path = os.path.join('/home/jetty/lib', oxauth_client_fn)
args = ['wget', '-nv',oxauth_client_url, '-O', oxauth_client_fn_path]
output = run_command(args)


# Delete current keys
args = [ '/opt/jre/bin/keytool', '-delete',
        '-alias dummy', '-keystore', keystore_fn,
        '-storepass', keyStoreSecret,
        '-keypass', keyStoreSecret,
        '-dname', '"CN=oxAuth CA Certificates"'
        ]

output = run_command(args)

if output[1]:
    print "ERROR:", output[1]


print "Genereting keys"

ver_tmp_list = gluu_ver.split('.')
if not ver_tmp_list[-1].isdigit():
    ver_tmp_list.pop()
gluu_ver_real = '.'.join(ver_tmp_list)

#Generete keys
args = ['/opt/jre/bin/java', '-Dlog4j.defaultInitOverride=true',
    '-cp', oxauth_client_fn_path, key_gen_path,
    '-keystore oxauth-keys.jks',
    '-keypasswd', keyStoreSecret]


if gluu_ver_real < '3.1.2':
    args += ['-algorithms', ' '.join(key_algs)]
else:
    args += ['-sig_keys', ' '.join(key_algs), '-enc_keys', ' '.join(key_algs)]
    
args += ['-dnname', "'CN=oxAuth CA Certificates'",
    '-expiration', '365','>', oxauth_keys_json_fn]

output = run_command(args)

oxauth_keys_json = open(oxauth_keys_json_fn).read()

keystore_fn_gluu = os.path.join('/etc/certs', keystore_fn)
backup_file(keystore_fn_gluu)
shutil.copy(keystore_fn, keystore_fn_gluu)

output = run_command(['chown', 'jetty:jetty', keystore_fn_gluu])

print "Validating ... "

args = ['/opt/jre/bin/keytool', '-list', '-v',
        '-keystore', keystore_fn,
        '-storepass', keyStoreSecret,
        '|', 'grep', '"Alias name:"'
        ]
cmd = ' '.join(args)
print cmd

output = run_command(args)

jsk_aliases = []
for l in output[0].splitlines():
    ls = l.strip()
    n = ls.find(':')
    alias_name = ls[n+1:].strip()
    jsk_aliases.append(alias_name)

json_aliases = []

with open(oxauth_keys_json_fn) as f:
    oxauth_keys_json = json.load(f)

json_aliases = [ wkey['kid'] for wkey in oxauth_keys_json['keys'] ]

valid1 = True
for alias_name in json_aliases:
    if not alias_name in jsk_aliases:
        print keystore_fn, "does not contain", alias_name
        valid1 = False

valid2 = True
for alias_name in jsk_aliases:
    if not alias_name in json_aliases:
        print oxauth_keys_json_fn, "does not contain", alias_name
        valid2 = False

if valid1 and valid2:
    print "Content of {} and {} matches".format(oxauth_keys_json_fn, keystore_fn)
else:
    print "Validation failed, not updating db"
    sys.exit(1)

print "Updating oxAuthConfWebKeys in db"

with open(oxauth_keys_json_fn) as f:
    oxauth_oxAuthConfWebKeys = f.read()

if defaul_storage == 'ldap':
    ldap_conn.modify_s(dn, [    ( ldap.MOD_REPLACE, 'oxAuthConfWebKeys',  oxauth_oxAuthConfWebKeys),
                                ( ldap.MOD_REPLACE, 'oxRevision',  str(oxRevision))
                            ])
else:
    result = cbm.exec_query("update gluu USE KEYS 'configuration_oxauth' set gluu.oxAuthConfWebKeys='{}'".format(oxauth_oxAuthConfWebKeys))
    result = cbm.exec_query("update gluu USE KEYS 'configuration_oxauth' set gluu.oxRevision={}".format(oxRevision))
print "Please exit container and restart Gluu Server"
