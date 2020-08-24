#!/usr/bin/python3
from __future__ import print_function

import os
import json
import zipfile
import re
import sys
import base64
import platform
import glob

cur_dir = os.path.dirname(os.path.realpath(__file__))

try:
    import pyDes
except:
    pyDes_fn = os.path.join(cur_dir, 'pyDes.py')
    os.system('wget -nv https://raw.githubusercontent.com/twhiteman/pyDes/master/pyDes.py -O {}'.format(pyDes_fn)) 

from pyDes import triple_des, ECB, PAD_PKCS5

if ((3, 0) <= sys.version_info <= (3, 9)):
    from urllib.parse import urlparse
elif ((2, 0) <= sys.version_info <= (2, 9)):
    from urlparse import urlparse

p = platform.linux_distribution()
os_type = p[0].split()[0].lower()
os_version = p[1].split('.')[0]


if os.path.exists('/etc/yum.repos.d/'):
    package_type = 'rpm'
elif os.path.exists('/etc/apt/sources.list'):
    package_type = 'deb'

missing_packages = []

needs_restart = False
dev_env = True if os.environ.get('update_dev') else False

try:
    import ldap3
except:
    missing_packages.append('python-ldap3')

try:
    import requests
except:
    missing_packages.append('python-requests')

if sys.version_info.major == 3:
    raw_input = input
    for i in range(len(missing_packages)):
        missing_packages[i] = missing_packages[i].replace('python', 'python3')

if missing_packages:
    needs_restart = True

    result = raw_input("Missing package(s): {0}. Install now? (Y|n): ".format(' '.join(missing_packages)))
    if result.strip() and result.strip().lower()[0] == 'n':
        sys.exit("Can't continue without installing these packages. Exiting ...")

    if package_type == 'rpm':
        cmd = 'yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm'
        os.system(cmd)
        cmd = 'yum clean all'
        os.system(cmd)

        if 'python3-ldap' in missing_packages:
            cmd = 'yum install -y http://162.243.99.240/icrby8xcvbcv/python3-ldap/python3-ldap-3.1.0-5.el7.x86_64.rpm'
            print("Executing", cmd)
            os.system(cmd)
            missing_packages.remove('python3-ldap')

        if missing_packages:
            packages_str = ' '.join(missing_packages)
            cmd = "yum install -y {0}".format(packages_str)

    else:
        packages_str = ' '.join(missing_packages)
        os.system('apt-get update')
        
        if ('python3-ldap' in missing_packages) and (os_type=='ubuntu' and os_version=='16'):
            cmd_list = ('wget -nv http://162.243.99.240/icrby8xcvbcv/python3-ldap/python3-ldap_3.0.0-1_amd64.deb -O /tmp/python3-ldap_3.0.0-1_amd64.deb',
                        'dpkg -i /tmp/python3-ldap_3.0.0-1_amd64.deb',
                        'apt-get install -f -y',
                        )
            for cmd in cmd_list:
                print("Executing", cmd)
                os.system(cmd)
            missing_packages.remove('python3-ldap')

        cmd = "apt-get install -y {0}".format(packages_str)

    print("Installing package(s) with command: "+ cmd)
    os.system(cmd)


prop_path = os.path.join(cur_dir, 'jproperties.py')
if not os.path.exists(prop_path):
    os.system('wget -nv https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/pylib/jproperties.py -O ' + prop_path)

cbm_path = os.path.join(cur_dir, 'cbm.py')
if not os.path.exists(cbm_path):
    os.system('wget -nv https://raw.githubusercontent.com/GluuFederation/community-edition-setup/master/pylib/cbm.py -O' + cbm_path)

if needs_restart:
    python_ = sys.executable
    os.execl(python_, python_, * sys.argv)


import ldap3
from ldap3.utils import dn as dnutils


try:
    from .jproperties import Properties
except:
    from jproperties import Properties


def read_properties_file(fn):
    retDict = {}
    p = Properties()
    if os.path.exists(fn):
        with open(fn, 'rb') as f:
            p.load(f, 'utf-8')
      
        for k in p.keys():
            retDict[str(k)] = str(p[k].data)
            
    return retDict


salt_file = '/opt/tomcat/conf/salt'
if not os.path.exists(salt_file):
    salt_file = '/etc/gluu/conf/salt'

salt_prop = read_properties_file(salt_file)
salt = salt_prop['encodeSalt']

