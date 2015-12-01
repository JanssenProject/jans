#!/usr/bin/python

import time, subprocess, traceback, sys, os

log = "./export23.log"
logError = "./export23.error"
bu_folder = "./backup23"
password_file = "/root/.pw"
folders_to_backup = ['/opt/apache-tomcat-7.0.55/conf',
                     '/opt/apache-tomcat-7.0.55/conf/python',
                     '/opt/opendj/config',
                     '/etc/certs',
                     '/opt/idp/conf',
                     '/opt/idp/metadata',
                     '/opt/tomcat/endorsed']
mkdir = '/bin/mkdir'
cp = '/bin/cp'
ldapsearch = "/opt/opendj/bin/ldapsearch"
ldap_creds = ['-h', 'localhost', '-p', '1389', '-D', '"cn=directory', 'manager"', '-j', password_file]
base_dns = ['ou=people',
            'ou=groups',
            'ou=attributes',
            'ou=scopes',
            'ou=clients',
            'ou=scripts',
            'ou=uma',
            'ou=hosts',
            'ou=u2f']
orgInum = None

def backupFiles():
    for folder in folders_to_backup:
        run([cp, '-r', folder, bu_folder])

def getLdif():
    # get organization id
    args = [ldapsearch] + ldap_creds + ['-s', 'one', '-b', 'o=gluu', 'o=*', 'dn']
    output = getOutput(args)
    orgInum = output.split(",")[0].split("o=")[-1]

    # Backup the data
    for basedn in base_dns:
        args = [ldapsearch] + ldap_creds + ['-b',
                                            '"%s,o=%s,o=gluu"' % (basedn, orgInum),
                                            '"objectclass=*"']
        output = getOutput(args)
        ou = basedn.split("=")[-1]
        f = open("%s/ldif/%s.ldif" % (bu_folder, ou), 'w')
        f.write(output)
        f.close()

    # Backup the config
    args = [ldapsearch] + ldap_creds + ['-b', '"ou=appliances,o=gluu"', '"objectclass=*"']
    output = getOutput(args)
    f = open("%s/ldif/appliance.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

def getOutput(args):
        try:
            p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=None)
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logIt("Error running command : %s" % " ".join(args), True)
            logIt(traceback.format_exc(), True)
            sys.exit(2)

def logIt(msg, errorLog=False):
    if errorLog:
        f = open(logError, 'a')
        f.write('%s %s\n' % (time.strftime('%X %x'), msg))
        f.close()
    f = open(log, 'a')
    f.write('%s %s\n' % (time.strftime('%X %x'), msg))
    f.close()

def makeFolders():
    try:
        run([mkdir, '-p', bu_folder])
        run([mkdir, '-p', "%s/ldif" % bu_folder])
    except:
        logIt("Error making folders", True)
        logIt(traceback.format_exc(), True)

def run(args, cwd=None):
    logIt('Running: %s' % ' '.join(args))
    try:
        p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cwd)
        output, err = p.communicate()
        if output:
            logIt(output)
        if err:
            logIt("Error running command : %s" % " ".join(args), True)
            logIt(err, True)
            sys.exit(3)
    except:
        logIt("Error running command : %s" % " ".join(args), True)
        logIt(traceback.format_exc(), True)
        sys.exit(4)

makeFolders()
backupFiles()
getLdif()