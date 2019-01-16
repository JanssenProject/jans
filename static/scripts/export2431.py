#!/usr/bin/env python
"""export2431.py - A script to export all the data from Gluu Server 2.4.x

Usage: python export2431.py

Running this creates a folder named `backup_2431` which contains all the data
needed for migration of Gluu Server to a higher version. This script backs up
the following data:
    1. LDAP data
    2. Configurations of Tomcat and OpenDJ
    3. CA certificates in /etc/certs
    4. Webapp Customization files

This backup folder should be used as the input for the `import___.py` script
of appropriate version to migrate to that version.

Read complete migration procedure at:
    https://www.gluu.org/docs/deployment/upgrading/
"""
import getpass
import json
import logging
import os
import os.path
import sys
import logging
import traceback
import subprocess
import tempfile
import getpass
from ldif import LDIFParser, LDIFWriter, CreateLDIF
from distutils.dir_util import copy_tree
import json
from shutil import copyfile
import os.path


class MyLDIF(LDIFParser):
    def __init__(self, input, output):
        LDIFParser.__init__(self, input)
        self.targetDN = None
        self.targetAttr = None
        self.targetEntry = None
        self.DNs = []
        self.lastEntry = None
        self.lastDN = None
        self.entries = []

    def getResults(self):
        return (self.targetDN, self.targetAttr)

    def getDNs(self):
        return self.DNs

    def getLastEntry(self):
        return self.lastEntry

    def parseAttrTypeandValue(self):
        return LDIFParser._parseAttrTypeandValue(self)

    def handle(self, dn, entry):
        if self.targetDN is None:
            self.targetDN = dn
        self.lastDN = dn
        self.DNs.append(dn)
        self.lastEntry = entry
        self.entries.append(entry)
        if dn.lower().strip() == self.targetDN.lower().strip():
            self.targetEntry = entry
            if self.targetAttr in entry:
                self.targetAttr = entry[self.targetAttr]


SKIP_DN = []

# configure logging
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)-8s %(name)s %(message)s',
                    filename='export2431.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)


