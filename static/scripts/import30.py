import os
import os.path
import sys
import logging
import traceback
import shutil
import json

from ldif import LDIFParser, LDIFWriter
from jsonmerge import merge

# configure logging
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)-8s %(name)s %(message)s',
                    filename='import30.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)
logging.getLogger('jsonmerge').setLevel(logging.WARNING)


class MyLDIF(LDIFParser):
    def __init__(self, input, output):
        LDIFParser.__init__(self, input)
        self.targetDN = None
        self.targetAttr = None
        self.targetEntry = None
        self.DNs = []
        self.lastDN = None
        self.lastEntry = None

    def getResults(self):
        return (self.targetDN, self.targetAttr)

    def getDNs(self):
        return self.DNs

    def getLastEntry(self):
        return self.lastEntry

    def handle(self, dn, entry):
        if self.targetDN is None:
            self.targetDN = dn
        self.lastDN = dn
        self.DNs.append(dn)
        self.lastEntry = entry
        if dn.lower().strip() == self.targetDN.lower().strip():
            self.targetEntry = entry
            if self.targetAttr in entry:
                self.targetAttr = entry[self.targetAttr]


class Migration(object):
    def __init__(self, backup):
        logging.info("Starting migration.")
        self.backupDir = backup
        self.ldifDir = os.path.join(backup, 'ldif')
        self.currentDir = os.path.dirname(os.path.realpath(__file__))
        self.workingDir = os.path.join(self.currentDir, 'migration')
        # TODO get the correct service command based on the OS
        self.service = "/usr/sbin/service"

        self.slapdConf = "/opt/symas/etc/openldap/slapd.conf"
        self.slapcat = "/opt/symas/bin/slapcat"
        self.slapadd = "/opt/symas/bin/slapadd"

    def verifyBackupData(self):
        if not os.path.exists(self.backupDir):
            logging.error("Backup folder %s doesn't exist! Quitting migration",
                          self.backupDir)
            sys.exit(1)
        if not os.path.exists(self.ldifDir):
            logging.error("Backup doesn't contain directory for LDIF data."
                          " Nothing to migrate. Quitting.")
            sys.exit(1)

    def setupWorkDirectory(self):
        if not os.path.exists(self.workingDir):
            os.mkdir(self.workingDir)
        else:
            # Clean the directory in case its already present
            shutil.rmtree(self.workingDir)
            os.mkdir(self.workingDir)

    def getOutput(self, args):
        try:
            logging.debug("Running command : %s" % " ".join(args))
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logging.error("Error running command : %s" % " ".join(args))
            logging.error(traceback.format_exc())
            sys.exit(1)

    def stopSolserver(self):
        logging.info("Stopping OpenLDAP Server.")
        output = self.getOutput([self.service, 'solserver', 'stop'])
        if "Symas OpenLDAP LDAP services slapd stopping....  done." in output:
            return
        else:
            logging.error(output)
            sys.exit(1)

    def startSolserver(self):
        logging.info("Starting OpenLDAP Server.")
        output = self.getOutput([self.service, 'solserver', 'start'])
        if "Symas OpenLDAP LDAP services slapd starting...  done." in output:
            return
        else:
            logging.error(output)
            sys.exit(1)

    def exportInstallData(self):
        logging.info("Exporting LDAP data.")
        self.installDataLdif = os.path.join(self.workingDir, 'o_gluu.ldif')
        output = self.getOutput([self.slapcat, '-f', self.slapdConf, '-b',
                                'o=gluu', '-l', self.installDataLdif])
        logging.debug(output)

    def getEntry(self, fn, dn):
        parser = MyLDIF(open(fn, 'rb'), sys.stdout)
        parser.targetDN = dn
        parser.parse()
        return parser.targetEntry

    def getDns(self, fn):
        parser = MyLDIF(open(fn, 'rb'), sys.stdout)
        parser.parse()
        return parser.DNs

    def getOldEntryMap(self):
        files = os.listdir(self.ldifDir)
        dnMap = {}

        # get the new admin DN
        admin_ldif = '/install/community-edition-setup/output/people.ldif'
        admin_dn = self.getDns(admin_ldif)[0]

        for fn in files:
            dnList = self.getDns(os.path.join(self.ldifDir, fn))
            for dn in dnList:
                # skip the entry of Admin DN and appliance data
                if (fn == 'people.ldif' and admin_dn in dn) or \
                        ('appliance' in fn):
                    continue
                dnMap[dn] = fn
        return dnMap

    def processBackupData(self):
        logging.info('Processing the LDIF data.')

        self.processedLDIF = os.path.join(self.workingDir, "processed.ldif")
        self.processTempFile = os.path.join(self.workingDir, "temp.ldif")
        processed_fp = open(self.processTempFile, 'w')
        ldif_writer = LDIFWriter(processed_fp)

        currentDNs = self.getDns(self.installDataLdif)
        old_dn_map = self.getOldEntryMap(self.backupDir)

        ignoreList = ['objectClass', 'ou', 'oxAuthJwks', 'oxAuthConfWebKeys']
        multivalueAttrs = ['oxTrustEmail', 'oxTrustPhoneValue', 'oxTrustImsValue',
                           'oxTrustPhotos', 'oxTrustAddresses', 'oxTrustRole',
                           'oxTrustEntitlements', 'oxTrustx509Certificate']

        # Rewriting all the new DNs in the new installation to ldif file
        for dn in currentDNs:
            new_entry = self.getEntry(self.installDataLdif, dn)
            if dn not in old_dn_map.keys():
                #  Write to the file if there is no matching old DN data
                ldif_writer.unparse(dn, new_entry)
                continue

            old_entry = self.getEntry(os.path.join(self.ldifDir, old_dn_map[dn]), dn)
            for attr in old_entry.keys():
                if attr in ignoreList:
                    continue

                if attr not in new_entry:
                    new_entry[attr] = old_entry[attr]
                elif old_entry[attr] != new_entry[attr]:
                    if len(old_entry[attr]) == 1:
                        try:
                            old_json = json.loads(old_entry[attr][0])
                            new_json = json.loads(new_entry[attr][0])
                            new_json = merge(new_json, old_json)
                            new_entry[attr] = [json.dumps(new_json)]
                        except:
                            new_entry[attr] = old_entry[attr]
                            logging.debug("Keeping old value for %s", attr)
                    else:
                        new_entry[attr] = old_entry[attr]
                        logging.debug("Keep multiple old values for %s", attr)
            ldif_writer.unparse(dn, new_entry)

        # Pick all the left out DNs from the old DN map and write them to the LDIF
        for dn in sorted(old_dn_map, key=len):
            if dn in currentDNs:
                continue  # Already processed

            entry = self.getEntry(os.path.join(self.ldifDir, old_dn_map[dn]), dn)

            for attr in entry.keys():
                if attr not in multivalueAttrs:
                    continue  # skip conversion

                attr_values = []
                for val in entry[attr]:
                    json_value = None
                    try:
                        json_value = json.loads(val)
                        if type(json_value) is list:
                            attr_values.extend([json.dumps(v) for v in json_value])
                    except:
                        logging.debug('Cannot parse multival %s in DN %s', attr, dn)
                        attr_values.append(val)
                entry[attr] = attr_values

            ldif_writer.unparse(dn, entry)

        # Finally
        processed_fp.close()

        # Update the Schema change for lastModifiedTime
        with open(self.processTempFile, 'r') as infile:
            with open(self.processedLDIF, 'w') as outfile:
                for line in infile:
                    line.replace("lastModifiedTime", "oxLastAccessTime")
                    line.replace("cn=directory manager", "cn=directory manager,o=gluu")
                    outfile.write(line)

    def importProcessedData(self):
        logging.info("Importing Processed LDAP data.")
        output = self.getOutput([self.slapadd, '-c', '-b', 'o=gluu', '-f',
                                self.slapdConf, '-l', self.processedLDIF])
        logging.debug(output)

    def migrate(self):
        """Main function for the migration of backup data
        """
        self.verifyBackupData()
        self.setupWorkDirectory()
        self.stopSolserver()
        self.exportInstallData()
        self.processBackupData()
        self.importProcessedData()
        self.startSolserver()


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print "Usage: ./import30.py <path_to_backup_folder>"
        print "Example:\n ./import30.py /root/backup_24"
    else:
        migrator = Migration(sys.argv[1])
        migrator.migrate()
