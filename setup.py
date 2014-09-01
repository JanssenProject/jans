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
import traceback
import uuid

class Setup():
    def __init__(self):
        self.hostname = None
        self.ip = None
        self.orgName = None
        self.countryCode = None
        self.city = None
        self.state = None
        self.jksPass = None
        self.encoded_ox_ldap_pw = None
        self.encoded_ldap_pw = None
        self.oxauthClient_encoded_pw = None
        self.baseInum = None
        self.inumOrg = None
        self.inumAppliance = None
        self.inumOrgFN = None
        self.inumApplianceFN = None
        self.ldapPass = None
        self.oxauth_client_id = None
        self.oxauthClient_pw = None

	self.outputFolder = os.getcwd() + '/output'
        self.templateFolder = './templates'
        self.tomcatHome = '/opt/tomcat'
        self.configFolder = '/etc/gluu/config'
        self.certFolder = '/etc/certs'
        self.gluuHome = '/opt/gluu'
        self.keystoreGenerator = '%s/bin/keystoreGenerator.sh' % self.gluuHome
        self.certGenerator = '%s/bin/certGenerator.sh' % self.gluuHome

        self.ldap_binddn = 'cn=directory manager'
        self.ldap_port = '1389'
        self.ldaps_port = '1636'
        self.ldapBaseFolder = '/opt/OpenDJ-2.6.0'  # TODO I'd like this to be /opt/gluu-ldap
        self.ldapStartTimeOut = 30
        self.ldapSetupCommand = '%s/setup' % self.ldapBaseFolder
        self.ldapDsconfigCommand = "%s/bin/dsconfig" % self.ldapBaseFolder
        self.ldapPassFn = '%s/.pw' % self.outputFolder
        self.importLdifCommand = '%s/bin/import-ldif' % self.ldapBaseFolder

        self.ldap_start_script = '/etc/init.d/opendj'  # TODO I'd like this to be /etc/init.d/gluu-ldap
        self.apache_start_script = '/etc/init.d/httpd'
        self.tomcat_start_script = '/etc/init.d/tomcat'

        self.ldapEncodePWCommand = '%s/bin/encode-password' % self.ldapBaseFolder
        self.oxEncodePWCommand = '%s/bin/encode.py' % self.gluuHome

        self.oxtrust_openid_client_id = None
        self.oxtrust_uma_client_id = None

        self.log = './setup.log'
        self.logError = './error.log'

        # Stuff that gets rendered
        self.oxauth_ldap_properties = '/opt/tomcat/conf/oxauth-ldap.properties'
        self.oxauth_config_xml = '/opt/tomcat/conf/oxauth-config.xml'
        self.oxTrust_properties = '/opt/tomcat/conf/oxTrust.properties'
        self.oxtrust_ldap_properties = '/opt/tomcat/conf/oxTrustLdap.properties'
        self.tomcat_server_xml = '/opt/tomcat/conf/server.xml'
        self.tomcat_gluuTomcatWrapper = '/opt/tomcat/conf/gluuTomcatWrapper.conf'
        self.tomcat_oxauth_static_conf_json = '/opt/tomcat/conf/oxauth-static-conf.json'
        self.config_ldif = '/opt/OpenDJ-2.6.0/config/config.ldif'
        self.user_schema_ldif = '/opt/OpenDJ-2.6.0/config/schema/100-user.ldif'
	self.apache2_conf = '/etc/httpd/conf/httpd.conf'
        self.apache2_idp_conf = '/etc/httpd/conf.d/idp.conf'
        self.etc_hosts = '/etc/hosts'
        self.etc_hostname = '/etc/hostname'
        self.ldif_base = '%s/base.ldif' % self.outputFolder
        self.ldif_appliance = '%s/appliance.ldif' % self.templateFolder
        self.ldif_attributes = '%s/attributes.ldif' % self.templateFolder
        self.ldif_scopes = '%s/scopes.ldif' % self.templateFolder
        self.ldif_clients = '%s/clients.ldif' % self.templateFolder
        self.ldif_people = '%s/people.ldif' % self.templateFolder
        self.ldif_groups = '%s/groups.ldif' % self.templateFolder

        self.ldap_setup_properties = '%s/opendj-setup.properties' % self.templateFolder
        self.ce_files = {self.oxauth_ldap_properties: True,
                         self.oxauth_config_xml: True,
                         self.oxTrust_properties: True,
                         self.oxtrust_ldap_properties: True,
                         self.tomcat_server_xml: True,
                         self.tomcat_gluuTomcatWrapper: True,
                         self.tomcat_oxauth_static_conf_json: True,
			 self.ldap_setup_properties: False,
                         self.config_ldif: True,
                         self.user_schema_ldif: True,
			 self.apache2_conf: True,
                         self.apache2_idp_conf: True,
                         self.etc_hosts: True,
                         self.etc_hostname: True,
                         self.ldif_base: False,
                         self.ldif_appliance: False,
                         self.ldif_attributes: False,
                         self.ldif_scopes: False,
                         self.ldif_clients: False,
                         self.ldif_people: False,
                         self.ldif_groups: False }

    def __repr__(self):
        return ( 'hostname'.ljust(20) + self.hostname.rjust(40) + "\n" 
            + 'ip'.ljust(20) + self.ip.rjust(40) + "\n" 
            + 'orgName'.ljust(20) + self.orgName.rjust(40) + "\n" 
            + 'countryCode'.ljust(20) + self.countryCode.rjust(40) + "\n" 
            + 'city'.ljust(20) + self.city.rjust(40) + "\n" 
            + 'state'.ljust(20) + self.state.rjust(40) + "\n" 
            + 'jksPass'.ljust(20) + self.jksPass.rjust(40) + "\n" 
            + 'ldapPass'.ljust(20) + self.ldapPass.rjust(40) + "\n" 
            + 'inumOrg'.ljust(20) + self.inumOrg.rjust(40) + "\n" 
            + 'inumAppliance'.ljust(20) + self.inumAppliance.rjust(40))

    def logIt(self, s, errorLog=False):
	print s
        if errorLog:
            f = open(self.logError, 'a')
            f.write('\n%s : ' % time.strftime('%X %x %Z'))
            f.write(s)
        f = open(self.log, 'a')
        f.write('\n%s : ' % time.strftime('%X %x %Z'))
        f.write(s)
        f.close()

    # args = command + args, i.e. ['ls', '-ltr']
    def run(self, args):
        self.logIt('Running: %s' % ' '.join(args))
        p = subprocess.Popen(args, stdout=subprocess.PIPE)
        output, err = p.communicate()
        if output:
            self.logIt(output)
        if err:
            self.logIt(err, True)

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
        p = Properties.Properties()
        p.load(open(fn))
        self.hostname = p['hostName']
	print self.hostname
        self.ip = p['ip']
        self.orgName = p['orgName']
        self.countryCode = p['countryCode']
        self.city = p['city']
        self.state = p['state']
        self.jksPass = p['jksPass']
        self.ldapPass = p['ldapPass']
        self.inumOrg = p['inumOrg']
        self.inumAppliance = p['inumAppliance']

    def check_properties(self):
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
        if not self.jksPass:
            self.jksPass = self.getPW()
        if not self.ldapPass:
            self.ldapPass = self.getPW()
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
        if not os.path.exists(self.outputFolder):
            os.makedirs(self.outputFolder)
        p = Properties.Properties()
        p['hostname'] = self.hostname
        p['ip'] = self.ip
        p['orgName'] = self.orgName
        p['countryCode'] = self.countryCode
        p['city'] = self.city
        p['state'] = self.state
        p['jksPass'] = self.jksPass
        p['ldapPass'] = self.ldapPass
        p['oxauth_client_id'] = self.oxauth_client_id
        p['oxauthClient_pw'] = self.oxauthClient_pw
        p['inumOrg'] = self.inumOrg
        p['inumAppliance'] = self.inumAppliance
        p.store(open('%s/setup.properties.last' % self.outputFolder, 'w'))

    ### Generate certificates and JKS to be used in tomcat and apache and Gluu-LDAP
    def gen_certs(self):
        if os.path.isfile('%s/%s-java.crt' % (self.certFolder, self.inumOrgFN)):
            return
        self.run(['rm', '-frv', '%s/*java*' % self.certFolder])
        self.run([self.certGenerator,
                  '-u', self.hostname,
                  '-n', self.inumOrgFN,
                  '-o', self.orgName,
                  '-c', self.countryCode,
                  '-s', self.state,
                  '-l', self.city])
        self.run([self.keystoreGenerator,
                  '-n', self.inumOrgFN,
                  '-u', self.hostname,
                  '-p', self.jksPass])

    def add_ldap_schema(self):
        # copy ox schema to '%s/config/schema' % self.ldapBaseFolder
        # copy custom schema file to '%s/config/schema' % self.ldapBaseFolder
        # make sure schema files are owned by ldap:ldap
        return

    def encode_passwords(self):
        cmd = "%s -f %s -s SSHA" % (self.ldapEncodePWCommand, self.ldapPassFn)
        self.encoded_ldap_pw = os.popen(cmd, 'r').read().strip()
        cmd = "%s %s" % (self.oxEncodePWCommand, self.ldapPass)
        self.encoded_ox_ldap_pw = os.popen(cmd, 'r').read().strip()
        self.oxauthClient_pw = self.getPW()
        cmd = "%s %s" % (self.oxEncodePWCommand, self.oxauthClient_pw)
        self.oxauthClient_encoded_pw = os.popen(cmd, 'r').read().strip()

    def setup_ldap(self):
        self.run([self.ldapSetupCommand, '--no-prompt', '--cli',
                  '--propertiesFilePath', os.path.join(self.outputFolder, 'opendj-setup.properties'),
                  '--acceptLicense'])
        config_changes = [['set-global-configuration-prop',  '--set', 'single-structural-objectclass-behavior:accept'],
                          ['set-attribute-syntax-prop', '--syntax-name', 'Directory String',   '--set', 'allow-zero-length-values:true'],
                          ['set-password-policy-prop', '--policy-name', 'Default Password Policy',
                           '--set', 'allow-pre-encoded-passwords:true']]
        for changes in config_changes:
            self.run([self.ldapDsconfigCommand,
                     '--trustAll', '--no-prompt',
                     '--hostname',  'localhost',
                     '--port', '4444',
                     '--bindDN', self.ldap_binddn,
                     '--bindPasswordFile', self.ldapPassFn] + changes)
        # add proper indexes -- define a datastructure in __init__ and
        # iterate through it for dsconfig commands


    def import_ldif(self):
        # TODO Need to add support for multiple ldif files
        self.run([self.importLdifCommand,
                  '--ldifFile', self.ldif_base,
                  '--includeBranch', 'o=gluu',
                  '--backendID', 'userRoot',
                  '--hostname', 'localhost',
                  '--port', '4444',
                  '--bindDN', self.ldap_binddn,
                  '-j', self.ldapPassFn,
                  '--trustAll'])

    ### Change hostname in the relevant files
    def render_templates(self):
        for fullPath in self.ce_files.keys():
            fn = os.path.split(fullPath)[-1]
            self.logIt(fn, True)
            f = open(os.path.join(self.templateFolder, fn))
            s = f.read()
            f.close()
            newFn = open(os.path.join(self.outputFolder, fn), 'w+')
            newFn.write(s % self.__dict__)
            newFn.close()

    def copy_output(self):
        for fullPath in self.ce_files.keys():
            if self.ce_files[fullPath]:
                fn = os.path.split(fullPath)[-1]
                shutil.copyfile(os.path.join(self.outputFolder, fn), fullPath)

    def change_ownership(self):
        self.run(['chown', '-R', 'tomcat:tomcat', self.tomcatHome])
        self.run(['chown', '-R', 'ldap:ldap', self.ldapBaseFolder])
        self.run(['chown', '-R', 'tomcat:tomcat', self.certFolder])

    # Restarts either just LDAP or all services. Waits for LDAP to start before starting tomcat
    def restart_all_services(self):
        self.run([self.ldap_start_script, 'restart'])
        self.run([self.apache_start_script, 'restart'])
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

if __name__ == '__main__':
    ok = False
    s = None
    while not ok:
        s = Setup()
        setup_properties = './setup.properties'
        if os.path.isfile(setup_properties):
	    print 'properties found'
            s.load_properties(setup_properties)
        s.check_properties()
        print '\n%s\n' % `s`
        proceed = raw_input('Proceed with these values [Y|n] ').lower()
        if (len(proceed) and (proceed[0] == 'y')):
            if not os.path.exists(s.configFolder):
                os.makedirs(s.configFolder)
            if not os.path.exists(s.certFolder):
                os.makedirs(s.certFolder)
            f = open(s.ldapPassFn, 'w+')
            f.write(s.ldapPass)
            f.close()
            ok = True

    try:
        s.gen_certs()
        s.add_ldap_schema()
        s.encode_passwords()
        s.render_templates()
        s.setup_ldap()
        s.import_ldif()
        s.copy_output()
        s.change_ownership()
        s.restart_all_services()
        s.save_properties()
    except:
        s.logIt(traceback.format_exc(), True)
    finally:
        s.run(['rm', '-f', '/tmp/pw'])
