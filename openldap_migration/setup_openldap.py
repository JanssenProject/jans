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
                    filename='setup_openldap.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)


class SetupOpenLDAP(object):

    def __init__(self):
        self.miniSetupFile = os.path.abspath(__file__)
        self.miniSetupFolder = os.path.dirname(self.miniSetupFile)
        self.setupFolder = os.path.dirname(self.miniSetupFolder)
        self.templateFolder = os.path.join(self.setupFolder, 'templates')
        self.outputFolder = os.path.join(self.setupFolder, 'output')
        self.backupFolder = os.path.join(self.miniSetupFolder, 'opendj_export')
        self.backupLdifFolder = os.path.join(self.backupFolder, 'ldif')

        self.cmd_mkdir = '/bin/mkdir'
        self.ldif_export = "/opt/opendj/bin/export-ldif"

        self.ip = None
        self.inumOrg = None
        self.inumOrgFN = None
        self.orgName = None
        self.ldapPass = None

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
        self.openldapPassHash = None
        self.openldapSlapdConf = '%s/slapd.conf' % self.outputFolder
        self.openldapSymasConf = '%s/symas-openldap.conf' % self.outputFolder
        self.slaptest = '%s/slaptest' % self.openldapBinFolder
        self.openldapDataDir = '/opt/gluu/data'
        self.o_gluu = '%s/o_gluu.ldif' % self.miniSetupFolder
        self.o_site = '%s/o_site.ldif' % self.miniSetupFolder

    def copyFile(self, infile, destfolder):
        try:
            shutil.copy(infile, destfolder)
            logging.debug("copied %s to %s" % (infile, destfolder))
        except:
            logging.error("error copying %s to %s" % (infile, destfolder))
            logging.error(traceback.format_exc())

    def renderTemplate(self, filePath, templateFolder, outputFolder):
        logging.debug("Rendering template %s" % filePath)
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
        # 0. Create the data dir
        self.run([self.cmd_mkdir, '-p', self.openldapDataDir])
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

    def export_opendj(self):
        logging.info("Exporting all the data from OpenDJ")
        command = [self.ldif_export, '-n', 'userRoot', '-l', self.o_gluu]
        self.run(command)
        command = [self.ldif_export, '-n', 'site', '-l', self.o_site]
        self.run(command)

    def import_openldap(self):
        logging.info("Importing LDIF files into OpenLDAP")
        cmd = os.path.join(self.openldapBinFolder, 'slapadd')
        config = os.path.join(self.openldapConfFolder, 'slapd.conf')

        # Import the base.ldif
        self.run([cmd, '-b', 'o=gluu', '-f', config, '-l', self.o_gluu])
        self.run([cmd, '-b', 'o=site', '-f', config, '-l', self.o_site])

    def get_old_data(self):
        # grab the old inumOrgFN
        with open('/install/community-edition-setup/setup.properties.last') as ofile:
            for line in ofile:
                if 'inumOrgFN=' in line:
                    self.inumOrgFN = line.split('=')[-1].strip()
                elif 'inumOrg=' in line:
                    self.inumOrg = line.split('=')[-1].strip()
                elif 'orgName=' in line:
                    self.orgName = line.split('=')[-1].strip()
                elif 'ip=' in line:
                    self.ip = line.split('=')[-1].strip()
                elif 'ldapPass=' in line:
                    self.ldapPass = line.split('=')[-1].strip()

if __name__ == '__main__':
    setup = SetupOpenLDAP()
    setup.get_old_data()
    setup.render_templates()
    setup.configure_openldap()
    setup.export_opendj()
    setup.import_openldap()
