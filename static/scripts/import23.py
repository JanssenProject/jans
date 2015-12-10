#!/usr/bin/python

import os
import os.path
import shutil
import sys
import time
import traceback
from ldif import LDIFParser, CreateLDIF

log = "./import23.log"
logError = "./import23.error"
ouputFolder = "./output_ldif"

service = "/usr/sbin/service"
ldapmodify = "/opt/opendj/bin/ldapmodify"

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

fixManagerGroupLdif = """dn: %s
changetype: modify
replace: gluuManagerGroup
gluuManagerGroup: %s

"""

bulk_ldif_files =  [ "groups.ldif",
                "people.ldif",
                "u2f.ldif",
                "attributes.ldif",
                "hosts.ldif",
                "scopes.ldif",
                "uma.ldif",
                "clients.ldif",
                "scripts.ldif",
                "site.ldif"
                ]

class ConfigLDIF(LDIFParser):
    def __init__(self, input, output):
        LDIFParser.__init__(self, input)
        self.targetDN = None
        self.targetAttr = None
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
        self.lastDN = dn
        self.DNs.append(dn)
        self.lastEntry = entry
        if dn.lower().strip() == targetDN.lower().strip():
            if entry.has_key(self.targetAttr):
                self.targetAttr = entry(self.targetAttr)

def copyFiles():
    os.path.walk ("./etc", walk_function, None)
    os.path.walk ("./opt", walk_function, None)

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

def updateConfiguration():
    # Load Config From LDIF
    fn = "./ldif/config.ldif"
    parser = ConfigLDIF(open(fn, 'rb'), sys.stdout)
    parser.parse()

    # Update oxAuth Config

    # Update oxTrust Config


def startOpenDJ():
    output = getOutput([service, 'opendj', 'start'])
    if output.index("Directory Server has started successfully") > 0:
        logIt("Directory Server has started successfully")
    else:
        logIt("OpenDJ did not start properly... exiting. Check /opt/opendj/logs/errors")
        sys.exit(2)

def stopOpenDJ():
    output = getOutput([service, 'opendj', 'start'])
    if output.index("Directory Server is now stopped") > 0:
        logIt("Directory Server is now stopped")
    else:
        logIt("OpenDJ did not stop properly... exiting. Check /opt/opendj/logs/errors")
        sys.exit(3)

def uploadLDIF():
    for ldif_file in ldif_files:
        cmd = [ldapmodify] + ldap_creds + ['-a', '-c', '-f', './ldif/%s' % ldif_file]
        output = getOutput(cmd)
        if output:
            logIt(output)
    if not os.path.exists(ouputFolder):
        os.mkdir(ouputFolder)

    # Load SAML Trust Relationships
    fn = "./ldif/trust_relationships.ldif"
    cmd = [ldapmodify] + ldap_creds + ['-a', '-f', fn]
    output = getOutput(cmd)
    if output:
        logIt(output)

    # Update Organization
    fn = "./ldif/organization.ldif"
    parser = ConfigLDIF(open(fn, 'rb'), sys.stdout)
    parser.parse()
    orgDN = parser.lastDN
    gluuManagerGroup = parser.lastEntry['gluuManagerGroup'][0]
    f = open("%s/updateManagerGroup.ldif" % ouputFolder, 'w')
    f.write(fixManagerGroupLdif % (orgDN, gluuManagerGroup))
    f.close()
    cmd = [ldapmodify] + ldap_creds + ['-a', '-f', fn]
    output = getOutput(cmd)
    if output:
        logIt(output)

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

stopOpenDJ()
copyFiles()
startOpenDJ()
uploadLDIF()
updateConfiguration()
