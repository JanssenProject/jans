#####################################################
# ! NOTE! Under development, not use for production #
#####################################################

##################
#   passportSpJksPass has no usage
#
#
##################


import os
import json
import zipfile
import re
import sys
import base64
import platform
import glob

from urlparse import urlparse
from pyDes import triple_des, ECB, PAD_PKCS5

if os.path.exists('/etc/yum.repos.d/'):
    package_type = 'rpm'
elif os.path.exists('/etc/apt/sources.list'):
    package_type = 'deb'

missing_packages = []

needs_restart = False
dev_env = True if os.environ.get('update_dev') else False

try:
    import ldap
except:
    missing_packages.append('python-ldap')

try:
    import requests
except:
    missing_packages.append('python-requests')

if missing_packages:
    needs_restart = True
    packages_str = ' '.join(missing_packages)
    result = raw_input("Missing package(s): {0}. Install now? (Y|n): ".format(packages_str))
    if result.strip() and result.strip().lower()[0] == 'n':
        sys.exit("Can't continue without installing these packages. Exiting ...")
            

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

if not os.path.exists('Properties.py'):
    os.system('wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/pylib/Properties.py')

if not os.path.exists('cbm.py'):
    os.system('wget https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/pylib/cbm.py')


if needs_restart:
    python_ = sys.executable
    os.execl(python_, python_, * sys.argv)


from ldap.dn import explode_dn, str2dn, dn2str
import ldap

from cbm import CBM
import Properties

ldap.set_option(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_ALLOW)

setup_prop = Properties.Properties()

def read_properties_file(fn):
    prop = Properties.Properties()
    with open(fn) as f:
        prop.load(f)
    return prop

salt_prop = read_properties_file('/etc/gluu/conf/salt')
salt = salt_prop['encodeSalt']

def unobscure(s=""):
    engine = triple_des(salt, ECB, pad=None, padmode=PAD_PKCS5)
    cipher = triple_des(salt)
    decrypted = cipher.decrypt(base64.b64decode(s), padmode=PAD_PKCS5)
    return decrypted


def get_ssl_subject(ssl_fn):
    retDict = {}
    cmd = 'openssl x509  -noout -subject -nameopt RFC2253 -in {}'.format(ssl_fn)
    s = os.popen(cmd).read()
    s = s.strip() + ','

    for k in ('emailAddress', 'CN', 'O', 'L', 'ST', 'C'):
        rex = re.search('{}=(.*?),'.format(k), s)
        retDict[k] = rex.groups()[0] if rex else ''

    return retDict

def get_key_from(dn):
    dns = []
    for d in str2dn(dn):
        for rd in d:
            if rd[0] == 'o' and rd[1] == 'gluu':
                continue
            dns.append(rd[1])

    dns.reverse(),
    key = '_'.join(dns)

    if not key:
        key = '_'

    return key

def get_cb_result(n1ql):
    result = cbm.exec_query(n1ql)
    if result.ok:
        data = result.json()
        return data.get('results')


default_storage = 'ldap'
setup_prop['persistence_type'] = 'ldap'
setup_prop['encode_salt'] = salt
mappingLocations = {'default': 'ldap', 'token': 'ldap', 'cache': 'ldap', 'user': 'ldap', 'site': 'ldap'}


#Determine gluu version
war_zip = zipfile.ZipFile('/opt/gluu/jetty/oxauth/webapps/oxauth.war', 'r')
menifest = war_zip.read('META-INF/MANIFEST.MF')

for l in menifest.splitlines():
    ls = l.strip()
    n = ls.find(':')

    if ls[:n].strip() == 'Implementation-Version':
        gluu_version_str = ls[n+1:].strip()
        gluu_version_list = gluu_version_str.split('.')

        if not gluu_version_list[-1].isdigit():
            gluu_version_list.pop(-1)

        gluu_version = '.'.join(gluu_version_list)

print "Current Gluu Version is determined as", gluu_version

gluu_3x = '.'.join(gluu_version.split('.')[:2]) < '4.0'

