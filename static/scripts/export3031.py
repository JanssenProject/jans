#!/usr/bin/env python
"""export3031.py - A script to export all the data from Gluu Server 3.0.x

Usage: python export3031.py

Running this creates a folder named `backup_3031` which contains all the data
needed for migration of Gluu Server to a higher version. This script backs up
the following data:
    1. LDAP data
    2. Configurations of various components installed inside Gluu Server
    3. CA certificates in /etc/certs
    4. Webapp Customization files

This backup folder should be used as the input for the `import___.py` script
of appropriate version to migrate to that version.

Read complete migration procedure at:
    https://www.gluu.org/docs/deployment/upgrading/
"""
import os
import os.path
import sys
import logging
import traceback
import subprocess
import tempfile
import getpass
import platform

if len(sys.argv) > 1 and sys.argv[1] and os.path.exists(sys.argv[1]):
    backupRoot = sys.argv[1]
    os.chdir(sys.argv[1])
else:
    os.chdir('/root')
    backupRoot = '/root'


if not os.path.exists('/etc/gluu/conf/ox-ldap.properties'):
    sys.exit("Please run this script inside Gluu 3.x container.")

cmd_list =[]

cur_dir=os.path.dirname(os.path.realpath(__file__))

if not os.path.exists(os.path.join(cur_dir, 'ldif.py')):
    cmd_list.append(("Downloading ldif.py", "wget -c https://raw.githubusercontent.com/GluuFederation/community-edition-setup/version_3.1.6/ldif.py"))

for message, cmd in cmd_list:
    print message
    result = os.system(cmd)
    if result:
        sys.exit("An error occurred while running command. Please fix it")


from ldif import LDIFParser, LDIFWriter, CreateLDIF
from distutils.dir_util import copy_tree
import json
from shutil import copyfile
import os.path


class MyLDIF(LDIFParser):
    def __init__(self, input, output, keep_dn=False):
        LDIFParser.__init__(self, input)
        self.keep_dn = keep_dn
        self.targetDN = None
        self.targetAttr = None
        self.targetEntry = None
        self.DNs = []
        self.lastDN = None
        self.lastEntry = None
        self.entries = []
        self.dn_entry = []

    def getResults(self):
        return (self.targetDN, self.targetAttr)

    def getDNs(self):
        return self.DNs

    def getLastEntry(self):
        return self.lastEntry

    def parseAttrTypeandValue(self):
        return LDIFParser._parseAttrTypeandValue(self)

    def handle(self, dn, entry):
        if self.keep_dn:
            self.dn_entry.append((dn, entry))
        
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
                    filename='export30.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)


