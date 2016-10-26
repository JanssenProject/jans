import sys
import os
import os.path
import logging
import shutil
import traceback
import subprocess

# configure logging
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)-8s %(name)s %(message)s',
                    filename='openldap_import.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)


class SetupOpenLDAP(object):

    def __init__(self, ip, ldap_pass):
        self.miniSetupFile = os.path.abspath(__file__)
        self.miniSetupFolder = os.path.dirname(self.miniSetupFile)
        self.setupFolder = os.path.dirname(self.miniSetupFolder)
        self.templatesFolder = os.path.join(self.setupFolder, 'templates')
        self.outputFolder = os.path.join(self.setupFolder, 'output')
        self.backupFolder = os.path.join(self.miniSetupFolder, 'opendj_export')
        self.backupLdifFolder = os.path.join(self.backupFolder, 'ldif')

        self.cmd_mkdir = '/bin/mkdir'

        self.ip = ip
        self.inumOrgFN = None
        self.certFolder = '/etc/certs'
        self.openldapBaseFolder = '/opt/symas'
        self.openldapBinFolder = '/opt/symas/bin'
        self.openldapConfFolder = '/opt/symas/etc/openldap'
        self.openldapCnConfig = '%s/slapd.d' % self.openldapConfFolder
        self.openldapRootUser = "cn=directory manager,o=gluu"
        self.user_schema = '%s/user.schema' % self.outputFolder
        self.openldapKeyPass = None
        self.openldapTLSCACert = '%s/openldap.pem' % self.certFolder
        self.openldapTLSCert = '%s/openldap.crt' % self.certFolder
        self.openldapTLSKey = '%s/openldap.key' % self.certFolder
        self.ldapPass = ldap_pass
        self.openldapPassHash = None
        self.openldapSlapdConf = '%s/slapd.conf' % self.outputFolder
        self.openldapSymasConf = '%s/symas-openldap.conf' % self.outputFolder
        self.slaptest = '%s/slaptest' % self.openldapBinFolder

        self.ldif_files = [
                           "%s/appliance.ldif" % self.backupLdifFolder,
                           "%s/attributes.ldif" % self.backupLdifFolder,
                           "%s/clients.ldif" % self.backupLdifFolder,
                           "%s/groups.ldif" % self.backupLdifFolder,
                           "%s/hosts.ldif" % self.backupLdifFolder,
                           "%s/organization.ldif" % self.backupLdifFolder,
                           "%s/oxauth_config.ldif" % self.backupLdifFolder,
                           "%s/oxtrust_config.ldif" % self.backupLdifFolder,
                           "%s/people.ldif" % self.backupLdifFolder,
                           "%s/scopes.ldif" % self.backupLdifFolder,
                           "%s/scripts.ldif" % self.backupLdifFolder,
                           "%s/site.ldif" % self.backupLdifFolder,
                           "%s/trust_relationships.ldif" % self.backupLdifFolder,
                           "%s/u2f.ldif" % self.backupLdifFolder,
                           "%s/uma.ldif" % self.backupLdifFolder,
                           ]

    def copyfile(self, infile, destfolder):
        try:
            shutil.copy(infile, destfolder)
            logging.debug("copied %s to %s" % (infile, destfolder))
        except:
            logging.error("error copying %s to %s" % (infile, destfolder))
            logging.error(traceback.format_exc())

    def renderTemplate(self, filePath, templateFolder, outputFolder):
        self.logIt("Rendering template %s" % filePath)
        fn = os.path.split(filePath)[-1]
        f = open(os.path.join(templateFolder, fn))
        template_text = f.read()
        f.close()
        newFn = open(os.path.join(outputFolder, fn), 'w+')
        newFn.write(template_text % self.__dict__)
        newFn.close()

    def render_templates(self):
        # 1. slapd.conf
        cmd = os.path.join(self.openldapBinFolder, "slappasswd") + " -s " \
            + self.ldapPass
        self.openldapPassHash = os.popen(cmd).read().strip()
        self.renderTemplate(self.openldapSlapdConf, self.templateFolder,
                            self.outputFolder)
        # 2. symas-openldap.conf
        self.renderTemplate(self.openldapSymasConf, self.templateFolder,
                            self.outputFolder)
        # 3. user.schema
        self.renderTemplate(self.user_schema, self.templateFolder,
                            self.outputFolder)

    # args = command + args, i.e. ['ls', '-ltr']
    def run(self, args, cwd=None, env=None):
        logging.debug('Running: %s' % ' '.join(args))
        try:
            p = subprocess.Popen(args, stdout=subprocess.PIPE,
                                 stderr=subprocess.PIPE, cwd=cwd, env=env)
            output, err = p.communicate()
            if output:
                logging.debug(output)
            if err:
                logging.error(err)
        except:
            logging.error("Error running command : %s" % " ".join(args))
            logging.error(traceback.format_exc())

    def configure_openldap(self):
        logging.info("Configuring OpenLDAP")
        # 1. Copy the conf files to
        self.copyFile(self.openldapSlapdConf, self.openldapConfFolder)
        self.copyFile(self.openldapSymasConf, self.openldapConfFolder)
        # 2. Copy the schema files into place
        self.copyFile("%s/static/openldap/gluu.schema" % self.setupFolder, "/opt/gluu/")
        self.copyFile("%s/static/openldap/custom.schema" % self.setupFolder, "/opt/gluu/")
        self.copyFile(self.user_schema, "/opt/gluu/")
        # 4. Create the PEM file from key and crt
        with open(self.openldapTLSCACert, 'w') as pem:
            with open(self.openldapTLSCert, 'r') as crt:
                pem.write(crt.read())
            with open(self.openldapTLSKey, 'r') as key:
                pem.write(key.read())
        # 5. Generate the cn=config directory
        self.run([self.cmd_mkdir, '-p', self.openldapCnConfig])
        self.run([self.slaptest, '-f', self.openldapSlapdConf, '-F', self.openldapCnConfig])

    def import_ldif_openldap(self):
        self.logIt("Importing LDIF files into OpenLDAP")
        cmd = os.path.join(self.openldapBinFolder, 'slapadd')
        config = os.path.join(self.openldapConfFolder, 'slapd.conf')
        for ldif in self.ldif_files:
            if 'site.ldif' in ldif:
                self.run([cmd, '-b', 'o=site', '-f', config, '-l', ldif])
            else:
                self.run([cmd, '-b', 'o=gluu', '-f', config, '-l', ldif])


if __name__ == '__main__':
    # Check if the opendj export has been done
    if not os.path.isdir('opendj_export'):
        print "OpenDJ export not found! Did you run export_opendj.py?"
        sys.exit(1)

    ip = raw_input('Enter the IP address: ').strip()
    ldapPass = raw_input('Enter the LDAP Password: ').strip()

    setup = SetupOpenLDAP(ip, ldapPass)
    # grab the old inumOrgFN
    with open('/install/community-edition-setup/setup.properties.last') as ofile:
        for line in ofile:
            if 'inumOrgFN' in line:
                setup.inumOrgFN = line.split('=')[-1].strip()
                break

    setup.render_templates()
    setup.configure_openldap()
    setup.import_ldif_openldap()
