#!/usr/bin/python

# Requires JSON Merge library
# wget https://github.com/avian2/jsonmerge/archive/master.zip
# unzip master.zip
# cd jsonmerge-master
# python setup.py install

# Also requires ldif.py in same folder

import os
import os.path
import shutil
import sys
import traceback
from ldif import LDIFParser
from jsonmerge import merge
import base64
import json
import uuid
import tempfile
import logging

password_file = tempfile.mkstemp()[1]
backup24_folder = None
backup_version = None
current_version = None

service = "/usr/sbin/service"
ldapmodify = "/opt/opendj/bin/ldapmodify"
ldapsearch = "/opt/opendj/bin/ldapsearch"
ldapdelete = "/opt/opendj/bin/ldapdelete"

ignore_files = ['101-ox.ldif',
                'gluuImportPerson.properties',
                'oxTrust.properties',
                'oxauth-config.xml',
                'oxauth-errors.json',
                'oxauth.config.reload',
                'oxauth-static-conf.json',
                'oxtrust.config.reload',
                ]

ldap_creds = ['-h', 'localhost',
              '-p', '1636', '-Z', '-X',
              '-D', '"cn=directory manager"',
              '-j', password_file
              ]

# configure logging
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)-8s %(message)s',
                    filename='import_24.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)


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


def addEntry(dn, entry, ldifModFolder):
    newLdif = """dn: %s
changetype: add
""" % dn
    for attr in entry.keys():
        for value in entry[attr]:
            newLdif = newLdif + getMod(attr, value)
    newLdif = newLdif + "\n"
    new_fn = str(len(dn.split(','))) + '_' + str(uuid.uuid4())
    filename = '%s/%s.ldif' % (ldifModFolder, new_fn)
    f = open(filename, 'w')
    f.write(newLdif)
    f.close()


def getNewConfig(fn):
    # Backup the appliance config just in case!
    args = [ldapsearch] + ldap_creds + \
           ['-b',
            'o=gluu',
            'objectclass=*']
    output = getOutput(args)
    f = open(fn, 'w')
    f.write(output)
    f.close()
    logging.info("Created backup of new ldif to %s" % fn)


def copyFiles(backup24_folder):
    logging.info('Copying backup files from /etc, /opt and /usr')
    os.path.walk("%s/etc" % backup24_folder, walk_function, None)
    os.path.walk("%s/opt" % backup24_folder, walk_function, None)
    os.path.walk("%s/usr" % backup24_folder, walk_function, None)


def deleteEntries(dn_list):
    for dn in dn_list:
        cmd = [ldapdelete] + ldap_creds + [dn]
        output = getOutput(cmd)
        if output:
            logging.info(output)
        else:
            logging.error("Error deleting %s" % dn)


def getAttributeValue(fn, targetAttr):
    # Load oxAuth Config From LDIF
    parser = MyLDIF(open(fn, 'rb'), sys.stdout)
    parser.targetAttr = targetAttr
    parser.parse()
    value = parser.targetAttr
    return value


def getOldEntryMap(folder):
    files = os.listdir(folder)
    dnMap = {}

    # get the new admin DN
    admin_dn = getDns('/opt/opendj/ldif/people.ldif')[0]

    for fn in files:
        # oxIDPAuthentication in appliance.ldif file is incompatible with
        # the new version of gluu-server. Hence skip the file
        if 'appliance' in fn and '2.3' in backup_version:
            continue
        dnList = getDns("%s/%s" % (folder, fn))
        for dn in dnList:
            # skip the entry of Admin DN and its leaves
            if fn == 'people.ldif' and admin_dn in dn:
                continue
            dnMap[dn] = fn
    return dnMap


def getEntry(fn, dn):
    parser = MyLDIF(open(fn, 'rb'), sys.stdout)
    parser.targetDN = dn
    parser.parse()
    return parser.targetEntry


def getDns(fn):
    parser = MyLDIF(open(fn, 'rb'), sys.stdout)
    parser.parse()
    return parser.DNs


def getMod(attr, s):
    val = str(s).strip()
    if val.find('\n') > -1:
        val = base64.b64encode(val)
        return "%s\n" % tab_attr(attr, val, True)
    elif len(val) > (78 - len(attr)):
        return "%s\n" % tab_attr(attr, val)
    else:
        return "%s: %s\n" % (attr, val)


