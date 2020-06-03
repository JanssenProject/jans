import os
import glob

from setup_app import paths
from setup_app.config import Config
from setup_app.utils.setup_utils import SetupUtils
from setup_app.installers.base import BaseInstaller


class CouchbaseInstaller(SetupUtils, BaseInstaller):

    def __init__(self):
        self.service_name = 'couchbase'
        self.pbar_text = "Installing Couchbase"

    def install(self):

        if not self.cbm:
             self.cbm = CBM(self.couchbase_hostname, self.couchebaseClusterAdmin, self.cb_password)

        if self.cb_install == LOCAL:
            self.couchbaseInstall()
            self.checkIfGluuBucketReady()
            self.couchebaseCreateCluster()

        self.couchbaseSSL()

        self.create_couchbase_buckets()

        couchbase_mappings = self.getMappingType('couchbase')

        if self.mappingLocations['default'] == 'couchbase':
            self.import_ldif_couchebase(self.couchbaseBucketDict['default']['ldif'], self.couchbase_bucket_prefix)

        for group in couchbase_mappings:
            bucket = '{}_{}'.format(self.couchbase_bucket_prefix, group)
            if self.couchbaseBucketDict[group]['ldif']:
                self.import_ldif_couchebase(self.couchbaseBucketDict[group]['ldif'], bucket)

        self.couchbaseProperties()


    def couchbaseInstall(self):
        coucbase_package = None
        
        tmp = []

        for f in os.listdir(self.couchbasePackageFolder):
            if f.startswith('couchbase-server-enterprise'):
                tmp.append(f)

        if not tmp:
            err_msg = "Couchbase package not found at %s. Exiting with error..." % (self.couchbasePackageFolder)
            self.logIt(err_msg, True)
            sys.exit(2)

        packageName = os.path.join(self.couchbasePackageFolder, max(tmp))
        self.logIt("Found package '%s' for install" % packageName)
        installOutput = self.installPackage(packageName)
        self.post_messages.append(installOutput)

        if self.os_type == 'ubuntu' and self.os_version == '16':
            script_name = os.path.basename(self.couchbaseInitScript)
            target_file = os.path.join('/etc/init.d', script_name)
            self.copyFile(self.couchbaseInitScript, target_file)
            self.run([self.cmd_chmod, '+x', target_file])
            self.run(["/usr/sbin/update-rc.d", script_name, 'defaults'])
            self.run(["/usr/sbin/update-rc.d", script_name, 'enable'])
            self.run_service_command('couchbase-server', 'start')

    def couchebaseCreateCluster(self):
        
        self.logIt("Initializing Couchbase Node")
        result = self.cbm.initialize_node()
        if result.ok:
            self.logIt("Couchbase Node was initialized")
        else:
            self.logIt("Failed to initilize Couchbase Node, reason: "+ result.text, errorLog=True)
        
        #wait a while for node initialization completed
        time.sleep(2)
        
        self.logIt("Renaming Couchbase Node")
        result = self.cbm.rename_node()
        if not result.ok:
            time.sleep(2)
            result = self.cbm.rename_node()

        if result.ok:
            self.logIt("Couchbase Node was renamed")
        else:
            self.logIt("Failed to rename Couchbase Node, reason: "+ result.text, errorLog=True)


        self.logIt("Setting Couchbase index storage mode")
        result = self.cbm.set_index_storage_mode()
        if result.ok:
            self.logIt("Couchbase index storage mode was set")
        else:
            self.logIt("Failed to set Couchbase index storage mode, reason: "+ result.text, errorLog=True)


        self.logIt("Setting Couchbase indexer memory quota to 1GB")
        result = self.cbm.set_index_memory_quta()
        if result.ok:
            self.logIt("Couchbase indexer memory quota was set to 1GB")
        else:
            self.logIt("Failed to set Couchbase indexer memory quota, reason: "+ result.text, errorLog=True)


        self.logIt("Setting up Couchbase Services")
        result = self.cbm.setup_services()
        if result.ok:
            self.logIt("Couchbase services were set up")
        else:
            self.logIt("Failed to setup Couchbase services, reason: "+ result.text, errorLog=True)


        self.logIt("Setting Couchbase Admin password")
        result = self.cbm.set_admin_password()
        if result.ok:
            self.logIt("Couchbase admin password  was set")
        else:
            self.logIt("Failed to set Couchbase admin password, reason: "+ result.text, errorLog=True)
            

    def couchebaseCreateBucket(self, bucketName, bucketType='couchbase', bucketRamsize=1024):
        result = self.cbm.add_bucket(bucketName, bucketRamsize, bucketType)
        self.logIt("Creating bucket {0} with type {1} and ramsize {2}".format(bucketName, bucketType, bucketRamsize))
        if result.ok:
            self.logIt("Bucket {} successfully created".format(bucketName))
        else:
            self.logIt("Failed to create bucket {}, reason: {}".format(bucketName, result.text), errorLog=True)
        #wait 1 second 
        time.sleep(1)

    def exec_n1ql_query(self, query):
        result = self.cbm.exec_query(query)
        if result.ok:
            self.logIt("Query execution was successful: {}".format(query))
        else:
            self.logIt("Failed to execute query {}, reason:".format(query, result.text), errorLog=True)

    def couchbaseExecQuery(self, queryFile):
        self.logIt("Running Couchbase query from file " + queryFile)
        
        query_file = open(queryFile)
        
        for line in query_file:
            query = line.strip()
            if query:
                self.exec_n1ql_query(query)

    def couchbaseMakeIndex(self, bucket, ind):

        if isinstance(ind[0], list):
            attrquoted = []
            attribs = ind[0]
            wherec = ind[1]
            for a in attribs:
                if not '(' in a:
                    attrquoted.append('`{}`'.format(a))
                else:
                    attrquoted.append(a)

            attrquoteds = ', '.join(attrquoted)
            
            index_name = '{0}_static_{1}'.format(bucket, str(uuid.uuid4()).split('-')[1])
            cmd = 'CREATE INDEX `{0}` ON `{1}`({2}) WHERE ({3})'.format(index_name, bucket, attrquoteds, wherec)
        
        else:
            if '(' in ''.join(ind):
                attr_ = ind[0]
                index_name_ = ind[0].replace('(','_').replace(')','_').replace('`','').lower()
                if index_name_.endswith('_'):
                    index_name_ = index_name_[:-1]
                index_name = 'def_{0}_{1}'.format(bucket, index_name_)
            else:
                attr_ = ','.join(['`{}`'.format(a) for a in ind])
                index_name = 'def_{0}_{1}'.format(bucket, '_'.join(ind))

            cmd = 'CREATE INDEX %s ON `%s`(%s) USING GSI WITH {"defer_build":true}' % (index_name, bucket, attr_)

        return cmd, index_name


    def couchebaseCreateIndexes(self, bucket):
        
        self.couchbaseBuckets.append(bucket)
        couchbase_index_str = self.readFile(self.couchbaseIndexJson)
        couchbase_index_str = couchbase_index_str.replace('!bucket_prefix!', self.couchbase_bucket_prefix)
        couchbase_index = json.loads(couchbase_index_str)

        self.logIt("Running Couchbase index creation for " + bucket + " bucket")

        if not os.path.exists(self.n1qlOutputFolder):
            os.mkdir(self.n1qlOutputFolder)
        
        tmp_file = os.path.join(self.n1qlOutputFolder, 'index_%s.n1ql' % bucket)

        with open(tmp_file, 'w') as W:
            index_list = couchbase_index.get(bucket,{})

            index_names = []
            for ind in index_list['attributes']:
                cmd, index_name = self.couchbaseMakeIndex(bucket, ind)
                W.write(cmd+';\n')
                index_names.append(index_name)

            for ind in index_list['static']:
                cmd, index_name = self.couchbaseMakeIndex(bucket, ind)
                W.write(cmd+';\n')

            if index_names:
                W.write('BUILD INDEX ON `%s` (%s) USING GSI;\n' % (bucket, ', '.join(index_names)))

        self.couchbaseExecQuery(tmp_file)


    def checkIfAttributeExists(self, key, atribute,  documents):
        ka = key + '::' + atribute
        retVal = False

        if ka in self.processedKeys:
            return True
         
        for d in documents:
            if d[0] == key:
                if 'changetype' in d[1]:
                    continue
                if atribute in d[1]:
                    retVal = True
                else:
                    self.processedKeys.append(ka)
                    return True
                
        return retVal

    def checkCBRoles(self, buckets=[]):
        result = self.cbm.whoami()
        bc = buckets[:]
        bucket_roles = {}
        if 'roles' in result:
            
            for role in result['roles']:
                if role['role'] == 'admin':
                    self.isCouchbaseUserAdmin = True
                    return True, None

                if not role['bucket_name'] in bucket_roles:
                    bucket_roles[role['bucket_name']] = []

                bucket_roles[role['bucket_name']].append(role['role'])

        for b_ in bc[:]:
            for r_ in self.cb_bucket_roles:
                if not r_ in bucket_roles[b_]:
                    break
            else:
                bc.remove(b_)

        if bc:
            return False, bc

        return True, None

    def import_ldif_couchebase(self, ldif_file_list=[], bucket=None):
        
        self.processedKeys = []

        key_prefixes = {}
        for cb in self.couchbaseBucketDict:
            for prefix in self.couchbaseBucketDict[cb]['document_key_prefix']:
                key_prefixes[prefix] = cb

        if not ldif_file_list:
            ldif_file_list = self.ldif_files[:]
        
        for ldif in ldif_file_list:
            self.logIt("Importing ldif file %s to Couchebase bucket %s" % (ldif, bucket))
            documents = gluu_utils.get_documents_from_ldif(ldif)

            ldif_base_name = os.path.basename(ldif)
            name, ext = os.path.splitext(ldif_base_name)

            if not os.path.exists(self.n1qlOutputFolder):
                os.mkdir(self.n1qlOutputFolder)

            tmp_file = os.path.join(self.n1qlOutputFolder, name+'.n1ql')
            
            with open(tmp_file, 'w') as o:
                for e in documents:
                    if bucket:
                        cur_bucket = bucket
                    else:
                        n_ = e[0].find('_')
                        document_key_prefix = e[0][:n_+1]
                        cur_bucket = self.couchbase_bucket_prefix + '_' + key_prefixes[document_key_prefix] if document_key_prefix in key_prefixes else 'gluu'

                    query = ''

                    if 'changetype' in e[1]:
                        if 'replace' in e[1]:
                            query = 'UPDATE `%s` USE KEYS "%s" SET %s="%s";\n' % (cur_bucket, e[0], e[1]['replace'], e[1][e[1]['replace']])
                        elif 'add' in e[1]:
                            for m in e[1][e[1]['add']]:
                                if self.checkIfAttributeExists(e[0], e[1]['add'],  documents):
                                    query += 'UPDATE `%s` USE KEYS "%s" SET %s=["%s"];\n' % (cur_bucket, e[0], e[1]['add'], m)
                                else:
                                    query += 'UPDATE `%s` USE KEYS "%s" SET %s=ARRAY_APPEND(%s, "%s");\n' % (cur_bucket, e[0], e[1]['add'], e[1]['add'], m)
                    else:
                        query = 'UPSERT INTO `%s` (KEY, VALUE) VALUES ("%s", %s);\n' % (cur_bucket, e[0], json.dumps(e[1]))

                    o.write(query)

            self.couchbaseExecQuery(tmp_file)

    def checkIfGluuBucketReady(self):

        for i in range(12):
            self.logIt("Checking if gluu bucket is ready for N1QL query. Try %d ..." % (i+1))
            cbm_result = self.cbm.test_connection()
            if cbm_result.ok:
                return True
            else:
                time.sleep(5)
        print("Couchbase server was not ready. Giving up" + str(cbm_result.reason))
        sys.exit(1)

    def couchbaseSSL(self):
        self.logIt("Exporting Couchbase SSL certificate to " + self.couchebaseCert)
        
        for cb_host in re_split_host.findall(self.couchbase_hostname):

            cbm_ = CBM(cb_host.strip(), self.couchebaseClusterAdmin, self.cb_password)
            cert = cbm_.get_certificate()
            with open(self.couchebaseCert, 'w') as w:
                w.write(cert)

            cmd_args = [self.cmd_keytool, "-import", "-trustcacerts", "-alias", "%s_couchbase" % cb_host, \
                      "-file", self.couchebaseCert, "-keystore", self.couchbaseTrustStoreFn, \
                      "-storepass", self.couchbaseTrustStorePass, "-noprompt"]

            self.run(cmd_args)

    def couchbaseDict(self):
        prop_dict = {
                    'hostname': ','.join(re_split_host.findall(self.couchbase_hostname)),
                    'couchbase_server_user': self.couchebaseClusterAdmin,
                    'encoded_couchbase_server_pw': self.encoded_cb_password,
                    'couchbase_buckets': ', '.join(self.couchbaseBuckets),
                    'default_bucket': self.couchbase_bucket_prefix,
                    'encryption_method': 'SSHA-256',
                    'ssl_enabled': 'true',
                    'couchbaseTrustStoreFn': self.couchbaseTrustStoreFn,
                    'encoded_couchbaseTrustStorePass': self.encoded_couchbaseTrustStorePass,
                    'certFolder': self.certFolder,
                    'gluuOptPythonFolder': self.gluuOptPythonFolder
                    }

        couchbase_mappings = []

        for group in list(self.couchbaseBucketDict.keys())[1:]:
            bucket = self.couchbase_bucket_prefix if group == 'default' else self.couchbase_bucket_prefix + '_' + group
            if bucket in self.couchbaseBuckets:
                cb_key = 'couchbase_{}_mapping'.format(group)
                if self.mappingLocations[group] == 'couchbase':
                    if self.couchbaseBucketDict[group]['mapping']:
                        couchbase_mappings.append('bucket.{}_{}.mapping: {}'.format(self.couchbase_bucket_prefix, group, self.couchbaseBucketDict[group]['mapping']))
                        self.templateRenderingDict[cb_key] = self.couchbaseBucketDict[group]['mapping']
                    else:
                         self.templateRenderingDict[cb_key] = ''
                else:
                    self.templateRenderingDict[cb_key] = ''

        prop_dict['couchbase_mappings'] = '\n'.join(couchbase_mappings)
        couchbase_test_mappings = [ 'config.' + mapping for mapping in couchbase_mappings ]
        prop_dict['couchbase_test_mappings'] = '\n'.join(couchbase_test_mappings)

        return prop_dict
        
    def couchbaseProperties(self):
        prop_file = os.path.basename(self.gluuCouchebaseProperties)
        prop = open(os.path.join(self.templateFolder, prop_file)).read()

        prop_dict = self.couchbaseDict()

        prop = prop % prop_dict
        
        out_file = os.path.join(self.outputFolder, prop_file)
        self.writeFile(out_file, prop)
        self.writeFile(self.gluuCouchebaseProperties, prop)


    def create_couchbase_buckets(self):
        #Determine ram_size for buckets
        system_info = self.cbm.get_system_info()
        couchbaseClusterRamsize = (system_info['storageTotals']['ram']['quotaTotal'] - system_info['storageTotals']['ram']['quotaUsed']) / (1024*1024)

        couchbase_mappings = self.getMappingType('couchbase')

        min_cb_ram = 0
        
        for group in couchbase_mappings:
             min_cb_ram += self.couchbaseBucketDict[group]['memory_allocation']
        
        min_cb_ram += self.couchbaseBucketDict['default']['memory_allocation']

        if couchbaseClusterRamsize < min_cb_ram:
            print("Available quota on couchbase server is less than {} MB. Exiting installation".format(min_cb_ram))
            sys.exit(1)

        self.logIt("Ram size for Couchbase buckets was determined as {0} MB".format(couchbaseClusterRamsize))

        min_cb_ram *= 1.0

        existing_buckets = []
        r = self.cbm.get_buckets()

        if r.ok:
            b_ = r.json()
            existing_buckets = [ bucket['name'] for bucket in b_ ]

        if not self.couchbase_bucket_prefix in existing_buckets:

            if self.mappingLocations['default'] != 'couchbase':
                self.couchebaseCreateBucket(self.couchbase_bucket_prefix, bucketRamsize=100)
            else:
                bucketRamsize = int((self.couchbaseBucketDict['default']['memory_allocation']/min_cb_ram)*couchbaseClusterRamsize)
                self.couchebaseCreateBucket(self.couchbase_bucket_prefix, bucketRamsize=bucketRamsize)

        if self.mappingLocations['default'] == 'couchbase':
            self.couchebaseCreateIndexes(self.couchbase_bucket_prefix)


        for group in couchbase_mappings:
            bucket = '{}_{}'.format(self.couchbase_bucket_prefix, group)
            if not bucket in existing_buckets:
                bucketRamsize = int((self.couchbaseBucketDict[group]['memory_allocation']/min_cb_ram)*couchbaseClusterRamsize)
                self.couchebaseCreateBucket(bucket, bucketRamsize=bucketRamsize)
            else:
                self.logIt("Bucket {} already exists, not creating".format(bucket))

            self.couchebaseCreateIndexes(bucket)

        if self.installSaml:
            
            shib_user = 'couchbaseShibUser'
            shib_user_password = self.couchbaseShibUserPassword
            shib_user_roles = 'query_select[*]'
            if self.isCouchbaseUserAdmin:
                self.logIt("Creating couchbase readonly user for shib")
                self.cbm.create_user(shib_user, shib_user_password, 'Shibboleth IDP', shib_user_roles)
            else:
                self.post_messages.append('{}Please create a user on Couchbase Server with the following credidentals and roles{}'.format(gluu_utils.colors.WARNING, gluu_utils.colors.ENDC))
                self.post_messages.append('Username: {}'.format(shib_user))
                self.post_messages.append('Password: {}'.format(shib_user_password))
                self.post_messages.append('Roles: {}'.format(shib_user_roles))