def dooxAuthChangesFor31(self, oxAuthPath):
    parser = MyLDIF(open(oxAuthPath, 'rb'), sys.stdout)
    parser.targetAttr = "oxAuthConfDynamic"
    atr = parser.parse()
    oxAuthConfDynamic = parser.lastEntry['oxAuthConfDynamic'][0]
    oxAuthConfDynamic = oxAuthConfDynamic.replace('seam/resource/', '')
    oxAuthConfDynamic = oxAuthConfDynamic.replace('restv1/oxauth/', 'restv1/')
    oxAuthConfDynamic = oxAuthConfDynamic.replace('restv1/uma-configuration', 'restv1/uma2-configuration')
    dataOxAuthConfDynamic = json.loads(oxAuthConfDynamic)
    dataOxAuthConfDynamic['grantTypesSupported'].append('password')
    dataOxAuthConfDynamic['grantTypesSupported'].append('client_credentials')
    dataOxAuthConfDynamic['grantTypesSupported'].append('refresh_token')
    dataOxAuthConfDynamic['grantTypesSupported'].append('urn:ietf:params:oauth:grant-type:uma-ticket')
    dataOxAuthConfDynamic['accessTokenLifetime'] = 300
    dataOxAuthConfDynamic['sessionIdLifetime'] = 86400
    dataOxAuthConfDynamic['enableClientGrantTypeUpdate'] = True
    dataOxAuthConfDynamic['externalLoggerConfiguration'] = ""
    dataOxAuthConfDynamic['httpLoggingEnabled'] = False
    dataOxAuthConfDynamic['httpLoggingExludePaths'] = []
    dataOxAuthConfDynamic['logClientIdOnClientAuthentication'] = True
    dataOxAuthConfDynamic['logClientNameOnClientAuthentication'] = False
    dataOxAuthConfDynamic['persistIdTokenInLdap'] = False
    dataOxAuthConfDynamic['persistRefreshTokenInLdap'] = True
    dataOxAuthConfDynamic['personCustomObjectClassList'] = ["gluuCustomPerson", "gluuPerson"]
    dataOxAuthConfDynamic['clientBlackList'] = ["*.attacker.com/*"]
    dataOxAuthConfDynamic['clientWhiteList'] = ["*"]
    dataOxAuthConfDynamic['customHeadersWithAuthorizationResponse'] = True
    dataOxAuthConfDynamic['defaultSubjectType'] = True
    dataOxAuthConfDynamic['endSessionWithAccessToken'] = False
    dataOxAuthConfDynamic['frontChannelLogoutSessionSupported'] = True
    dataOxAuthConfDynamic['idTokenSigningAlgValuesSupported'].append('none')
    dataOxAuthConfDynamic['legacyIdTokenClaims'] = False
    dataOxAuthConfDynamic['umaValidateClaimToken'] = False
    dataOxAuthConfDynamic['updateClientAccessTime'] = True
    dataOxAuthConfDynamic['updateUserLastLogonTime'] = True
    self.hostname = dataOxAuthConfDynamic['issuer'].replace("https://", "")

    # del dataOxAuthConfDynamic['sessionStateHttpOnly']
    # del dataOxAuthConfDynamic['shortLivedAccessTokenLifetime']
    # del dataOxAuthConfDynamic['validateTokenEndpoint']
    # del dataOxAuthConfDynamic['longLivedAccessTokenLifetime']

    dataOxAuthConfDynamic['corsConfigurationFilters'] = []
    dataCross = {
        'corsAllowedHeaders': 'Origin,Authorization,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers',
        'corsAllowedMethods': 'GET,POST,HEAD,OPTIONS', 'corsAllowedOrigins': '*', 'corsExposedHeaders': '',
        'corsLoggingEnabled': False, 'corsPreflightMaxAge': 1800, 'corsRequestDecorate': True,
        'corsSupportCredentials': True, 'filterName': 'CorsFilter'}
    dataOxAuthConfDynamic['corsConfigurationFilters'].append(dataCross)

    DynamicGrantTypeDefault = '["authorization_code","implicit","client_credentials","refresh_token","urn:ietf:params:oauth:grant-type:uma-ticket"]'
    dataOxAuthConfDynamic['dynamicGrantTypeDefault'] = (json.loads(DynamicGrantTypeDefault))

    dataOxAuthConfDynamic['responseTypesSupported'] = []
    dataOxAuthConfDynamic['responseTypesSupported'].append(json.loads('["code"]'))
    dataOxAuthConfDynamic['responseTypesSupported'].append(json.loads('["code","id_token"]'))
    dataOxAuthConfDynamic['responseTypesSupported'].append(json.loads('["token"]'))
    dataOxAuthConfDynamic['responseTypesSupported'].append(json.loads('["token","id_token"]'))
    dataOxAuthConfDynamic['responseTypesSupported'].append(json.loads('["code","token","id_token"]'))
    dataOxAuthConfDynamic['responseTypesSupported'].append(json.loads('["id_token"]'))

    dataOxAuthConfDynamic['dynamicRegistrationCustomObjectClass'] = ""
    dataOxAuthConfDynamic['dynamicRegistrationCustomAttributes'] = ['oxAuthTrustedClient']

    printOxAuthConfDynamic = json.dumps(dataOxAuthConfDynamic, indent=4, sort_keys=True)
    # print (printOxAuthConfDynamic)


    parser.lastEntry['oxAuthConfDynamic'][0] = printOxAuthConfDynamic

    dataOxAuthConfErrors = json.loads(parser.lastEntry['oxAuthConfErrors'][0])
    grant = {'id': ("invalid_grant_and_session"), 'description': (
        "he provided access token and session state are invalid or were issued to another client."), 'uri': (None)}

    session = {'id': ("session_not_passed"), 'description': ("The provided session state is empty."), 'uri': (None)}

    post_logout = {'id': ("post_logout_uri_not_passed"), 'description': ("The provided post logout uri is empty."),
                   'uri': (None)}

    post_logout_uri = {'id': ("post_logout_uri_not_associated_with_client"),
                       'description': ("The provided post logout uri is not associated with client."), 'uri': (None)}

    invalid_logout_uri = {'id': ("invalid_logout_uri"), 'description': ("The provided logout_uri is invalid."),
                          'uri': (None)}

    dataOxAuthConfErrors['endSession'].append(grant)
    dataOxAuthConfErrors['endSession'].append(session)
    dataOxAuthConfErrors['endSession'].append(post_logout)
    dataOxAuthConfErrors['endSession'].append(post_logout_uri)
    dataOxAuthConfErrors['endSession'].append(invalid_logout_uri)

    register = {'description': "Value of one or more claims_redirect_uris is invalid.",
                'id': "invalid_claims_redirect_uri", 'uri': None}

    dataOxAuthConfErrors['register'].append(register)

    uma = {'description': "The provided session is invalid.", 'id': "invalid_session", 'uri': None}
    dataOxAuthConfErrors['uma'].append(uma)

    uma1 = {'description': 'Forbidden by policy (policy returned false).', 'id': "forbidden_by_policy", 'uri': None}

    dataOxAuthConfErrors['uma'].append(uma1)

    uma2 = {'description': 'The provided permission request is not valid.', 'id': "invalid_permission_request",
            'uri': None}

    dataOxAuthConfErrors['uma'].append(uma2)

    uma3 = {
        'description': 'The claims-gathering script name is not provided or otherwise failed to load script with this name(s).',
        'id': "invalid_claims_gathering_script_name", 'uri': None}

    dataOxAuthConfErrors['uma'].append(uma3)

    uma4 = {'description': 'The provided ticket was not found at the AS.', 'id': "invalid_ticket", 'uri': None}

    dataOxAuthConfErrors['uma'].append(uma4)

    uma5 = {'description': 'The provided client_id is not valid.', 'id': "invalid_client_id", 'uri': None}

    dataOxAuthConfErrors['uma'].append(uma5)

    uma6 = {'description': 'The provided claims_redirect_uri is not valid.', 'id': "invalid_claims_redirect_uri",
            'uri': None}

    dataOxAuthConfErrors['uma'].append(uma6)

    uma7 = {
        'description': 'The claim token format is blank or otherwise not supported (supported format is http://openid.net/specs/openid-connect-core-1_0.html#IDToken).',
        'id': "invalid_claims_redirect_uri", 'uri': None}

    dataOxAuthConfErrors['uma'].append(uma7)

    uma8 = {
        'description': 'The claim token is not valid or unsupported. (If format is http://openid.net/specs/openid-connect-core-1_0.html#IDToken then claim token has to be ID Token).',
        'id': "invalid_claim_token", 'uri': None}

    dataOxAuthConfErrors['uma'].append(uma8)

    uma9 = {'description': 'PCT is invalid (revoked, expired or does not exist anymore on AS)', 'id': "invalid_pct",
            'uri': None}

    dataOxAuthConfErrors['uma'].append(uma9)

    uma10 = {'description': 'RPT is invalid (revoked, expired or does not exist anymore on AS)', 'id': "invalid_rpt",
             'uri': None}

    dataOxAuthConfErrors['uma'].append(uma10)

    uma11 = {
        'description': 'The provided grant_type valid does not equal to urn:ietf:params:oauth:grant-type:uma-ticket value which is required by UMA 2.',
        'id': "invalid_grant_type", 'uri': None}

    dataOxAuthConfErrors['uma'].append(uma11)

    # printOxAuthConfErrors = json.dumps(dataOxAuthConfErrors, indent=4, sort_keys=True)
    # print (printOxAuthConfErrors)

    base64Types = ["oxAuthConfStatic", "oxAuthConfWebKeys", "oxAuthConfErrors", "oxAuthConfDynamic"]

    out = CreateLDIF(parser.lastDN, parser.getLastEntry(), base64_attrs=base64Types)
    newfile = oxAuthPath.replace('/oxauth_config.ldif', '/oxauth_config_new.ldif')
    # print (newfile)
    f = open(newfile, 'w')
    f.write(out)
    f.close()

    os.remove(oxAuthPath)
    os.rename(newfile, oxAuthPath)


