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
import hashlib

class Setup(object):
    def __init__(self, install_dir=None):
        self.install_dir = install_dir
        self.setup_properties_fn = "%s/setup.properties" % self.install_dir
        self.log = '%s/setup.log' % self.install_dir
        self.logError = '%s/setup_error.log' % self.install_dir
        self.savedProperties = "%s/setup.properties.last" % self.install_dir

        self.gluuOptFolder = "/opt/gluu"
        self.gluuOptBinFolder = "/opt/gluu/bin"
        self.configFolder = '/etc/gluu/config'
        self.certFolder = '/etc/certs'
        self.tomcatHome = '/opt/tomcat'
        self.tomcat_user_home_lib = "/home/tomcat/lib"
        self.oxauth_lib = "/opt/tomcat/webapps/oxauth/WEB-INF/lib"
        self.tomcatWebAppFolder = "/opt/tomcat/webapps"
        self.oxBaseDataFolder = "/var/ox"
        self.oxPhotosFolder = "/var/ox/photos"
        self.oxTrustRemovedFolder = "/var/ox/oxtrust/removed"
        self.etc_hosts = '/etc/hosts'
        self.etc_hostname = '/etc/hostname'

        self.idpFolder = "/opt/idp"
        self.idpMetadataFolder = "/opt/idp"
        self.idpLogsFolder = "/opt/idp/logs"
        self.idpLibFolder = "/opt/idp/lib"
        self.idpConfFolder = "/opt/idp/conf"
        self.idpSslFolder = "/opt/idp/ssl"
        self.idpTempMetadataFolder = "/opt/idp/temp_metadata"
        self.idpWarFolder = "/opt/idp/war"

        self.downloadWars = False
        self.modifyNetworking = False
        self.downloadSaml = False

        self.oxtrust_war = 'https://ox.gluu.org/maven/org/xdi/oxtrust-server/1.7.0-SNAPSHOT/oxtrust-server-1.7.0-SNAPSHOT.war'
        self.oxauth_war = 'https://ox.gluu.org/maven/org/xdi/oxauth-server/1.7.0-SNAPSHOT/oxauth-server-1.7.0-SNAPSHOT.war'
        self.ce_setup_zip = 'https://github.com/GluuFederation/community-edition-setup/archive/master.zip'
        self.idp_war = 'https://ox.gluu.org/maven/org/xdi/oxIdp/2.4.0-Final/oxIdp-2.4.0-Final.war'

        self.os_types = ['centos', 'redhat', 'fedora', 'ubuntu', 'debian']
        self.os_type = None

        self.hostname = None
        self.ip = None
        self.orgName = None
        self.orgShortName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.admin_email = None

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

        self.outputFolder = '%s/output' % self.install_dir
        self.templateFolder = '%s/templates' % self.install_dir
        self.staticFolder = '%s/static/opendj' % self.install_dir
        self.indexJson = '%s/static/opendj/opendj_index.json' % self.install_dir
        self.oxauth_error_json = '%s/static/oxauth/oxauth-errors.json' % self.install_dir

        self.httpdKeyPass = None
        self.httpdKeyFn = '%s/httpd.key' % self.certFolder
        self.httpdCertFn = '%s/httpd.crt' % self.certFolder
        self.shibJksPass = None
        self.shibJksFn = '%s/shibIDP.jks' % self.certFolder
        self.asimbaJksPass = None
        self.asimbaJksFn = '%s/asimbaIDP.jks' % self.certFolder

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
        self.ldapPassFn = '/home/ldap/.pw'
        self.importLdifCommand = '%s/bin/import-ldif' % self.ldapBaseFolder
        self.schemaFolder = "%s/template/config/schema" % self.ldapBaseFolder
        self.org_custom_schema = "%s/config/schema/100-user.ldif" % self.ldapBaseFolder
        self.schemaFiles = ["%s/static/%s/96-eduperson.ldif" % (self.install_dir, self.ldap_type),
                            "%s/static/%s/101-ox.ldif" % (self.install_dir, self.ldap_type),
                            "%s/static/%s/77-customAttributes.ldif" % (self.install_dir, self.ldap_type),
                            "%s/output/100-user.ldif" % self.install_dir]
        self.gluuScriptFiles = ['%s/static/scripts/logmanager.sh' % self.install_dir,
                                '%s/static/scripts/testBind.py' % self.install_dir]
        self.init_files = ['%s/static/tomcat/tomcat' % self.install_dir,
                           '%s/static/opendj/opendj' % self.install_dir]
        self.redhat_services = ['tomcat', 'opendj', 'httpd']
        self.debian_services = [{ 'name' : 'opendj', 'order' : '40', 'runlevel' : '3'},
                                { 'name' : 'tomcat', 'order' : '50', 'runlevel' : '3'},
                                { 'name' : 'apache2', 'order' : '60', 'runlevel' : '3'}]

        self.ldap_start_script = '/etc/init.d/opendj'
        self.apache_start_script = '/etc/init.d/httpd'
        self.tomcat_start_script = '/etc/init.d/tomcat'

        self.ldapEncodePWCommand = '%s/bin/encode-password' % self.ldapBaseFolder
        self.oxEncodePWCommand = '%s/bin/encode.py' % self.gluuOptFolder
        self.keytoolCommand = '/usr/java/latest/bin/keytool'
        self.opensslCommand = '/usr/bin/openssl'
        self.defaultTrustStoreFN = '/usr/java/latest/lib/security/cacerts'
        self.defaultTrustStorePW = 'changeit'

        self.oxtrust_openid_client_id = None
        self.oxtrust_uma_client_id = None

        # Stuff that gets rendered; filname is necessary. Full path should
        # reflect final path if the file must be copied after its rendered.
        self.oxauth_ldap_properties = '/opt/tomcat/conf/oxauth-ldap.properties'
        self.oxauth_config_xml = '/opt/tomcat/conf/oxauth-config.xml'
        self.oxTrust_properties = '/opt/tomcat/conf/oxTrust.properties'
        self.oxtrust_ldap_properties = '/opt/tomcat/conf/oxTrustLdap.properties'
        self.tomcat_gluuTomcatWrapper = '/opt/tomcat/conf/gluuTomcatWrapper.conf'
        self.tomcat_oxauth_static_conf_json = '/opt/tomcat/conf/oxauth-static-conf.json'
        self.tomcat_log_folder = "%s/logs" % self.tomcatHome
        self.tomcat_max_ram = None    # in MB
        self.oxTrust_log_rotation_configuration = "%s/conf/oxTrustLogRotationConfiguration.xml" % self.tomcatHome
        self.eduperson_schema_ldif = '%s/config/schema/96-eduperson.ldif'
        self.apache2_conf = '%s/httpd.conf' % self.outputFolder
        self.apache2_ssl_conf = '%s/https_gluu.conf' % self.outputFolder
        self.ldif_base = '%s/base.ldif' % self.outputFolder
        self.ldif_appliance = '%s/appliance.ldif' % self.outputFolder
        self.ldif_attributes = '%s/attributes.ldif' % self.outputFolder
        self.ldif_scopes = '%s/scopes.ldif' % self.outputFolder
        self.ldif_clients = '%s/clients.ldif' % self.outputFolder
        self.ldif_people = '%s/people.ldif' % self.outputFolder
        self.ldif_groups = '%s/groups.ldif' % self.outputFolder
        self.ldif_site = '%s/static/cache-refresh/o_site.ldif' % self.install_dir
        self.encode_script = '%s/bin/encode.py' % self.gluuOptFolder

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
                     self.tomcat_gluuTomcatWrapper: True,
                     self.tomcat_oxauth_static_conf_json: True,
                     self.oxTrust_log_rotation_configuration: True,
                     self.ldap_setup_properties: False,
                     self.org_custom_schema: False,
                     self.apache2_conf: False,
                     self.apache2_ssl_conf: False,
                     self.etc_hosts: False,
                     self.etc_hostname: False,
                     self.ldif_base: False,
                     self.ldif_appliance: False,
                     self.ldif_attributes: False,
                     self.ldif_scopes: False,
                     self.ldif_clients: False,
                     self.ldif_people: False,
                     self.ldif_groups: False}

    def __repr__(self):
        s = 'hostname'.ljust(30) + self.hostname.rjust(35) + "\n" \
            + 'ip'.ljust(30) + self.ip.rjust(35) + "\n" \
            + 'orgName'.ljust(30) + self.orgName.rjust(35) + "\n" \
            + 'os'.ljust(30) + self.os_type.rjust(35) + "\n" \
            + 'city'.ljust(30) + self.city.rjust(35) + "\n" \
            + 'state'.ljust(30) + self.state.rjust(35) + "\n" \
            + 'countryCode'.ljust(30) + self.countryCode.rjust(35) + "\n" \
            + 'support email'.ljust(30) + self.admin_email.rjust(35) + "\n" \
            + 'tomcat max ram'.ljust(30) + self.tomcat_max_ram.rjust(35) + "\n" \
            + 'Admin Pass'.ljust(30) + self.ldapPass.rjust(35) + "\n" \
            + 'Modify Networking'.ljust(30) + `self.modifyNetworking`.rjust(35) + "\n" \
            + 'Download latest wars'.ljust(30) + `self.downloadWars`.rjust(35) + "\n" \
            + 'Download and install SAML'.ljust(30) + `self.downloadSaml`.rjust(35) + "\n"
        return s

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
            p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
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
        if not self.admin_email:
            tld = None
            try:
                tld = ".".join(self.hostname.split(".")[-2:])
            except:
                tld = self.hostname
            self.admin_email = "support@%s" % tld
        if not self.httpdKeyPass:
            self.httpdKeyPass = self.getPW()
        if not self.ldapPass:
            self.ldapPass = self.getPW()
        if not self.shibJksPass:
            self.shibJksPass = self.getPW()
        if not self.asimbaJksPass:
            self.asimbaJksPass = self.getPW()
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

    def gen_cert(self, suffix, password, user='root'):
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
        self.run(["/bin/chown", '%s:%s' % (user, user), key_with_password])
        self.run(["/bin/chmod", '700', key_with_password])
        self.run(["/bin/chown", '%s:%s' % (user, user), key])
        self.run(["/bin/chmod", '700', key])

        self.run(["/usr/bin/keytool", "-import", "-trustcacerts", "-alias", self.hostname, "-file", public_certificate, "-keystore", "/usr/java/latest/lib/security/cacerts", "-storepass", "changeit", "-noprompt"])

    def gen_keystore(self, suffix, keystoreFN, keystorePW, inKey, inCert, user='root'):
        self.logIt("Creating keystore %s" % suffix)
        # Convert key to pkcs12
        pkcs_fn = '%s/%s.pkcs12' % (self.certFolder, suffix)
        self.run([self.opensslCommand,
                  'pkcs12',
                  '-export',
                  '-inkey',
                  inKey,
                  '-in',
                  inCert,
                  '-out',
                  pkcs_fn,
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
        self.run(["/bin/chown", '%s:%s' % (user, user), pkcs_fn])
        self.run(["/bin/chmod", '700', pkcs_fn])
        self.run(["/bin/chown", '%s:%s' % (user, user), keystoreFN])
        self.run(["/bin/chmod", '700', keystoreFN])

    def gen_openid_keys(self):
        self.logIt("Generating oxAuth OpenID Connect keys")
        self.copyFile("%s/static/oxauth/java.security" % self.install_dir, "/usr/java/latest/lib/security")
        self.copyFile("%s/static/oxauth/lib/oxauth.jar" % self.install_dir, self.tomcat_user_home_lib)
        self.copyFile("%s/static/oxauth/lib/jettison-1.3.jar" % self.install_dir, self.tomcat_user_home_lib)
        self.copyFile("%s/static/oxauth/lib/oxauth-model.jar" % self.install_dir, self.tomcat_user_home_lib)
        self.copyFile("%s/static/oxauth/lib/bcprov-jdk16-1.46.jar" % self.install_dir, self.tomcat_user_home_lib)
        self.copyFile("%s/static/oxauth/lib/commons-codec-1.5.jar" % self.install_dir, self.tomcat_user_home_lib)
        self.copyFile("%s/static/oxauth/lib/commons-lang-2.6.jar" % self.install_dir, self.tomcat_user_home_lib)
        self.copyFile("%s/static/oxauth/lib/log4j-1.2.14.jar" % self.install_dir, self.tomcat_user_home_lib)

        self.change_ownership()

        requiredJars =['%s/bcprov-jdk16-1.46.jar' % self.tomcat_user_home_lib,
                       '%s/commons-lang-2.6.jar' % self.tomcat_user_home_lib,
                       '%s/log4j-1.2.14.jar' % self.tomcat_user_home_lib,
                       '%s/commons-codec-1.5.jar' % self.tomcat_user_home_lib,
                       '%s/jettison-1.3.jar' % self.tomcat_user_home_lib,
                       '%s/oxauth-model.jar' % self.tomcat_user_home_lib,
                       '%s/oxauth.jar' % self.tomcat_user_home_lib]

        cmd = " ".join(["/usr/java/latest/bin/java",
                        "-cp",
                        ":".join(requiredJars),
                        "org.xdi.oxauth.util.KeyGenerator"])
        args = ["/bin/su", "tomcat", "-c", cmd]

        self.logIt("Runnning: %s" % " ".join(args))
        try:
            p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            output, err = p.communicate()
            if err:
                self.logIt(err, True)
            if output:
                openid_key_json_fn = "%s/oxauth-web-keys.json" % self.certFolder
                f = open(openid_key_json_fn, 'w')
                f.write(output)
                f.close()
                self.run(["/bin/chown", 'tomcat:tomcat', openid_key_json_fn])
                self.run(["/bin/chmod", '700', openid_key_json_fn])
                self.logIt("Wrote oxauth OpenID Connect key to %s" % openid_key_json_fn)
        except:
            self.logIt("Error running command : %s" % " ".join(args), True)
            self.logIt(traceback.format_exc(), True)

    def gen_crypto(self):
        try:
            self.logIt('Generating certificates and keystores')
            self.gen_cert('httpd', self.httpdKeyPass, 'apache')
            self.gen_cert('shibIDP', self.shibJksPass, 'tomcat')
            self.gen_cert('asimba', self.asimbaJksPass, 'tomcat')
            # Shibboleth IDP and Asimba will be added soon...
            self.gen_keystore('shibIDP',
                              self.shibJksFn,
                              self.shibJksPass,
                              '%s/shibIDP.key' % self.certFolder,
                              '%s/shibIDP.crt' % self.certFolder,
                              'tomcat')
            self.gen_keystore('asimba',
                              self.asimbaJksFn,
                              self.asimbaJksPass,
                              '%s/asimba.key' % self.certFolder,
                              '%s/asimba.crt' % self.certFolder,
                              'tomcat')
            self.gen_openid_keys()
            self.run(['/bin/chown', '-R', 'tomcat:tomcat', self.certFolder])
            self.run(['/bin/chmod', '-R', '500', self.certFolder])
        except:
            self.logIt("Error generating cyrpto")
            self.logIt(traceback.format_exc(), True)

    def configure_httpd(self):
        if self.os_type in ['centos', 'redhat', 'fedora']:
            self.copyFile(self.apache2_conf, '/etc/httpd/conf/httpd.conf')
            self.copyFile(self.apache2_ssl_conf, '/etc/httpd/conf.d/https_gluu.conf')
        if self.os_type in ['debian', 'ubuntu']:
            self.copyFile(self.apache2_ssl_conf, '/etc/apache2/sites-available/https_gluu.conf')
            self.run(['ln', '-s', '/etc/apache2/sites-available/https_gluu.conf',
                      '/etc/apache2/sites-enabled/https_gluu.conf'])

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

    def ldap_encode(self, password):
        salt = os.urandom(4)
        sha = hashlib.sha1(password)
        sha.update(salt)
        b64encoded = '{0}{1}'.format(sha.digest(), salt).encode('base64').strip()
        encrypted_password = '{{SSHA}}{0}'.format(b64encoded)
        return encrypted_password

    def encode_passwords(self):
        self.logIt("Encoding passwords")
        try:
            self.encoded_ldap_pw = self.ldap_encode(self.ldapPass)
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
                      setupCmd])
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
        self.copyFile("%s/static/cache-refresh/o_site.ldif" % self.install_dir, ldifFolder)
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

    ### Notify system about hostname update
    def update_hostname(self):
        self.logIt("Copying hosts and hostname to final destination")

        self.copyFile("%s/hostname" % self.outputFolder, self.etc_hostname)
        self.run(['/bin/hostname', self.hostname])

        self.copyFile("%s/hosts" % self.outputFolder, self.etc_hosts)

    def copy_output(self):
        self.logIt("Copying rendered templates to final destination")

        # Detect sevice path and apache service name
        service_path = '/sbin/service'
        apache_service_name = 'httpd'
        if self.os_type in ['debian', 'ubuntu']:
            service_path = '/usr/sbin/service'
            apache_service_name = 'apache2'

        self.run([service_path, apache_service_name, 'stop'])
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

        self.run([service_path, apache_service_name, 'start'])

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

    def setup_init_scripts(self):
        for init_file in self.init_files:
            try:
                script_name = os.path.split(init_file)[-1]
                self.copyFile(init_file, "/etc/init.d")
                self.run(["chmod", "755", "/etc/init.d/%s" % script_name])
            except:
                self.logIt("Error copying script file %s to /etc/init.d" % init_file)
                self.logIt(traceback.format_exc(), True)
        if self.os_type in ['centos', 'redhat', 'fedora']:
            for service in self.redhat_services:
                self.run(["/sbin/chkconfig", service, "on"])
        elif self.os_type in ['ubuntu', 'debian']:
            for service in self.debian_services:
                self.run(["/usr/sbin/update-rc.d", service['name'], 'start', service['order'], service['runlevel'], "."])

    def start_services(self):
        # Detect sevice path and apache service name
        service_path = '/sbin/service'
        apache_service_name = 'httpd'
        if self.os_type in ['debian', 'ubuntu']:
            service_path = '/usr/sbin/service'
            apache_service_name = 'apache2'

        # Apache HTTPD
        self.run([service_path, apache_service_name, 'start'])

        # Apache Tomcat
        try:
            # Giving LDAP a few seconds to load the data...
            i = 0
            wait_time = 5
            while i < wait_time:
                time.sleep(1)
                print ".",
                i = i + 1
            self.run([service_path, 'tomcat', 'start'])
        except:
            self.logIt("Error starting tomcat")
            self.logIt(traceback.format_exc(), True)

    def change_ownership(self):
        self.logIt("Changing ownership")
        self.run(['/bin/chown', '-R', 'tomcat:tomcat', self.tomcatHome])
        self.run(['/bin/chown', '-R', 'ldap:ldap', self.ldapBaseFolder])
        self.run(['/bin/chown', '-R', 'tomcat:tomcat', self.tomcat_user_home_lib])
        self.run(['/bin/chown', '-R', 'tomcat:tomcat', self.oxBaseDataFolder])

    def change_permissions(self):
        self.run(['/bin/chmod', 'a-x', self.certFolder])
        self.run(['/bin/chmod', '-R', 'u+X', self.certFolder])

    def copy_static(self):
        self.copyFile("%s/static/oxauth/oxauth-id-gen.py" % self.install_dir, "%s/conf" % self.tomcatHome)
        self.copyFile("%s/static/tomcat/server.xml" % self.install_dir, "%s/conf" % self.tomcatHome)

        os.makedirs("%s/conf/template/conf" % self.tomcatHome)
        self.copyFile("%s/static/oxtrust/oxTrustCacheRefresh-template.properties.vm" % self.install_dir, "%s/conf/template/conf" % self.tomcatHome)

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
            self.run(["/bin/chown", 'ldap:ldap', self.ldapPassFn])
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
            if not os.path.exists(self.tomcat_user_home_lib):
                os.makedirs(self.tomcat_user_home_lib)
            if not os.path.exists(self.configFolder):
                os.makedirs(self.configFolder)
            if not os.path.exists(self.certFolder):
                os.makedirs(self.certFolder)
            if not os.path.exists(self.oxPhotosFolder):
                os.makedirs(self.oxPhotosFolder)
            if not os.path.exists(self.oxTrustRemovedFolder):
                os.makedirs(self.oxTrustRemovedFolder)
            if self.downloadSaml:
                if not os.path.exists(self.idpFolder):
                    os.makedirs(self.idpFolder)
                if not os.path.exists(self.idpMetadataFolder):
                    os.makedirs(self.idpMetadataFolder)
                if not os.path.exists(self.idpLogsFolder):
                    os.makedirs(self.idpLogsFolder)
                if not os.path.exists(self.idpLibFolder):
                    os.makedirs(self.idpLibFolder)
                if not os.path.exists(self.idpConfFolder):
                    os.makedirs(self.idpConfFolder)
                if not os.path.exists(self.idpSslFolder):
                    os.makedirs(self.idpSslFolder)
                if not os.path.exists(self.idpTempMetadataFolder):
                    os.makedirs(self.idpTempMetadataFolder)
                if not os.path.exists(self.idpWarFolder):
                    os.makedirs(self.idpWarFolder)
        except:
            self.logIt("Error making folders", True)
            self.logIt(traceback.format_exc(), True)

    def make_salt(self):
        try:
            f = open("%s/conf/salt" % self.tomcatHome, 'w')
            f.write('encodeSalt = %s' % self.encode_salt)
            f.close()
        except:
            self.logIt("Error writing salt", True)
            self.logIt(traceback.format_exc(), True)
            sys.exit()

    def choose_from_list(self, list_of_choices, choice_name="item", default_choice_index=0):
        return_value = None
        choice_map = {}
        chosen_index = 0
        print "\nSelect the number for the %s from the following list:" % choice_name
        for choice in list_of_choices:
            choice_map[chosen_index] = choice
            chosen_index += 1
            print "  [%i]   %s" % (chosen_index, choice)
        while not return_value:
            choice_number = self.getPrompt("Please select a number listed above", str(default_choice_index + 1))
            try:
                choice_number = int(choice_number) - 1
                if (choice_number >= 0) & (choice_number < len(list_of_choices)):
                    return_value = choice_map[choice_number]
                else:
                    print '"%i" is not a valid choice' % (choice_number + 1)
            except:
                print 'Cannot convert "%s" to a number' % choice_number
                self.logIt(traceback.format_exc(), True)
        return return_value

    def modify_netowrking_prompt(self):
        if self.modifyNetworking:
            self.ce_templates[self.etc_hosts] = True
            self.ce_templates[self.etc_hostname] = True

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

        # Get the OS type
        installObject.os_type = installObject.choose_from_list(installObject.os_types, "Operating System")

        # Get city and state|province code
        installObject.city = installObject.getPrompt("Enter your city or locality")
        long_enough = False
        while not long_enough:
            state = installObject.getPrompt("Enter your state or province two letter code")
            if len(state) != 2:
                print "State or province code must be two characters"
            else:
                installObject.state = state
                long_enough = True

        # Get the Country Code
        long_enough = False
        while not long_enough:
            countryCode = installObject.getPrompt("Enter two letter Country Code")
            if len(countryCode) != 2:
                print "Country code must be two characters"
            else:
                installObject.countryCode = countryCode
                long_enough = True

        installObject.orgName = installObject.getPrompt("Enter Organization Name")
        installObject.admin_email = installObject.getPrompt("Enter email address for support at your organization")
        installObject.tomcat_max_ram = installObject.getPrompt("Enter maximum RAM for tomcat in MB", '1024')
        randomPW = installObject.getPW()
        installObject.ldapPass = installObject.getPrompt("Optional: enter password for oxTrust and LDAP superuser", randomPW)
        modifyNetworking = self.getPrompt("Update the hostname, hosts, and resolv.conf files?", "No")[0].lower()
        if modifyNetworking == 'y':
            installObject.modifyNetworking = True
        download_wars = self.getPrompt("Download latest oxAuth and oxTrust war files?", "No")[0].lower()
        if download_wars == 'y':
            installObject.downloadWars = True
        deploy_saml = self.getPrompt("Download and deploy saml IDP and SP?", "No")[0].lower()
        if deploy_saml == 'y':
            installObject.downloadSaml = True

    def downloadWarFiles(self):
        if self.downloadSaml:
            print "Downloading latest Shibboleth idp war file..."
            self.run(['/usr/bin/wget', self.idp_war, '-O', '%s/idp.war' % self.idpWarFolder])

        if self.downloadWars:
            print "Downloading latest oxAuth war file..."
            self.run(['/usr/bin/wget', self.oxauth_war, '-O', '%s/oxauth.war' % self.tomcatWebAppFolder])
            print "Downloading latest oxTrust war file..."
            self.run(['/usr/bin/wget', self.oxtrust_war, '-O', '%s/identity.war' % self.tomcatWebAppFolder])
            print "Finished downloading latest war files"