def getOutput(args):
        try:
            logging.debug("Running command : %s" % " ".join(args))
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logging.error("Error running command : %s" % " ".join(args))
            logging.error(traceback.format_exc())
            sys.exit(1)


def restoreConfig(ldifFolder, newLdif, ldifModFolder):
    logging.info('Comparing old LDAP data and creating `modify` files.')
    ignoreList = ['objectClass', 'ou', 'oxAuthJwks', 'oxAuthConfWebKeys']
    current_config_dns = getDns(newLdif)
    oldDnMap = getOldEntryMap(ldifFolder)
    for dn in oldDnMap.keys():
        old_entry = getEntry("%s/%s" % (ldifFolder, oldDnMap[dn]), dn)
        if dn not in current_config_dns:
            addEntry(dn, old_entry, ldifModFolder)
            continue
        new_entry = getEntry(newLdif, dn)
        for attr in old_entry.keys():
            # Note: Prefixing with part length helps in inserting base
            # before leaf entries in the LDAP
            # Filename = DN_part_length + unique random string
            new_fn = str(len(dn.split(','))) + '_' + str(uuid.uuid4())
            filename = '%s/%s.ldif' % (ldifModFolder, new_fn)

            if attr in ignoreList:
                continue

            if attr not in new_entry:
                writeMod(dn, attr, old_entry[attr], filename, True)
                logging.debug("Adding attr %s to %s", attr, dn)
            elif old_entry[attr] != new_entry[attr]:
                mod_list = None
                if len(old_entry[attr]) == 1:
                    try:
                        logging.debug("Merging json value for %s", attr)
                        old_json = json.loads(old_entry[attr][0])
                        new_json = json.loads(new_entry[attr][0])
                        new_json = merge(new_json, old_json)
                        mod_list = [json.dumps(new_json)]
                    except:
                        mod_list = old_entry[attr]
                        logging.debug("Keeping old value for %s", attr)
                else:
                    mod_list = old_entry[attr]
                    logging.debug("Keeping multiple old value for %s", attr)
                writeMod(dn, attr, mod_list, filename)


def startOpenDJ():
    logging.info('Starting Directory Server ...')
    output = getOutput([service, 'opendj', 'start'])
    if output.find("Directory Server has started successfully") > 0:
        logging.info("Directory Server has started successfully")
    else:
        logging.critical("OpenDJ did not start properly... exiting."
                         " Check /opt/opendj/logs/errors")
        sys.exit(2)


def stopOpenDJ():
    logging.info('Stopping Directory Server ...')
    output = getOutput([service, 'opendj', 'stop'])
    if output.find("Directory Server is now stopped") > 0:
        logging.info("Directory Server is now stopped")
    else:
        logging.critical("OpenDJ did not stop properly... exiting."
                         " Check /opt/opendj/logs/errors")
        sys.exit(3)


def tab_attr(attr, value, encoded=False):
    lines = ['%s: ' % attr]
    if encoded:
        lines = ['%s:: ' % attr]
    for char in value:
        current_line = lines[-1]
        if len(current_line) < 80:
            new_line = current_line + char
            del lines[-1]
            lines.append(new_line)
        else:
            lines.append(" " + char)
    return "\n".join(lines)


def uploadLDIF(ldifFolder, outputLdifFolder):
    logging.info('Uploading LDAP data.')
    files = sorted(os.listdir(outputLdifFolder))
    for fn in files:
        cmd = [ldapmodify] + ldap_creds + ['-a', '-f',
                                           "%s/%s" % (outputLdifFolder, fn)]
        output = getOutput(cmd)
        if output:
            logging.debug(output)
        else:
            logging.error("Error adding file %s", fn)


def walk_function(a, directory, files):
    # Skip copying the openDJ config from older versions to 2.4.3
    if '2.4.3' in current_version and '2.4.3' not in backup_version:
        ignore_folders = ['opendj', 'template', 'endorsed']
        for folder in ignore_folders:
            if folder in directory:
                return

    for f in files:
        if f in ignore_files:
            continue
        fn = "%s/%s" % (directory, f)
        targetFn = fn.replace(backup24_folder, '')
        if os.path.isdir(fn):
            if not os.path.exists(targetFn):
                os.mkdir(targetFn)
        else:
            # It's a file...
            try:
                logging.debug("copying %s", targetFn)
                shutil.copyfile(fn, targetFn)
            except:
                logging.error("Error copying %s", targetFn)