def removeDeprecatedScripts(self, oxScriptPath):
    parser = MyLDIF(open(oxScriptPath, 'rb'), sys.stdout)
    parser.parse()
    base64Types = ["oxScript"]
    newfile = oxScriptPath.replace('/scripts.ldif', '/scripts_new.ldif')
    f = open(newfile, 'a')
    for idx, val in enumerate(parser.entries):
        if 'displayName' in val:
            if val['displayName'][0] != 'uma_authorization_policy':
                out = CreateLDIF(parser.getDNs()[idx], parser.entries[idx], base64_attrs=base64Types)
                f.write(out)
    f.close()

    os.remove(oxScriptPath)
    os.rename(newfile, oxScriptPath)


def doClientsChangesForUMA2(self, clientPath):
    parser = MyLDIF(open(clientPath, 'rb'), sys.stdout)
    parser.parse()
    atr = parser.parse()
    newfile = clientPath.replace('/clients.ldif', '/clients_new.ldif')
    f = open(newfile, 'w')
    base64Types = []
    for idx, val in enumerate(parser.entries):
        if 'displayName' in val:
            if val['displayName'][0] == 'Pasport Resource Server Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
                self.passport_rs_client_id = parser.entries[idx]["inum"][0]
            elif val['displayName'][0] == 'SCIM Resource Server Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
                self.scim_rs_client_id = parser.entries[idx]["inum"][0]
            elif val['displayName'][0] == 'Passport Requesting Party Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
                self.passport_rp_client_id = parser.entries[idx]["inum"][0]
            elif val['displayName'][0] == 'SCIM Requesting Party Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
                self.scim_rp_client_id = parser.entries[idx]["inum"][0]
            out = CreateLDIF(parser.getDNs()[idx], parser.entries[idx], base64_attrs=base64Types)
            f.write(out)
    f.close()
    os.remove(clientPath)
    os.rename(newfile, clientPath)


