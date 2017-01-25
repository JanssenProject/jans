#!/usr/bin/python
"""import23.py - Script to import the data into Gluu Server 2.3.x
"""

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
import time
import traceback
from ldif import LDIFParser
from jsonmerge import merge
import base64
import json
import uuid

log = "./import23.log"
logError = "./import23.error"
password_file = "/root/.pw"

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
                'oxtrust.config.reload'
                ]

ldap_creds = ['-h',
              'localhost',
              '-p',
              '1389',
              '-D',
              '"cn=directory',
              'manager"',
              '-j',
              password_file
              ]

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
        if self.targetDN == None:
            self.targetDN = dn
        self.lastDN = dn
        self.DNs.append(dn)
        self.lastEntry = entry
        if dn.lower().strip() == self.targetDN.lower().strip():
            self.targetEntry = entry
            if entry.has_key(self.targetAttr):
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
    logIt("Wrote new ldif to %s" % fn)

def copyFiles(backup23_folder):
    os.path.walk ("%s/etc" % backup23_folder, walk_function, None)
    os.path.walk ("%s/opt" % backup23_folder, walk_function, None)
    os.path.walk ("%s/usr" % backup23_folder, walk_function, None)

def deleteEntries(dn_list):
    for dn in dn_list:
        cmd = [ldapdelete] + ldap_creds + [dn]
        output = getOutput(cmd)
        if output:
            logIt(output)
        else:
            logIt("Error deleting %s" % dn)

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
    for fn in files:
        if (fn == "site.ldif") or (fn == "people.ldif"):
            continue
        dnList = getDns("%s/%s" % (folder,fn))
        for dn in dnList:
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
            logIt("Running command : %s" % " ".join(args))
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logIt("Error running command : %s" % " ".join(args), True)
            logIt(traceback.format_exc(), True)
            sys.exit(1)

def logIt(msg, errorLog=False):
    if errorLog:
        f = open(logError, 'a')
        f.write('%s %s\n' % (time.strftime('%X %x'), msg))
        f.close()
    f = open(log, 'a')
    f.write('%s %s\n' % (time.strftime('%X %x'), msg))
    f.close()

def restoreConfig(ldifFolder, newLdif, ldifModFolder):
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
            if attr in ignoreList:
                continue
            if not new_entry.has_key(attr):
                writeMod(dn, attr, old_entry[attr], '%s/%s.ldif' % (ldifModFolder, str(uuid.uuid4())), True)
                logIt("Adding attr %s to %s" % (attr, dn))
            else:
                mod_list = None
                if old_entry[attr] != new_entry[attr]:
                    if len(old_entry[attr]) == 1:
                        try:
                            logIt("Merging json value for %s " % attr)
                            old_json = json.loads(old_entry[attr][0])
                            new_json = json.loads(new_entry[attr][0])
                            new_json = merge(new_json, old_json)
                            mod_list = [json.dumps(new_json)]
                        except:
                            mod_list = old_entry[attr]
                    else:
                        mod_list = old_entry[attr]
                        logIt("Keeping multiple old values for %s" % attr)
                else:
                    continue
                writeMod(dn, attr, mod_list, '%s/%s.ldif' % (ldifModFolder, str(uuid.uuid4())))

def startOpenDJ():
    output = getOutput([service, 'opendj', 'start'])
    if output.find("Directory Server has started successfully") > 0:
        logIt("Directory Server has started successfully")
    else:
        logIt("OpenDJ did not start properly... exiting. Check /opt/opendj/logs/errors", True)
        sys.exit(2)

def stopOpenDJ():
    output = getOutput([service, 'opendj', 'stop'])
    if output.find("Directory Server is now stopped") > 0:
        logIt("Directory Server is now stopped")
    else:
        logIt("OpenDJ did not stop properly... exiting. Check /opt/opendj/logs/errors", True)
        sys.exit(3)

def tab_attr(attr, value, encoded=False):
    targetLength = 80
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
    files = os.listdir(outputLdifFolder)
    for fn in files:
        cmd = [ldapmodify] + ldap_creds + ['-a', '-f', "%s/%s" % (outputLdifFolder, fn)]
        output = getOutput(cmd)
        if output:
            logIt(output)
        else:
            logIt("Error adding file %s" % fn, True)

    # delete default admin user created in 2.4 install
    dn_list = getDns("/opt/opendj/ldif/people.ldif")
    deleteEntries(dn_list)

    # Add People
    cmd = [ldapmodify] + ldap_creds + ['-a', '-c', '-f', "%s/people.ldif" % ldifFolder]
    output = getOutput(cmd)
    if output:
        logIt(output)
    else:
        logIt("Error adding people.ldif", True)

    dn_list = getDns("%s/site.ldif" % ldifFolder)
    if dn_list > 2:
        cmd = [ldapmodify] + ldap_creds + ['-a', '-c', '-f', "%s/site.ldif" % ldifFolder]
        output = getOutput(cmd)
        if output:
            logIt(output)
        else:
            logIt("Error adding site.ldif", True)

def walk_function(a, dir, files):
    for file in files:
        if file in ignore_files:
            continue
        fn = "%s/%s" % (dir, file)
        targetFn = fn[1:]
        if os.path.isdir(fn):
            if not os.path.exists(targetFn):
                os.mkdir(targetFn)
        else:
            # It's a file...
            try:
                logIt("copying %s" % targetFn)
                shutil.copyfile(fn, targetFn)
            except:
                logIt("Error copying %s" % targetFn, True)

def writeMod(dn, attr, value_list, fn, add=False):
    operation = "replace"
    if add:
        operation = "add"
    modLdif = """dn: %s
changetype: modify
%s: %s\n""" % (dn, operation, attr)
    if value_list == None: return
    for val in value_list:
        modLdif = modLdif + getMod(attr, val)
    modLdif = modLdif + "\n"
    f = open(fn, 'w')
    f.write(modLdif)
    f.close()

backup23_folder = None
error = False
try:
    backup23_folder = sys.argv[1]
    if (not os.path.exists("%s/ldif" % backup23_folder)):
        error = True
    if (not os.path.exists("%s/etc" % backup23_folder)):
        error = True
    if (not os.path.exists("%s/opt" % backup23_folder)):
        error = True
except:
    error = True

if error:
    print "backup folders not found"
    print "Usage: ./import.py <path_to_backup_folders>"
    sys.exit(1)

ldif_folder = "%s/ldif" % backup23_folder
outputFolder = "./output_ldif"
outputLdifFolder = "%s/config" % outputFolder

if not os.path.exists(outputFolder):
    os.mkdir(outputFolder)

if not os.path.exists(outputLdifFolder):
    os.mkdir(outputLdifFolder)

newLdif = "%s/current_config.ldif" % outputFolder

stopOpenDJ()
copyFiles(backup23_folder)
startOpenDJ()
getNewConfig(newLdif)
restoreConfig(ldif_folder, newLdif, outputLdifFolder)
uploadLDIF(ldif_folder, outputLdifFolder)
