#!/usr/bin/python

# The MIT License (MIT)
#
# Copyright (c) 2014 Gluu
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.


import os
import os.path
import Properties
import random
import shutil
import socket
import string
import time
import uuid
import json
import traceback
import subprocess
import sys
import getopt

class Setup(object):
    def __init__(self):
        self.setup_properties_fn = "./setup.properties"
        self.log = 'setup.log'
        self.logError = 'setup_error.log'
        self.savedProperties = "./setup.properties.last"
        self.gluuOptFolder = "/opt/gluu"
        self.gluuOptBinFolder = "/opt/gluu/bin"

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.orgShortName = None
        self.countryCode = None
        self.city = None
        self.state = None

        self.encoded_ox_ldap_pw = None
        self.encoded_ldap_pw = None
        self.oxauthClient_encoded_pw = None
        self.baseInum = None
        self.inumOrg = None
        self.inumAppliance = None
        self.inumOrgFN = None
        self.inumApplianceFN = None
        self.ldapBaseFolderldapPass = None
        self.oxauth_client_id = None
        self.oxauthClient_pw = None
        self.encode_salt = "123456789012345678901234"

        self.outputFolder = './output'
        self.templateFolder = './templates'
        self.staticFolder = './static/opendj'
        self.tomcatHome = '/opt/tomcat'
        self.configFolder = '/etc/gluu/config'
        self.indexJson = './static/opendj/opendj_index.json'
        self.certFolder = '/etc/certs'
        self.gluuHome = '/opt/gluu'
        self.oxauth_error_json = 'static/oxauth/oxauth-errors.json'


        self.httpdKeyPass = None
        self.httpdKeyFn = '%s/httpd.key' % self.certFolder
        self.httpdCertFn = '%s/httpd.crt' % self.certFolder
        self.shibJksPass = None
        self.shibJksFn = '%s/shibIDP.jks' % self.certFolder
        self.tomcatJksPass = None
        self.tomcatJksFn = '%s/tomcat.jks' % self.certFolder

        self.ldap_type = "opendj"
        self.ldap_binddn = 'cn=directory manager'
        self.ldap_port = '1389'
        self.ldaps_port = '1636'
        self.ldapBaseFolder = '/opt/opendj'
        self.ldapStartTimeOut = 30
        self.ldapSetupCommand = '%s/setup' % self.ldapBaseFolder
        self.ldapDsconfigCommand = "%s/bin/dsconfig" % self.ldapBaseFolder
        self.ldapDsCreateRcCommand = "%s/bin/create-rc-script" % self.ldapBaseFolder
        self.ldapDsJavaPropCommand = "%s/bin/dsjavaproperties" % self.ldapBaseFolder
        self.ldapPassFn = '/opt/opendj/.pw'
        self.importLdifCommand = '%s/bin/import-ldif' % self.ldapBaseFolder
        self.schemaFolder = "%s/template/config/schema" % self.ldapBaseFolder
        self.org_custom_schema = "%s/config/schema/100-user.ldif" % self.ldapBaseFolder
        self.schemaFiles = ["static/%s/96-eduperson.ldif" % self.ldap_type,
                            "static/%s/101-ox.ldif" % self.ldap_type,
                            "static/%s/77-customAttributes.ldif" % self.ldap_type,
                            "output/100-user.ldif"]
        self.gluuScriptFiles = ['static/scripts/logmanager.sh',
                                'static/scripts/testBind.py']
        self.init_files = ['static/tomcat/tomcat', 'static/opendj/opendj']

        self.ldap_start_script = '/etc/init.d/opendj'
        self.apache_start_script = '/etc/init.d/httpd'
        self.tomcat_start_script = '/etc/init.d/tomcat'

        self.ldapEncodePWCommand = '%s/bin/encode-password' % self.ldapBaseFolder
        self.oxEncodePWCommand = '%s/bin/encode.py' % self.gluuHome
        self.keytoolCommand = '/usr/java/latest/bin/keytool'
        self.opensslCommand = '/usr/bin/openssl'
        self.defaultTrustStoreFN = '/usr/java/latest/jre/lib/security/cacerts'
        self.defaultTrustStorePW = 'changeit'

        self.oxtrust_openid_client_id = None
        self.oxtrust_uma_client_id = None

        # Stuff that gets rendered; filname is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.
        self.oxauth_ldap_properties = '/opt/tomcat/conf/oxauth-ldap.properties'
        self.oxauth_config_xml = '/opt/tomcat/conf/oxauth-config.xml'
        self.oxTrust_properties = '/opt/tomcat/conf/oxTrust.properties'
        self.oxtrust_ldap_properties = '/opt/tomcat/conf/oxTrustLdap.properties'
        self.tomcat_server_xml = '/opt/tomcat/conf/server.xml'
        self.tomcat_gluuTomcatWrapper = '/opt/tomcat/conf/gluuTomcatWrapper.conf'
        self.tomcat_oxauth_static_conf_json = '/opt/tomcat/conf/oxauth-static-conf.json'
        self.eduperson_schema_ldif = '%s/config/schema/96-eduperson.ldif'
        self.apache2_conf = '/etc/httpd/conf/httpd.conf'
        self.apache2_ssl_conf = '/etc/httpd/conf.d/https_gluu.conf'
        self.etc_hosts = '/etc/hosts'
        self.etc_hostname = '/etc/hostname'
        self.ldif_base = '%s/base.ldif' % self.outputFolder
        self.ldif_appliance = '%s/appliance.ldif' % self.outputFolder
        self.ldif_attributes = '%s/attributes.ldif' % self.outputFolder
        self.ldif_scopes = '%s/scopes.ldif' % self.outputFolder
        self.ldif_clients = '%s/clients.ldif' % self.outputFolder
        self.ldif_people = '%s/people.ldif' % self.outputFolder
        self.ldif_groups = '%s/groups.ldif' % self.outputFolder
        self.ldif_site = './static/cache-refresh/o_site.ldif'
        self.encode_script = '%s/bin/encode.py' % self.gluuHome

        self.ldap_setup_properties = '%s/opendj-setup.properties' % self.templateFolder

        self.ldif_files = [self.ldif_base,
                           self.ldif_appliance,
                           self.ldif_attributes,
                           self.ldif_scopes,
                           self.ldif_clients,
                           self.ldif_people,
                           self.ldif_groups,
                           self.ldif_site]

        self.ce_templates = {self.oxauth_ldap_properties: True,
                     self.oxauth_config_xml: True,
                     self.oxTrust_properties: True,
                     self.oxtrust_ldap_properties: True,
                     self.tomcat_server_xml: True,
                     self.tomcat_gluuTomcatWrapper: True,
                     self.tomcat_oxauth_static_conf_json: True,
                     self.ldap_setup_properties: False,
                     self.org_custom_schema: False,
                     self.apache2_conf: True,
                     self.apache2_ssl_conf: True,
                     self.etc_hosts: True,
                     self.etc_hostname: True,
                     self.ldif_base: False,
                     self.ldif_appliance: False,
                     self.ldif_attributes: False,
                     self.ldif_scopes: False,
                     self.ldif_clients: False,
                     self.ldif_people: False,
                     self.ldif_groups: False}

    def __repr__(self):
        return ( 'hostname'.ljust(20) + self.hostname.rjust(40) + "\n"
                 + 'ip'.ljust(20) + self.ip.rjust(40) + "\n"
                 + 'orgName'.ljust(20) + self.orgName.rjust(40) + "\n"
                 + 'countryCode'.ljust(20) + self.countryCode.rjust(40) + "\n"
                 + 'city'.ljust(20) + self.city.rjust(40) + "\n"
                 + 'state'.ljust(20) + self.state.rjust(40) + "\n"
                 + 'ldapPass'.ljust(20) + self.ldapPass.rjust(40) + "\n")

    def logIt(self, msg, errorLog=False):
        if errorLog:
            f = open(self.logError, 'a')
            f.write('%s %s\n' % (time.strftime('%X %x'), msg))
            f.close()
        f = open(self.log, 'a')
        f.write('%s %s\n' % (time.strftime('%X %x'), msg))
        f.close()

    # args = command + args, i.e. ['ls', '-ltr']
    def run(self, args):
        self.logIt('Running: %s' % ' '.join(args))
        try:
            p = subprocess.Popen(args, stdout=subprocess.PIPE)
            output, err = p.communicate()
            if output:
                self.logIt(output)
            if err:
                self.logIt(err, True)
        except:
            self.logIt("Error running command : %s" % " ".join(args), True)
            self.logIt(traceback.format_exc(), True)

    def getQuad(self):
        return str(uuid.uuid4())[:4].upper()

    def isIP(self, address):
        try:
            socket.inet_aton(address)
            return True
        except socket.error:
            return False

    def getPW(self, size=12, chars=string.ascii_uppercase + string.digits + string.lowercase):
        return ''.join(random.choice(chars) for _ in range(size))

    def load_properties(self, fn):
        self.logIt('Loading Properties %s' % fn)
        p = Properties.Properties()
        try:
            p.load(open(fn))
            properties_list = p.keys()
            for prop in properties_list:
                try:
                    self.__dict__[prop] = p[prop]
                except:
                    self.logIt("Error loading property %s" % prop)
                    installObject.logIt(traceback.format_exc(), True)
        except:
            self.logIt("Error loading properties", True)
            self.logIt(traceback.format_exc(), True)

    def load_json(self, fn):
        self.logIt('Loading JSON from %s' % fn)
        try:
            json_file = open(fn)
            json_text = json_file.read()
            json_file.close()
            return json.loads(json_text)
        except:
            self.logIt("Unable to read or parse json file from %s" % fn, True)
            self.logIt(traceback.format_exc(), True)
        return None

    def check_properties(self):
        self.logIt('Checking properties')
        while not self.hostname:
            testhost = raw_input('Hostname of this server: ').strip()
            if len(testhost.split('.')) >= 3:
                self.hostname = testhost
            else:
                print 'The hostname has to be at least three domain components. Try again\n'
        while not self.ip:
            testIP = raw_input('IP address of the server %s : ' % self.hostname).strip()
            if self.isIP(testIP):
                self.ip = testIP
            else:
                print 'ERROR: The IP Address is invalid. Try again\n'
        while not self.orgName:
            self.orgName = raw_input('Organization Name (for ceritificate)').strip()
        while not self.countryCode:
            testCode = raw_input('2 Character Country Code (for ceritificate)').strip()
            if len(testCode) == 2:
                self.countryCode = testCode
            else:
                print 'Country code should only be two characters. Try again\n'
        while not self.city:
            self.city = raw_input('City (for certificate)').strip()
        while not self.state:
            self.state = raw_input('State or Province (for certificate)').strip()
        if not self.httpdKeyPass:
            self.httpdKeyPass = self.getPW()
        if not self.tomcatJksPass:
            self.tomcatJksPass = self.getPW()
        if not self.ldapPass:
            self.ldapPass = self.getPW()
        if not self.shibJksPass:
            self.shibJksPass = self.getPW()
        if not self.encode_salt:
            self.encode_salt= self.getPW() + self.getPW()
        if not self.baseInum:
            self.baseInum = '@!%s.%s.%s.%s' % tuple([self.getQuad() for i in xrange(4)])
        if not self.inumOrg:
            twoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.inumOrg = '%s!0001!%s' % (self.baseInum, twoQuads)
        if not self.inumAppliance:
            twoQuads = '%s.%s' % tuple([self.getQuad() for i in xrange(2)])
            self.inumAppliance = '%s!0002!%s' % (self.baseInum, twoQuads)
        if not self.oxauth_client_id:
            self.oxauth_client_id = '%s!0008!%s' % (self.baseInum, twoQuads)
        if not self.inumApplianceFN:
            self.inumApplianceFN = self.inumAppliance.replace('@', '').replace('!', '').replace('.', '')
        if not self.inumOrgFN:
            self.inumOrgFN = self.inumOrg.replace('@', '').replace('!', '').replace('.', '')

    def save_properties(self):
        self.logIt('Saving properties to %s' % self.savedProperties)
        def getString(object):
            if type(object) == type(""):
                return object.strip()
            else:
                return ''
        try:
            p = Properties.Properties()
            keys = self.__dict__.keys()
            keys.sort()
            for key in keys:
                value = getString(self.__dict__[key])
                if value != '':
                    p[key] = value
            p.store(open(self.savedProperties, 'w'))
        except:
            self.logIt("Error saving properties", True)
            self.logIt(traceback.format_exc(), True)

    def gen_cert(self, suffix, password):
        self.logIt('Generating Certificate for %s' % suffix)
        key_with_password = '%s/%s.key.orig' % (self.certFolder, suffix)
        key = '%s/%s.key' % (self.certFolder, suffix)
        csr = '%s/%s.csr' % (self.certFolder, suffix)
        public_certificate = '%s/%s.crt' % (self.certFolder, suffix)
        self.run([self.opensslCommand,
                  'genrsa',
                  '-des3',
                  '-out',
                  key_with_password,
                  '-passout',
                  'pass:%s' % password,
                  '2048'
        ])
        self.run([self.opensslCommand,
                  'rsa',
                  '-in',
                  key_with_password,
                  '-passin',
                  'pass:%s' % password,
                  '-out',
                  key
        ])
        self.run([self.opensslCommand,
                  'req',
                  '-new',
                  '-key',
                  key,
                  '-out',
                  csr,
                  '-subj',
                  '/CN=%s/O=%s/C=%s/ST=%s/L=%s' % (self.hostname, self.orgName, self.countryCode, self.state, self.city)
        ])
        self.run([self.opensslCommand,
                  'x509',
                  '-req',
                  '-days',
                  '365',
                  '-in',
                  csr,
                  '-signkey',
                  key,
                  '-out',
                  public_certificate
        ])

    def gen_keystore(self, suffix, keystoreFN, keystorePW, inKey, inCert):
        self.logIt("Creating keystore %s" % suffix)
        # Convert key to pkcs12
        self.run([self.opensslCommand,
                  'pkcs12',
                  '-export',
                  '-inkey',
                  '%s/%s.key' % (self.certFolder, suffix),
                  '-in',
                  '%s/%s.crt' % (self.certFolder, suffix),
                  '-out',
                  '%s/%s.pkcs12' % (self.certFolder, suffix),
                  '-name',
                  self.hostname,
                  '-passout',
                  'pass:%s' % keystorePW
        ])
        # Import p12 to keystore
        self.run([self.keytoolCommand,
                  '-importkeystore',
                  '-srckeystore',
                  '%s/%s.pkcs12' % (self.certFolder, suffix),
                  '-srcstorepass',
                  keystorePW,
                  '-srcstoretype',
                  'PKCS12',
                  '-destkeystore',
                  keystoreFN,
                  '-deststorepass',
                  keystorePW,
                  '-deststoretype',
                  'JKS',
                  '-keyalg',
                  'RSA',
                  '-noprompt'
        ])

    def gen_crypto(self):
        try:
            self.logIt('Generating certificates and keystores')
            self.gen_cert('httpd', self.httpdKeyPass)
            self.gen_cert('tomcat', self.tomcatJksPass)
            self.gen_cert('shibIDP', self.shibJksPass)
            self.gen_keystore('tomcat',
                              self.tomcatJksFn,
                              self.tomcatJksPass,
                              '%s/tomcat.key' % self.certFolder,
                              '%s/tomcat.crt' % self.certFolder)
            # Will be used soon...
            self.gen_keystore('shibIDP',
                              self.shibJksFn,
                              self.shibJksPass,
                              '%s/shibIDP.key' % self.certFolder,
                              '%s/shibIDP.crt' % self.certFolder)
        except:
            self.logIt("Error generating cyrpto")
            self.logIt(traceback.format_exc(), True)

    def copyFile(self, inFile, destFolder):
        try:
            shutil.copy(inFile, destFolder)
            self.logIt("Copied %s to %s" % (inFile, destFolder))
        except:
            self.logIt("Error copying %s to %s" % (inFile, destFolder), True)
            self.logIt(traceback.format_exc(), True)

    def add_ldap_schema(self):
        try:
            self.logIt("Copying LDAP schema")
            for schemaFile in self.schemaFiles:
                self.copyFile(schemaFile, self.schemaFolder)
            self.run(['chown', '-R', 'ldap:ldap', self.ldapBaseFolder])
        except:
            self.logIt("Error adding schema")
            self.logIt(traceback.format_exc(), True)

    def encode_passwords(self):
        self.logIt("Encoding passwords")
        try:
            cmd = "%s -f %s -s SSHA" % (self.ldapEncodePWCommand, self.ldapPassFn)
            self.encoded_ldap_pw = os.popen(cmd, 'r').read().strip()
            cmd = "%s %s" % (self.oxEncodePWCommand, self.ldapPass)
            self.encoded_ox_ldap_pw = os.popen(cmd, 'r').read().strip()
            self.oxauthClient_pw = self.getPW()
            cmd = "%s %s" % (self.oxEncodePWCommand, self.oxauthClient_pw)
            self.oxauthClient_encoded_pw = os.popen(cmd, 'r').read().strip()
        except:
            self.logIt("Error encoding passwords", True)
            self.logIt(traceback.format_exc(), True)

    def setup_opendj(self):
        self.logIt("Running OpenDJ Setup")
        try:
            self.add_ldap_schema()
        except:
            self.logIt('Error adding ldap schema', True)
            self.logIt(traceback.format_exc(), True)

        # Copy opendj-setup.properties so user ldap can find it in /opt/opendj
        setupPropsFN = os.path.join(self.ldapBaseFolder, 'opendj-setup.properties')
        shutil.copy("%s/opendj-setup.properties" % self.outputFolder, setupPropsFN)
        self.change_ownership()
        try:
            setupCmd = "cd /opt/opendj ; " + " ".join([self.ldapSetupCommand,
                                      '--no-prompt',
                                      '--cli',
                                      '--propertiesFilePath',
                                      setupPropsFN,
                                      '--acceptLicense'])
            self.run(['/bin/su',
                      'ldap',
                      '-c',
                      setupCmd
                    ])
        except:
            self.logIt("Error running LDAP setup script", True)
            self.logIt(traceback.format_exc(), True)

        try:
            dsjavaCmd = "cd /opt/opendj/bin ; %s" % self.ldapDsJavaPropCommand
            self.run(['/bin/su',
                      'ldap',
                      '-c',
                      dsjavaCmd
            ])
        except:
            self.logIt("Error running dsjavaproperties", True)
            self.logIt(traceback.format_exc(), True)

    def configure_opendj(self):
        try:
            self.logIt("Making LDAP configuration changes")
            config_changes = [['set-global-configuration-prop', '--set', 'single-structural-objectclass-behavior:accept'],
                              ['set-attribute-syntax-prop', '--syntax-name', '"Directory String"',   '--set', 'allow-zero-length-values:true'],
                              ['set-password-policy-prop', '--policy-name', '"Default Password Policy"', '--set', 'allow-pre-encoded-passwords:true'],
                              ['set-log-publisher-prop', '--publisher-name', '"File-Based Audit Logger"', '--set', 'enabled:true'],
                              ['create-backend', '--backend-name', 'site', '--set', 'base-dn:o=site', '--type local-db', '--set', 'enabled:true']]
            for changes in config_changes:
                dsconfigCmd = " ".join(['cd %s/bin ; ' % self.ldapBaseFolder,
                                         self.ldapDsconfigCommand,
                                         '--trustAll',
                                         '--no-prompt',
                                         '--hostname',
                                         'localhost',
                                         '--port',
                                         '4444',
                                         '--bindDN',
                                         '"%s"' % self.ldap_binddn,
                                         '--bindPasswordFile',
                                         self.ldapPassFn] + changes)
                self.run(['/bin/su',
                         'ldap',
                         '-c',
                         dsconfigCmd])
        except:
            self.logIt("Error executing config changes", True)
            self.logIt(traceback.format_exc(), True)

    def index_opendj(self):
        try:
            self.logIt("Running LDAP index creation commands")
            # This json file contains a mapping of the required indexes.
            # [ { "attribute": "inum", "type": "string", "index": ["equality"] }, ...}
            index_json = self.load_json(self.indexJson)
            if index_json:
                for attrDict in index_json:
                    attr_name = attrDict['attribute']
                    index_types = attrDict['index']
                    for index_type in index_types:
                        self.logIt("Creating %s index for attribute %s" % (index_type, attr_name))
                        indexCmd = " ".join(['cd %s/bin ; ' % self.ldapBaseFolder,
                                            self.ldapDsconfigCommand,
                                            'create-local-db-index',
                                            '--backend-name',
                                            'userRoot',
                                            '--type',
                                            'generic',
                                            '--index-name',
                                            attr_name,
                                            '--set',
                                            'index-type:%s' % index_type,
                                            '--set',
                                            'index-entry-limit:4000',
                                            '--hostName',
                                            'localhost',
                                            '--port',
                                            '4444',
                                            '--bindDN',
                                            '"%s"' % self.ldap_binddn,
                                            '-j', self.ldapPassFn,
                                            '--trustAll',
                                            '--noPropertiesFile',
                                            '--no-prompt'])
                        self.run(['/bin/su',
                          'ldap',
                          '-c',
                          indexCmd])
            else:
                self.logIt('NO indexes found %s' % self.indexJson, True)
        except:
            self.logIt("Error occured during LDAP indexing", True)
            self.logIt(traceback.format_exc(), True)

    def import_ldif(self):
        self.logIt("Importing userRoot LDIF data")
        ldifFolder = '%s/ldif' % self.ldapBaseFolder
        for ldif_file_fn in self.ldif_files:
            ldifFolder = '%s/ldif' % self.ldapBaseFolder
            self.copyFile(ldif_file_fn, ldifFolder)
            ldif_file_fullpath = "%s/ldif/%s" % (self.ldapBaseFolder,
                                                 os.path.split(ldif_file_fn)[-1])
            self.run(['/bin/chown', 'ldap:ldap', ldif_file_fullpath])
            importCmd = " ".join(['cd %s/bin ; ' % self.ldapBaseFolder,
                                  self.importLdifCommand,
                                  '--ldifFile',
                                  ldif_file_fullpath,
                                  '--backendID',
                                  'userRoot',
                                  '--hostname',
                                  'localhost',
                                  '--port',
                                  '4444',
                                  '--bindDN',
                                  '"%s"' % self.ldap_binddn,
                                  '-j',
                                  self.ldapPassFn,
                                  '--append',
                                  '--trustAll'])
            self.run(['/bin/su',
                      'ldap',
                      '-c',
                      '%s' % importCmd])

        self.logIt("Importing site LDIF")
        self.copyFile("static/cache-refresh/o_site.ldif", ldifFolder)
        site_ldif_fn = "%s/o_site.ldif" % ldifFolder
        self.run(['/bin/chown', 'ldap:ldap', site_ldif_fn])
        importCmd = " ".join(['cd %s/bin ; ' % self.ldapBaseFolder,
                              self.importLdifCommand,
                              '--ldifFile',
                              site_ldif_fn,
                              '--backendID',
                              'site',
                              '--hostname',
                              'localhost',
                              '--port',
                              '4444',
                              '--bindDN',
                              '"%s"' % self.ldap_binddn,
                              '-j',
                              self.ldapPassFn,
                              '--append',
                              '--trustAll'])
        self.run(['/bin/su',
                  'ldap',
                  '-c',
                  '%s' % importCmd])

    ### Change hostname in the relevant files
    def render_templates(self):
        self.logIt("Rendering templates")
        for fullPath in self.ce_templates.keys():
            try:
                self.logIt("Rendering template %s" % fullPath)
                fn = os.path.split(fullPath)[-1]
                f = open(os.path.join(self.templateFolder, fn))
                template_text = f.read()
                f.close()
                newFn = open(os.path.join(self.outputFolder, fn), 'w+')
                newFn.write(template_text % self.__dict__)
                newFn.close()
            except:
                self.logIt("Error writing template %s" % fullPath, True)
                self.logIt(traceback.format_exc(), True)

    def copy_output(self):
        self.logIt("Copying rendered templates to final destination")
        for dest_fn in self.ce_templates.keys():
            if self.ce_templates[dest_fn]:
                fn = os.path.split(dest_fn)[-1]
                output_fn = os.path.join(self.outputFolder, fn)
                try:
                    self.logIt("Copying %s to %s" % (output_fn, dest_fn))
                    shutil.copyfile(output_fn, dest_fn)
                except:
                    self.logIt("Error writing %s to %s" % (output_fn, dest_fn), True)
                    self.logIt(traceback.format_exc(), True)
        self.copyFile(self.oxauth_error_json, "%s/conf" % self.tomcatHome)

    def copy_scripts(self):
        self.logIt("Copying script files")
        for script in self.gluuScriptFiles:
            self.copyFile(script, self.gluuOptBinFolder)
        self.logIt("Rendering encode.py")
        try:
            f = open('%s/encode.py' % self.templateFolder)
            encode_script = f.read()
            f.close()
            f = open("%s/encode.py" % self.gluuOptBinFolder, 'w')
            f.write(encode_script % self.__dict__)
            f.close()
        except:
            self.logIt("Error rendering encode script")
            self.logIt(traceback.format_exc(), True)
        self.run(["chmod", '-R', '700', self.gluuOptBinFolder])

    def copy_init_files(self):
        for init_file in self.init_files:
            try:
                script_name = os.path.split(init_file)[-1]
                self.copyFile(init_file, "/etc/init.d")
                self.run(["chmod", "755", "/etc/init.d/%s" % script_name])
                self.run(["/sbin/chkconfig", script_name, "on"])
            except:
                self.logIt("Error copying script file %s to /etc/init.d" % init_file)
                self.logIt(traceback.format_exc(), True)

    def start_tomcat(self):
        try:
            self.logIt("Attempting to start tomcat")
            i = 0
            wait_time = 5
            print "Giving LDAP %i seconds to perform imports" % wait_time
            while i < wait_time:
                time.sleep(1)
                print ".",
                i = i + 1
            os.system("/etc/init.d/tomcat start")
        except:
            self.logIt("Error starting tomcat")
            self.logIt(traceback.format_exc(), True)

    def change_ownership(self):
        self.logIt("Changing ownership")
        self.run(['chown', '-R', 'tomcat:tomcat', self.tomcatHome])
        self.run(['chown', '-R', 'ldap:ldap', self.ldapBaseFolder])
        self.run(['chown', '-R', 'tomcat:tomcat', self.certFolder])

    def getPrompt(self, prompt, defaultValue=None):
        try:
            if defaultValue:
                user_input = raw_input("%s [%s] : " % (prompt, defaultValue)).strip()
                if user_input == '':
                    return defaultValue
                else:
                    return user_input
            else:
                input = False
                while not input:
                    user_input = raw_input("%s : " % prompt).strip()
                    if user_input != '':
                        input = True
                        return user_input
        except KeyboardInterrupt:
            sys.exit()
        except:
            return None

    def writeLdapPW(self):
        try:
            f = open(self.ldapPassFn, 'w')
            f.write(self.ldapPass)
            f.close()
        except:
            self.logIt("Error writing temporary LDAP password.")

    def deleteLdapPw(self):
        try:
            os.remove(self.ldapPassFn)
            os.remove(os.path.join(self.ldapBaseFolder, 'opendj-setup.properties'))
        except:
            self.logIt("Error deleting ldap pw. Make sure %s is deleted" % self.ldapPassFn)
            self.logIt(traceback.format_exc(), True)

    def makeFolders(self):
        try:
            if not os.path.exists(self.gluuOptFolder):
                os.makedirs(self.gluuOptFolder)
            if not os.path.exists(self.gluuOptBinFolder):
                os.makedirs(self.gluuOptBinFolder)
            if not os.path.exists(self.configFolder):
                os.makedirs(self.configFolder)
            if not os.path.exists(self.certFolder):
                os.makedirs(self.certFolder)
        except:
            self.logIt("Error making folders", True)
            self.logIt(traceback.format_exc(), True)

    def promptForProperties(self):
        detectedIP = None
        try:
            testSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            detectedIP = [(testSocket.connect(('8.8.8.8', 80)),
                           testSocket.getsockname()[0],
                           testSocket.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]
        except:
            installObject.logIt("No detected IP address", True)
            self.logIt(traceback.format_exc(), True)
        if detectedIP:
            installObject.ip = installObject.getPrompt("Enter IP Address", detectedIP)
        else:
            installObject.ip = installObject.getPrompt("Enter IP Address")
        detectedHostname = None
        try:
            detectedHostname = socket.gethostbyaddr(socket.gethostname())[0]
        except:
            try:
                detectedHostname = os.popen("/bin/hostname").read().strip()
            except:
                installObject.logIt("No detected hostname", True)
                self.logIt(traceback.format_exc(), True)
        if detectedHostname:
            installObject.hostname = installObject.getPrompt("Enter hostname", detectedHostname)
        else:
            installObject.hostname = installObject.getPrompt("Enter hostname")

        installObject.orgName = installObject.getPrompt("Enter Organization Name")
        installObject.city = installObject.getPrompt("Enter your city or locality")
        installObject.state = installObject.getPrompt("Enter your state or province")
        installObject.countryCode = installObject.getPrompt("Enter two-digit Country Code")
        randomPW = installObject.getPW()
        installObject.ldapPass = installObject.getPrompt("Optional: enter password for LDAP superuser", randomPW)

    def print_help(self):
        print "\nUse setup.py to configure your Gluu Server and to add initial data required for"
        print "oxAuth and oxTrust to start. If setup.properties is found in this folder, these"
        print "properties will automatically be used instead of the interactive setup."
        print "Options:"
        print ""
        print "    -h   Help"
        print "    -f   specify setup.properties file"
        print "    -n   No interactive prompt before install starts."

    def getOpts(self, argv):
        self.logIt("Parsing command line options")
        setup_properties = None
        noPrompt = False
        try:
            opts, args = getopt.getopt(argv, "hnf:")
        except getopt.GetoptError:
            self.print_help()
            sys.exit(2)
        for opt, arg in opts:
            if opt == '-h':
                self.print_help()
                sys.exit()
            elif opt == "-f":
                try:
                    if os.path.isfile(arg):
                        setup_properties = arg
                        self.logIt("setup.properties specified as %s" % arg)
                        print "Found setup properties %s\n" % arg
                    else:
                        print "\nOoops... %s file not found\n" % arg
                except:
                    print "\nOoops... %s file not found\n" % arg
            elif opt == "-n":
                self.logIt("-n option specified. No interactive confirmation before proceeding.")
                noPrompt = True
        return setup_properties, noPrompt

if __name__ == '__main__':
    installObject = Setup()
    setup_properties = None
    noPrompt = False
    if len(sys.argv) > 1:
        setup_properties, noPrompt = installObject.getOpts(sys.argv[1:])
    print "\nInstalling Gluu Server...\nSee %s for setup log.\nSee %s for error log.\n\n" % (installObject.log, installObject.logError)
    try:
        os.remove(installObject.log)
        installObject.logIt('Removed %s' % installObject.log)
    except:
        pass
    try:
        os.remove(installObject.logError)
        installObject.logIt('Removed %s' % installObject.logError)
    except:
        pass
    installObject.logIt("Installing Gluu Server", True)
    if setup_properties:
        installObject.logIt('%s Properties found!\n' % installObject.setup_properties_fn)
        installObject.load_properties(installObject.setup_properties_fn)
    elif os.path.isfile(installObject.setup_properties_fn):
        installObject.logIt('%s Properties found!\n' % installObject.setup_properties_fn)
        installObject.load_properties(installObject.setup_properties_fn)
    else:
        installObject.logIt("%s Properties not found. Interactive setup commencing..." % installObject.setup_properties_fn)
        installObject.promptForProperties()

    # Validate Properties
    installObject.check_properties()

    # Show to properties for approval
    print '\n%s\n' % `installObject`
    proceed = "NO"
    if not noPrompt:
        proceed = raw_input('Proceed with these values [Y|n] ').lower().strip()
    if (noPrompt or not len(proceed) or (len(proceed) and (proceed[0] == 'y'))):
        try:
            installObject.makeFolders()
            installObject.writeLdapPW()
            installObject.copy_scripts()
            installObject.encode_passwords()
            installObject.render_templates()
            installObject.gen_crypto()
            installObject.setup_opendj()
            installObject.configure_opendj()
            installObject.index_opendj()
            installObject.import_ldif()
            installObject.deleteLdapPw()
            installObject.copy_output()
            installObject.copy_init_files()
            installObject.change_ownership()
            installObject.start_tomcat()
            installObject.save_properties()
        except:
            installObject.logIt("***** Error caught in main loop *****", True)
            installObject.logIt(traceback.format_exc(), True)
    else:
        installObject.save_properties()
        print "Properties saved to %s. Change filename to %s if you want to re-use" % \
                         (installObject.savedProperties, installObject.setup_properties_fn)
