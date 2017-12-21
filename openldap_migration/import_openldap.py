#!/usr/bin/python

# Requires JSON Merge library
# wget https://github.com/avian2/jsonmerge/archive/master.zip
# unzip master.zip
# cd jsonmerge-master
# python setup.py install

# Also requires ldif.py in same folder

import os
import os.path
import sys
import traceback
from ldif import LDIFParser, LDIFWriter
from jsonmerge import merge
import json
import logging

backup24_folder = None

slapadd = '/opt/symas/bin/slapadd'
slapcat = '/opt/symas/bin/slapcat'
ldap_creds = ['-c', '-f', '/opt/symas/etc/openldap/slapd.conf']
ldap_data_folder = '/opt/gluu/data/'

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


def getOldEntryMap(folder):
    files = os.listdir(folder)
    dnMap = {}

    # get the new admin DN
    admin_dn = getDns('/install/community-edition-setup/output/people.ldif')[0]

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


def getOutput(args):
        try:
            logging.debug("Running command : %s" % " ".join(args))
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logging.error("Error running command : %s" % " ".join(args))
            logging.error(traceback.format_exc())
            sys.exit(1)


def importLDIF(folder):
    ldif_file = os.path.join(folder, 'processed_gluu.ldif')
    logging.info("Running slapadd on %s", ldif_file)
    command = [slapadd] + ldap_creds + ['-b', 'o=gluu', '-l', ldif_file]
    output = getOutput(command)
    logging.debug(output)

    ldif_file = os.path.join(folder, 'processed_site.ldif')
    logging.info("Running slapadd on %s", ldif_file)
    command = [slapadd] + ldap_creds + ['-b', 'o=site', '-l', ldif_file]
    output = getOutput(command)
    logging.debug(output)


def exportLDIF(folder):
    logging.info('Exporting the current LDAP data')
    o_gluu = os.path.join(folder, 'o_gluu.ldif')
    command = [slapcat] + ldap_creds + ['-b', 'o=gluu', '-s', 'o=gluu',
                                        '-l', o_gluu]
    output = getOutput(command)
    logging.debug(output)

    o_site = os.path.join(folder, 'o_site.ldif')
    command = [slapcat] + ldap_creds + ['-b', 'o=site', '-s', 'o=site',
                                        '-l', o_site]
    output = getOutput(command)
    logging.debug(output)


def processLDIF(backupFolder, newFolder):
    logging.info('Processing the LDIF data')
    o_gluu_ldif = os.path.join(newFolder, 'o_gluu.ldif')
    o_site_ldif = os.path.join(newFolder, 'o_site.ldif')
    currentDNs = getDns(o_gluu_ldif) + getDns(o_site_ldif)

    processed_gluu = open(os.path.join(newFolder, 'processed_gluu.ldif'), 'w')
    processed_site = open(os.path.join(newFolder, 'processed_site.ldif'), 'w')
    gluu_writer = LDIFWriter(processed_gluu)
    site_writer = LDIFWriter(processed_site)

    ignoreList = ['objectClass', 'ou', 'oxAuthJwks', 'oxAuthConfWebKeys']
    old_dn_map = getOldEntryMap(backupFolder)

    multivalueAttrs = ['oxTrustEmail', 'oxTrustPhoneValue', 'oxTrustImsValue',
                       'oxTrustPhotos', 'oxTrustAddresses', 'oxTrustRole',
                       'oxTrustEntitlements', 'oxTrustx509Certificate']
    # Rewriting all the new DNs in the new installation to ldif file
    for dn in currentDNs:
        if 'o=site' in dn:
            ldif_writer = site_writer
            current_ldif = o_site_ldif
        else:
            ldif_writer = gluu_writer
            current_ldif = o_gluu_ldif

        new_entry = getEntry(current_ldif, dn)
        if dn not in old_dn_map.keys():
            #  Write directly to the file if there is no matching old DN data
            ldif_writer.unparse(dn, new_entry)
            continue

        old_entry = getEntry(os.path.join(backupFolder, old_dn_map[dn]), dn)
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
        if 'o=site' in dn:
            ldif_writer = site_writer
        else:
            ldif_writer = gluu_writer

        if dn in currentDNs:
            continue  # Already processed

        entry = getEntry(os.path.join(backupFolder, old_dn_map[dn]), dn)

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
                    logging.debug('Cant parse multival %s in DN %s', attr, dn)
                    attr_values.append(val)
            entry[attr] = attr_values

        ldif_writer.unparse(dn, entry)

    # Finally
    processed_gluu.close()
    processed_site.close()


def removeDBFile():
    for the_file in os.listdir(ldap_data_folder):
        file_path = os.path.join(ldap_data_folder, the_file)
        os.unlink(file_path)


def main(folder_name):
    # Verify that all required folders are present
    backup24_folder = folder_name
    ldif_folder = os.path.join(backup24_folder, 'ldif')
    outputFolder = "./output_ldif"

    if not os.path.exists(backup24_folder):
        logging.critical("Backup folder %s does not exist.", backup24_folder)
        sys.exit(1)

    if not os.path.exists(ldif_folder):
        logging.critical("Backup folder doesn't have LDIF information."
                         " Rerun export_opendj.py")
        sys.exit(1)

    if not os.path.exists(outputFolder):
        os.mkdir(outputFolder)

    exportLDIF(outputFolder)
    processLDIF(ldif_folder, outputFolder)
    removeDBFile()
    importLDIF(outputFolder)
    logging.info("Import finished.")


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print "Usage: ./import_openldap.py <path_to_backup_folder>"
        print "Example:\n ./import_openldap.py /root/backup_24"
    else:
        main(sys.argv[1])