if gluu_3x:
    gluu_ldap_prop = read_properties_file('/etc/gluu/conf/ox-ldap.properties')
    oxauth_ConfigurationEntryDN = gluu_ldap_prop['oxauth_ConfigurationEntryDN']
    oxtrust_ConfigurationEntryDN = gluu_ldap_prop['oxtrust_ConfigurationEntryDN']
    oxidp_ConfigurationEntryDN = gluu_ldap_prop['oxidp_ConfigurationEntryDN']
    gluu_ConfigurationDN = ','.join(oxauth_ConfigurationEntryDN.split(',')[2:])
    inum_org_str = oxauth_ConfigurationEntryDN.split(',')[2]
else:
    gluu_prop = read_properties_file('/etc/gluu/conf/gluu.properties')
    setup_prop['persistence_type'] = gluu_prop['persistence.type']
    oxauth_ConfigurationEntryDN = gluu_prop['oxauth_ConfigurationEntryDN']
    oxtrust_ConfigurationEntryDN = gluu_prop['oxtrust_ConfigurationEntryDN']
    oxidp_ConfigurationEntryDN = gluu_prop['oxidp_ConfigurationEntryDN']
    gluu_ConfigurationDN = 'ou=configuration,o=gluu'

    if setup_prop['persistence_type'] == 'couchbase':
        mappingLocations = {'default': 'couchbase', 'token': 'couchbase', 'cache': 'couchbase', 'user': 'couchbase', 'site': 'couchbase'}
        default_storage = 'couchbase'

    if setup_prop['persistence_type'] in ('ldap', 'hybrid'):

        gluu_ldap_prop_fn = '/etc/gluu/conf/gluu-ldap.properties'
        if os.path.exists(gluu_ldap_prop_fn):
            gluu_ldap_prop = read_properties_file(gluu_ldap_prop_fn)
        
    if setup_prop['persistence_type'] != 'ldap':
        gluu_cb_prop_fn = '/etc/gluu/conf/gluu-couchbase.properties'
        if os.path.exists(gluu_cb_prop_fn):
            gluu_cb_prop = read_properties_file(gluu_cb_prop_fn)
            
            setup_prop['couchebaseClusterAdmin'] = gluu_cb_prop['auth.userName']
            setup_prop['encoded_cb_password'] = gluu_cb_prop['auth.userPassword']
            setup_prop['cb_password'] = unobscure(setup_prop['encoded_cb_password'])

            setup_prop['couchbase_bucket_prefix'] = gluu_cb_prop['bucket.default']

            setup_prop['couchbase_hostname'] = gluu_cb_prop['servers'].split(',')[0].strip()
            setup_prop['couchbaseTrustStorePass'] = unobscure(gluu_cb_prop['ssl.trustStore.pin'])

            cbm = CBM(setup_prop['couchbase_hostname'], setup_prop['couchebaseClusterAdmin'], setup_prop['cb_password'])
            cb_who = cbm.whoami()
            if cb_who.get('roles'):
                for rd in cb_who['roles']:
                    for r in rd:
                        if r == 'role' and rd[r] == 'admin':
                            setup_prop['isCouchbaseUserAdmin'] = 'true'
                            break

if gluu_version < '4.1.0':
    jetty_services = {
        'oxauth':    ('installOxAuth', 0.3, 0.7),
        'identity':  ('installOxTrust', 0.2),
        'idp':       ('installSaml', 0.2),
        'oxauth-rp': ('installOxAuthRP', 0.1),
        'passport':  ('installPassport', 0.1),
    }
else:
    jetty_services = {
        'oxauth':    ('installOxAuth', 0.2, 0.7),
        'identity':  ('installOxTrust', 0.25),
        'idp':       ('installSaml', 0.25),
        'oxauth-rp': ('installOxAuthRP', 0.1),
        'casa':      ('installCasa', 0.1),
        'passport':  ('installPassport', 0.1),
    }

if setup_prop['persistence_type'] != 'couchbase':
    setup_prop['ldap_binddn'] = gluu_ldap_prop['bindDN']
    setup_prop['ldapPass'] = unobscure(gluu_ldap_prop['bindPassword'])
    setup_prop['opendj_p12_pass'] = unobscure(gluu_ldap_prop['ssl.trustStorePin'])
    setup_prop['ldap_hostname'], setup_prop['ldaps_port']  = gluu_ldap_prop['servers'].split(',')[0].split(':')

    ldap_conn = ldap.initialize('ldaps://{0}:1636'.format(setup_prop['ldap_hostname']))
    ldap_conn.simple_bind_s(setup_prop['ldap_binddn'], setup_prop['ldapPass'])



