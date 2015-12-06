#!/usr/bin/python

import os
import os.path
import shutil
import subprocess
import sys
import time
import traceback
from ldif import LDIFParser, LDIFWriter

log = "./import23.log"
logError = "./import23.error"

service = "/usr/sbin/service"

ignore_files = ['101-ox.ldif']

ldif_files =  [ "groups.ldif",
                "people.ldif",
                "u2f.ldif",
                "attributes.ldif",
                "hosts.ldif",
                "scopes.ldif",
                "uma.ldif",
                "clients.ldif",
                "organization.ldif",
                "scripts.ldif"
                ]

class MyLDIF(LDIFParser):
    def __init__(self, input, output):
        LDIFParser.__init__(self, input)
        self.writer = LDIFWriter(output)

    def handle(self, dn, entry):
        for attr in entry.keys():
            pass
            # search current ldap server
            # if entry not present... add it
            # if entry is present: compare attributes - and union values

def copyFiles():
    os.path.walk ("./etc", walk_function, None)
    os.path.walk ("./opt", walk_function, None)

def getOutput(args):
        try:
            logIt("Running command : %s" % " ".join(args))
            p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=None)
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

def uploadLDIF():
    output, error = getOutput([service, 'opendj', 'start'])
    if not error and out.index("Directory Server has started successfully") > 0:
        if not os.path.exists("./output_ldif"):
            os.mkdir("./output_ldif")
        for ldif_file in ldif_files:
            fn = "./ldif/%s" % ldif_file
            parser = MyLDIF(open(fn, 'rb'), sys.stdout)
            parser.parse()
    else:
        if error:
            login("Error starting OpenDJ:\t%s" % error)
            sys.exit(2)
        else:
            logIt("OpenDJ did not start properly... exiting. Check /opt/opendj/logs/errors")
            sys.exit(3)

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

copyFiles()
uploadLDIF()