def doUmaResourcesChangesForUma2(self, UmaPath):
    scimClient = ''
    passportClient = ''
    inumOrg = self.inumOrg
    with open('/install/community-edition-setup/setup.properties.last', 'r') as f:
        content = f.readlines()
        for line in content:
            if 'scim_rp_client_id' in line:
                scimClient = line.replace("scim_rp_client_id=", "")
            elif 'passport_rp_client_id' in line:
                passportClient = line.replace("passport_rp_client_id=", "")

    parser = MyLDIF(open(UmaPath, 'rb'), sys.stdout)
    parser.parse()
    atr = parser.parse()
    newfile = UmaPath.replace('/uma.ldif', '/uma_new.ldif')
    f = open(newfile, 'w')
    base64Types = []
    for idx, val in enumerate(parser.entries):
        if 'displayName' in val:
            if val['oxId'][0] == 'scim_access':
                parser.entries[idx]["oxId"] = ['https://' + self.hostname + '/oxauth/restv1/uma/scopes/scim_access']
                if 'oxId' in val and 'ou=resource_sets' in parser.getDNs()[idx] and len(val['oxId'][0]) > 1:
                    parser.getDNs()[idx] = parser.getDNs()[idx].replace('inum=' + val['inum'][0],
                                                                        'oxId=' + val['oxId'][0]).replace(
                        'resource_sets',
                        'resources')
            elif val['oxId'][0] == 'passport_access':
                parser.entries[idx]["oxId"] = ['https://' + self.hostname + '/oxauth/restv1/uma/scopes/passport_access']
            if val['displayName'][0] == 'SCIM Resource Set':
                parser.entries[idx]["oxResource"] = ['https://' + self.hostname + '/identity/restv1/scim/v1']
                parser.entries[idx]['oxAssociatedClient'] = [
                    ('inum=' + self.scim_rp_client_id + ',ou=clients,o=' + inumOrg + ",o=gluu").replace("\n", '')]
            elif val['displayName'][0] == 'Passport Resource Set':
                parser.entries[idx]["oxResource"] = ['https://' + self.hostname + '/identity/restv1/passport/config']
                parser.entries[idx]['oxAssociatedClient'] = [
                    ('inum=' + self.passport_rp_client_id + ',ou=clients,o=' + inumOrg + ",o=gluu").replace("\n", '')]

        out = CreateLDIF(parser.getDNs()[idx], parser.entries[idx], base64_attrs=base64Types)
        f.write(out)
    f.close()
    os.remove(UmaPath)
    os.rename(newfile, UmaPath)


def doOxTrustChanges(oxTrustPath):
    parser = MyLDIF(open(oxTrustPath, 'rb'), sys.stdout)
    parser.targetAttr = "oxTrustConfApplication"
    atr = parser.parse()
    oxTrustConfApplication = parser.lastEntry['oxTrustConfApplication'][0]
    oxTrustConfApplication = oxTrustConfApplication.replace('seam/resource/', '')
    parser.lastEntry['oxTrustConfApplication'][0] = oxTrustConfApplication
    oxTrustConfApplicationJson = json.loads(oxTrustConfApplication)
    oxTrustConfApplicationJson['clientBlackList'] = ["*.attacker.com/*"]
    oxTrustConfApplicationJson['clientWhiteList'] = ["*"]
    oxTrustConfApplicationJson['idp3EncryptionCert'] = '/etc/certs/idp-encryption.crt'
    oxTrustConfApplicationJson['idp3SigningCert'] = '/etc/certs/idp-signing.crt'
    oxTrustConfApplicationJson['organizationName'] = 'Gluu Inc.'
    oxTrustConfApplicationJson['velocityLog'] = '/opt/gluu/jetty/identity/logs/velocity.log'
    oxTrustConfApplicationJson['personObjectClassDisplayNames'] = ["gluuCustomPerson", "gluuPerson", "eduPerson"]
    oxTrustConfApplicationJson['personObjectClassTypes'] = ["gluuCustomPerson", "gluuPerson", "eduPerson"]
    oxTrustConfApplicationJson['rptConnectionPoolCustomKeepAliveTimeout'] = 5
    oxTrustConfApplicationJson['rptConnectionPoolDefaultMaxPerRoute'] = 20
    oxTrustConfApplicationJson['rptConnectionPoolMaxTotal'] = 200
    oxTrustConfApplicationJson['rptConnectionPoolUseConnectionPooling'] = True
    oxTrustConfApplicationJson['rptConnectionPoolValidateAfterInactivity'] = 10
    oxTrustConfApplicationJson['ScimProperties'] = json.loads('{"maxCount": "200"}')
    oxTrustConfApplicationJson['shibboleth3FederationRootDir'] = "/opt/shibboleth-federation"
    oxTrustConfApplicationJson['shibboleth3IdpRootDir'] = "/opt/shibboleth-idp"
    oxTrustConfApplicationJson['shibboleth3SpConfDir'] = '/opt/shibboleth-idp/sp'
    oxTrustConfApplicationJson['shibbolethVersion'] = 'v3'
    oxTrustConfApplicationJson['ldifStore'] = "/var/ox/identity/removed"
    oxTrustConfApplicationJson['personCustomObjectClass'] = 'gluuCustomPerson'
    try:
        del oxTrustConfApplicationJson['umaClientKeyId']
        del oxTrustConfApplicationJson['umaClientKeyStoreFile']
        del oxTrustConfApplicationJson['umaResourceId']
        del oxTrustConfApplicationJson['umaScope']
        del oxTrustConfApplicationJson['umaClientId']
        del oxTrustConfApplicationJson['shibboleth2SpConfDir']
        del oxTrustConfApplicationJson['shibboleth2IdpRootDir']
        del oxTrustConfApplicationJson['shibboleth2FederationRootDir']
        del oxTrustConfApplicationJson['schemaAddObjectClassWithoutAttributeTypesDefinition']
        del oxTrustConfApplicationJson['schemaAddObjectClassWithAttributeTypesDefinition']
        del oxTrustConfApplicationJson['schemaAddAttributeDefinition']
        del oxTrustConfApplicationJson['recaptchaSiteKey']
        del oxTrustConfApplicationJson['recaptchaSecretKey']
        del oxTrustConfApplicationJson['oxAuthUserInfo']
        del oxTrustConfApplicationJson['oxAuthTokenValidationUrl']
        del oxTrustConfApplicationJson['oxAuthTokenUrl']
        del oxTrustConfApplicationJson['oxAuthRegisterUrl']
        del oxTrustConfApplicationJson['oxAuthEndSessionUrl']
        del oxTrustConfApplicationJson['oxAuthLogoutUrl']
        del oxTrustConfApplicationJson['oxAuthAuthorizeUrl']
        del oxTrustConfApplicationJson['mysqlPassword']
        del oxTrustConfApplicationJson['mysqlUrl']
        del oxTrustConfApplicationJson['mysqlUser']
        del oxTrustConfApplicationJson['authMode']
        del oxTrustConfApplicationJson['umaClientKeyStorePassword']
    except:
        logging.debug("Error")

    parser.lastEntry['oxTrustConfApplication'][0] = json.dumps(oxTrustConfApplicationJson, indent=4, sort_keys=True)

    oxTrustConfCacheRefresh = parser.lastEntry['oxTrustConfCacheRefresh'][0]
    oxTrustConfCacheRefreshJson = json.loads(oxTrustConfCacheRefresh)
    oxTrustConfCacheRefreshJson['inumConfig']['bindDN'] = oxTrustConfCacheRefreshJson['inumConfig']['bindDN'].replace(
        'cn=directory manager', 'cn=directory manager,o=site')
    oxTrustConfCacheRefreshJson['snapshotFolder'] = '/var/ox/identity/cr-snapshots/'
    parser.lastEntry['oxTrustConfCacheRefresh'][0] = json.dumps(oxTrustConfCacheRefreshJson, indent=4, sort_keys=True)

    parser.lastEntry['oxTrustConfImportPerson'] = [json.dumps(json.loads(
        '{	"mappings": [{		"ldapName": "uid",		"displayName": "Username",		"dataType": "string",		"required": "true"	},	{		"ldapName": "givenName",		"displayName": "First Name",		"dataType": "string",		"required": "true"	},	{		"ldapName": "sn",		"displayName": "Last Name",		"dataType": "string",		"required": "true"	},	{		"ldapName": "mail",		"displayName": "Email",		"dataType": "string",		"required": "true"	},	{		"ldapName": "userPassword",		"displayName": "Password",		"dataType": "string",		"required": "false"	}]}'))]
    base64Types = ["oxTrustConfApplication", "oxTrustConfImportPerson", "oxTrustConfCacheRefresh",
                   "oxTrustConfImportPerson"]

    out = CreateLDIF(parser.lastDN, parser.getLastEntry(), base64_attrs=base64Types)
    newfile = oxTrustPath.replace('/oxtrust_config.ldif', '/oxtrust_config_new.ldif')
    # print (newfile)
    f = open(newfile, 'w')
    f.write(out)
    f.close()

    os.remove(oxTrustPath)
    os.rename(newfile, oxTrustPath)


