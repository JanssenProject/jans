import os
import sys
import json
import subprocess
import uuid
import glob
import urllib
import ssl
import re
import pymysql
import psycopg2
import inspect
import ldap3
import tempfile

from setup_app import paths
from setup_app.messages import msg
from setup_app.utils import base
from setup_app.utils.cbm import CBM
from setup_app.static import InstallTypes, colors, BackendStrings

from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.db_utils import dbUtils
from setup_app.pylib.jproperties import Properties

if base.current_app.profile == 'jans':
    from setup_app.utils.spanner import Spanner


class PropertiesUtils(SetupUtils):

    def getDefaultOption(self, val):
        return 'Yes' if val else 'No'

    def check_input_type(self, ival, itype=None):
        
        if not itype:
            return ival

        if itype == int and not ival.isnumeric():
            raise ValueError("Please enter numeric value")

        if itype == int:
            return int(ival)

    def getPrompt(self, prompt, defaultValue=None, itype=None, indent=0):
        try:
            if defaultValue:
                while True:
                    user_input = input("%s [%s] : " % (prompt, defaultValue)).strip()
                    if user_input == '':
                        return defaultValue
                    else:
                        try:
                            retval = self.check_input_type(user_input, itype)
                            return retval 
                        except Exception as e:
                            print(indent*' ', e)

            else:
                while True:
                    user_input = input("%s : " % prompt).strip()
                    if user_input != '':
                        try:
                            retval = self.check_input_type(user_input, itype)
                            return retval 
                        except Exception as e:
                            print(indent*' ', e)

        except KeyboardInterrupt:
            sys.exit()
        except Exception:
            return None

    def check_properties(self):
        self.logIt('Checking properties')
        while not Config.hostname:
            testhost = input('Hostname of this server: ').strip()
            if len(testhost.split('.')) >= 3:
                Config.hostname = testhost
            else:
                print('The hostname has to be at least three domain components. Try again\n')
        while not Config.ip:
            Config.ip = self.get_ip()
        while not Config.orgName:
            Config.orgName = input('Organization Name: ').strip()
        while not Config.countryCode:
            test_code = input('2 Character Country Code: ').strip()
            if len(test_code) == 2:
                Config.countryCode = test_code
            else:
                print('Country code should only be two characters. Try again\n')
        while not Config.city:
            Config.city = input('City: ').strip()
        while not Config.state:
            Config.state = input('State or Province: ').strip()
        if not Config.admin_email:
            tld = None
            try:
                tld = ".".join(self.hostname.split(".")[-2:])
            except Exception:
                tld = Config.hostname
            Config.admin_email = "support@%s" % tld


        if not Config.admin_password and Config.ldapPass:
            Config.admin_password = Config.ldapPass

        if not Config.admin_password:
            Config.admin_password = self.getPW()

        if Config.profile == 'jans':

            if not (Config.cb_install or Config.rdbm_install):
                Config.opendj_install = InstallTypes.LOCAL

            if not Config.ldapPass:
                Config.ldapPass = Config.admin_password

            if Config.cb_install and not Config.get('cb_password'):
                Config.cb_password = Config.admin_password

            if not Config.opendj_install:
                if Config.cb_install:
                    Config.mapping_locations = { group: 'couchbase' for group in Config.couchbaseBucketDict }

                if Config.rdbm_install:
                    Config.mapping_locations = { group: 'rdbm' for group in Config.couchbaseBucketDict }

            if Config.opendj_install == InstallTypes.LOCAL and not Config.installed_instance:
                used_ports = self.opendj_used_ports()
                if used_ports:
                    print(msg.used_ports.format(','.join(used_ports)))
                    sys.exit(1)


            self.set_persistence_type()

            if not Config.opendj_p12_pass:
                Config.opendj_p12_pass = self.getPW()

            self.check_oxd_server_https()

        if not Config.encode_salt:
            Config.encode_salt = self.getPW() + self.getPW()

        if not Config.jans_max_mem:
            Config.jans_max_mem = int(base.current_mem_size * .83 * 1000) # 83% of physical memory


    def check_oxd_server_https(self):

        if Config.get('oxd_server_https'):
            Config.templateRenderingDict['oxd_hostname'], Config.templateRenderingDict['oxd_port'] = self.parse_url(Config.oxd_server_https)
            if not Config.templateRenderingDict['oxd_port']: 
                Config.templateRenderingDict['oxd_port'] = 8443
        else:
            Config.templateRenderingDict['oxd_hostname'] = Config.hostname
            Config.oxd_server_https = 'https://{}:8443'.format(Config.hostname)

    def decrypt_properties(self, fn, passwd):
        out_file = fn[:-4] + '.' + uuid.uuid4().hex[:8] + '-DEC~'

        for digest in ('sha256', 'md5'):
            cmd = [paths.cmd_openssl, 'enc', '-md', digest, '-d', '-aes-256-cbc', '-in',  fn, '-out', out_file, '-k', passwd]
            self.logIt('Running: ' + ' '.join(cmd))
            p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            output, err = p.communicate()
            if not err.decode().strip():
                break
        else:
            print("Can't decrypt {} with password {}\n Exiting ...".format(fn, passwd))
            self.run(['rm', '-f', out_file])
            sys.exit(False)

        return out_file

    def load_properties(self, prop_file, no_update=[]):
        self.logIt('Loading Properties %s' % prop_file)

        no_update += ['noPrompt', 'jre_version', 'node_version', 'jetty_version', 'jython_version', 'jreDestinationPath']

        map_db = []

        if prop_file.endswith('.enc'):
            if not Config.properties_password:
                print("setup.properties password was not supplied. Please run with argument -properties-password")
                sys.exit(False)

            prop_file = self.decrypt_properties(prop_file, Config.properties_password)

        try:
            p = base.read_properties_file(prop_file)
        except Exception:
            self.logIt("Error loading properties", True)

        if p.get('ldap_type') == 'openldap':
            self.logIt("ldap_type in setup.properties was changed from openldap to opendj")
            p['ldap_type'] = 'opendj'

        if p.get('cb_install') == '0':
           p['cb_install'] = InstallTypes.NONE

        if p.get('opendj_install') == '0':
            p['opendj_install'] = InstallTypes.NONE

        if p.get('enable-script'):
            base.argsp.enable_script = p['enable-script'].split()

        properties_list = list(p.keys())

        for prop in properties_list:
            if prop in no_update:
                continue
            try:
                setattr(Config, prop, p[prop])
                if prop == 'mapping_locations':
                    mapping_locations = json.loads(p[prop])
                    setattr(Config, prop, mapping_locations)
                    for l in mapping_locations:
                        if mapping_locations[l] not in map_db:
                            map_db.append(mapping_locations[l])

                if p[prop] == 'True':
                    setattr(Config, prop, True)
                elif p[prop] == 'False':
                    setattr(Config, prop, False)
            except Exception:
                self.logIt("Error loading property %s" % prop)

        if prop_file.endswith('-DEC~'):
            self.run(['rm', '-f', prop_file])

        if 'admin_password' not in properties_list and 'ldapPass' in p:
            Config.admin_password = p['ldapPass']
            
        if p.get('ldap_hostname') != 'localhost':
            if p.get('remoteLdap','').lower() == 'true':
                Config.opendj_install = InstallTypes.REMOTE
            elif p.get('installLdap','').lower() == 'true':
                Config.opendj_install = InstallTypes.LOCAL
            elif p.get('opendj_install'):
                Config.opendj_install = p['opendj_install']   
            else:
                Config.opendj_install = InstallTypes.NONE

        if map_db and 'ldap' not in map_db:
            Config.opendj_install = InstallTypes.NONE

        if 'couchbase' in map_db:
            if 'remoteCouchbase' in properties_list and p.get('remoteCouchbase','').lower() == 'true':
                Config.cb_install = InstallTypes.REMOTE
            elif p.get('cb_install'):
                Config.cb_install = p['cb_install']
            elif 'persistence_type' in properties_list and p.get('persistence_type') in ('couchbase', 'hybrid'):
                Config.cb_install = InstallTypes.LOCAL
            else:
                Config.cb_install = InstallTypes.NONE

        if Config.cb_install == InstallTypes.LOCAL:
            available_backends = self.getBackendTypes()
            if 'couchbase' not in available_backends:
                print("Couchbase package is not available exiting.")
                sys.exit(1)


        if ('cb_password' not in properties_list) and Config.cb_install:
            Config.cb_password = p.get('ldapPass')

        if Config.cb_install == InstallTypes.REMOTE:
            cbm_ = CBM(Config.couchbase_hostname, Config.couchebaseClusterAdmin, Config.cb_password)
            if not cbm_.test_connection().ok:
                print("Can't connect to remote Couchbase Server with credentials found in setup.properties.")
                sys.exit(1)

        if Config.opendj_install == InstallTypes.REMOTE:
            conn_check = self.check_remote_ldap(Config.ldap_hostname, Config.ldap_binddn, Config.ldapPass)
            if not conn_check['result']:
                print("Can't connect to remote LDAP Server with credentials found in setup.properties.")
                sys.exit(1)

        if not (Config.cb_install or Config.rdbm_install):
            p['opendj_install'] = InstallTypes.LOCAL

        return p

    def save_properties(self, prop_fn=None, obj=None):

        if not prop_fn:
            prop_fn = Config.savedProperties

        if not obj:
            obj = self

        self.logIt('Saving properties to %s' % prop_fn)

        def get_string(value):
            if isinstance(value, str):
                return str(value).strip()
            elif isinstance(value, bool) or isinstance(value, int) or isinstance(value, float):
                return str(value)
            else:
                return ''

        try:
            p = Properties()
            for obj_name, obj in inspect.getmembers(Config):
                obj_name = str(obj_name)
                if obj_name in ('couchbaseInstallOutput', 'post_messages', 'properties_password', 'non_setup_properties', 'addPostSetupService'):
                    continue

                if obj_name.startswith('cmd_'):
                    continue

                if not obj_name.startswith('__') and (not callable(obj)):

                    if obj_name == 'mapping_locations':
                        p[obj_name] = json.dumps(obj)
                    else:
                        value = get_string(obj)
                        if value != '':
                            p[obj_name] = value

            with open(prop_fn, 'wb') as f:
                p.store(f, encoding="utf-8")

            self.run([paths.cmd_chmod, '600', prop_fn])

            # uncomment later
            return

            self.run([paths.cmd_openssl, 'enc', '-aes-256-cbc', '-in', prop_fn, '-out', prop_fn+'.enc', '-k', Config.admin_password])

            Config.post_messages.append(
                "Encrypted properties file saved to {0}.enc with password {1}\nDecrypt the file with the following command if you want to re-use:\nopenssl enc -d -aes-256-cbc -in {2}.enc -out {3}".format(
                prop_fn,  Config.admin_password, os.path.basename(prop_fn), os.path.basename(Config.setup_properties_fn)))

            self.run(['rm', '-f', prop_fn])

        except Exception:
            self.logIt("Error saving properties", True)

    def getBackendTypes(self):

        backend_types = []

        if glob.glob(Config.distFolder+'/app/opendj-server-*4*.zip'):
            backend_types.append('opendj')

        if glob.glob(Config.distFolder+'/couchbase/couchbase-server-enterprise*.' + base.clone_type):
            backend_types.append('couchbase')

        return backend_types



    def test_cb_servers(self, couchbase_hostname):
        cb_hosts = base.re_split_host.findall(couchbase_hostname)

        retval = {'result': True, 'reason': ''}

        for i, cb_host in enumerate(cb_hosts):

                cbm_ = CBM(cb_host, Config.couchebaseClusterAdmin, Config.cb_password)
                if not Config.thread_queue:
                    print("    Checking Couchbase connection for " + cb_host)

                cbm_result = cbm_.test_connection()
                if not cbm_result.ok:
                    if not Config.thread_queue:
                        print("    Can't establish connection to Couchbase server with given parameters.")
                        print("**", cbm_result.reason)
                    retval['result'] = False
                    retval['reason'] = cb_host + ': ' + cbm_result.reason
                    return retval
                try:
                    result = cbm_.get_services()
                    if result.ok:
                        data = result.json()
                        for node in data.get('nodesExt', []):
                            if node.get('thisNode'):
                                if 'n1qlSSL' in node.get('services', []):
                                    Config.cb_query_node = cb_host
                                    retval['result'] = True
                                    if not Config.thread_queue:
                                        print("{}    Successfully connected to Couchbase server{}".format(colors.OKGREEN, colors.ENDC))
                                    return retval
                except Exception:
                    pass


        if not Config.thread_queue:
            print("Can't find any query node")

        retval['result'] = False
        retval['reason'] = "Can't find any query node"

        return retval

    def prompt_remote_couchbase(self):

        while True:
            Config.couchbase_hostname = self.getPrompt("    Couchbase hosts", Config.get('couchbase_hostname'))
            Config.couchebaseClusterAdmin = self.getPrompt("    Couchbase User", Config.get('couchebaseClusterAdmin'))
            Config.cb_password =self.getPrompt("    Couchbase Password", Config.get('cb_password'))

            result = self.test_cb_servers(Config.get('couchbase_hostname'))

            if result['result']:
                break

    def check_remote_ldap(self, ldap_host, ldap_binddn, ldap_password):

        result = {'result': True, 'reason': ''}

        ldap_server = ldap3.Server(ldap_host, port=int(Config.ldaps_port), use_ssl=True)
        conn = ldap3.Connection(
            ldap_server,
            user=ldap_binddn,
            password=ldap_password,
            )

        try:
            conn.bind()
        except Exception as e:
            result['result'] = False
            result['reason'] = str(e)

        if not conn.bound:
            result['result'] = False
            result['reason'] = str(conn.last_error)

        return result

    def check_oxd_server(self, oxd_url, error_out=True, log_error=True):

        oxd_url = os.path.join(oxd_url, 'health-check')

        ctx = ssl.create_default_context()
        ctx.check_hostname = True
        ctx.verify_mode = ssl.CERT_NONE

        try:
            result = urllib.request.urlopen(
                        oxd_url,
                        timeout=2,
                        context=ctx
                    )
            if result.code == 200:
                oxd_status = json.loads(result.read().decode())
                if oxd_status['status'] == 'running':
                    return True
        except Exception as e:
            if log_error:
                if Config.thread_queue:
                    return str(e)
                if error_out:
                    print(colors.DANGER)
                    print("Can't connect to oxd-server with url {}".format(oxd_url))
                    print("Reason: ", e)
                    print(colors.ENDC)

    def check_oxd_ssl_cert(self, oxd_hostname, oxd_port):

        oxd_cert = ssl.get_server_certificate((oxd_hostname, oxd_port))
        with tempfile.TemporaryDirectory() as tmpdirname:
            oxd_crt_fn = os.path.join(tmpdirname, 'oxd.crt')
            self.writeFile(oxd_crt_fn, oxd_cert)
            ssl_subjects = self.get_ssl_subject(oxd_crt_fn)

            if ssl_subjects.get('commonName') != oxd_hostname:
                return ssl_subjects


    def promptForBackendMappings(self):

        options = []
        options_text = []
        
        bucket_list = list(Config.couchbaseBucketDict.keys())

        for i, m in enumerate(bucket_list):
            options_text.append('({0}) {1}'.format(i+1,m))
            options.append(str(i+1))

        options_text = 'Use opendj to store {}'.format(' '.join(options_text))

        re_pattern = '^[1-{0}]+$'.format(len(Config.couchbaseBucketDict))

        while True:
            prompt = self.getPrompt(options_text)
            if re.match(re_pattern, prompt):
                break
            else:
                print("Please select one of {0}.".format(", ".join(options)))

        couchbase_mappings = bucket_list[:]

        for i in prompt:
            m = bucket_list[int(i)-1]
            couchbase_mappings.remove(m)

        for m in couchbase_mappings:
            Config.mapping_locations[m] = 'couchbase'

    def set_persistence_type(self):
        if Config.opendj_install and (not Config.cb_install) and (not Config.rdbm_install):
            Config.persistence_type = 'ldap'
        elif (not Config.opendj_install) and (not Config.rdbm_install) and Config.cb_install:
            Config.persistence_type = 'couchbase'
        elif Config.rdbm_type == 'spanner':
            Config.persistence_type = 'spanner'
        elif (not Config.opendj_install) and Config.rdbm_install and (not Config.cb_install):
            Config.persistence_type = 'sql'
        elif Config.opendj_install and Config.cb_install:
            Config.persistence_type = 'hybrid'


    def promptForHTTPD(self):
        if Config.installed_instance and Config.installHttpd:
            return

        prompt_for_httpd = self.getPrompt("Install Apache HTTPD Server", 
                                        self.getDefaultOption(Config.installHTTPD)
                                        )[0].lower()

        Config.installHttpd = prompt_for_httpd == 'y'

        if Config.installed_instance and Config.installHttpd:
            Config.addPostSetupService.append('installHttpd')


    def promptForScimServer(self):
        if Config.installed_instance and Config.install_scim_server:
            return

        prompt_for_scim_server = self.getPrompt("Install Scim Server?",
                                            self.getDefaultOption(Config.install_scim_server)
                                            )[0].lower()

        Config.install_scim_server =  prompt_for_scim_server == 'y'

        if Config.installed_instance and Config.install_scim_server:
            Config.addPostSetupService.append('install_scim_server')

    def promptForFido2Server(self):
        if Config.installed_instance and Config.installFido2:
            return

        prompt_for_fido2_server = self.getPrompt("Install Fido2 Server?",
                                            self.getDefaultOption(Config.installFido2)
                                            )[0].lower()
        Config.installFido2 = prompt_for_fido2_server == 'y'

        if Config.installed_instance and Config.installFido2:
            Config.addPostSetupService.append('installFido2')


    def promptForOxd(self):

        if Config.installed_instance and Config.installOxd:
            return

        prompt_for_oxd = self.getPrompt("Install Oxd?", 
                                            self.getDefaultOption(Config.installOxd)
                                            )[0].lower()
        Config.installOxd = prompt_for_oxd == 'y'

        if Config.installOxd:
            use_jans_storage = self.getPrompt("  Use Janssen Storage for Oxd?",
                                                self.getDefaultOption(Config.get('oxd_use_jans_storage'))
                                                )[0].lower()
            Config.oxd_use_jans_storage = use_jans_storage == 'y'


        if Config.installed_instance and Config.installOxd:
            Config.addPostSetupService.append('installOxd')


    def promptForEleven(self):
        if Config.installed_instance and Config.installEleven:
            return

        promp_for_eleven = self.getPrompt("Install Eleven Server?",
                                            self.getDefaultOption(Config.installEleven)
                                            )[0].lower()

        Config.installEleven = promp_for_eleven == 'y'

        if Config.installed_instance and Config.installEleven:
            Config.addPostSetupService.append('installEleven')


    def promptForConfigApi(self):
        if Config.installed_instance and Config.install_config_api:
            return

        prompt_for_config_api = self.getPrompt("Install Jans Config API?", 
                            self.getDefaultOption(Config.install_config_api)
                            )[0].lower()

        Config.install_config_api = prompt_for_config_api == 'y'

        if Config.installed_instance and Config.install_config_api:
            Config.addPostSetupService.append('install_config_api')


    def prompt_for_rdbm(self):
        while True:
            Config.rdbm_type = self.getPrompt("RDBM Type", Config.rdbm_type)
            if Config.rdbm_type in ('mysql', 'pgsql'):
                break
            print("Please enter mysql or pgsql")

        remote_local = input("Use remote RDBM [Y|n] : ")

        if remote_local.lower().startswith('n'):
            Config.rdbm_install_type = InstallTypes.LOCAL
            if not Config.rdbm_password:
                Config.rdbm_password = self.getPW()
        else:
            Config.rdbm_install_type = InstallTypes.REMOTE

        if Config.rdbm_install_type == InstallTypes.REMOTE:
            while True:
                Config.rdbm_host = self.getPrompt("  {} host".format(Config.rdbm_type.upper()), Config.rdbm_host)
                Config.rdbm_port = self.getPrompt("  {} port".format(Config.rdbm_type.upper()), Config.rdbm_port)
                Config.rdbm_db = self.getPrompt("  Jnas Database", Config.rdbm_db)
                Config.rdbm_user = self.getPrompt("  Jans Database Username", Config.rdbm_user)
                Config.rdbm_password = self.getPrompt("  Jans Database Password", Config.rdbm_password)

                result = dbUtils.sqlconnection()

                if result[0]:
                    print("  {}Successfully connected to {} server{}".format(colors.OKGREEN, Config.rdbm_type.upper(), colors.ENDC))
                    break
                else:
                    print("  {}Can't connect to {} server with provided credidentals.{}".format(colors.FAIL, Config.rdbm_type.upper(), colors.ENDC))
                    print("  ERROR:", result[1])


    def prompt_for_backend(self):
        print('Chose Backend Type:')

        backend_types = [
                    BackendStrings.LOCAL_OPENDJ,
                    BackendStrings.LOCAL_MYSQL,
                    BackendStrings.REMOTE_MYSQL,
                    BackendStrings.LOCAL_PGSQL,
                    BackendStrings.REMOTE_PGSQL,
                    ]

        backend_types += [BackendStrings.REMOTE_COUCHBASE, BackendStrings.CLOUD_SPANNER]
        if 'couchbase' in self.getBackendTypes():
            backend_types.insert(2, BackendStrings.LOCAL_COUCHBASE)

        nlist = []
        for i, btype in enumerate(backend_types):
            nn = i+1
            print(" ", nn, btype)
            nlist.append(str(nn))

        while True:
            n = input('Selection [1]: ')
            choice = None
            if not n:
                choice = 1
            elif not n in nlist:
                print("Please enter one of {}".format(', '.join(nlist)))
            else:
                choice = n

            if choice == 1:
                used_ports = self.opendj_used_ports()
                if used_ports:
                    print(colors.DANGER, msg.used_ports.format(','.join(used_ports)), colors.ENDC)
                    choice = None

            if choice:
                break

        backend_type_str = backend_types[int(choice)-1]

        if backend_type_str == BackendStrings.LOCAL_OPENDJ:
            Config.opendj_install = InstallTypes.LOCAL
            ldapPass = Config.ldapPass or Config.admin_password or self.getPW(special='.*=!%&+/-')

            while True:
                ldapPass = self.getPrompt("Enter Password for LDAP Admin ({})".format(Config.ldap_binddn), ldapPass)

                if self.checkPassword(ldapPass):
                    break
                else:
                    print("Password must be at least 6 characters and include one uppercase letter, one lowercase letter, one digit, and one special character.")

            Config.ldapPass = ldapPass


        elif backend_type_str == BackendStrings.REMOTE_OPENDJ:
            Config.opendj_install = InstallTypes.REMOTE
            while True:
                ldapHost = self.getPrompt("    LDAP hostname")
                ldapPass = self.getPrompt("    Password for '{0}'".format(Config.ldap_binddn))
                conn_check = self.check_remote_ldap(ldapHost, Config.ldap_binddn, ldapPass)
                if conn_check['result']:
                    break
                else:
                    print("    {}Error connecting to LDAP server: {} {}".format(colors.FAIL, conn_check['reason'], colors.ENDC))

            Config.ldapPass = ldapPass
            Config.ldap_hostname = ldapHost

        elif backend_type_str == BackendStrings.LOCAL_COUCHBASE:
            Config.opendj_install = InstallTypes.NONE
            Config.cb_install = InstallTypes.LOCAL
            Config.isCouchbaseUserAdmin = True

            while True:
                cbPass = self.getPrompt("Enter Password for Couchbase {}admin{} user".format(colors.BOLD, colors.ENDC), Config.admin_password)

                if self.checkPassword(cbPass):
                    break
                else:
                    print("Password must be at least 6 characters and include one uppercase letter, one lowercase letter, one digit, and one special character.")

            Config.cb_password = cbPass
            Config.mapping_locations = { group: 'couchbase' for group in Config.couchbaseBucketDict }

        elif backend_type_str == BackendStrings.REMOTE_COUCHBASE:
            Config.opendj_install = InstallTypes.NONE
            Config.cb_install = InstallTypes.REMOTE

            while True:
                Config.couchbase_hostname = self.getPrompt("  Couchbase hosts", Config.get('couchbase_hostname'))
                Config.couchebaseClusterAdmin = self.getPrompt("  Couchbase User", Config.get('couchebaseClusterAdmin'))
                Config.cb_password =self.getPrompt("  Couchbase Password", Config.get('cb_password'))
                result = self.test_cb_servers(Config.get('couchbase_hostname'))
                if result['result']:
                    break

            Config.mapping_locations = { group: 'couchbase' for group in Config.couchbaseBucketDict }

        elif backend_type_str in (BackendStrings.LOCAL_MYSQL, BackendStrings.LOCAL_PGSQL):
            Config.opendj_install = InstallTypes.NONE
            Config.rdbm_install = True
            Config.rdbm_install_type = InstallTypes.LOCAL
            if backend_type_str == BackendStrings.LOCAL_MYSQL:
                Config.rdbm_port = 3306
                Config.rdbm_type = 'mysql'
            else:
                Config.rdbm_port = 5432
                Config.rdbm_type = 'pgsql'

        elif backend_type_str in (BackendStrings.REMOTE_MYSQL, BackendStrings.REMOTE_PGSQL):
            Config.opendj_install = InstallTypes.NONE
            Config.rdbm_install = True
            Config.rdbm_install_type = InstallTypes.REMOTE
            if backend_type_str == BackendStrings.REMOTE_MYSQL:
                Config.rdbm_port = 3306
                Config.rdbm_type = 'mysql'
            else:
                Config.rdbm_port = 5432
                Config.rdbm_type = 'pgsql'

            while True:
                Config.rdbm_host = self.getPrompt("  {} host".format(Config.rdbm_type.upper()), Config.get('rdbm_host'))
                Config.rdbm_port = self.getPrompt("  {} port".format(Config.rdbm_type.upper()), Config.rdbm_port, itype=int, indent=1)
                Config.rdbm_user = self.getPrompt("  {} user".format(Config.rdbm_type.upper()), Config.get('rdbm_user'))
                Config.rdbm_password = self.getPrompt("  {} password".format(Config.rdbm_type.upper()))
                Config.rdbm_db = self.getPrompt("  {} database".format(Config.rdbm_type.upper()), Config.get('rdbm_db'))

                try:
                    if Config.rdbm_type == 'mysql':
                        pymysql.connect(host=Config.rdbm_host, user=Config.rdbm_user, password=Config.rdbm_password, database=Config.rdbm_db, port=Config.rdbm_port)
                    else:
                        psycopg2.connect(dbname=Config.rdbm_db, user=Config.rdbm_user, password=Config.rdbm_password, host=Config.rdbm_host, port=Config.rdbm_port)
                    print("  {}{} connection was successful{}".format(colors.OKGREEN, Config.rdbm_type.upper(), colors.ENDC))
                    break
                except Exception as e:
                    print("  {}Can't connect to {}: {}{}".format(colors.DANGER,Config.rdbm_type.upper(), e, colors.ENDC))

        elif backend_type_str == BackendStrings.CLOUD_SPANNER:
            Config.opendj_install = InstallTypes.NONE
            Config.rdbm_type = 'spanner'
            Config.rdbm_install = True
            Config.rdbm_install_type = InstallTypes.REMOTE

            emulator = self.getPrompt("  Is it emulator?", "N|y")[0].lower()
            if emulator == 'y':
                Config.spanner_emulator_host = self.getPrompt("  Emulator host", Config.get('spanner_emulator_host'))

            Config.spanner_project = self.getPrompt("  Spanner project", Config.get('spanner_project'))
            Config.spanner_instance = self.getPrompt("  Spanner instance", Config.get('spanner_instance'))
            Config.spanner_database = self.getPrompt("  Spanner database", Config.get('spanner_database'))
            if not Config.get('spanner_emulator_host'):
                while True:
                    cred_fn = self.getPrompt("  Google application creditentals file", Config.get('google_application_credentials'))
                    if os.path.exists(cred_fn):
                        try:
                            with open(cred_fn) as f:
                                json.load(f)
                                break
                        except:
                            pass
                    print("  Please enter valid json path")
                Config.google_application_credentials = cred_fn

            print("  Checking spanner connection")
            try:
                spanner = Spanner()
                spanner.get_session()
                print("  {}Spanner connection was successfull{}".format(colors.OKGREEN, colors.ENDC))
            except Exception as e:
                print("{}ERROR getting session from spanner: {}{}".format(colors.DANGER, e, colors.ENDC))
                sys.exit()

    def openbanking_properties(self):

        self.prompt_for_rdbm()

        Config.static_kid = self.getPrompt("Enter Openbanking static kid: ", Config.static_kid)

        use_external_key_prompt = input('Use external key? [Y|n] : ')
        Config.use_external_key = not use_external_key_prompt.lower().startswith('n')

        if Config.use_external_key:
            while True:
                ob_key_fn = self.getPrompt('  Openbanking Key File', Config.ob_key_fn)
                if os.path.isfile(ob_key_fn):
                    Config.ob_key_fn = ob_key_fn
                    break
                print("  {}File {} does not exist{}".format(colors.WARNING, ob_key_fn, colors.ENDC))

            while True:
                ob_cert_fn = self.getPrompt('  Openbanking Certificate File', Config.ob_cert_fn)
                if not os.path.isfile(ob_cert_fn):
                    self.download_ob_cert(ob_cert_fn)

                if os.path.isfile(ob_cert_fn):
                    Config.ob_cert_fn = ob_cert_fn
                    break
                print("  {}File {} does not exist{}".format(colors.WARNING, ob_key_fn, colors.ENDC))

            Config.ob_alias = self.getPrompt('  Openbanking Key Alias', Config.ob_alias)


    def promptForProperties(self):

        if Config.noPrompt or '-x' in sys.argv:
            return


        if Config.installed_instance:
            print("This is previously installed instance. Available components will be prompted for installation.")

        else:
            promptForMITLicense = self.getPrompt("Do you acknowledge that use of the Janssen Server is under the Apache-2.0 license?", "N|y")[0].lower()
            if promptForMITLicense != 'y':
                sys.exit(0)

            # IP address needed only for Apache2 and hosts file update
            if Config.installHttpd:
                Config.ip = self.get_ip()

            if base.argsp.host_name:
                detectedHostname = base.argsp.host_name
            else:
                detectedHostname = self.detect_hostname()

                if detectedHostname == 'localhost':
                    detectedHostname = None

            while True:
                if detectedHostname:
                    Config.hostname = self.getPrompt("Enter hostname", detectedHostname)
                else:
                    Config.hostname = self.getPrompt("Enter hostname")

                if Config.hostname != 'localhost':
                    break
                else:
                    print("Hostname can't be \033[;1mlocalhost\033[0;0m")

            Config.oxd_server_https = 'https://{}:8443'.format(Config.hostname)

            # Get city and state|province code
            Config.city = self.getPrompt("Enter your city or locality", Config.city)
            Config.state = self.getPrompt("Enter your state or province two letter code", Config.state)

            # Get the Country Code
            long_enough = False
            while not long_enough:
                countryCode = self.getPrompt("Enter two letter Country Code", Config.countryCode)
                if len(countryCode) != 2:
                    print("Country code must be two characters")
                else:
                    Config.countryCode = countryCode
                    long_enough = True

            Config.orgName = self.getPrompt("Enter Organization Name", Config.orgName)

            while True:
                Config.admin_email = self.getPrompt('Enter email address for support at your organization', Config.admin_email)
                if self.check_email(Config.admin_email):
                    break
                else:
                    print("Please enter valid email address")
            
            Config.jans_max_mem = self.getPrompt("Enter maximum RAM for applications in MB", str(Config.jans_max_mem))


        admin_password =  Config.ldapPass or Config.cb_password or Config.rdbm_password or self.getPW(special='.*=!%&+/-')

        while True:
            adminPass = self.getPrompt("Enter Password for Admin User", admin_password)
            if len(adminPass) > 3:
                break
            else:
                print("Admin password should be at least four characters in length.")

        Config.admin_password = adminPass

        if Config.profile == 'openbanking':
            self.openbanking_properties()
        else:
            self.prompt_for_backend()
            self.promptForConfigApi()
            self.promptForScimServer()
            self.promptForFido2Server()
            #self.promptForEleven()
            #if (not Config.installOxd) and Config.oxd_package:
            #    self.promptForOxd()



propertiesUtils = PropertiesUtils()