def doOxTrustChanges(oxTrustPath):
    parser = MyLDIF(open(oxTrustPath, 'rb'), sys.stdout)
    parser.targetAttr = "oxTrustConfApplication"
    atr = parser.parse()
    oxTrustConfApplication = parser.lastEntry['oxTrustConfApplication'][0]
    oxTrustConfApplication = oxTrustConfApplication.replace('seam/resource/', '')
    parser.lastEntry['oxTrustConfApplication'][0] = oxTrustConfApplication
    oxTrustConfApplicationJson = json.loads(oxTrustConfApplication)
    oxTrustConfApplicationJson['ScimProperties'] = json.loads('{"maxCount": "200"}')

    del oxTrustConfApplicationJson['loggingLevel']
    del oxTrustConfApplicationJson['oxIncommonFlag']
    del oxTrustConfApplicationJson['recaptchaSiteKey']
    del oxTrustConfApplicationJson['recaptchaSecretKey']

    parser.lastEntry['oxTrustConfApplication'][0] = json.dumps(oxTrustConfApplicationJson, indent=4, sort_keys=True)

    oxTrustConfCacheRefresh = parser.lastEntry['oxTrustConfCacheRefresh'][0]
    oxTrustConfCacheRefreshJson = json.loads(oxTrustConfCacheRefresh)
    oxTrustConfCacheRefreshJson['inumConfig']['bindDN'] = oxTrustConfCacheRefreshJson['inumConfig']['bindDN'].replace(
        'cn=directory manager', 'cn=directory manager,o=site')
    oxTrustConfCacheRefreshJson['snapshotFolder'] = '/var/ox/identity/cr-snapshots/'
    parser.lastEntry['oxTrustConfCacheRefresh'][0] = json.dumps(oxTrustConfCacheRefreshJson, indent=4, sort_keys=True)

    oxTrustConfImportPerson = [""]
    oxTrustConfImportPerson[0] = json.dumps(json.loads('{	"mappings": [{		"ldapName": "uid",		"displayName": "Username",		"dataType": "string",		"required": "true"	},	{		"ldapName": "givenName",		"displayName": "First Name",		"dataType": "string",		"required": "true"	},	{		"ldapName": "sn",		"displayName": "Last Name",		"dataType": "string",		"required": "true"	},	{		"ldapName": "mail",		"displayName": "Email",		"dataType": "string",		"required": "true"	},	{		"ldapName": "userPassword",		"displayName": "Password",		"dataType": "string",		"required": "false"	}]}'))
    parser.lastEntry['oxTrustConfImportPerson'] = oxTrustConfImportPerson[0]

    base64Types = ["oxTrustConfApplication", "oxTrustConfImportPerson", "oxTrustConfCacheRefresh","oxTrustConfImportPerson"]

    out = CreateLDIF(parser.lastDN, parser.getLastEntry(), base64_attrs=base64Types)
    newfile = oxTrustPath.replace('/oxtrust_config.ldif', '/oxtrust_config_new.ldif')
    # print (newfile)
    f = open(newfile, 'w')
    f.write(out)
    f.close()

    os.remove(oxTrustPath)
    os.rename(newfile, oxTrustPath)



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
    dataOxAuthConfDynamic['idTokenSigningAlgValuesSupported'].append("ES512")
    dataOxAuthConfDynamic['idTokenSigningAlgValuesSupported'].append("none")


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


def addMigratedData(data):
    json_file = os.path.join(exporter.backupDir, 'migrate_data.json')
    
    if os.path.exists(json_file):
        cur_data = json.loads(open(json_file).read())
    else:
        cur_data = []

    cur_data.append(data)
    
    with open(json_file,'w') as w:
        w.write(json.dumps(cur_data))


def removeDeprecatedScripts(self, oxScriptPath):
    parser = MyLDIF(open(oxScriptPath, 'rb'), sys.stdout)
    parser.parse()
    base64Types = ["oxScript"]
    newfile = oxScriptPath.replace('/scripts.ldif', '/scripts_new.ldif')
    f = open(newfile, 'a')
    for idx, val in enumerate(parser.entries):
        if 'inum' in val and val['inum'][0].endswith('2DAF.F995'):
            addMigratedData({'inum':val['inum'][0], 'gluuStatus': val['gluuStatus'][0]})
            continue
        out = CreateLDIF(parser.getDNs()[idx], parser.entries[idx], base64_attrs=base64Types)
        f.write(out)
    f.close()

    os.remove(oxScriptPath)
    os.rename(newfile, oxScriptPath)

def doApplinceChanges(oxappliancesPath):
    parser = MyLDIF(open(oxappliancesPath, 'rb'), sys.stdout)
    parser.parse()
    base64Types = []

    idpConfig = json.loads(parser.entries[0]['oxIDPAuthentication'][0])
    idpConfig['name'] = None
    idpConfig['priority'] = 1
    del idpConfig['version']
    del idpConfig['level']
    idpConfigJson = json.loads(idpConfig['config'])

    idpConfig['config'] = json.dumps(idpConfigJson, indent=4, sort_keys=True)
    parser.entries[0]['oxIDPAuthentication'][0] = json.dumps(idpConfig, indent=4, sort_keys=True)
    out = CreateLDIF(parser.lastDN, parser.getLastEntry(), base64_attrs=base64Types)
    newfile = oxappliancesPath.replace('/appliance.ldif', '/appliancenew.ldif')

    f = open(newfile, 'w')
    f.write(out)
    f.close()

    os.remove(oxappliancesPath)
    os.rename(newfile, oxappliancesPath)


