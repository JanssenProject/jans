#!/usr/bin/python

import os
import re
import time
import sys
import glob
import zipfile

cur_dir = os.path.dirname(os.path.realpath(__file__))

sys.path.append(cur_dir)

package_type = None
setup_properties_fn = '/install/community-edition-setup/setup.properties.last'

#Determine gluu version
war_zip = zipfile.ZipFile('/opt/gluu/jetty/oxauth/webapps/oxauth.war', 'r')
menifest = war_zip.read('META-INF/MANIFEST.MF')

for l in menifest.splitlines():
    ls = l.strip()
    n = ls.find(':')

    if ls[:n].strip() == 'Implementation-Version':
        gluu_version_str = ls[n+1:].strip()
        gluu_version_list = gluu_version_str.split('.')

        if not gluu_version_list[-1].isdigit():
            gluu_version_list.pop(-1)

        gluu_version = '.'.join(gluu_version_list)

ces_version = '4.1.0' if gluu_version == '4.0' else gluu_version

if not os.path.exists(setup_properties_fn):
    print "Upgrade script needs {0}.\nCan't continue without {0}.\nPlease put {0} and\nre-run upgrade script. Exiting for now...".format(setup_properties_fn)
    sys.exit()
    

if os.path.exists('/etc/yum.repos.d/'):
    package_type = 'rpm'
elif os.path.exists('/etc/apt/sources.list'):
    package_type = 'deb'
        

missing_packages = []

needs_restart = False
dev_env = True if os.environ.get('update_dev') else False

try:
    import ldap
except:
    missing_packages.append('python-ldap')

try:
    import requests
except:
    missing_packages.append('python-requests')

if missing_packages:
    needs_restart = True
    packages_str = ' '.join(missing_packages)
    result = raw_input("Missing package(s): {0}. Install now? (Y|n): ".format(packages_str))
    if result.strip() and result.strip().lower()[0] == 'n':
        sys.exit("Can't continue without installing these packages. Exiting ...")
            

    if package_type == 'rpm':
        cmd = 'yum install -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm'
        os.system(cmd)
        cmd = 'yum clean all'
        os.system(cmd)
        cmd = "yum install -y {0}".format(packages_str)
    else:
        os.system('apt-get update')
        cmd = "apt-get install -y {0}".format(packages_str)

    print "Installing package(s) with command: "+ cmd
    os.system(cmd)


if not os.path.exists(os.path.join(cur_dir, 'setup')):
    os.system('wget https://github.com/GluuFederation/community-edition-setup/archive/version_{}.zip -O /tmp/community-edition-setup-master.zip'.format(ces_version))
    os.system('unzip -qo /tmp/community-edition-setup-master.zip -d /tmp')
    os.system('mv /tmp/community-edition-setup-version_{} {}/setup'.format(ces_version, cur_dir))
    os.system('touch setup/__init__.py')


if needs_restart:
    python_ = sys.executable
    os.execl(python_, python_, * sys.argv)

def get_as_bool(val):

    if str(val).lower() in ('true', 'on', 'ok', 'yes'):
        return True
        
    return False

class LDAP2CB:
    def __init__(self):

        self.update_dir = cur_dir
        self.app_dir = os.path.join(self.update_dir,'app')
        self.war_dir = os.path.join(self.update_dir,'war')
        self.gluuBaseFolder = '/etc/gluu'
        self.certFolder = '/etc/certs'
        self.configFolder = '%s/conf' % self.gluuBaseFolder
        self.gluu_app_dir = '/opt/gluu/jetty'
        self.backup_time = time.strftime('%Y-%m-%d.%H:%M:%S')
        self.backup_folder = os.path.join(cur_dir, 'backup_{}'.format(self.backup_time))
        self.temp_dir = os.path.join(cur_dir, 'temp')
        self.services_dir = os.path.join(cur_dir, 'services')
        self.current_ldif_fn = os.path.join(cur_dir, 'gluu.ldif')

        self.ldap_type = 'opendj'
        self.bindDN = 'cn=directory manager'

    def get_first_backup(self, fn):
        file_list = glob.glob(fn+'.gluu-{0}-*~'.format(setupObject.currentGluuVersion))


        if not file_list:
            return fn

        file_list.sort(key=lambda fn_: [ c for c in re.split(r'(\d+)', fn_) ])

        print "Using backed up file", file_list[0]

        return file_list[0]

    def backup_(self, f, keep=False):
        if os.path.exists(f):
            if keep:
                setupObject.run(['cp','-r', '-f', f, f+'.back_'+self.backup_time])
            else:
                setupObject.run(['mv', f, f+'.back_'+self.backup_time])

    def dump_current_db(self):
        print "Dumping ldap to gluu.ldif"
        
        if os.path.exists(self.current_ldif_fn):
            print "Previously dumped gluu.ldif file was found."
            while True:
                use_old = setupObject.getPrompt("Use previously dumped gluu.ldif [yes/no]")
                if not use_old.lower() in ('yes', 'no'):
                    print "Please type \033[1myes\033[0m or \033[1mno\033[0m"
                else:
                    break
            if get_as_bool(use_old):
                return
            else:
                self.backup_(self.current_ldif_fn)
        
        setupObject.createLdapPw()
        
        setupObject.run(' '.join([
                        '/opt/opendj/bin/ldapsearch',
                        '-X', '-Z', '-D',
                        '"{}"'.format(self.bindDN),
                        '-j',
                        setupObject.ldapPassFn,
                        '-h',
                        setupObject.ldap_hostname,
                        '-p',
                        '1636',
                        '-b',
                        'o=gluu',
                        'ObjectClass=*',
                        '>',
                        self.current_ldif_fn]), shell=True)

        fs = os.stat(self.current_ldif_fn)

        if fs.st_size < 500000:
            sys.exit("Dumped ldif size is unexpectedly small. Please examine log files. Giving up ...")


        setupObject.deleteLdapPw()

    def fix_saml(self):
        prop_fn = os.path.join(setupObject.idp3Folder, 'conf', setupObject.idp3_configuration_ldap_properties)
        print prop_fn
        idp_ldap_prop = setupObject.readFile(prop_fn)
        idp_ldap_prop_l = idp_ldap_prop.splitlines()

        for i, l in enumerate(idp_ldap_prop_l[:]):
            ls = l.strip()
            if ls.startswith('idp.attribute.resolver.LDAP.searchFilter'):
                idp_ldap_prop_l[i] = 'idp.attribute.resolver.LDAP.searchFilter        = (&(|(lower(uid)=$requestContext.principalName)(mail=$requestContext.principalName))(objectClass=gluuPerson))'

        setupObject.writeFile(prop_fn, 
                        '\n'.join(idp_ldap_prop_l))
                        
        setupObject.saml_couchbase_settings()