def doApplinceChanges(oxappliancesPath):
    parser = MyLDIF(open(oxappliancesPath, 'rb'), sys.stdout)
    parser.parse()
    base64Types = []

    idpConfig = json.loads(parser.entries[0]['oxIDPAuthentication'][0])
    idpConfig['name'] = None
    idpConfig['priority'] = 1
    idpConfigJson = json.loads(idpConfig['config'])
    # idpConfigJson['bindDN'] = 'cn=directory manager,o=gluu'
    if (idpConfigJson['useSSL']):
        idpConfigJson['useSSL'] = "true"
    else:
        idpConfigJson['useSSL'] = 'false'
    # del idpConfigJson['level']
    # del idpConfigJson['version']
    idpConfig['config'] = json.dumps(idpConfigJson, indent=4, sort_keys=True)
    parser.entries[0]['oxIDPAuthentication'][0] = json.dumps(idpConfig, indent=4, sort_keys=True)
    out = CreateLDIF(parser.lastDN, parser.getLastEntry(), base64_attrs=base64Types)
    newfile = oxappliancesPath.replace('/appliance.ldif', '/appliancenew.ldif')
    # print (newfile)
    f = open(newfile, 'w')
    f.write(out)
    f.close()

    os.remove(oxappliancesPath)
    os.rename(newfile, oxappliancesPath)


def doAttributeChange(self, attributePath):
    parser = MyLDIF(open(attributePath, 'rb'), sys.stdout)
    parser.parse()
    parser.parse()
    newfile = attributePath.replace('attributes.ldif', 'attributes_new.ldif')
    f = open(newfile, 'w')
    base64Types = []
    for idx, val in enumerate(parser.entries):
        if 'inum' in val:
            if (val['gluuAttributeName'][0] == "gluuStatus"):
                #print(parser.entries[idx])
                parser.entries[idx]["displayName"] = ['User Status']

        out = CreateLDIF(parser.getDNs()[idx], parser.entries[idx], base64_attrs=base64Types)
        f.write(out)

    f.close()
    os.remove(attributePath)
    os.rename(newfile, attributePath)