def doClientsChangesForUMA2(self, clientPath):
    parser = MyLDIF(open(clientPath, 'rb'), sys.stdout)
    parser.parse()
    atr = parser.parse()
    newfile = clientPath.replace('/clients.ldif', '/clients_new.ldif')
    f = open(newfile, 'w')
    base64Types = []
    for idx, val in enumerate(parser.entries):
        if 'displayName' in val:
            if val['displayName'][0] == 'Passport Resource Server Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
            elif val['displayName'][0] == 'SCIM Resource Server Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
            elif val['displayName'][0] == 'Passport Requesting Party Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
            elif val['displayName'][0] == 'SCIM Requesting Party Client':
                parser.entries[idx]["oxAuthGrantType"] = ['client_credentials']
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
    hostname = self.getOutput([self.hostname]).strip()
    for idx, val in enumerate(parser.entries):
        if 'displayName' in val:
            if len(val['oxId'][0]) > 1 and 'ou=resource_sets' in parser.getDNs()[idx]:
                parser.getDNs()[idx] = parser.getDNs()[idx].replace('inum=' + val['inum'][0],
                                                                    'oxId=' + val['oxId'][0]).replace('resource_sets',
                                                                                                      'resources')
            if val['oxId'][0] == 'scim_access':
                parser.entries[idx]["oxId"] = ['https://' + hostname + '/oxauth/restv1/uma/scopes/scim_access']
            elif val['oxId'][0] == 'passport_access':
                parser.entries[idx]["oxId"] = ['https://' + hostname + '/oxauth/restv1/uma/scopes/passport_access']
            if val['displayName'][0] == 'SCIM Resource Set':
                parser.entries[idx]["oxResource"] = ['https://' + hostname + '/identity/restv1/scim/v1']
                parser.entries[idx]['oxAssociatedClient'] = [
                    ('inum=' + scimClient + ',ou=clients,o=' + inumOrg + ",o=gluu").replace("\n", '')]
            elif val['displayName'][0] == 'Passport Resource Set':
                parser.entries[idx]["oxResource"] = ['https://' + hostname + '/identity/restv1/passport/config']
                parser.entries[idx]['oxAssociatedClient'] = [
                    ('inum=' + passportClient + ',ou=clients,o=' + inumOrg + ",o=gluu").replace("\n", '')]

        out = CreateLDIF(parser.getDNs()[idx], parser.entries[idx], base64_attrs=base64Types)
        f.write(out)
    f.close()
    os.remove(UmaPath)
    os.rename(newfile, UmaPath)


