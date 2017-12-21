#!/usr/bin/env python
"""export30.py - A script to export all the data from Gluu Server 3.0.x

Usage: python export30.py

Running this creates a folder named `backup_30` which contains all the data
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

from distutils.dir_util import copy_tree

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


class Exporter(object):
    def __init__(self):
        self.backupDir = 'backup_30'
        self.foldersToBackup = ['/etc/certs',
                                '/etc/gluu/conf',
                                '/opt/shibboleth-idp/conf',
                                '/opt/shibboleth-idp/metadata',
                                '/opt/gluu/jetty/identity/custom',
                                '/opt/gluu/jetty/identity/lib',
                                '/opt/gluu/jetty/oxauth/custom',
                                '/opt/gluu/jetty/oxauth/lib',
                                ]
        self.passwordFile = tempfile.mkstemp()[1]

        self.ldapsearch = '/opt/opendj/bin/ldapsearch'
        self.slapcat = '/opt/symas/bin/slapcat'
        self.mkdir = '/bin/mkdir'
        self.cat = '/bin/cat'
        self.grep = '/bin/grep'
        self.hostname = '/bin/hostname'

        self.ldapCreds = ['-h', 'localhost', '-p', '1636', '-Z', '-X', '-D',
                          'cn=directory manager,o=gluu', '-j',
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

    def getOrgInum(self):
        args = [self.ldapsearch] + self.ldapCreds + ['-s', 'one', '-b',
                                                     'o=gluu', 'o=*', 'dn']
        output = self.getOutput(args)
        return output.split(",")[0].split("o=")[-1]

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
        sample = self.getOrgInum()
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

    def getLdif(self):
        logging.info('Creating backup of LDAP data')
        orgInum = self.getOrgInum()
        # Backup the data
        for basedn in self.base_dns:
            args = [self.ldapsearch] + self.ldapCreds + [
                '-b', '%s,o=%s,o=gluu' % (basedn, orgInum), 'objectclass=*']
            output = self.getOutput(args)
            ou = basedn.split("=")[-1]
            f = open("%s/ldif/%s.ldif" % (self.backupDir, ou), 'w')
            f.write(output)
            f.close()

        # Backup the appliance config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                'ou=appliances,o=gluu',
                '-s',
                'one',
                'objectclass=*']
        output = self.getOutput(args)
        f = open("%s/ldif/appliance.ldif" % self.backupDir, 'w')
        f.write(output)
        f.close()

        # Backup the oxtrust config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                'ou=appliances,o=gluu',
                'objectclass=oxTrustConfiguration']
        output = self.getOutput(args)
        f = open("%s/ldif/oxtrust_config.ldif" % self.backupDir, 'w')
        f.write(output)
        f.close()

        # Backup the oxauth config
        args = [self.ldapsearch] + self.ldapCreds + \
               ['-b',
                'ou=appliances,o=gluu',
                'objectclass=oxAuthConfiguration']
        output = self.getOutput(args)
        f = open("%s/ldif/oxauth_config.ldif" % self.backupDir, 'w')
        f.write(output)
        f.close()

        # Backup the trust relationships
        args = [self.ldapsearch] + self.ldapCreds + [
                '-b', 'ou=appliances,o=gluu', 'objectclass=gluuSAMLconfig']
        output = self.getOutput(args)
        f = open("%s/ldif/trust_relationships.ldif" % self.backupDir, 'w')
        f.write(output)
        f.close()

        # Backup the org
        args = [self.ldapsearch] + self.ldapCreds + [
                '-s', 'base', '-b', 'o=%s,o=gluu' % orgInum, 'objectclass=*']
        output = self.getOutput(args)
        f = open("%s/ldif/organization.ldif" % self.backupDir, 'w')
        f.write(output)
        f.close()

        # Backup o=site
        args = [self.ldapsearch] + self.ldapCreds + [
                '-b', 'ou=people,o=site', '-s', 'one', 'objectclass=*']
        output = self.getOutput(args)
        f = open("%s/ldif/site.ldif" % self.backupDir, 'w')
        f.write(output)
        f.close()

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
        props['hostname'] = self.getOutput([self.hostname]).strip()
        props['inumAppliance'] = self.getOutput(
            [self.grep, "^inum", "%s/ldif/appliance.ldif" % self.backupDir]
        ).split("\n")[0].split(":")[-1].strip()
        props['inumApplianceFN'] = self.clean(props['inumAppliance'])
        props['inumOrg'] = self.getOrgInum()
        props['inumOrgFN'] = self.clean(props['inumOrg'])
        props['baseInum'] = props['inumOrg'][:21]
        props['encode_salt'] = self.getOutput(
            [self.cat, "%s/etc/gluu/conf/salt" % self.backupDir]
            ).split("=")[-1].strip()

        props['oxauth_client_id'] = self.getProp('oxauth_client_id')
        props['scim_rs_client_id'] = self.getProp('scim_rs_client_id')
        props['scim_rp_client_id'] = self.getProp('scim_rp_client_id')
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

    def export(self):
        # Call the sequence of functions that would backup the various stuff
        print("-------------------------------------------------------------")
        print("            Gluu Server Data Export Tool For v3.x            ")
        print("-------------------------------------------------------------")
        print("")
        self.prepareLdapPW()
        self.makeFolders()
        self.backupFiles()
        self.getLdif()
        self.genProperties()
        print("")
        print("-------------------------------------------------------------")
        print("The data has been exported to %s" % self.backupDir)
        print("-------------------------------------------------------------")


if __name__ == "__main__":
    if len(sys.argv) != 1:
        print "Usage: python export30.py"
    else:
        exporter = Exporter()
        exporter.export()
