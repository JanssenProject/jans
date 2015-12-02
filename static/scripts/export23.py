#!/usr/bin/python

import time, subprocess, traceback, sys, os, shutil

# Unix commands
mkdir = '/bin/mkdir'
cat = '/bin/cat'
hostname = '/bin/hostname'
grep = '/bin/grep'
ldapsearch = "/opt/opendj/bin/ldapsearch"

# File system stuff
log = "./export23.log"
logError = "./export23.error"
bu_folder = "./backup23"
password_file = "/root/.pw"
propertiesFn = "%s/setup.properties" % bu_folder
folders_to_backup = ['/opt/tomcat/conf',
                     '/opt/tomcat/endorsed',
                     '/opt/opendj/config',
                     '/etc/certs',
                     '/opt/idp/conf',
                     '/opt/idp/metadata']

# LDAP Stuff
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

def backupFiles():
    for folder in folders_to_backup:
        shutil.copytree(folder, bu_folder + folder)

def clean(s):
    return s.replace('@', '').replace('!', '').replace('.', '')

def getOrgInum():
    args = [ldapsearch] + ldap_creds + ['-s', 'one', '-b', 'o=gluu', 'o=*', 'dn']
    output = getOutput(args)
    return output.split(",")[0].split("o=")[-1]

def getLdif():
    orgInum = getOrgInum()
    # Backup the data
    for basedn in base_dns:
        args = [ldapsearch] + ldap_creds + ['-b',
                                            '%s,o=%s,o=gluu' % (basedn, orgInum),
                                            'objectclass=*']
        output = getOutput(args)
        ou = basedn.split("=")[-1]
        f = open("%s/ldif/%s.ldif" % (bu_folder, ou), 'w')
        f.write(output)
        f.close()

    # Backup the config
    args = [ldapsearch] + ldap_creds + ['-b', 'ou=appliances,o=gluu', 'objectclass=*']
    output = getOutput(args)
    f = open("%s/ldif/appliance.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

    # Backup the org
    args = [ldapsearch] + ldap_creds + ['-s', 'base', '-b', 'o=%s,o=gluu' % orgInum, 'objectclass=*']
    output = getOutput(args)
    f = open("%s/ldif/organization.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

def getOutput(args):
        try:
            logIt("Running command : %s" % " ".join(args))
            p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=None)
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logIt("Error running command : %s" % " ".join(args), True)
            logIt(traceback.format_exc(), True)
            sys.exit(2)

def genProperties():
    props = {}
    props['ldapPass'] = getOutput([cat, password_file])
    props['hostname'] = getOutput([hostname])
    props['inumAppliance'] = getOutput([grep, "^inum", "%s/ldif/appliance.ldif" % bu_folder]).split("\n")[0].split(":")[-1].strip()
    props['inumApplianceFN'] = clean(props['inumAppliance'])
    props['inumOrg'] = getOrgInum()
    props['inumOrgFN'] = clean(props['inumOrg'])
    props['baseInum'] = props['inumOrg'][:21]
    props['encode_salt'] = getOutput([cat, "%s/opt/tomcat/conf/salt" % bu_folder]).split("=")[-1].strip()
    f = open(propertiesFn, 'a')
    for key in props.keys():
        f.write("%s=%s\n" % (key, props[key]))
    f.close()

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
        output = getOutput([mkdir, '-p', bu_folder])
        output = getOutput([mkdir, '-p', "%s/ldif" % bu_folder])
    except:
        logIt("Error making folders", True)
        logIt(traceback.format_exc(), True)
        sys.exit(3)

makeFolders()
backupFiles()
getLdif()
genProperties()

