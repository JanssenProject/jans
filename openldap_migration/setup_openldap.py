import sys
import os
import os.path
import logging
import shutil
import traceback
import subprocess
import re

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

        self.gluuOptFolder = '/opt/gluu'
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
        self.openldapSchemaFolder = "%s/schema/openldap" % self.gluuOptFolder
        self.slaptest = '%s/slaptest' % self.openldapBinFolder
        self.openldapDataDir = '/opt/gluu/data'
        self.o_gluu = '%s/o_gluu.ldif' % self.miniSetupFolder
        self.o_gluu_temp = '%s/o_gluu.temp' % self.miniSetupFolder
        self.o_site = '%s/o_site.ldif' % self.miniSetupFolder
        self.o_site_temp = '%s/o_site.temp' % self.miniSetupFolder

        self.attrs = 1000
        self.objclasses = 1000

    def copyFile(self, infile, destfolder):
        try:
            shutil.copy(infile, destfolder)
            logging.debug("copied %s to %s" % (infile, destfolder))
        except:
            logging.error("error copying %s to %s" % (infile, destfolder))
            logging.error(traceback.format_exc())

    def createDirs(self, name):
        try:
            if not os.path.exists(name):
                os.makedirs(name, 0700)
                logging.debug('created dir: %s' % name)
        except:
            logging.error("error making directory %s" % name)
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
        self.createDirs(self.openldapSchemaFolder)
        self.copyFile("%s/static/openldap/gluu.schema" % self.setupFolder, self.openldapSchemaFolder)
        self.copyFile("%s/static/openldap/custom.schema" % self.setupFolder, self.openldapSchemaFolder)
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
        command = [self.ldif_export, '-n', 'userRoot', '-l', self.o_gluu_temp]
        self.run(command)
        command = [self.ldif_export, '-n', 'site', '-l', self.o_site_temp]
        self.run(command)

    def import_openldap(self):
        logging.info("Importing LDIF files into OpenLDAP")
        cmd = os.path.join(self.openldapBinFolder, 'slapadd')
        config = os.path.join(self.openldapConfFolder, 'slapd.conf')

        # Import the base.ldif
        self.run([cmd, '-b', 'o=gluu', '-f', config, '-l', self.o_gluu])
        self.run([cmd, '-b', 'o=site', '-f', config, '-l', self.o_site])

    def get_old_properties(self):
        # grab the old inumOrgFN
        logging.info('Scanning setup.properties.last for data')
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

    def clean_ldif_data(self):
        with open(self.o_gluu_temp, 'r') as infile:
            with open(self.o_gluu, 'w') as outfile:
                for line in infile:
                    outfile.write(line.replace("lastModifiedTime", "oxLastAccessTime"))

        with open(self.o_site_temp, 'r') as infile:
            with open(self.o_site, 'w') as outfile:
                for line in infile:
                    outfile.write(line.replace("lastModifiedTime", "oxLastAccessTime"))

    def convert_schema(self, f):
        infile = open(f, 'r')
        output = ""

        for line in infile:
            if re.match('^dn:', line) or re.match('^objectClass:', line) or \
                    re.match('^cn:', line):
                continue
            # empty lines and the comments are copied as such
            if re.match('^#', line) or re.match('^\s*$', line):
                pass
            elif re.match('^\s\s', line):  # change the space indendation to tabs
                line = re.sub('^\s\s', '\t', line)
            elif re.match('^\s', line):
                line = re.sub('^\s', '\t', line)
            # Change the keyword for attributetype
            elif re.match('^attributeTypes:\s', line, re.IGNORECASE):
                line = re.sub('^attributeTypes:', '\nattributetype', line, 1,
                              re.IGNORECASE)
                oid = 'oxAttribute:' + str(self.attrs+1)
                line = re.sub('\s[\d]+\s', ' '+oid+' ', line, 1, re.IGNORECASE)
                self.attrs += 1
            # Change the keyword for objectclass
            elif re.match('^objectClasses:\s', line, re.IGNORECASE):
                line = re.sub('^objectClasses:', '\nobjectclass', line, 1,
                              re.IGNORECASE)
                oid = 'oxObjectClass:' + str(self.objclasses+1)
                line = re.sub('ox-[\w]+-oid', oid, line, 1, re.IGNORECASE)
                self.objclasses += 1
            else:
                logging.warning("Skipping Line: {}".format(line))
                line = ""

            output += line

        infile.close()
        return output

    def create_user_schema(self):
        logging.info('Converting existing custom attributes to OpenLDAP schema')
        schema_99 = '/opt/opendj/config/schema/99-user.ldif'
        schema_100 = '/opt/opendj/config/schema/100-user.ldif'
        new_user = '%s/new_99.ldif' % self.miniSetupFolder

        with open(schema_99, 'r') as olduser:
            with open(new_user, 'w') as newuser:
                for line in olduser:
                    if 'SUP top' in line:
                        line = line.replace('SUP top', 'SUP gluuPerson')
                    newuser.write(line)

        outfile = open('%s/user.schema' % self.outputFolder, 'w')
        output = self.convert_schema(schema_100)
        output = output + "\n" + self.convert_schema(new_user)
        outfile.write(output)
        outfile.close()


if __name__ == '__main__':
    setup = SetupOpenLDAP()
    setup.get_old_properties()
    setup.create_user_schema()
    setup.render_templates()
    setup.configure_openldap()
    setup.export_opendj()
    setup.clean_ldif_data()
    setup.import_openldap()
