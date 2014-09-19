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
import Queue
import random
import shutil
import socket
import string
import subprocess
import time
import uuid
import json
import sys
import traceback

class Setup(object):
    def __init__(self):
        self.setup_properties_fn = "./setup.properties"
        self.log = './setup.log'
        self.logError = './error.log'
        self.savedProperties = "./setup.properties.last"

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
        self.blowfish_passphrase = None

        self.outputFolder = './output'
        self.templateFolder = './templates'
        self.staticFolder = './static/opendj'
        self.tomcatHome = '/opt/tomcat'
        self.configFolder = '/etc/gluu/config'
        self.indexJson = './static/opendj/opendj_index.json'
        self.certFolder = '/etc/certs'
        self.gluuHome = '/opt/gluu'

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
        self.ldapBaseFolder = '/opt/opendj'  # TODO I'd like this to be /opt/gluu-opendj
        self.ldapStartTimeOut = 30
        self.ldapSetupCommand = '%s/setup' % self.ldapBaseFolder
        self.ldapDsconfigCommand = "%s/bin/dsconfig" % self.ldapBaseFolder
        self.ldapDsCreateRcCommand = "%s/bin/create-rc-script" % self.ldapBaseFolder
        self.ldapDsJavaPropCommand = "%s/bin/dsjavaproperties" % self.ldapBaseFolder
        self.ldapPassFn = '%s/.pw' % self.outputFolder
        self.importLdifCommand = '%s/bin/import-ldif' % self.ldapBaseFolder
        self.schemaFolder = "%s/config/schema" % self.ldapBaseFolder
        self.org_custom_schema = "%s/config/schema/100-user.ldif" % self.ldapBaseFolder
        self.schemaFiles = ["static/%s/96-eduperson.ldif" % self.ldap_type,
                            "static/%s/101-ox.ldif" % self.ldap_type,
                            "static/%s/77-customAttributes.ldif" % self.ldap_type,
                            "output/100-user.ldif"]

        self.ldap_start_script = '/etc/init.d/opendj'  # TODO I'd like this to be /etc/init.d/gluu-opendj
        self.apache_start_script = '/etc/init.d/httpd'
        self.tomcat_start_script = '/etc/init.d/tomcat'

        self.ldapEncodePWCommand = '%s/bin/encode-password' % self.ldapBaseFolder
        self.oxEncodePWCommand = '%s/bin/encode.py' % self.gluuHome
        self.keytoolCommand = '/usr/java/latest/bin/keytool'
        self.opensslCommand = '/usr/bin/openssl'

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

        self.ce_files = {self.oxauth_ldap_properties: True,
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
                     self.ldif_groups: False,
                     self.encode_script: True}

    def __repr__(self):
        return ( 'hostname'.ljust(20) + self.hostname.rjust(40) + "\n"
                 + 'ip'.ljust(20) + self.ip.rjust(40) + "\n"
                 + 'orgName'.ljust(20) + self.orgName.rjust(40) + "\n"
                 + 'countryCode'.ljust(20) + self.countryCode.rjust(40) + "\n"
                 + 'city'.ljust(20) + self.city.rjust(40) + "\n"
                 + 'state'.ljust(20) + self.state.rjust(40) + "\n"
                 + 'tomcatJksPass'.ljust(20) + self.tomcatJksPass.rjust(40) + "\n"
                 + 'httpdKeyPass'.ljust(20) + self.httpdKeyPass.rjust(40) + "\n"
                 + 'shibIdpPass'.ljust(20) + self.shibJksPass.rjust(40) + "\n"
                 + 'ldapPass'.ljust(20) + self.ldapPass.rjust(40) + "\n" )

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
        if not self.shibJksPass    :
            self.shibJksPass     = self.getPW()
        if not self.blowfish_passphrase:
            self.blowfish_passphrase = self.getPW()
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
        self.logIt('Generating certificates and keystores')
        self.gen_cert('http', self.httpdKeyPass)
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

    def add_ldap_schema(self):
        self.logIt('Coping schema to %s' % self.schemaFolder)
        for schemaFile in self.schemaFiles:
            try:
                self.logIt(schemaFile, self.schemaFolder)
                shutil.copy(schemaFile, self.schemaFolder)
            except:
                self.logIt("Error copying %s" % schemaFile, True)
        self.run(['chown', '-R', 'ldap:ldap', self.ldapBaseFolder])

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

    def setup_ldap(self):
        self.logIt("Setting up ldap")
        try:
            self.run([self.ldapSetupCommand, '--no-prompt', '--cli',
                      '--propertiesFilePath', os.path.join(self.outputFolder, 'opendj-setup.properties'),
                      '--acceptLicense'])
            config_changes = [['set-global-configuration-prop',  '--set', 'single-structural-objectclass-behavior:accept'],
                              ['set-attribute-syntax-prop', '--syntax-name', 'Directory String',   '--set', 'allow-zero-length-values:true'],
                              ['set-password-policy-prop', '--policy-name', 'Default Password Policy', '--set', 'allow-pre-encoded-passwords:true'],
                              ['set-log-publisher-prop', '--publisher-name', 'File-Based Audit Logger', '--set', 'enabled:true'],
                              ['create-backend', '--backend-name', 'site', '--set-base-dn:o=site', '--set', 'enabled:true']]
            for changes in config_changes:
                self.run([self.ldapDsconfigCommand,
                          '--trustAll', '--no-prompt',
                          '--hostname',  'localhost',
                          '--port', '4444',
                          '--bindDN', self.ldap_binddn,
                          '--bindPasswordFile', self.ldapPassFn] + changes)
            # Load indexing
            index_json = self.load_json(self.indexJson)
            if index_json:
                for attrDict in index_json:
                    attr_name = attrDict['attribute']
                    index_types = attrDict['index']
                    for index_type in index_types:
                        self.create_local_db_index(attr_name, index_type, 'userRoot')
                self.create_local_db_index('inum', 'equality', 'site')
            else:
                self.logIt('NO indexes found %s' % self.indexJson, True)

            # Add opendj init script
            self.run([self.ldapDsCreateRcCommand,
                      '-f',
                     '/etc/init.d/opendj',
                     '-u',
                     'ldap'
            ])

        except:
            self.logIt("Error setting up LDAP", True)
            self.logIt(traceback.format_exc(), True)

    def import_ldif(self):
        self.logIt("Importing LDIF data", True)
        for fullPath in self.ldif_files:
            self.run([self.importLdifCommand,
                      '--ldifFile', fullPath,
                      '--includeBranch', 'o=gluu',
                      '--backendID', 'userRoot',
                      '--hostname', 'localhost',
                      '--port', '4444',
                      '--bindDN', self.ldap_binddn,
                      '-j', self.ldapPassFn,
                      '--append',
                      '--trustAll'])

    def create_local_db_index(self, attributeName, indexType, db):
        self.logIt("Creating %s index for attribute %s" % (indexType, attributeName))
        self.run([self.ldapDsconfigCommand, 'create-local-db-index',
                  '--backend-name', db,
                  '--type', 'generic',
                  '--index-name', attributeName,
                  '--set index-type:', indexType,
                  '--set index-entry-limit:', '4000',
                  '--hostName', 'localhost',
                  '--port', '4444',
                  '--bindDN', self.ldap_binddn,
                  '-j', self.ldapPassFn,
                  '--trustAll',
                  '--noPropertiesFile',
                  '--no-prompt'])

    ### Change hostname in the relevant files
    def render_templates(self):
        self.logIt("Rendering templates")
        for fullPath in self.ce_files.keys():
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
        for dest_fn in self.ce_files.keys():
            if self.ce_files[dest_fn]:
                fn = os.path.split(dest_fn)[-1]
                output_fn = os.path.join(self.outputFolder, fn)
                try:
                    self.logIt("Copying %s to %s" % (output_fn, dest_fn))
                    shutil.copyfile(output_fn, dest_fn)
                except:
                    self.logIt("Error writing %s to %s" % (output_fn, dest_fn), True)
                    self.logIt(traceback.format_exc(), True)

    def copy_static(self):
        self.logIt("Copying static files")
        # Placeholder to copy any files from the static folder
        # LDAP schema is copied in the add_ldap_schema method

    def change_ownership(self):
        self.logIt("Changing ownership")
        self.run(['chown', '-R', 'tomcat:tomcat', self.tomcatHome])
        self.run(['chown', '-R', 'ldap:ldap', self.ldapBaseFolder])
        self.run(['chown', '-R', 'tomcat:tomcat', self.certFolder])

    # Restarts either just LDAP or all services. Waits for LDAP to start before starting tomcat
    def restart_all_services(self):
        self.logIt("Restarting all services")
        self.run([self.ldap_start_script, 'restart'])
        self.run([self.apache_start_script, 'restart'])
        try:
            startOk = 'Directory Server has started successfully'
            tailq = Queue.Queue(maxsize=10)  # buffer at most 100 lines
            p = subprocess.Popen(['tail', '-f', '%s/logs/errors' % self.ldapBaseFolder], stdout=subprocess.PIPE)
            starttime = time.time()
            while 1:
                line = p.stdout.readline()
                self.logIt(line)
                tailq.put(line)
                if (time.time() - starttime > self.ldapStartTimeOut):
                    self.logIt('LDAP startup timed out. Tomcat not started.', True)
                    break
                if line.find(startOk) > -1:
                    self.logIt(startOk)
                    self.run([self.tomcat_start_script, 'restart'])
                    break
        except:
            self.logIt("Error restarting service", True)
            self.logIt(traceback.format_exc(), True)

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
        f = open(self.ldapPassFn, 'w+')
        f.write(self.ldapPass)
        f.close()

    def deleteLdapPw(self):
        try:
            os.remove(self.ldapPassFn)
        except:
            self.logIt("Error deleting ldap pw. Make sure %s is deleted" % self.ldapPassFn)

    def makeFolders(self):
        if not os.path.exists(self.outputFolder):
            os.makedirs(self.outputFolder)
        if not os.path.exists(self.configFolder):
            os.makedirs(self.configFolder)
        if not os.path.exists(self.certFolder):
            os.makedirs(self.certFolder)

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
        installObject.countryCode = installObject.getPrompt("Enter two-digit Country Code")
        installObject.city = installObject.getPrompt("Enter your city or locality")
        installObject.state = installObject.getPrompt("Enter your state or province")
        randomPW = installObject.getPW()
        installObject.httpdKeyPass = installObject.getPrompt("Optional: enter password for httpd private key", randomPW)
        installObject.ldapPass = installObject.getPrompt("Optional: enter password for LDAP superuser", randomPW)

    def setup_ldap_user(self):
        self.logIt("Setting up variables for user OpenDJ environment variables ldap")
        os.setgid(1389)
        os.setuid(1389)
        self.run(self.ldapDsJavaPropCommand)

