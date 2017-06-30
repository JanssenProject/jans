#!/usr/bin/env python
"""export24.py - A script to export all the data from Gluu Server 2.4.x

Usage: python export24.py

Running this creates a folder named `backup_24` which contains all the data
needed for migration of Gluu Server to a higher version. This script backs up
the following data:
    1. LDAP data
    2. Configurations of Tomcat and OpenDJ
    3. CA certificates in /etc/certs
    4. Webapp Customization files

This backup folder should be used as the input for the `import___.py` script
of appropriate version to migrate to that version.

Read complete migration procedure at:
    https://www.gluu.org/docs/deployment/upgrading/
"""

import traceback
import sys
import os
import shutil
import hashlib
import getpass
import tempfile
import logging

# Unix commands
mkdir = '/bin/mkdir'
cat = '/bin/cat'
hostname = '/bin/hostname'
grep = '/bin/grep'
ldapsearch = "/opt/opendj/bin/ldapsearch"
unzip = "/usr/bin/unzip"
find = "/usr/bin/find"
mkdir = "/bin/mkdir"

# File system stuff
oxauth_war = "/opt/tomcat/webapps/oxauth.war"
oxtrust_war = "/opt/tomcat/webapps/identity.war"
oxauth_original_dir = "/tmp/oxauth-original"
oxtrust_original_dir = "/tmp/oxtrust-original"
oxauth_modified_dir = "/opt/tomcat/webapps/oxauth"
oxtrust_modified_dir = "/opt/tomcat/webapps/identity"
log = "./export_24.log"
logError = "./export_24.error"
bu_folder = "./backup_24"
propertiesFn = "%s/setup.properties" % bu_folder
folders_to_backup = ['/opt/tomcat/conf',
                     '/opt/tomcat/endorsed',
                     '/opt/opendj/config',
                     '/etc/certs',
                     '/opt/idp/conf',
                     '/opt/idp/metadata',
                     '/var/gluu/webapps']

# LDAP Stuff
password_file = tempfile.mkstemp()[1]
ldap_creds = ['-h', 'localhost', '-p', '1636', '-Z', '-X', '-D',
              '"cn=directory manager"', '-j', password_file]
base_dns = ['ou=people',
            'ou=groups',
            'ou=attributes',
            'ou=scopes',
            'ou=clients',
            'ou=scripts',
            'ou=uma',
            'ou=hosts',
            'ou=u2f']

# configure logging
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)-8s %(message)s',
                    filename='export_24.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)


def backupCustomizations():
    logging.info('Creating backup of UI customizations')
    dirs = [oxauth_original_dir, oxtrust_original_dir]
    for dir in dirs:
        if not os.path.exists(dir):
            os.mkdir(dir)
    output = runCommand([unzip, '-q', oxauth_war, '-d', oxauth_original_dir])
    output = runCommand([unzip, '-q', oxtrust_war, '-d', oxtrust_original_dir])
    logging.debug(output)
    dirs = [(oxauth_modified_dir, oxauth_original_dir),
            (oxtrust_modified_dir, oxtrust_original_dir)]
    for dir_tup in dirs:
        modified_dir = dir_tup[0]
        original_dir = dir_tup[1]
        files = runCommand([find, modified_dir], True)
        for modified_file in files:
            modified_file = modified_file.strip()
            original_file = modified_file.replace(modified_dir, original_dir)
            if not os.path.isdir(modified_file):
                if not os.path.exists(original_file):
                    logging.debug("Found new file: %s", modified_file)
                    copyFile(modified_file, bu_folder)
                else:
                    modified_hash = hash_file(modified_file)
                    original_hash = hash_file(original_file)
                    if not modified_hash == original_hash:
                        logging.debug("Found changed file: %s", modified_file)
                        copyFile(modified_file, bu_folder)
    shutil.rmtree(oxauth_original_dir)
    shutil.rmtree(oxtrust_original_dir)


def backupFiles():
    logging.info('Creating backup of files')
    for folder in folders_to_backup:
        try:
            shutil.copytree(folder, bu_folder + folder)
        except:
            logging.error("Failed to backup %s", folder)


def clean(s):
    return s.replace('@', '').replace('!', '').replace('.', '')


def copyFile(fn, dir):
    parent_Dir = os.path.split(fn)[0]
    bu_dir = "%s/%s" % (bu_folder, parent_Dir)
    if not os.path.exists(bu_dir):
        runCommand([mkdir, "-p", bu_dir])
    bu_fn = os.path.join(bu_dir, os.path.split(fn)[-1])
    shutil.copyfile(fn, bu_fn)


def getOrgInum():
    args = [ldapsearch] + ldap_creds + ['-s', 'one', '-b', 'o=gluu',
                                        'o=*', 'dn']
    output = runCommand(args)
    return output.split(",")[0].split("o=")[-1]