def changePassportConfigJson(self, param):
    # Read in the file
    if os.path.exists(param):
        with open(param, 'r') as file:
            filedata = file.read()

        # Replace the target string
        filedata = filedata.replace('seam/resource/', '')

        # Write the file out again
        with open(param, 'w') as file:
            file.write(filedata)


class Exporter(object):
    def __init__(self):
        self.backupDir = 'backup_2431'
        self.foldersToBackup = ['/opt/tomcat/conf',
                                '/opt/tomcat/endorsed',
                                '/opt/opendj/config',
                                '/etc/certs',
                                '/opt/idp/conf',
                                '/opt/idp/metadata',
                                '/var/gluu/webapps',
                                '/var/ox/photos',
                                ]
        self.passwordFile = tempfile.mkstemp()[1]

        self.ldapsearch = '/opt/opendj/bin/ldapsearch'
        self.slapcat = '/opt/symas/bin/slapcat'
        self.mkdir = '/bin/mkdir'
        self.cat = '/bin/cat'
        self.grep = '/bin/grep'
        self.hostname = '/bin/hostname'

        self.ldapCreds = ['-h', 'localhost', '-p', '1636', '-Z', '-X', '-D', '"cn=directory manager"', '-j',
                          self.passwordFile]

        self.base_dns = ['ou=people',
                         'ou=groups',
                         'ou=attributes',
                         'ou=scopes',
                         'ou=clients',
                         'ou=scripts',
                         'ou=uma',
                         'ou=hosts',
                         'ou=u2f']

        self.propertiesFn = os.path.join(self.backupDir, 'setup.properties')
        self.hostname = ""
        self.passport_rs_client_id = ""
        self.scim_rs_client_id = ""
        self.passport_rp_client_id = ""
        self.scim_rp_client_id = ""
        self.os_types = ['centos', 'redhat', 'fedora', 'ubuntu', 'debian']
        self.os = self.detect_os_type()
        self.service = "/usr/sbin/service"
        self.choice = 0
        if self.os is 'centos':
            self.service = "/sbin/service"

    def detect_os_type(self):
        distro_info = self.readFile('/etc/redhat-release')
        if distro_info is None:
            distro_info = self.readFile('/etc/os-release')
        if 'CentOS' in distro_info:
            return self.os_types[0]
        elif 'Red Hat' in distro_info:
            return self.os_types[1]
        elif 'Ubuntu' in distro_info:
            return self.os_types[3]
        elif 'Debian' in distro_info:
            return self.os_types[4]
        else:
            return self.choose_from_list(self.os_types, "Operating System")

    def readFile(self, inFilePath):
        if not os.path.exists(inFilePath):
            logging.debug("Cannot read: %s. File does not exist.", inFilePath)
            return None

        inFilePathText = None
        try:
            f = open(inFilePath)
            inFilePathText = f.read()
            f.close
        except:
            logging.warning("Error reading %s", inFilePath)
            logging.debug(traceback.format_exc())

        return inFilePathText

    def getOutput(self, args):
        try:
            logging.debug("Running command : %s", " ".join(args))
            output = None
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logging.error("Error running command : %s", " ".join(args))
            logging.debug(traceback.format_exc())
            sys.exit(1)

    def makeFolders(self):
        folders = [self.backupDir, "%s/ldif" % self.backupDir]
        for folder in folders:
            try:
                if not os.path.exists(folder):
                    self.getOutput([self.mkdir, '-p', folder])
            except:
                logging.error("Error making folder: %s", folder)
                logging.debug(traceback.format_exc())
                sys.exit(1)


    def prepareLdapPW(self):
        ldap_pass = None
        # read LDAP pass from setup.properties
        with open('/install/community-edition-setup/setup.properties.last',
                  'r') as sfile:
            for line in sfile:
                if 'ldapPass=' in line:
                    ldap_pass = line.split('=')[-1]
        # write it to the tmp file
        with open(self.passwordFile, 'w') as pfile:
            pfile.write(ldap_pass)
        # perform sample search
        sample = self.inumOrg
        if not sample:
            # get the password from the user if it fails
            ldap_pass = getpass.getpass("Enter LDAP Passsword: ")
            with open(self.passwordFile, 'w') as pfile:
                pfile.write(ldap_pass)

    def backupFiles(self):
        logging.info('Creating backup of files')
        for folder in self.foldersToBackup:
            try:
                copy_tree(folder, self.backupDir + folder)
            except:
                logging.error("Failed to backup %s", folder)
                logging.debug(traceback.format_exc())

    def runAndLog(self, args):
        try:
            logging.debug("Running command : %s" % " ".join(args))
            si,so,se = os.popen3(' '.join(args))
            error = se.read()
            if error:
                logging.error(error)
                logging.debug(traceback.format_exc())
            return so.read()
        except:
            logging.error("Error running command : %s" % " ".join(args))
            logging.error(traceback.format_exc())
            sys.exit(1)


    def getLdif(self):
        logging.info('Creating backup of LDAP data')
        orgInum = self.inumOrg
        # Backup the data
        for basedn in self.base_dns:
            ou = basedn.split("=")[-1]
            if ou =='people':
                logging.info('Backing up o=%s, may take time' % ou)
            
            out_file = "%s/ldif/%s.ldif" % (self.backupDir, ou)
            args = [self.ldapsearch] + self.ldapCreds + [
                '-b', '"%s,o=%s,o=gluu"' % (basedn, orgInum), 'objectclass=*', '>', out_file]

            self.runAndLog(args)
            
            if basedn == 'ou=uma':
                args = ("sed", "-i", "'s/oxAuthUmaResourceSet/oxUmaResource/g' %s" % out_file)
                self.runAndLog(args)

        removeDeprecatedScripts(self, "%s/ldif/scripts.ldif" % self.backupDir)
        doClientsChangesForUMA2(self, "%s/ldif/clients.ldif" % self.backupDir)
        doAttributeChange(self, "%s/ldif/attributes.ldif" % self.backupDir)
        changePassportConfigJson(self, "%s/etc/gluu/conf/passport-config.json" % self.backupDir)

        out_file = "%s/ldif/appliance.ldif" % self.backupDir
        # Backup the appliance config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                '"ou=appliances,o=gluu"',
                '-s',
                'one',
                '"objectclass=*"', '>', out_file]
        self.runAndLog(args)

        output = open(out_file).read()

        output = output.replace('IN_MEMORY', '"IN_MEMORY"')
        output = output.replace('""IN_MEMORY""', "IN_MEMORY")
        output = output.replace('DEFAULT', '"DEFAULT"')
        output = output.replace('""DEFAULT""', "DEFAULT")

        with open(out_file,'w') as f:
            f.write(output)

        # Backup the oxtrust config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                '"ou=appliances,o=gluu"',
                '"objectclass=oxTrustConfiguration"', '>', "%s/ldif/oxtrust_config.ldif" % self.backupDir]
        self.runAndLog(args)
        doOxTrustChanges("%s/ldif/oxtrust_config.ldif" % self.backupDir)
        doApplinceChanges("%s/ldif/appliance.ldif" % self.backupDir)

        # Backup the oxauth config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                '"ou=appliances,o=gluu"',
                '"objectclass=oxAuthConfiguration"', '>', "%s/ldif/oxauth_config.ldif" % self.backupDir]
        self.runAndLog(args)
        dooxAuthChangesFor31(self, "%s/ldif/oxauth_config.ldif" % self.backupDir)
        doUmaResourcesChangesForUma2(self, "%s/ldif/uma.ldif" % self.backupDir)

        # Backup the trust relationships
        args = [self.ldapsearch] + self.ldapCreds + [
            '-b', '"ou=appliances,o=gluu"', '"objectclass=gluuSAMLconfig"', '>', "%s/ldif/trust_relationships.ldif" % self.backupDir]
        self.runAndLog(args)

        # Backup the org
        args = [self.ldapsearch] + self.ldapCreds + [
            '-s', 'base', '-b', 'o=%s,o=gluu' % orgInum, 'objectclass=*', '>', "%s/ldif/organization.ldif" % self.backupDir]
        self.runAndLog(args)
        logging.info('Backing up o=site, may take time')

        # Backup o=site
        args = [self.ldapsearch] + self.ldapCreds + [
            '-b', '"o=site"', '-s', 'one', '"objectclass=*"','>', "%s/ldif/site.ldif" % self.backupDir]
        self.runAndLog(args)

    def clean(self, s):
        return s.replace('@', '').replace('!', '').replace('.', '')

    def getProp(self, prop):
        with open('/install/community-edition-setup/setup.properties.last',
                  'r') as sf:
            for line in sf:
                if "{0}=".format(prop) in line:
                    return line.split('=')[-1].strip()

    def getLDAPServerTypeChoice(self):
        try:
            self.choice = int(raw_input("\nChoose the target LDAP Server - 1.OpenLDAP, 2.OpenDJ [2]: "))
        except ValueError:
            logging.error("You did not enter a integer value. "
                          "Cannot decide LDAP server type. Quitting.")
            sys.exit(1)

        if self.choice != 1 and self.choice != 2:
            logging.error("Invalid selection of LDAP Server. Cannot Migrate.")
            sys.exit(1)

    def genProperties(self):
        logging.info('Creating setup.properties backup file')
        props = {}
        props['ldapPass'] = self.getOutput([self.cat, self.passwordFile]).strip()

        ldap_type = 'openldap'
        if self.choice == 1:
            ldap_type = 'openldap'
        elif self.choice == 2:
            ldap_type = 'opendj'
            props['opendj_version'] = 3.0

        props['ldap_type'] = ldap_type
        props['hostname'] = self.hostname
        props['inumAppliance'] = self.getOutput(
            [self.grep, "^inum", "%s/ldif/appliance.ldif" % self.backupDir]
        ).split("\n")[0].split(":")[-1].strip()
        props['inumApplianceFN'] = self.clean(props['inumAppliance'])
        props['inumOrg'] = self.getProp('inumOrg')
        props['inumOrgFN'] = self.clean(props['inumOrg'])
        props['baseInum'] = props['inumOrg'][:21]
        props['encode_salt'] = self.getOutput(
            [self.cat, "%s/opt/tomcat/conf/salt" % self.backupDir]
        ).split("=")[-1].strip()

        props['oxauth_client_id'] = self.getProp('oxauth_client_id')
        props['scim_rs_client_id'] = self.scim_rs_client_id
        props['scim_rp_client_id'] = self.scim_rp_client_id
        props['version'] = self.getProp('githubBranchName').replace(
            'version_', '')
        # As the certificates are copied to the new installation, their pass
        # are required for accessing them and validating them
        props['httpdKeyPass'] = self.getProp('httpdKeyPass')
        props['shibJksPass'] = self.getProp('shibJksPass')
        props['asimbaJksPass'] = self.getProp('asimbaJksPass')

        # Preferences for installation of optional components
        installSaml = raw_input("\tIs Shibboleth SAML IDP installed? (y/N): [N]") or "N"
        props['installSaml'] = 'y' in installSaml.lower()
        # if installSaml:
        #     shibv = raw_input("\tAre you migrating to Gluu Server v3.x? (y/N): [y]") or "y"
        # if shibv:
        props['shibboleth_version'] = 'v3'
        props['installAsimba'] = os.path.isfile(
            '/opt/tomcat/webapps/asimba.war')
        props['installOxAuthRP'] = os.path.isfile(
            '/opt/tomcat/webapps/oxauth-rp.war')
        installPassport = raw_input("\tInstall passport in new version? (y/N): [N]") or "N"
        props['installPassport'] = 'y' in installPassport.lower()

        self.props = props
        f = open(self.propertiesFn, 'w')
        for key in props.keys():
            if props[key]:
                f.write("%s=%s\n" % (key, props[key]))
        f.close()

    def stopOpenDJ(self):
        logging.info('Stopping OpenDJ Directory Server...')
        if (os.path.isfile('/usr/bin/systemctl')):
            self.getOutput(['systemctl', 'stop', 'opendj'])
            output = self.getOutput(['systemctl', 'is-active', 'opendj'])
        else:
            output = self.getOutput([self.service, 'opendj', 'stop'])
        if output.find("Directory Server is now stopped") > 0 or output.find("Server already stopped") > 0 \
                or output.strip() == "failed":
            logging.info("Directory Server is now stopped")
        else:
            logging.error(
                "OpenDJ did not stop properly. Export cannot run without "
                "stopping the directory server. Exiting from import. Check"
                " /opt/opendj/logs/errors")
            sys.exit(1)

    def editLdapConfig(self):

        replacements = {'ds-cfg-size-limit: 1000': 'ds-cfg-size-limit: 100000'}

        lines = []
        with open('/opt/opendj/config/config.ldif') as infile:
            for line in infile:
                for src, target in replacements.iteritems():
                    line = line.replace(src, target)
                lines.append(line)
        with open('/opt/opendj/config/config.ldif', 'w') as outfile:
            for line in lines:
                outfile.write(line)

    def removeLdapConfig(self):

        replacements = {'ds-cfg-size-limit: 100000': 'ds-cfg-size-limit: 1000'}

        lines = []
        with open('/opt/opendj/config/config.ldif') as infile:
            for line in infile:
                for src, target in replacements.iteritems():
                    line = line.replace(src, target)
                lines.append(line)
        with open('/opt/opendj/config/config.ldif', 'w') as outfile:
            for line in lines:
                outfile.write(line)

    def startOpenDJ(self):
        logging.info('Starting OpenDJ Directory Server...')
        if (os.path.isfile('/usr/bin/systemctl')):
            self.getOutput(['systemctl', 'start', 'opendj'])
            output = self.getOutput(['systemctl', 'is-active', 'opendj'])
        output = self.getOutput([self.service, 'opendj', 'start'])
        if output.find("Directory Server has started successfully") > 0 or \
                        output.strip() == "active":
            logging.info("Directory Server has started successfully")
        else:
            logging.error("OpenDJ did not start properly. Check "
                          "/opt/opendj/logs/errors. Restart it manually.")

    def export(self):
        # Call the sequence of functions that would backup the various stuff
        print("-------------------------------------------------------------")
        print("            Gluu Server Data Export Tool For v2.4x to v3.1x           ")
        print("-------------------------------------------------------------")
        print("")
        self.makeFolders()
        self.inumOrg = self.getProp('inumOrg')
        #self.stopOpenDJ()
        #self.editLdapConfig()
        #self.startOpenDJ()
        self.prepareLdapPW()
        self.backupFiles()
        self.getLdif()
        self.getLDAPServerTypeChoice()
        self.genProperties()
        #self.removeLdapConfig()
        print("")
        print("-------------------------------------------------------------")
        print("The data has been exported to %s" % self.backupDir)
        print("-------------------------------------------------------------")


if __name__ == "__main__":
    if len(sys.argv) != 1:
        print ("Usage: python export2431.py")
    else:
        exporter = Exporter()
        exporter.export()