def print_help():
    print "\nUse setup.py to configure your Gluu Server and to add initial data required for"
    print "oxAuth and oxTrust to start. If setup.properties is found in this folder, these"
    print "properties will automatically be used instead of the interactive setup."
    print "Options:"
    print ""
    print "    -h   Help"
    print "    -f   specify setup.properties file"
    print "    -d   specify directory of installation"
    print "    -n   No interactive prompt before install starts."

def getOpts(argv, install_dir=None):
    setup_properties = None
    noPrompt = False
    try:
        opts, args = getopt.getopt(argv, "d:hnf:")
    except getopt.GetoptError:
        print_help()
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print_help()
            sys.exit()
        elif opt == '-d':
            install_dir = arg
        elif opt == "-f":
            try:
                if os.path.isfile(arg):
                    setup_properties = arg
                    print "Found setup properties %s\n" % arg
                else:
                    print "\nOoops... %s file not found\n" % arg
            except:
                print "\nOoops... %s file not found\n" % arg
        elif opt == "-n":
            noPrompt = True
    return setup_properties, noPrompt, install_dir

if __name__ == '__main__':
    setup_properties = None
    noPrompt = False
    install_dir = "."
    if len(sys.argv) > 1:
        setup_properties, noPrompt, install_dir = getOpts(sys.argv[1:], install_dir)
    installObject = Setup(install_dir)

    print "\nInstalling Gluu Server...\n\nFor more info see:\n  %s  \n  %s\n" % (installObject.log, installObject.logError)
    print "\n** All clear text passwords contained in %s.\n" % installObject.savedProperties
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
        installObject.logIt('%s Properties found!\n' % setup_properties)
        installObject.load_properties(setup_properties)
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
            installObject.make_salt()
            installObject.downloadWarFiles()
            installObject.modify_netowrking_prompt()
            installObject.writeLdapPW()
            installObject.copy_scripts()
            installObject.encode_passwords()
            installObject.render_templates()
            installObject.update_hostname()
            installObject.gen_crypto()
            installObject.configure_httpd()
            installObject.setup_opendj()
            installObject.configure_opendj()
            installObject.index_opendj()
            installObject.import_ldif()
            installObject.deleteLdapPw()
            installObject.copy_output()
            installObject.setup_init_scripts()
            installObject.copy_static()
            installObject.change_ownership()
            installObject.change_permissions()
            installObject.start_services()
            installObject.save_properties()
        except:
            installObject.logIt("***** Error caught in main loop *****", True)
            installObject.logIt(traceback.format_exc(), True)
        print "\n\n Gluu Server installation successful! Point your broswer to https://%s\n\n" % installObject.hostname
    else:
        installObject.save_properties()
        print "Properties saved to %s. Change filename to %s if you want to re-use" % \
                         (installObject.savedProperties, installObject.setup_properties_fn)