if __name__ == '__main__':
    installObject = Setup()
    print "Installing Gluu Server\nSee %s for all logs, and %s for just errors," % (installObject.log, installObject.logError)
    try:
        os.remove(installObject.log)
    except:
        pass
    try:
        os.remove(installObject.logError)
    except:
        pass
    installObject.logIt("Installing Gluu Server", True)
    if os.path.isfile(installObject.setup_properties_fn):
        installObject.logIt('%s Properties found!\n' % installObject.setup_properties_fn)
        installObject.load_properties(installObject.setup_properties_fn)
    else:
        installObject.logIt("%s Properties not found. Interactive setup commencing..." % installObject.setup_properties_fn)
        installObject.promptForProperties()

    # Validate Properties
    installObject.check_properties()

    # Show to properties for approval
    print '\n%s\n' % `installObject`
    proceed = raw_input('Proceed with these values [Y|n] ').lower().strip()
    if (not len(proceed) or (len(proceed) and (proceed[0] == 'y'))):
        try:
            installObject.writeLdapPW()
            installObject.encode_passwords()
            installObject.render_templates()
            installObject.makeFolders()
            installObject.gen_crypto()
            installObject.add_ldap_schema()
            installObject.setup_ldap()
            installObject.import_ldif()
            installObject.copy_output()
            installObject.copy_static()
            installObject.change_ownership()
            installObject.restart_all_services()
            installObject.save_properties()
            installObject.setup_ldap_user()
        except:
            installObject.logIt("Error caught in main loop")
            installObject.logIt(traceback.format_exc(), True)
        finally:
            installObject.deleteLdapPw()
    else:
        installObject.save_properties()
        print "Properties saved to %s. Change filename to %s if you want to re-use" % \
                         (installObject.savedProperties, installObject.setup_properties_fn)
