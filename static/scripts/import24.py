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

service = "/usr/sbin/service"
ldapmodify = "/opt/opendj/bin/ldapmodify"
ldapsearch = "/opt/opendj/bin/ldapsearch"
ldapdelete = "/opt/opendj/bin/ldapdelete"
stoptomcat = "/opt/tomcat/bin/shutdown.sh"
starttomcat = "/opt/tomcat/bin/startup.sh"

ignore_files = ['101-ox.ldif',
                'gluuImportPerson.properties',
                'oxTrust.properties',
                'oxauth-config.xml',
                'oxauth-errors.json',
                'oxauth.config.reload',
                'oxauth-static-conf.json',
                'oxtrust.config.reload'
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
    f = open("%s/%s.ldif" % (ldifModFolder, str(uuid.uuid4())), 'w')
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
    logging.debug('String of Value: %s', val)
    if (val.find('\n') > -1) or ('{' in val):
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
            logging.error("Error running command : %s" % " ".join(args), True)
            logging.error(traceback.format_exc(), True)
            sys.exit(1)


def restoreConfig(ldifFolder, newLdif, ldifModFolder):
    logging.info('Comparing old LDAP data and creating `modify` files.')
    ignoreList = ['objectClass', 'ou']
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
                        logging.debug("Keeping multiple old vals for %s", attr)
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
                         " Check /opt/opendj/logs/errors", True)
        sys.exit(2)


def stopOpenDJ():
    logging.info('Stopping Directory Server ...')
    output = getOutput([service, 'opendj', 'stop'])
    if output.find("Directory Server is now stopped") > 0:
        logging.info("Directory Server is now stopped")
    else:
        logging.critical("OpenDJ did not stop properly... exiting."
                         " Check /opt/opendj/logs/errors", True)
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
    files = os.listdir(outputLdifFolder)
    for fn in files:
        cmd = [ldapmodify] + ldap_creds + ['-a', '-f',
                                           "%s/%s" % (outputLdifFolder, fn)]
        output = getOutput(cmd)
        if output:
            logging.debug(output)
        else:
            logging.error("Error adding file %s", fn)


def walk_function(a, directory, files):
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


def main():
    global backup24_folder

    backup24_folder = sys.argv[1]
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

    outputFolder = "./output_ldif"
    outputLdifFolder = "%s/config" % outputFolder
    newLdif = "%s/current_config.ldif" % outputFolder

    if not os.path.exists(outputFolder):
        os.mkdir(outputFolder)

    if not os.path.exists(outputLdifFolder):
        os.mkdir(outputLdifFolder)

    # prepare password_file
    with open('/install/community-edition-setup/setup.properties.last', 'r') \
            as sfile:
        for line in sfile:
            if 'ldapPass=' in line:
                with open(password_file, 'w') as pfile:
                    pfile.write(line.split('=')[-1])
                break

    stopOpenDJ()
    copyFiles(backup24_folder)
    startOpenDJ()
    getNewConfig(newLdif)
    restoreConfig(ldif_folder, newLdif, outputLdifFolder)
    uploadLDIF(ldif_folder, outputLdifFolder)

    # remove the password_file
    os.remove(password_file)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "Usage: ./import24.py <path_to_backup_folder>"
        print "Example:\n ./import24.py /root/backup_24"
    else:
        main()
