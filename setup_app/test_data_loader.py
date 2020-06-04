from setup_app.utils import base

class TestDataLoader:

    def create_test_client_keystore(self):
        self.logIt("Creating client_keystore.jks")
        client_keystore_fn = os.path.join(self.outputFolder, 'test/oxauth/client/client_keystore.jks')
        keys_json_fn =  os.path.join(self.outputFolder, 'test/oxauth/client/keys_client_keystore.json')
        
        args = [self.cmd_keytool, '-genkey', '-alias', 'dummy', '-keystore', 
                    client_keystore_fn, '-storepass', 'secret', '-keypass', 
                    'secret', '-dname', 
                    "'{}'".format(self.default_openid_jks_dn_name)
                    ]

        self.run(' '.join(args), shell=True)

        args = [self.cmd_java, '-Dlog4j.defaultInitOverride=true',
                '-cp', self.non_setup_properties['oxauth_client_jar_fn'], self.non_setup_properties['key_gen_path'],
                '-keystore', client_keystore_fn,
                '-keypasswd', 'secret',
                '-sig_keys', self.default_key_algs,
                '-enc_keys', self.default_key_algs,
                '-dnname', "'{}'".format(self.default_openid_jks_dn_name),
                '-expiration', '365','>', keys_json_fn]

        cmd = ' '.join(args)
        
        self.run(cmd, shell=True)

        self.copyFile(client_keystore_fn, os.path.join(self.outputFolder, 'test/oxauth/server'))
        self.copyFile(keys_json_fn, os.path.join(self.outputFolder, 'test/oxauth/server'))

    def load_test_data(self):
        self.logIt("Loading test ldif files")

        if not self.installPassport:
            self.generate_passport_configuration()

        ox_auth_test_ldif = os.path.join(self.outputFolder, 'test/oxauth/data/oxauth-test-data.ldif')
        ox_auth_test_user_ldif = os.path.join(self.outputFolder, 'test/oxauth/data/oxauth-test-data-user.ldif')
        
        scim_test_ldif = os.path.join(self.outputFolder, 'test/scim-client/data/scim-test-data.ldif')
        scim_test_user_ldif = os.path.join(self.outputFolder, 'test/scim-client/data/scim-test-data-user.ldif')

        ldif_files = [ox_auth_test_ldif, scim_test_ldif]
        ldif_user_files = [ox_auth_test_user_ldif, scim_test_user_ldif]

        cb_hosts = base.re_split_host.findall(self.couchbase_hostname)

        if self.mappingLocations['default'] == 'ldap':
            self.import_ldif_opendj(ldif_files)
        else:
            cb_host = cb_hosts[self.cb_query_node]
            self.cbm = CBM(cb_host, self.couchebaseClusterAdmin, self.cb_password)
            self.import_ldif_couchebase(ldif_files)

        if self.mappingLocations['user'] == 'ldap':
            self.import_ldif_opendj(ldif_user_files)
        else:
            cb_host = cb_hosts[self.cb_query_node]
            self.cbm = CBM(cb_host, self.couchebaseClusterAdmin, self.cb_password)
            bucket = '{}_user'.format(self.couchbase_bucket_prefix)
            self.import_ldif_couchebase(ldif_user_files,  bucket='gluu_user')

        apache_user = 'www-data'
        if self.os_type in ('red', 'centos', 'fedora'):
            apache_user = 'apache'


        # Client keys deployment
        self.run(['wget', '--no-check-certificate', 'https://raw.githubusercontent.com/GluuFederation/oxAuth/master/Client/src/test/resources/oxauth_test_client_keys.zip', '-O', '/var/www/html/oxauth_test_client_keys.zip'])
        self.run(['unzip', '-o', '/var/www/html/oxauth_test_client_keys.zip', '-d', '/var/www/html/'])
        self.run(['rm', '-rf', 'oxauth_test_client_keys.zip'])
        self.run(['chown', '-R', 'root:'+apache_user, '/var/www/html/oxauth-client'])


        oxAuthConfDynamic_changes = (
                                    ('dynamicRegistrationCustomObjectClass', 'oxAuthClientCustomAttributes'),
                                    ('dynamicRegistrationCustomAttributes', [ "oxAuthTrustedClient", "myCustomAttr1", "myCustomAttr2", "oxIncludeClaimsInIdToken" ]),
                                    ('dynamicRegistrationExpirationTime', 86400),
                                    ('dynamicGrantTypeDefault', [ "authorization_code", "implicit", "password", "client_credentials", "refresh_token", "urn:ietf:params:oauth:grant-type:uma-ticket" ]),
                                    ('legacyIdTokenClaims', True),
                                    ('authenticationFiltersEnabled', True),
                                    ('clientAuthenticationFiltersEnabled', True),
                                    ('keyRegenerationEnabled',True),
                                    ('openidScopeBackwardCompatibility', False),
                                    )


        custom_scripts = ('2DAF-F995', '2DAF-F996', '4BBE-C6A8')
        
        config_servers = ['{0}:{1}'.format(self.hostname, self.ldaps_port)]
        

        if self.mappingLocations['default'] == 'ldap':
            # oxAuth config changes
            ldap_conn = self.getLdapConnection()

            dn = 'ou=oxauth,ou=configuration,o=gluu'
            ldap_conn.search(
                            search_base=dn,
                            search_scope=BASE,
                            search_filter='(objectclass=*)',
                            attributes=['oxAuthConfDynamic']
                        )

            oxAuthConfDynamic = json.loads(ldap_conn.response[0]['attributes']['oxAuthConfDynamic'][0])

            for k, v in oxAuthConfDynamic_changes:
                oxAuthConfDynamic[k] = v

            oxAuthConfDynamic_js = json.dumps(oxAuthConfDynamic, indent=2)
            ldap_conn.modify(dn, {'oxAuthConfDynamic': [MODIFY_REPLACE, oxAuthConfDynamic_js]})

            # Enable custom scripts
            for inum in custom_scripts:
                dn = 'inum={0},ou=scripts,o=gluu'.format(inum)
                ldap_conn.modify(dn, {'oxEnabled': [MODIFY_REPLACE, 'true']})



            # Update LDAP schema
            self.copyFile(os.path.join(self.outputFolder, 'test/oxauth/schema/102-oxauth_test.ldif'), '/opt/opendj/config/schema/')
            self.copyFile(os.path.join(self.outputFolder, 'test/scim-client/schema/103-scim_test.ldif'), '/opt/opendj/config/schema/')

            schema_fn = os.path.join(self.openDjSchemaFolder,'77-customAttributes.ldif')

            obcl_parser = gluu_utils.myLdifParser(schema_fn)
            obcl_parser.parse()

            for i, o in enumerate(obcl_parser.entries[0][1]['objectClasses']):
                objcl = ObjectClass(o)
                if 'gluuCustomPerson' in objcl.tokens['NAME']:
                    may_list = list(objcl.tokens['NAME'])
                    for a in ('scimCustomFirst','scimCustomSecond', 'scimCustomThird'):
                        if not a in may_list:
                            may_list.append(a)
                    
                    objcl.tokens['MAY'] = tuple(may_list)
                    obcl_parser.entries[0][1]['objectClasses'][i] = objcl.getstr()

            tmp_fn = '/tmp/77-customAttributes.ldif'
            with open(tmp_fn, 'wb') as w:
                ldif_writer = LDIFWriter(w)
                for dn, entry in obcl_parser.entries:                
                    ldif_writer.unparse(dn, entry)

            self.copyFile(tmp_fn, self.openDjSchemaFolder)
            cwd = os.path.join(self.ldapBaseFolder, 'bin')
            dsconfigCmd = (
                '{} --trustAll --no-prompt --hostname {} --port {} '
                '--bindDN "{}" --bindPasswordFile /home/ldap/.pw set-connection-handler-prop '
                '--handler-name "LDAPS Connection Handler" --set listen-address:0.0.0.0'
                    ).format(
                        self.ldapBaseFolder, 
                        self.ldapDsconfigCommand, 
                        self.ldap_hostname, 
                        self.ldap_admin_port,
                        self.ldap_binddn
                    )
            
            self.run(['/bin/su', 'ldap', '-c', dsconfigCmd], cwd=cwd)
            
            ldap_conn.unbind()
            
            self.run_service_command('opendj', 'restart')

            for atr in ('myCustomAttr1', 'myCustomAttr2'):
                cmd = (
                    'create-backend-index --backend-name userRoot --type generic '
                    '--index-name {} --set index-type:equality --set index-entry-limit:4000 '
                    '--hostName {} --port {} --bindDN "{}" -j /home/ldap/.pw '
                    '--trustAll --noPropertiesFile --no-prompt'
                    ).format(
                        atr, 
                        self.ldap_hostname,
                        self.ldap_admin_port, 
                        self.ldap_binddn
                    )
                
                dsconfigCmd = '{1} {2}'.format(self.ldapBaseFolder, self.ldapDsconfigCommand, cmd)
                self.run(['/bin/su', 'ldap', '-c', dsconfigCmd], cwd=cwd)
            
            
            ldap_conn = self.getLdapConnection()
            
            dn = 'ou=configuration,o=gluu'

            ldap_conn.search(
                search_base=dn,
                search_scope=BASE,
                search_filter='(objectclass=*)',
                attributes=['oxIDPAuthentication']
            )
            
            
            oxIDPAuthentication = json.loads(ldap_conn.response[0]['attributes']['oxIDPAuthentication'][0])
            oxIDPAuthentication['config']['servers'] = config_servers
            oxIDPAuthentication_js = json.dumps(oxIDPAuthentication, indent=2)
            ldap_conn.modify(dn, {'oxIDPAuthentication': [MODIFY_REPLACE, oxIDPAuthentication_js]})

            ldap_conn.unbind()
            
        else:
            
            for k, v in oxAuthConfDynamic_changes:
                query = 'UPDATE gluu USE KEYS "configuration_oxauth" set gluu.oxAuthConfDynamic.{0}={1}'.format(k, json.dumps(v))
                self.exec_n1ql_query(query)
 
            for inum in custom_scripts:
                query = 'UPDATE gluu USE KEYS "scripts_{0}" set gluu.oxEnabled=true'.format(inum)
                self.exec_n1ql_query(query)

            self.exec_n1ql_query('CREATE INDEX def_gluu_myCustomAttr1 ON `gluu`(myCustomAttr1) USING GSI WITH {"defer_build":true}')
            self.exec_n1ql_query('CREATE INDEX def_gluu_myCustomAttr2 ON `gluu`(myCustomAttr2) USING GSI WITH {"defer_build":true}')
            self.exec_n1ql_query('BUILD INDEX ON `gluu` (def_gluu_myCustomAttr1, def_gluu_myCustomAttr2)')

            #query = 'UPDATE gluu USE KEYS "configuration" set gluu.oxIDPAuthentication.config.servers = {0}'.format(json.dumps(config_servers))
            #self.exec_n1ql_query(query)


        self.create_test_client_keystore()

        # Disable token binding module
        if self.os_type+self.os_version == 'ubuntu18':
            self.run(['a2dismod', 'mod_token_binding'])
            self.run_service_command('apache2', 'restart')

        self.run_service_command('oxauth', 'restart')
        
        # Prepare for tests run
        #install_command, update_command, query_command, check_text = self.get_install_commands()
        #self.run_command(install_command.format('git'))
        #self.run([self.cmd_mkdir, '-p', 'oxAuth/Client/profiles/ce_test'])
        #self.run([self.cmd_mkdir, '-p', 'oxAuth/Server/profiles/ce_test'])
        # Todo: Download and unzip file test_data.zip from CE server.
        # Todo: Copy files from unziped folder test/oxauth/client/* into oxAuth/Client/profiles/ce_test
        # Todo: Copy files from unziped folder test/oxauth/server/* into oxAuth/Server/profiles/ce_test
        #self.run([self.cmd_keytool, '-import', '-alias', 'seed22.gluu.org_httpd', '-keystore', 'cacerts', '-file', '%s/httpd.crt' % self.certFolder, '-storepass', 'changeit', '-noprompt'])
        #self.run([self.cmd_keytool, '-import', '-alias', 'seed22.gluu.org_opendj', '-keystore', 'cacerts', '-file', '%s/opendj.crt' % self.certFolder, '-storepass', 'changeit', '-noprompt'])
 

    def load_test_data_exit(self):
        print("Loading test data")
        prop_file = os.path.join(self.install_dir, 'setup.properties.last')
        
        if not os.path.exists(prop_file):
            prop_file += '.enc'
            if not os.path.exists(prop_file):
                print("setup.properties.last or setup.properties.last.enc were not found, exiting.")
                sys.exit(1)

        self.load_properties(prop_file)
        self.createLdapPw()
        self.load_test_data()
        self.deleteLdapPw()
        print("Test data loaded. Exiting ...")
        sys.exit()