def getLdif():
    logging.info('Creating backup of LDAP data')
    orgInum = getOrgInum()
    # Backup the data
    for basedn in base_dns:
        args = [ldapsearch] + ldap_creds + [
            '-b', '%s,o=%s,o=gluu' % (basedn, orgInum), 'objectclass=*']
        output = runCommand(args)
        ou = basedn.split("=")[-1]
        f = open("%s/ldif/%s.ldif" % (bu_folder, ou), 'w')
        f.write(output)
        f.close()

    # Backup the appliance config
    args = [ldapsearch] + ldap_creds + \
           ['-b',
            'ou=appliances,o=gluu',
            '-s',
            'one',
            'objectclass=*']
    output = runCommand(args)
    f = open("%s/ldif/appliance.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

    # Backup the oxtrust config
    args = [ldapsearch] + ldap_creds + \
           ['-b',
            'ou=appliances,o=gluu',
            'objectclass=oxTrustConfiguration']
    output = runCommand(args)
    f = open("%s/ldif/oxtrust_config.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

    # Backup the oxauth config
    args = [ldapsearch] + ldap_creds + \
           ['-b',
            'ou=appliances,o=gluu',
            'objectclass=oxAuthConfiguration']
    output = runCommand(args)
    f = open("%s/ldif/oxauth_config.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

    # Backup the trust relationships
    args = [ldapsearch] + ldap_creds + ['-b', 'ou=appliances,o=gluu',
                                        'objectclass=gluuSAMLconfig']
    output = runCommand(args)
    f = open("%s/ldif/trust_relationships.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

    # Backup the org
    args = [ldapsearch] + ldap_creds + ['-s', 'base', '-b',
                                        'o=%s,o=gluu' % orgInum,
                                        'objectclass=*']
    output = runCommand(args)
    f = open("%s/ldif/organization.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()

    # Backup o=site
    args = [ldapsearch] + ldap_creds + ['-b', 'ou=people,o=site',
                                        '-s', 'one', 'objectclass=*']
    output = runCommand(args)
    f = open("%s/ldif/site.ldif" % bu_folder, 'w')
    f.write(output)
    f.close()


def runCommand(args, return_list=False):
        try:
            logging.debug("Running command : %s", " ".join(args))
            output = None
            if return_list:
                output = os.popen(" ".join(args)).readlines()
            else:
                output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logging.error("Error running command : %s", " ".join(args))
            logging.debug(traceback.format_exc())
            sys.exit(1)


def getProp(prop):
    with open('/install/community-edition-setup/setup.properties.last', 'r') \
            as sf:
        for line in sf:
            if "{0}=".format(prop) in line:
                return line.split('=')[-1].strip()


def genProperties():
    logging.info('Creating setup.properties backup file')
    props = {}
    props['ldapPass'] = runCommand([cat, password_file])
    props['hostname'] = runCommand([hostname])
    props['inumAppliance'] = runCommand(
        [grep, "^inum", "%s/ldif/appliance.ldif" % bu_folder]
    ).split("\n")[0].split(":")[-1].strip()
    props['inumApplianceFN'] = clean(props['inumAppliance'])
    props['inumOrg'] = getOrgInum()
    props['inumOrgFN'] = clean(props['inumOrg'])
    props['baseInum'] = props['inumOrg'][:21]
    props['encode_salt'] = runCommand(
        [cat, "%s/opt/tomcat/conf/salt" % bu_folder]).split("=")[-1].strip()

    props['oxauth_client_id'] = getProp('oxauth_client_id')
    props['scim_rs_client_id'] = getProp('scim_rs_client_id')
    props['scim_rp_client_id'] = getProp('scim_rp_client_id')
    props['version'] = getProp('githubBranchName').replace('version_', '')
    # As the certificates are copied over to the new installation, their pass
    # are required for accessing them and validating them
    props['httpdKeyPass'] = getProp('httpdKeyPass')
    props['shibJksPass'] = getProp('shibJksPass')
    props['asimbaJksPass'] = getProp('asimbaJksPass')

    # Preferences for installation of optional components
    installSaml = raw_input("\tIs Shibboleth SAML IDP installed? (y/N): [N]") or "N"
    props['installSaml'] = 'y' in installSaml.lower()
    if installSaml:
        shibv = raw_input("\tAre you migrating to Gluu Server v3.x? (y/N): [y]") or "y"
        if shibv:
            props['shibboleth_version'] = 'v3'

    props['installAsimba'] = os.path.isfile('/opt/tomcat/webapps/asimba.war')
    props['installCas'] = os.path.isfile('/opt/tomcat/webapps/cas.war')
    props['installOxAuthRP'] = os.path.isfile(
        '/opt/tomcat/webapps/oxauth-rp.war')

    f = open(propertiesFn, 'a')
    for key in props.keys():
        # NOTE: old version of setup.py will interpret any string as True
        #       Hence, store only the True values, the defaults are False
        if props[key]:
            f.write("%s=%s\n" % (key, props[key]))
    f.close()


def hash_file(filename):
    # From http://www.programiz.com/python-programming/examples/hash-file
    h = hashlib.sha1()
    with open(filename, 'rb') as file:
        chunk = 0
        while chunk != b'':
            chunk = file.read(1024)
            h.update(chunk)
    return h.hexdigest()


def makeFolders():
    folders = [bu_folder, "%s/ldif" % bu_folder]
    for folder in folders:
        try:
            if not os.path.exists(folder):
                runCommand([mkdir, '-p', folder])
        except:
            logging.error("Error making folder: %s", folder)
            logging.debug(traceback.format_exc())
            sys.exit(3)


def prepareLdapPW():
    ldap_pass = None
    # read LDAP pass from setup.properties
    with open('/install/community-edition-setup/setup.properties.last', 'r') \
            as sfile:
        for line in sfile:
            if 'ldapPass=' in line:
                ldap_pass = line.split('=')[-1]
    # write it to the tmp file
    with open(password_file, 'w') as pfile:
        pfile.write(ldap_pass)
    # perform sample search
    sample = getOrgInum()
    if not sample:
        # get the password from the user if it fails
        ldap_pass = getpass.getpass("Enter LDAP Passsword: ")
        with open(password_file, 'w') as pfile:
            pfile.write(ldap_pass)


def main():
    prepareLdapPW()
    makeFolders()
    backupFiles()
    getLdif()
    genProperties()
    backupCustomizations()

    # remove the tempfile with the ldap password
    os.remove(password_file)

if __name__ == "__main__":
    main()