if gluu_3x:
    result = ldap_conn.search_s('o=gluu',ldap.SCOPE_ONELEVEL)
    inumOrg = str(result[1][1]['o'][0])
    uma_rpt_policy_inum = '{}!0011!2DAF.F995'.format(inumOrg)
    scim_access_policy_inum = '{}!0011!2DAF-F9A5'.format(inumOrg)
else:
    uma_rpt_policy_inum = '2DAF-F995'
    scim_access_policy_inum = '2DAF-F9A5'

    if default_storage == 'ldap':

        try:
            result = ldap_conn.search_s('ou=oxradius,ou=configuration,o=gluu',ldap.SCOPE_BASE)
            setup_prop['installGluuRadius'] = 'true'
            setup_prop['gluu_radius_client_id'] = result[0][1]['oxRadiusOpenidUsername'][0]
            setup_prop['gluu_ro_pw'] = unobscure(result[0][1]['oxRadiusOpenidPassword'][0])
        except:
            pass

        try:
            result = ldap_conn.search_s('inum=5866-4202,ou=scripts,o=gluu',ldap.SCOPE_BASE, attrlist=['oxEnabled'])
            if result[0][1]['oxEnabled'][0].lower() == 'true':
                setup_prop['enableRadiusScripts'] = 'true' 
        except:
            pass

        result = ldap_conn.search_s('ou=clients,o=gluu',ldap.SCOPE_SUBTREE, '(inum=1402.*)', ['inum'])
        if result:
            setup_prop['oxtrust_requesting_party_client_id'] = result[0][1]['inum'][0]

    elif default_storage == 'couchbase':
        n1ql = 'SELECT * from `{}` USE KEYS "configuration_oxradius"'.format(setup_prop['couchbase_bucket_prefix'])
        result = get_cb_result(n1ql)
        if result:
            setup_prop['installGluuRadius'] = 'true'
            setup_prop['gluu_radius_client_id'] = str(result[0]['gluu']['oxRadiusOpenidUsername'])
            setup_prop['gluu_ro_pw'] = unobscure(result[0]['gluu']['oxRadiusOpenidPassword'])
        
        n1ql = 'SELECT oxEnabled from `{}` USE KEYS "scripts_5866-4202"'.format(setup_prop['couchbase_bucket_prefix'])
        result = get_cb_result(n1ql)
        if result and result[0]['oxEnabled']:
            setup_prop['enableRadiusScripts'] = 'true' 

        n1ql =  'SELECT inum from `{}` WHERE objectClass="oxAuthClient" AND inum LIKE "1402.%"'.format(setup_prop['couchbase_bucket_prefix'])
        result = get_cb_result(n1ql)
        if result:
            setup_prop['oxtrust_requesting_party_client_id'] = str(result[0]['inum'])

admin_dn = None
if mappingLocations['user'] == 'ldap':
    result = ldap_conn.search_s('o=gluu',ldap.SCOPE_SUBTREE, '(gluuGroupType=gluuManagerGroup)', ['member'])
    if result and result[0][1]['member']:
        admin_dn = result[0][1]['member'][0]


if mappingLocations['user'] == 'couchbase':
    bucket = '{}_user'.format(setup_prop['couchbase_bucket_prefix'])
    n1ql = 'SELECT * from `{}` where objectClass="gluuGroup" and gluuGroupType="gluuManagerGroup"'.format(bucket)
    result = get_cb_result(n1ql)
    if result and result[0][bucket]['member']:
       admin_dn = result[0][bucket]['member'][0]

if admin_dn:
    for d in str2dn(admin_dn):
        for rd in d:
            if rd[0] == 'inum':
                setup_prop['admin_inum'] = str(rd[1])
                break

oxTrustConfApplication = None
oxConfApplication = None
oxAuthConfDynamic = None
oxAuthConfDynamic = None