if __name__ == '__main__':

    from setup.pylib.ldif import LDIFParser, LDIFWriter, ParseLDIF
    from setup.pylib.cbm import CBM
    from setup.setup import *
    from ldap.dn import explode_dn, str2dn, dn2str
    from setup.pylib.Properties import Properties

    setup_porperties = Properties()
    with open(setup_properties_fn) as f:
        setup_porperties.load(f)

    migratorObj = LDAP2CB()

    setup_install_dir = os.path.join(cur_dir,'setup')
    setupObject = Setup(setup_install_dir)
    setupObject.log = os.path.join(setup_install_dir, 'ldap2cb.log')
    setupObject.logError = os.path.join(setup_install_dir, 'ldap2cb_error.log')

    setupObject.load_properties(setup_properties_fn,
                                no_update = [
                                        'install_dir',
                                        'node_version',
                                        'jetty_version',
                                        'jetty_dist',
                                        'outputFolder',
                                        'templateFolder',
                                        'staticFolder',
                                        'openDjIndexJson',
                                        'openDjSchemaFolder',
                                        'openDjschemaFiles',
                                        'opendj_init_file',
                                        'opendj_service_centos7',
                                        'log',
                                        'logError',
                                        'passport_initd_script',
                                        'node_initd_script',
                                        'jre_version',
                                        'java_type',
                                        'jreDestinationPath',
                                        'couchbaseIndexJson',
                                        ]
                                )


    setupObject.check_properties()
    setupObject.backupFile(setup_properties_fn)

    setupObject.os_type, setupObject.os_version = setupObject.detect_os_type()
    setupObject.os_initdaemon = setupObject.detect_initd()

    migratorObj.dump_current_db()

    setupObject.remoteCouchbase=True
    setupObject.persistence_type='couchbase'

    setupObject.renderTemplate(setupObject.data_source_properties)

    print "Stopping WrenDS"
    setupObject.run_service_command('opendj', 'stop')

    print "Disabling WrenDS"
    setupObject.enable_service_at_start('opendj', action='disable')

    attribDataTypes.startup(setup_install_dir)
    setupObject.prompt_remote_couchbase()
    
    setupObject.mappingLocations = { group: 'couchbase' for group in setupObject.couchbaseBucketDict }
    
    setupObject.cbm = CBM(
                        setupObject.couchbase_hostname.split(',')[0].strip(),
                        setupObject.couchebaseClusterAdmin, 
                        setupObject.cb_password
                        )

    setupObject.mappingLocations = { group: 'couchbase' for group in setupObject.couchbaseBucketDict }

    print "Creating buckets and indexes"
    setupObject.create_couchbase_buckets()
    
    print "Importing ldif to Couchbase server"
    setupObject.import_ldif_couchebase([ os.path.join(cur_dir, 'gluu.ldif') ])
    
    print "Exporting Couchbase SSL"
    setupObject.couchbaseSSL()

    setupObject.encode_passwords()

    setupObject.renderTemplateInOut(
                    setupObject.gluu_properties_fn, 
                    os.path.join(setup_install_dir, 'templates'), 
                    setupObject.configFolder
                )

    setupObject.couchbaseProperties()

    if os.path.isdir(setupObject.idp3Folder):
        migratorObj.fix_saml()

    print
    if hasattr(setupObject, 'print_post_messages'):
        setupObject.print_post_messages()

    setupObject.backupFile(setupObject.ox_ldap_properties)
    setupObject.removeFile(setupObject.ox_ldap_properties)

    print "Please logout from container and restart Gluu Server"