def doOxTrustChanges(self, oxTrustPath):
    parser = MyLDIF(open(oxTrustPath, 'rb'), sys.stdout)
    parser.targetAttr = "oxTrustConfApplication"
    atr = parser.parse()
    oxTrustConfApplication = parser.lastEntry['oxTrustConfApplication'][0]
    oxTrustConfApplication = oxTrustConfApplication.replace('seam/resource/', '')
    parser.lastEntry['oxTrustConfApplication'][0] = oxTrustConfApplication
    base64Types = ["oxTrustConfApplication", "oxTrustConfImportPerson", "oxTrustConfCacheRefresh"]

    out = CreateLDIF(parser.lastDN, parser.getLastEntry(), base64_attrs=base64Types)
    newfile = oxTrustPath.replace('/oxtrust_config.ldif', '/oxtrust_config_new.ldif')
    # print (newfile)
    f = open(newfile, 'w')
    f.write(out)
    f.close()

    os.remove(oxTrustPath)
    os.rename(newfile, oxTrustPath)


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
        self.backupDir = os.path.join(backupRoot, 'backup_3031')
        
        self.foldersToBackup = ['/etc/certs',
                                '/etc/gluu/conf',
                                '/opt/shibboleth-idp/conf',
                                '/opt/shibboleth-idp/metadata',
                                '/opt/gluu/jetty/identity/custom',
                                '/opt/gluu/jetty/identity/lib',
                                '/opt/gluu/jetty/oxauth/custom',
                                '/opt/gluu/jetty/oxauth/lib',
                                '/var/ox/photos',
                                ]
        self.passwordFile = tempfile.mkstemp()[1]

        self.ldapsearch = '/opt/opendj/bin/ldapsearch'
        self.slapcat = '/opt/symas/bin/slapcat'
        self.mkdir = '/bin/mkdir'
        self.cat = '/bin/cat'
        self.grep = '/bin/grep'
        self.hostname = '/bin/hostname'

        

        ldap_type = self.getProp('ldap_type')
        if not ldap_type:
            ldap_type = 'openldap'
        if ldap_type == 'openldap':
            bind_dn = '"cn=directory manager,o=gluu"'
            bind_dn_site = '"cn=directory manager,o=site"'
        elif ldap_type == 'opendj':
            bind_dn = '"cn=directory manager"'
            bind_dn_site = '"cn=directory manager"'
        
        self.cur_ldap = ldap_type
        
        self.ldapCreds = ['-h', 'localhost', '-p', '1636', '-Z', '-X', '-D',
                          bind_dn, '-j', self.passwordFile]

        self.ldapCredsSite = ['-h', 'localhost', '-p', '1636', '-Z', '-X', '-D',
                          bind_dn_site, '-j',
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
                logging.debug("Running command : %s" % " ".join(args))
                p = subprocess.Popen(args, stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE)
                output, error = p.communicate()
                if error:
                    logging.error(error)
                    logging.debug(output)
                return output
            except:
                logging.error("Error running command : %s" % " ".join(args))
                logging.error(traceback.format_exc())
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

        ldap_type = self.getProp('ldap_type')
    
        #if setup.properties.last does not contain ldap_type, make it openldap
        if not ldap_type:
             ldap_type = 'openldap'


        if ldap_type == 'openldap':
            copyfile('/opt/gluu/schema/openldap/custom.schema', self.backupDir + "/custom.schema")
        elif ldap_type == 'opendj':
            if not os.path.exists(self.backupDir + '/opt/opendj/config/schema/'):
                os.makedirs(self.backupDir + '/opt/opendj/config/schema/')
            for sf in os.listdir('/opt/opendj/config/schema'):
                if sf.split('-')[0] not in ['00','01','02','03','04','05','06','77','96','101']:
                    copyfile(os.path.join('/opt/opendj/config/schema', sf), self.backupDir + '/opt/opendj/config/schema/'+sf)

    def runAndLog(self, args):
        try:
            logging.debug("Running command : %s" % " ".join(args))
            
            p = subprocess.Popen(' '.join(args), shell=True, stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE)
            so, se = p.communicate()
            
            if se:
                logging.error(se)
                logging.debug(traceback.format_exc())
            return so
        except:
            logging.error("Error running command : %s" % " ".join(args))
            logging.error(traceback.format_exc())
            sys.exit(1)

    def getLdif(self):
        logging.info('Creating backup of LDAP data')
        orgInum = self.inumOrg
        oxVersion = self.getProp('oxVersion')[:5].strip()
        # Backup the data
        for basedn in self.base_dns:
            ou = basedn.split("=")[-1]
            if ou =='people':
                logging.info('Backing up o=%s, may take time' % ou)
            
            out_file = "%s/ldif/%s.ldif" % (self.backupDir, ou)
            args = [self.ldapsearch] + self.ldapCreds + [
                '-b', "'%s,o=%s,o=gluu'" % (basedn, orgInum), 'objectclass=*', '>', out_file]

            self.runAndLog(args)

        if oxVersion < '3.1.2':
            #removeDeprecatedScripts(self, "%s/ldif/scripts.ldif" % self.backupDir)
            doClientsChangesForUMA2(self, "%s/ldif/clients.ldif" % self.backupDir)
            doUmaResourcesChangesForUma2(self, "%s/ldif/uma.ldif" % self.backupDir)
            changePassportConfigJson(self, "%s/etc/gluu/conf/passport-config.json" % self.backupDir)

        # Backup the appliance config
        out_file = "%s/ldif/appliance.ldif" % self.backupDir
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


        # Backup the oxidp config

        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                '"ou=appliances,o=gluu"',
                'objectclass=oxApplicationConfiguration', '>', "%s/ldif/oxidp_config.ldif" % self.backupDir]
        self.runAndLog(args)
        
        # Backup the oxtrust config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                '"ou=appliances,o=gluu"',
                '"objectclass=oxTrustConfiguration"', '>', "%s/ldif/oxtrust_config.ldif" % self.backupDir]
        self.runAndLog(args)

        if oxVersion < '3.1.2':
            doOxTrustChanges(self, "%s/ldif/oxtrust_config.ldif" % self.backupDir)
            doApplinceChanges("%s/ldif/appliance.ldif" % self.backupDir)

        # Backup the oxauth config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                '"ou=appliances,o=gluu"',
                '"objectclass=oxAuthConfiguration"', '>', "%s/ldif/oxauth_config.ldif" % self.backupDir]
        self.runAndLog(args)

        if oxVersion < '3.1.2':
             dooxAuthChangesFor31(self, "%s/ldif/oxauth_config.ldif" % self.backupDir)

        # Backup the trust relationships
        args = [self.ldapsearch] + self.ldapCreds + [
            '-b', '"ou=appliances,o=gluu"', '"objectclass=gluuSAMLconfig"', '>', "%s/ldif/trust_relationships.ldif" % self.backupDir]
        self.runAndLog(args)


        # Backup the Passport Configurations
        args = [self.ldapsearch] + self.ldapCreds + [
            '-b', '"ou=appliances,o=gluu"', '"objectclass=oxPassportConfiguration"', '>', "%s/ldif/passport.ldif" % self.backupDir]
        self.runAndLog(args)


        # Backup the org
        args = [self.ldapsearch] + self.ldapCreds + [
            '-s', 'base', '-b', 'o=%s,o=gluu' % orgInum, 'objectclass=*', '>', "%s/ldif/organization.ldif" % self.backupDir]
        self.runAndLog(args)
        logging.info('Backing up o=site, may take time')


        # Backup o=site
        args = [self.ldapsearch] + self.ldapCredsSite + [
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


    def genProperties(self):
        logging.info('Creating setup.properties backup file')
        props = {}
        props['ldapPass'] = self.getOutput([self.cat, self.passwordFile]).strip()

        props['ldap_type'] = 'opendj'
        props['hostname'] = self.getOutput([self.hostname]).strip()
        props['inumAppliance'] = self.getOutput(
            [self.grep, "^inum", "%s/ldif/appliance.ldif" % self.backupDir]
        ).split("\n")[0].split(":")[-1].strip()
        props['inumApplianceFN'] = self.clean(props['inumAppliance'])
        props['inumOrg'] = self.inumOrg
        props['inumOrgFN'] = self.clean(props['inumOrg'])
        props['baseInum'] = props['inumOrg'][:21]
        props['encode_salt'] = self.getOutput(
            [self.cat, "%s/etc/gluu/conf/salt" % self.backupDir]
        ).split("=")[-1].strip()

        props['oxauth_client_id'] = self.getProp('oxauth_client_id')
        props['scim_rs_client_id'] = self.getProp('scim_rs_client_id')
        props['scim_rp_client_id'] = self.getProp('scim_rp_client_id')
        props['passport_rp_client_id'] = self.getProp('passport_rp_client_id')
        props['passport_rs_client_id'] = self.getProp('passport_rs_client_id')
        props['version'] = self.getProp('githubBranchName').replace(
            'version_', '')
        # As the certificates are copied to the new installation, their pass
        # are required for accessing them and validating them
        props['httpdKeyPass'] = self.getProp('httpdKeyPass')
        props['shibJksPass'] = self.getProp('shibJksPass')
        props['asimbaJksPass'] = self.getProp('asimbaJksPass')

        # Preferences for installation of optional components
        props['installSaml'] = os.path.isfile(
            '/opt/shibboleth-idp/conf/idp.properties')
        props['shibboleth_version'] = 'v3'
        props['installAsimba'] = os.path.isfile(
            '/opt/gluu/jetty/asimba/webapps/asimba.war')
        props['installOxAuthRP'] = os.path.isfile(
            '/opt/gluu/jetty/oxauth-rp/webapps/oxauth-rp.war')
        props['installPassport'] = os.path.isfile(
            '/opt/gluu/node/passport/server/app.js')

        f = open(self.propertiesFn, 'w')
        for key in props.keys():
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

        replacements = {'ds-cfg-size-limit: 1000':'ds-cfg-size-limit: 100000'}

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

        replacements = {'ds-cfg-size-limit: 1000':'ds-cfg-size-limit: 100000'}

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


    def fixLdapBindDN(self):
        
        applience_fn = os.path.join(self.backupDir, 'ldif/appliance.ldif')
        parser = MyLDIF(open(applience_fn, 'rb'), None, True)
        parser.parse()
        tmp_fn = '/tmp/appliance.ldif'
        processed_fp = open(tmp_fn, 'w')
        ldif_writer = LDIFWriter(processed_fp)
        
        for dn, entry in parser.dn_entry:
            if 'oxIDPAuthentication' in entry:
                tmp_json = json.loads(entry['oxIDPAuthentication'][0])
                tmp_config = json.loads(tmp_json['config'])
                tmp_config['bindDN'] = 'cn=Directory Manager'
                tmp_json['config'] = json.dumps(tmp_config)
                entry['oxIDPAuthentication'] = [json.dumps(tmp_json)]

            ldif_writer.unparse(dn, entry)
        
        processed_fp.close()
        os.system('cp {0} {1}'.format(tmp_fn, applience_fn))
        os.remove(tmp_fn)
        
        
        oxtrust_config_fn = os.path.join(self.backupDir, 'ldif/oxtrust_config.ldif')
        parser = MyLDIF(open(oxtrust_config_fn, 'rb'), None, True)
        parser.parse()
        tmp_fn = '/tmp/oxtrust_config.ldif'
        processed_fp = open(tmp_fn, 'w')
        ldif_writer = LDIFWriter(processed_fp)
        
        for dn, entry in parser.dn_entry:
            if 'oxTrustConfCacheRefresh' in entry:
                tmp_json = json.loads(entry['oxTrustConfCacheRefresh'][0])
                tmp_json['inumConfig']['bindDN'] = 'cn=Directory Manager'
                entry['oxTrustConfCacheRefresh'] = [json.dumps(tmp_json)]

            ldif_writer.unparse(dn, entry)
        
        processed_fp.close()
        os.system('cp {0} {1}'.format(tmp_fn, oxtrust_config_fn))
        os.remove(tmp_fn)

    def export(self):
        # Call the sequence of functions that would backup the various stuff
        print("-------------------------------------------------------------")
        print("            Gluu Server Data Export Tool For v3.1.x          ")
        print("-------------------------------------------------------------")
        print("")
        # self.stopOpenDJ()
        # self.editLdapConfig()
        # self.startOpenDJ()
        self.inumOrg = self.getProp('inumOrg')
        self.prepareLdapPW()
        self.makeFolders()
        self.backupFiles()
        self.getLdif()
        self.genProperties()
        self.removeLdapConfig()
        if self.cur_ldap == 'openldap':
            self.fixLdapBindDN()
        print("")
        print("-------------------------------------------------------------")
        print("The data has been exported to %s" % self.backupDir)
        print("-------------------------------------------------------------")


if __name__ == "__main__":
    exporter = Exporter()
    exporter.export()