def unobscure(s=""):
    engine = triple_des(salt, ECB, pad=None, padmode=PAD_PKCS5)
    cipher = triple_des(salt)
    decrypted = cipher.decrypt(base64.b64decode(s), padmode=PAD_PKCS5)
    return decrypted.decode()


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

    for rd in dnutils.parse_dn(dn):
        if rd[0] == 'o' and rd[1] == 'gluu':
            continue
        dns.append(rd[1])

    dns.reverse()
    key = '_'.join(dns)

    if not key:
        key = '_'

    return key

def get_cb_result(cbm, n1ql):
    result = cbm.exec_query(n1ql)
    if result.ok:
        data = result.json()
        return data.get('results')

def generate_properties(as_dict=False):
    
    setup_prop = {}
    
    default_storage = 'ldap'
    setup_prop['persistence_type'] = 'ldap'
    setup_prop['encode_salt'] = salt
    mappingLocations = {'default': 'ldap', 'token': 'ldap', 'cache': 'ldap', 'user': 'ldap', 'site': 'ldap'}

    oxauth_file = '/opt/tomcat/webapps/oxauth.war'
    if not os.path.exists(oxauth_file):
        oxauth_file = '/opt/gluu/jetty/oxauth/webapps/oxauth.war'

    #Determine gluu version
    war_zip = zipfile.ZipFile(oxauth_file, 'r')
    menifest = war_zip.read('META-INF/MANIFEST.MF')

    for l in menifest.splitlines():
        ls = l.strip()
        if sys.version_info[0] > 2:
            ls = ls.decode('utf-8')
        n = ls.find(':')

        if ls[:n].strip() == 'Implementation-Version':
            gluu_version_str = ls[n+1:].strip()
            gluu_version_list = gluu_version_str.split('.')

            if not gluu_version_list[-1].isdigit():
                gluu_version_list.pop(-1)

            gluu_version = '.'.join(gluu_version_list)

    if __name__ == '__main__':
        print("Current Gluu Version is determined as", gluu_version)

    gluu_3x = '.'.join(gluu_version.split('.')[:2]) < '4.0'

    if gluu_3x:
        ox_ldap_prop_file = '/opt/tomcat/conf/ox-ldap.properties'
        if not os.path.exists(ox_ldap_prop_file):
            ox_ldap_prop_file = '/etc/gluu/conf/ox-ldap.properties'
        gluu_ldap_prop = read_properties_file(ox_ldap_prop_file)
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
        gluu_hybrid_properties_fn = '/etc/gluu/conf/gluu-hybrid.properties'

        if setup_prop['persistence_type'] == 'couchbase':
            mappingLocations = {'default': 'couchbase', 'token': 'couchbase', 'cache': 'couchbase', 'user': 'couchbase', 'site': 'couchbase'}
            default_storage = 'couchbase'


        if setup_prop['persistence_type'] in ['hybrid']:
            gluu_hybrid_properties = read_properties_file(gluu_hybrid_properties_fn)
            mappingLocations = {'default': gluu_hybrid_properties['storage.default']}
            storages = [ storage.strip() for storage in gluu_hybrid_properties['storages'].split(',') ]

            for ml, m in (('user', 'people'), ('cache', 'cache'), ('site', 'cache-refresh'), ('token', 'tokens')):
                for storage in storages:
                    if m in gluu_hybrid_properties.get('storage.{}.mapping'.format(storage),[]):
                        mappingLocations[ml] = storage

            default_storage = mappingLocations['default']

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
                setup_prop['encoded_couchbaseTrustStorePass'] = gluu_cb_prop['ssl.trustStore.pin']
                setup_prop['couchbaseTrustStorePass'] = unobscure(gluu_cb_prop['ssl.trustStore.pin'])


                if ((3, 0) <= sys.version_info <= (3, 9)):
                    from .cbm import CBM
                elif ((2, 0) <= sys.version_info <= (2, 9)):
                    from cbm import CBM

                cbm = CBM(setup_prop['couchbase_hostname'], setup_prop['couchebaseClusterAdmin'], setup_prop['cb_password'])
                cb_who = cbm.whoami()
                if cb_who.get('roles'):
                    for rd in cb_who['roles']:
                        for r in rd:
                            if r == 'role' and rd[r] == 'admin':
                                setup_prop['isCouchbaseUserAdmin'] = True
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
        try:
            setup_prop['opendj_p12_pass'] = unobscure(gluu_ldap_prop['ssl.trustStorePin'])
        except:
            pass
        setup_prop['ldap_hostname'], setup_prop['ldaps_port']  = gluu_ldap_prop['servers'].split(',')[0].split(':')


        ldap_server = ldap3.Server(setup_prop['ldap_hostname'], port=int(setup_prop['ldaps_port']), use_ssl=True)
        ldap_conn = ldap3.Connection(
                                ldap_server,
                                user=setup_prop['ldap_binddn'],
                                password=setup_prop['ldapPass'],
                                )

        ldap_conn.bind()


    if gluu_3x:
        ldap_conn.search(search_base='o=gluu', search_scope=ldap3.LEVEL, search_filter='(objectClass=*)', attributes=['*'])
        result = ldap_conn.response
        for entry in result:
            if 'gluuOrganization' in entry['attributes']['objectClass']:
                inumOrg = entry['attributes']['o'][0]
                uma_rpt_policy_inum = '{}!0011!2DAF.F995'.format(inumOrg)
                scim_access_policy_inum = '{}!0011!2DAF-F9A5'.format(inumOrg)


    else:
        uma_rpt_policy_inum = '2DAF-F995'
        scim_access_policy_inum = '2DAF-F9A5'

        if default_storage == 'ldap':

            ldap_conn.search(search_base='ou=oxradius,ou=configuration,o=gluu', search_scope=ldap3.BASE, search_filter='(objectClass=*)', attributes=['*'])
            result = ldap_conn.response
            if result:
                setup_prop['installGluuRadius'] = True

            ldap_conn.search(search_base='ou=clients,o=gluu', search_scope=ldap3.SUBTREE, search_filter='(inum=1701.*)', attributes=['*'])
            result = ldap_conn.response

            if result:
                setup_prop['gluu_radius_client_id'] = result[0]['attributes']['inum'][0]
                setup_prop['gluu_ro_encoded_pw'] = result[0]['attributes']['oxAuthClientSecret'][0]
                setup_prop['gluu_ro_pw'] = unobscure(setup_prop['gluu_ro_encoded_pw'])

            ldap_conn.search(search_base='inum=5866-4202,ou=scripts,o=gluu', search_scope=ldap3.BASE, search_filter='(objectClass=*)', attributes=['oxEnabled'])
            result = ldap_conn.response
            if result and result[0]['attributes']['oxEnabled'][0]:
                setup_prop['enableRadiusScripts'] = True

            ldap_conn.search(search_base='ou=clients,o=gluu', search_scope=ldap3.SUBTREE, search_filter='(inum=1402.*)', attributes=['inum'])
            result = ldap_conn.response
            if result:
                setup_prop['oxtrust_requesting_party_client_id'] = result[0]['attributes']['inum'][0]

        elif default_storage == 'couchbase':
            n1ql = 'SELECT * from `{}` USE KEYS "configuration_oxradius"'.format(setup_prop['couchbase_bucket_prefix'])
            result = get_cb_result(cbm, n1ql)
            if result:
                setup_prop['installGluuRadius'] = True
            
            n1ql =  'SELECT inum from `{}` WHERE objectClass="oxAuthClient" AND inum LIKE "1701.%"'.format(setup_prop['couchbase_bucket_prefix'])
            result = get_cb_result(cbm, n1ql)
            if result:
                setup_prop['gluu_radius_client_id'] = str(result[0]['inum'])
                if 'oxAuthClientSecret' in result[0]:
                    setup_prop['gluu_ro_encoded_pw'] = str(result[0]['oxAuthClientSecret'])
                    setup_prop['gluu_ro_pw'] = unobscure(setup_prop['gluu_ro_encoded_pw'])
            
            n1ql = 'SELECT oxEnabled from `{}` USE KEYS "scripts_5866-4202"'.format(setup_prop['couchbase_bucket_prefix'])
            result = get_cb_result(cbm, n1ql)
            if result and result[0]['oxEnabled']:
                setup_prop['enableRadiusScripts'] = True

            n1ql =  'SELECT inum from `{}` WHERE objectClass="oxAuthClient" AND inum LIKE "1402.%"'.format(setup_prop['couchbase_bucket_prefix'])
            result = get_cb_result(cbm, n1ql)
            if result:
                setup_prop['oxtrust_requesting_party_client_id'] = str(result[0]['inum'])

    admin_dn = None
    if mappingLocations['user'] == 'ldap':
        ldap_conn.search(search_base='o=gluu', search_scope=ldap3.SUBTREE, search_filter='(gluuGroupType=gluuManagerGroup)', attributes=['member'])
        result = ldap_conn.response
        if result and result[0]['attributes'].get('member'):
            admin_dn = result[0]['attributes']['member'][0]


    if mappingLocations['user'] == 'couchbase':
        bucket = '{}_user'.format(setup_prop['couchbase_bucket_prefix'])
        n1ql = 'SELECT * from `{}` where objectClass="gluuGroup" and gluuGroupType="gluuManagerGroup"'.format(bucket)
        result = get_cb_result(cbm, n1ql)
        if result and result[0][bucket]['member']:
           admin_dn = result[0][bucket]['member'][0]

    if admin_dn:
        for rd in dnutils.parse_dn(admin_dn):
            if rd[0] == 'inum':
                setup_prop['admin_inum'] = str(rd[1])
                break

    oxTrustConfApplication = None
    oxConfApplication = None
    oxAuthConfDynamic = None
    oxAuthConfDynamic = None

    if default_storage == 'ldap':

        ldap_conn.search(search_base=gluu_ConfigurationDN, search_scope=ldap3.BASE, search_filter='(objectClass=*)', attributes=['*'])
        result = ldap_conn.response
        if 'gluuIpAddress' in result[0]['attributes']:
            setup_prop['ip'] = str(result[0]['attributes']['gluuIpAddress'][0])

        try:
            oxCacheConfiguration = json.loads(result[0]['attributes']['oxCacheConfiguration'][0])
            setup_prop['cache_provider_type'] = str(oxCacheConfiguration['cacheProviderType'])
        except Exception as e:
            print("Error getting cache provider type", e)

        result = ldap_conn.search(search_base=oxidp_ConfigurationEntryDN, search_scope=ldap3.BASE, search_filter='(objectClass=oxApplicationConfiguration)', attributes=['oxConfApplication'])
        result = ldap_conn.response
        oxConfApplication = json.loads(result[0]['attributes']['oxConfApplication'][0])

        result = ldap_conn.search(search_base=oxauth_ConfigurationEntryDN, search_scope=ldap3.BASE, search_filter='(objectClass=oxAuthConfiguration)', attributes=['oxAuthConfDynamic'])
        result = ldap_conn.response
        oxAuthConfDynamic = json.loads(result[0]['attributes']['oxAuthConfDynamic'][0])

        result = ldap_conn.search(search_base=oxtrust_ConfigurationEntryDN, search_scope=ldap3.BASE, search_filter='(objectClass=oxTrustConfiguration)', attributes=['oxTrustConfApplication'])
        result = ldap_conn.response
        oxTrustConfApplication = json.loads(result[0]['attributes']['oxTrustConfApplication'][0])


    elif default_storage == 'couchbase':
        bucket = setup_prop['couchbase_bucket_prefix']

        s_key = get_key_from(gluu_ConfigurationDN)
        n1ql = 'SELECT * FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
        result = get_cb_result(cbm, n1ql)
        if result:
            if 'gluuIpAddress' in result[0][bucket]:
                setup_prop['ip'] = str(result[0][bucket]['gluuIpAddress'])
            
            setup_prop['cache_provider_type'] = str(result[0][bucket]['oxCacheConfiguration']['cacheProviderType'])

        s_key = get_key_from(oxidp_ConfigurationEntryDN)
        n1ql = 'SELECT oxConfApplication FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
        result = get_cb_result(cbm, n1ql)
        if result:
            oxConfApplication = result[0]['oxConfApplication']

        s_key = get_key_from(oxauth_ConfigurationEntryDN)
        n1ql = 'SELECT oxAuthConfDynamic FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
        result = get_cb_result(cbm, n1ql)
        if result:        
            oxAuthConfDynamic = result[0]['oxAuthConfDynamic']

        s_key = get_key_from(oxtrust_ConfigurationEntryDN)
        n1ql = 'SELECT oxTrustConfApplication FROM `{}` USE KEYS "{}"'.format(format(bucket), s_key)
        result = get_cb_result(cbm, n1ql)
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
        if 'organizationName' in oxTrustConfApplication:
            setup_prop['orgName'] =  str(oxTrustConfApplication['organizationName'])
        setup_prop['oxauth_client_id'] =  str(oxTrustConfApplication['oxAuthClientId'])
        setup_prop['oxauthClient_pw'] = str(unobscure(oxTrustConfApplication['oxAuthClientPassword']))
        if 'scimUmaClientId' in oxTrustConfApplication:
            setup_prop['scim_rs_client_id'] =  str(oxTrustConfApplication['scimUmaClientId'])
        if 'scimUmaClientId' in oxTrustConfApplication:
            setup_prop['scim_resource_oxid'] =  str(oxTrustConfApplication['scimUmaResourceId'])
        if 'scimTestMode' in oxTrustConfApplication:
            setup_prop['scimTestMode'] =  oxTrustConfApplication['scimTestMode']

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
        setup_prop['oxauth_openidScopeBackwardCompatibility'] =  oxAuthConfDynamic.get('openidScopeBackwardCompatibility', False)
        if 'pairwiseCalculationSalt' in oxAuthConfDynamic:
            setup_prop['pairwiseCalculationSalt'] =  str(oxAuthConfDynamic['pairwiseCalculationSalt'])
        if 'legacyIdTokenClaims' in oxAuthConfDynamic:
            setup_prop['oxauth_legacyIdTokenClaims'] = oxAuthConfDynamic['legacyIdTokenClaims']
        if 'pairwiseCalculationKey' in oxAuthConfDynamic:
            setup_prop['pairwiseCalculationKey'] = str(oxAuthConfDynamic['pairwiseCalculationKey'])
        if 'keyStoreFile' in oxAuthConfDynamic:
            setup_prop['oxauth_openid_jks_fn'] = str(oxAuthConfDynamic['keyStoreFile'])
        if 'keyStoreSecret' in oxAuthConfDynamic:
            setup_prop['oxauth_openid_jks_pass'] = str(oxAuthConfDynamic['keyStoreSecret'])


    ssl_subj = get_ssl_subject('/etc/certs/httpd.crt')
    setup_prop['countryCode'] = ssl_subj['C']
    setup_prop['state'] = ssl_subj['ST']
    setup_prop['city'] = ssl_subj['L']
    setup_prop['city'] = ssl_subj['L']
    if 'ldapPass' in setup_prop:
        setup_prop['oxtrust_admin_password'] = setup_prop['ldapPass']
    elif 'cb_password' in setup_prop:
            setup_prop['oxtrust_admin_password'] = setup_prop['cb_password']

    if not 'orgName' in setup_prop:
        setup_prop['orgName'] = ssl_subj['O']

    for service in jetty_services:
        setup_prop[jetty_services[service][0]] = os.path.exists('/opt/gluu/jetty/{0}/webapps/{0}.war'.format(service))

    if setup_prop['installSaml']:
        setup_prop['gluuSamlEnabled'] = True

    if os.path.exists('/opt/gluu/node/passport/server'):
        setup_prop['installPassport'] = True

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

    setup_prop['application_max_ram'] = application_max_ram

    if os.path.exists(os.path.join(default_dir, 'gluu-radius')):
        setup_prop['gluuRadiusEnabled'] = True
        setup_prop['oxauth_openidScopeBackwardCompatibility'] = True

    setup_prop['os_type'] = os_type
    setup_prop['os_version'] = os_version

    https_gluu_fn = '/etc/httpd/conf.d/https_gluu.conf' if setup_prop['os_type'] in ('red', 'fedora', 'centos') else '/etc/apache2/sites-available/https_gluu.conf'
    setup_prop['installHTTPD'] = os.path.exists(https_gluu_fn)

    setup_prop['mappingLocations'] = mappingLocations

    asimba_xml = '/opt/tomcat/webapps/asimba/WEB-INF/conf/asimba.xml'
    if os.path.exists(asimba_xml):
        for l in open(asimba_xml):
            m = re.search('<password>(.*)</password>', '<password>p49IXMHN06SL</password>')
            if m:
                setup_prop['asimbaJksPass'] = m.groups()[0]

    if not 'inumOrg' in setup_prop:
        setup_prop['inumOrg'] = setup_prop['admin_inum'].split('!0000!')[0]

    if not 'githubBranchName' in setup_prop:
        setup_prop['githubBranchName'] = 'version_'+gluu_version


    return setup_prop

if __name__ == '__main__':

    setup_prop = generate_properties()
    setup_prop_fn = 'setup.properties.last'

    if os.path.exists(setup_prop_fn):
        flist = glob.glob(setup_prop_fn+'.*')
        n = len(flist) + 1
        os.rename(setup_prop_fn, setup_prop_fn+'.'+str(n))

    with open(setup_prop_fn, 'w') as w:
        for p_key in setup_prop:
            p_val = setup_prop[p_key]
            if p_key == 'mappingLocations':
                p_val = json.dumps(p_val)
            elif isinstance(p_val, bool):
                p_val = str(p_val).lower()
            elif not isinstance(p_val, str):
                p_val = str(p_val)
            w.write('{}={}\n'.format(p_key, p_val))

    print("{} is written successfully".format(setup_prop_fn))