if default_storage == 'ldap':

    result = ldap_conn.search_s(gluu_ConfigurationDN, ldap.SCOPE_BASE,'(objectClass=*)')
    setup_prop['ip'] = str(result[0][1]['gluuIpAddress'][0])
    oxCacheConfiguration = json.loads(result[0][1]['oxCacheConfiguration'][0])

    setup_prop['cache_provider_type'] = str(oxCacheConfiguration['cacheProviderType'])

    result = ldap_conn.search_s(oxidp_ConfigurationEntryDN, ldap.SCOPE_BASE,'(objectClass=oxApplicationConfiguration)', ['oxConfApplication'])
    oxConfApplication = json.loads(result[0][1]['oxConfApplication'][0])

    result = ldap_conn.search_s(oxauth_ConfigurationEntryDN, ldap.SCOPE_BASE,'(objectClass=oxAuthConfiguration)', ['oxAuthConfDynamic'])
    oxAuthConfDynamic = json.loads(result[0][1]['oxAuthConfDynamic'][0])

    result = ldap_conn.search_s(oxtrust_ConfigurationEntryDN, ldap.SCOPE_BASE,'(objectClass=oxTrustConfiguration)', ['oxTrustConfApplication'])
    oxTrustConfApplication = json.loads(result[0][1]['oxTrustConfApplication'][0])


elif default_storage == 'couchbase':
    bucket = setup_prop['couchbase_bucket_prefix']

    s_key = get_key_from(gluu_ConfigurationDN)
    n1ql = 'SELECT * FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
    result = get_cb_result(n1ql)
    if result:
        setup_prop['ip'] = str(result[0][bucket]['gluuIpAddress'])
        setup_prop['cache_provider_type'] = str(result[0][bucket]['oxCacheConfiguration']['cacheProviderType'])

    s_key = get_key_from(oxidp_ConfigurationEntryDN)
    n1ql = 'SELECT oxConfApplication FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
    result = get_cb_result(n1ql)
    if result:
        oxConfApplication = result[0]['oxConfApplication']

    s_key = get_key_from(oxauth_ConfigurationEntryDN)
    n1ql = 'SELECT oxAuthConfDynamic FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
    result = get_cb_result(n1ql)
    if result:        
        oxAuthConfDynamic = result[0]['oxAuthConfDynamic']

    s_key = get_key_from(oxtrust_ConfigurationEntryDN)
    n1ql = 'SELECT oxTrustConfApplication FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
    result = get_cb_result(n1ql)
    if result:
        oxTrustConfApplication = result[0]['oxTrustConfApplication']
    
if oxTrustConfApplication:
    if 'apiUmaClientId' in oxTrustConfApplication:
        setup_prop['oxtrust_resource_server_client_id'] =  str(oxTrustConfApplication['apiUmaClientId'])


    if 'apiUmaClientKeyStorePassword' in oxTrustConfApplication:
        setup_prop['api_rs_client_jks_pass'] =  str(unobscure(oxTrustConfApplication['apiUmaClientKeyStorePassword']))

    if 'apiUmaResourceId' in oxTrustConfApplication:
        setup_prop['oxtrust_resource_id'] =  str(oxTrustConfApplication['apiUmaResourceId'])

    setup_prop['shibJksPass'] =  str(unobscure(oxTrustConfApplication['idpSecurityKeyPassword']))
    setup_prop['admin_email'] =  str(oxTrustConfApplication['orgSupportEmail'])
    setup_prop['orgName'] =  str(oxTrustConfApplication['organizationName'])
    setup_prop['oxauth_client_id'] =  str(oxTrustConfApplication['oxAuthClientId'])
    setup_prop['oxauthClient_pw'] = str(unobscure(oxTrustConfApplication['oxAuthClientPassword']))
    setup_prop['scim_rs_client_id'] =  str(oxTrustConfApplication['scimUmaClientId'])
    setup_prop['scim_resource_oxid'] =  str(oxTrustConfApplication['scimUmaResourceId'])
    setup_prop['scimTestMode'] =  str(oxTrustConfApplication['scimTestMode'])

    if 'apiUmaClientKeyStorePassword' in oxTrustConfApplication:
        setup_prop['api_rp_client_jks_pass'] = unobscure(oxTrustConfApplication['apiUmaClientKeyStorePassword'])
        setup_prop['api_rs_client_jks_fn'] = str(oxTrustConfApplication['apiUmaClientKeyStoreFile'])

    if 'scimUmaClientKeyStorePassword' in oxTrustConfApplication:
        setup_prop['scim_rs_client_jks_pass'] = unobscure(oxTrustConfApplication['scimUmaClientKeyStorePassword'])
        setup_prop['scim_rs_client_jks_fn'] = str(oxTrustConfApplication['scimUmaClientKeyStoreFile'])


