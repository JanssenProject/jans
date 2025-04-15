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
import tempfile

from setup_app import paths
from setup_app.messages import msg
from setup_app.utils import base
from setup_app.static import InstallTypes, colors, BackendStrings

from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.utils.db_utils import dbUtils
from setup_app.pylib.jproperties import Properties


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

        if not Config.admin_password:
            Config.admin_password = self.getPW()

        if Config.profile == 'jans':
            self.set_persistence_type()


        if not Config.encode_salt:
            Config.encode_salt = self.getPW() + self.getPW()

        if not Config.jans_max_mem:
            Config.jans_max_mem = int(base.current_mem_size * .83 * 1000) # 83% of physical memory


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


        if p.get('enable-script'):
            base.argsp.enable_script = p['enable-script'].split()

        if base.as_bool(p.get('loadTestData', False)):
            base.argsp.t = True

        if p.get('rdbm_type') == 'pgsql' and not p.get('rdbm_port'):
            p['rdbm_port'] = '5432'
        elif p.get('rdbm_type') == 'mysql' and not p.get('rdbm_port'):
            p['rdbm_port'] = '3306'

        properties_list = list(p.keys())

        for prop in properties_list:
            if prop in no_update:
                continue
            try:
                setattr(Config, prop, p[prop])

                if p[prop] == 'True':
                    setattr(Config, prop, True)
                elif p[prop] == 'False':
                    setattr(Config, prop, False)
            except Exception:
                self.logIt("Error loading property %s" % prop)

        if prop_file.endswith('-DEC~'):
            self.run(['rm', '-f', prop_file])

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
                if obj_name in ('post_messages', 'properties_password', 'non_setup_properties', 'addPostSetupService'):
                    continue

                if obj_name.startswith('cmd_'):
                    continue

                if not obj_name.startswith('__') and (not callable(obj)):
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


    def set_persistence_type(self):
        if Config.installed_instance:
            return

        Config.persistence_type = 'sql'


    def promptForHTTPD(self):
        if Config.installed_instance and Config.install_httpd:
            return

        prompt_for_httpd = self.getPrompt("Install Apache HTTPD Server", 
                                        self.getDefaultOption(Config.install_httpd)
                                        )[0].lower()

        Config.install_httpd = prompt_for_httpd == 'y'

        if Config.installed_instance and Config.install_httpd:
            Config.addPostSetupService.append('install_httpd')


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
        if Config.installed_instance and Config.install_fido2:
            return

        prompt_for_fido2_server = self.getPrompt("Install Fido2 Server?",
                                            self.getDefaultOption(Config.install_fido2)
                                            )[0].lower()
        Config.install_fido2 = prompt_for_fido2_server == 'y'

        if Config.installed_instance and Config.install_fido2:
            Config.addPostSetupService.append('install_fido2')


    def prompt_for_jans_link(self):
        if Config.installed_instance and Config.install_link:
            return

        prompt_jans_link = self.getPrompt("Install Jans LDAP Link Server?",
                                            self.getDefaultOption(Config.install_link)
                                            )[0].lower()

        Config.install_link = prompt_jans_link == 'y'

        if Config.installed_instance and Config.install_link:
            Config.addPostSetupService.append('install_link')


    def prompt_for_jans_keycloak_link(self):
        if Config.installed_instance and Config.install_jans_keycloak_link:
            return

        prompt_to_install = self.getPrompt("Install Jans KC Link Server?",
                                            self.getDefaultOption(Config.install_jans_keycloak_link)
                                            )[0].lower()

        Config.install_jans_keycloak_link = prompt_to_install == 'y'

        if Config.installed_instance and Config.install_jans_keycloak_link:
            Config.addPostSetupService.append('install_jans_keycloak_link')

    def prompt_for_casa(self):
        if Config.installed_instance and Config.install_casa:
            return

        prompt = self.getPrompt("Install Jans Casa?",
                                self.getDefaultOption(Config.install_casa)
                            )[0].lower()

        Config.install_casa = prompt == 'y'

        if Config.installed_instance and Config.install_casa:
            Config.addPostSetupService.append('install_casa')


    def prompt_to_install(self, install_var):
        if Config.installed_instance and Config.get(install_var):
            return False

        if not base.argsp.allow_pre_released_features and Config.get(install_var+'_pre_released'):
            return False

        return True


    def pompt_for_jans_lock(self):
        if not self.prompt_to_install('install_jans_lock'):
            return

        prompt = self.getPrompt("Install Jans Lock?",
                                            self.getDefaultOption(Config.install_jans_lock)
                                            )[0].lower()

        if prompt == 'y':
            prompt = self.getPrompt("  Install Jans Lock as Server?",
                                            self.getDefaultOption(Config.install_jans_lock)
                                            )[0].lower()
            if prompt == 'y':
                Config.install_jans_lock = True
                Config.install_jans_lock_as_server = True
            else:
                prompt = self.getPrompt("  Install Jans Lock as Auth Service?", self.getDefaultOption(True))[0].lower()
                if prompt == 'y':
                    Config.install_jans_lock = True

        if Config.installed_instance and Config.install_jans_lock:
            Config.addPostSetupService.append('install_jans_lock')

    def prompt_for_jans_saml(self):
        if not self.prompt_to_install('install_jans_saml'):
            return

        prompt = self.getPrompt("Install Jans KC?",
                                            self.getDefaultOption(Config.install_jans_saml)
                                            )[0].lower()

        Config.install_jans_saml = prompt == 'y'
        if Config.installed_instance:
            if Config.install_jans_saml:
                Config.addPostSetupService.append('install_jans_saml')
                if Config.install_config_api and not Config.install_scim_server:
                    Config.addPostSetupService.append('install_scim_server')
                    Config.install_scim_server = True
        else:
            if Config.install_jans_saml and Config.install_config_api:
                Config.install_scim_server = True

    def promptForConfigApi(self):
        if Config.installed_instance and Config.install_config_api:
            return

        prompt_for_config_api = self.getPrompt("Install Jans Config API?", 
                            self.getDefaultOption(Config.install_config_api)
                            )[0].lower()

        Config.install_config_api = prompt_for_config_api == 'y'
        Config.install_jans_cli = Config.install_config_api

        if Config.installed_instance and Config.install_config_api:
            Config.addPostSetupService.append('install_config_api')
            Config.addPostSetupService.append('install_jans_cli')


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
                    dbUtils.set_mysql_version()
                    if dbUtils.mariadb:
                        print("  {}MariaDB is not supported. Please use MySQL Server. {}".format(colors.FAIL, colors.ENDC))
                    else:
                        break
                else:
                    print("  {}Can't connect to {} server with provided credidentals.{}".format(colors.FAIL, Config.rdbm_type.upper(), colors.ENDC))
                    print("  ERROR:", result[1])


    def prompt_for_backend(self):
        if Config.installed_instance:
            return

        print('Chose Backend Type:')

        backend_types = [
                    BackendStrings.LOCAL_PGSQL,
                    BackendStrings.REMOTE_PGSQL,
                    BackendStrings.LOCAL_MYSQL,
                    BackendStrings.REMOTE_MYSQL,
                    ]

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

            if choice:
                break

        backend_type_str = backend_types[int(choice)-1]

        if backend_type_str in (BackendStrings.LOCAL_MYSQL, BackendStrings.LOCAL_PGSQL):
            Config.rdbm_install = True
            Config.rdbm_install_type = InstallTypes.LOCAL
            if backend_type_str == BackendStrings.LOCAL_MYSQL:
                Config.rdbm_port = 3306
                Config.rdbm_type = 'mysql'
            else:
                Config.rdbm_port = 5432
                Config.rdbm_type = 'pgsql'

        elif backend_type_str in (BackendStrings.REMOTE_MYSQL, BackendStrings.REMOTE_PGSQL):
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
                        conn = pymysql.connect(host=Config.rdbm_host, user=Config.rdbm_user, password=Config.rdbm_password, database=Config.rdbm_db, port=Config.rdbm_port)
                        if 'mariadb' in conn.server_version.lower():
                            print("  {}MariaDB is not supported. Please use MySQL Server. {}".format(colors.FAIL, colors.ENDC))
                            continue
                    else:
                        psycopg2.connect(dbname=Config.rdbm_db, user=Config.rdbm_user, password=Config.rdbm_password, host=Config.rdbm_host, port=Config.rdbm_port)
                    print("  {}{} connection was successful{}".format(colors.OKGREEN, Config.rdbm_type.upper(), colors.ENDC))
                    break
                except Exception as e:
                    print("  {}Can't connect to {}: {}{}".format(colors.DANGER,Config.rdbm_type.upper(), e, colors.ENDC))

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


    def prompt_for_http_cert_info(self):
        # IP address needed only for Apache2 and hosts file update
        if Config.install_httpd:
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


    def promptForProperties(self):

        if Config.noPrompt or '-x' in sys.argv:
            return


        if Config.installed_instance:
            print("This is previously installed instance. Available components will be prompted for installation.")

        else:
            promptForMITLicense = self.getPrompt("Do you acknowledge that use of the Janssen Server is under the Apache-2.0 license?", "N|y")[0].lower()
            if promptForMITLicense != 'y':
                sys.exit(0)

            self.prompt_for_http_cert_info()
            Config.jans_max_mem = self.getPrompt("Enter maximum RAM for applications in MB", str(Config.jans_max_mem))

            admin_password = Config.rdbm_password or self.getPW(special='.*=!%&+/-')

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
            #self.prompt_for_jans_link()
            self.prompt_for_jans_keycloak_link()
            self.prompt_for_casa()
            self.pompt_for_jans_lock()
            self.prompt_for_jans_saml()



propertiesUtils = PropertiesUtils()