def writeMod(dn, attr, value_list, fn, add=False):
    operation = "replace"
    if add:
        operation = "add"
    modLdif = """dn: %s
changetype: modify
%s: %s\n""" % (dn, operation, attr)
    if value_list is None:
        logging.warning('Skipping emtry value %s', attr)
        return
    for val in value_list:
        modLdif = modLdif + getMod(attr, val)
    modLdif = modLdif + "\n"
    f = open(fn, 'w')
    f.write(modLdif)
    f.close()
    logging.debug('Writing Mod for %s at %s', attr, fn)


def stopTomcat():
    logging.info('Stopping Tomcat ...')
    output = getOutput([service, 'tomcat', 'stop'])
    logging.debug(output)


def startTomcat():
    logging.info('Starting Tomcat ...')
    output = getOutput([service, 'tomcat', 'start'])
    logging.debug(output)


def preparePasswordFile():
    # prepare password_file
    with open('/install/community-edition-setup/setup.properties.last', 'r') \
            as sfile:
        for line in sfile:
            if 'ldapPass=' in line:
                with open(password_file, 'w') as pfile:
                    pfile.write(line.split('=')[-1])
                break


def getCurrentVersion():
    with open('/opt/tomcat/webapps/oxauth/META-INF/MANIFEST.MF', 'r') as f:
        for line in f:
            if 'Implementation-Version' in line:
                return line.split(':')[-1].strip()


def getBackupVersion():
    with open(os.path.join(backup24_folder, 'setup.properties'), 'r') as f:
        for line in f:
            if 'version=' in line:
                return line.split('=')[-1].strip()


def main(folder_name):
    global backup24_folder, backup_version, current_version, service

    # Verify that all required folders are present
    backup24_folder = folder_name
    if not os.path.exists(backup24_folder):
        logging.critical("Backup folder %s does not exist.", backup24_folder)
        sys.exit(1)

    etc_folder = os.path.join(backup24_folder, 'etc')
    opt_folder = os.path.join(backup24_folder, 'opt')
    ldif_folder = os.path.join(backup24_folder, 'ldif')

    if not (os.path.exists(etc_folder) and os.path.exists(opt_folder) and
            os.path.exists(ldif_folder)):
        logging.critical("Backup folder doesn't have all the information."
                         " Rerun export.")
        sys.exit(1)

    # Identify the version of the backup and installation
    backup_version = getBackupVersion()
    current_version = getCurrentVersion()

    # some version specific adjustment
    if '2.4.3' in current_version and '2.4.3' not in backup_version:
        skip_files = ['oxauth.xml',  # /opt/tomcat/conf/Catalina/localhost
                      'oxasimba-ldap.properties',
                      'oxauth-ldap.properties',
                      'oxidp-ldap.properties',
                      'oxtrust-ldap.properties',  # /opt/tomcat/conf
                      'gluuTomcatWrapper.conf',
                      'catalina.properties',
                      'oxTrustLdap.properties',  # from 2.3.6
                      ]
        global ignore_files
        ignore_files += skip_files

    outputFolder = "./output_ldif"
    outputLdifFolder = "%s/config" % outputFolder
    newLdif = "%s/current_config.ldif" % outputFolder

    if not os.path.exists(outputFolder):
        os.mkdir(outputFolder)

    if not os.path.exists(outputLdifFolder):
        os.mkdir(outputLdifFolder)

    # rewrite service location as CentOS and Ubuntu have different values
    service = getOutput(['whereis', 'service']).split(' ')[1].strip()

    stopTomcat()
    preparePasswordFile()
    stopOpenDJ()
    copyFiles(backup24_folder)
    startOpenDJ()
    getNewConfig(newLdif)
    restoreConfig(ldif_folder, newLdif, outputLdifFolder)
    uploadLDIF(ldif_folder, outputLdifFolder)
    startTomcat()

    # remove the password_file
    os.remove(password_file)
    logging.info("Import finished.")


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "Usage: ./import24.py <path_to_backup_folder>"
        print "Example:\n ./import24.py /root/backup_24"
    else:
        main(sys.argv[1])