if oxConfApplication:
    setup_prop['idpClient_pw'] =  str(unobscure(oxConfApplication['openIdClientPassword']))
    setup_prop['idp_client_id'] =  str(oxConfApplication['openIdClientId'])


if oxAuthConfDynamic:
    o_issuer = urlparse(oxAuthConfDynamic['issuer'])
    setup_prop['hostname'] = str(o_issuer.netloc)
    setup_prop['pairwiseCalculationSalt'] =  str(oxAuthConfDynamic['pairwiseCalculationSalt'])
    setup_prop['oxauth_openidScopeBackwardCompatibility'] =  str(oxAuthConfDynamic.get('openidScopeBackwardCompatibility', False)).lower()
    setup_prop['oxauth_legacyIdTokenClaims'] = str(oxAuthConfDynamic['legacyIdTokenClaims']).lower()
    setup_prop['pairwiseCalculationKey'] = str(oxAuthConfDynamic['pairwiseCalculationKey'])
    setup_prop['oxauth_openid_jks_fn'] = str(oxAuthConfDynamic['keyStoreFile'])
    setup_prop['oxauth_openid_jks_pass'] = str(oxAuthConfDynamic['keyStoreSecret'])


ssl_subj = get_ssl_subject('/etc/certs/httpd.crt')
setup_prop['countryCode'] = ssl_subj['C']
setup_prop['state'] = ssl_subj['ST']
setup_prop['city'] = ssl_subj['L']

for service in jetty_services:
    if os.path.exists('/opt/gluu/jetty/{0}/webapps/{0}.war'.format(service)):
        setup_prop[jetty_services[service][0]] = 'true'

if setup_prop['installSaml']:
    setup_prop['gluuSamlEnabled'] = 'true'

if os.path.exists('/opt/gluu/node/passport/server'):
    setup_prop['installPassport'] = 'true'

application_max_ram = 3072

default_dir = '/etc/default'
usedRatio = 0.001
oxauth_max_heap_mem = 0

for service in jetty_services:
    service_default_fn = os.path.join(default_dir, service)
    if os.path.exists(service_default_fn):
        usedRatio += jetty_services[service][1]
        if service == 'oxauth':
            service_prop = read_properties_file(service_default_fn)
            m = re.search('-Xmx(\d*)m', service_prop['JAVA_OPTIONS'])
            oxauth_max_heap_mem = int(m.groups()[0])

if oxauth_max_heap_mem:
    ratioMultiplier = 1.0 + (1.0 - usedRatio)/usedRatio
    applicationMemory = oxauth_max_heap_mem / jetty_services['oxauth'][2]
    allowedRatio = jetty_services['oxauth'][1] * ratioMultiplier
    application_max_ram = int(round(applicationMemory / allowedRatio))

setup_prop['application_max_ram'] = str(application_max_ram)

if os.path.exists(os.path.join(default_dir, 'gluu-radius')):
    setup_prop['gluuRadiusEnabled'] = 'true'
    setup_prop['oxauth_openidScopeBackwardCompatibility'] = 'true'

p = platform.linux_distribution()
setup_prop['os_type'] = p[0].split()[0].lower()
setup_prop['os_version'] = p[1].split('.')[0]

https_gluu_fn = '/etc/httpd/conf.d/https_gluu.conf' if setup_prop['os_type'] in ('red', 'fedora', 'centos') else '/etc/apache2/sites-available/https_gluu.conf'
setup_prop['installHTTPD'] = str(os.path.exists(https_gluu_fn)).lower()

setup_prop['mappingLocations'] = json.dumps(mappingLocations)

setup_prop_fn = 'setup.properties.last'

if os.path.exists(setup_prop_fn):
    flist = glob.glob(setup_prop_fn+'.*')
    n = len(flist) + 1
    os.rename(setup_prop_fn, setup_prop_fn+'.'+str(n))

with open(setup_prop_fn, 'w') as w:
    setup_prop.store(w)

print "{} is written successfully".format(setup_prop_fn)

#for p in setup_prop.keys():
#    print p,":", setup_prop[p]

