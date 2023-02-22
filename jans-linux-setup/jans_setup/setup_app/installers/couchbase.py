import os
import glob
import time
import sys
import json
import uuid
import shutil

from setup_app import paths
from setup_app.static import InstallTypes, AppType, InstallOption, BackendTypes, colors
from setup_app.config import Config
from setup_app.utils import base
from setup_app.utils.cbm import CBM
from setup_app.utils.package_utils import PackageUtils
from setup_app.installers.base import BaseInstaller


class CouchbaseInstaller(PackageUtils, BaseInstaller):

    source_files = [
                    (os.path.join(Config.dist_jans_dir, 'jans-orm-couchbase-libs-distribution.zip'), os.path.join(base.current_app.app_info['JANS_MAVEN'], 'maven/io/jans/jans-orm-couchbase-libs/{0}/jans-orm-couchbase-libs-{0}-distribution.zip'.format(base.current_app.app_info['ox_version']))),
                    ]

    def __init__(self):
        setattr(base.current_app, self.__class__.__name__, self)
        self.service_name = 'couchbase-server'
        self.app_type = AppType.SERVICE
        self.install_type = InstallOption.OPTONAL
        self.install_var = 'cb_install'
        self.register_progess()

        self.couchebaseInstallDir = '/opt/couchbase/'
        self.couchbasePackageFolder = os.path.join(Config.distFolder, 'couchbase')
        self.couchbaseTrustStoreFn = os.path.join(Config.certFolder, 'couchbase.pkcs12')
        self.couchbaseIndexJson = os.path.join(Config.install_dir, 'static/couchbase/index.json')
        self.couchbaseInitScript = os.path.join(Config.install_dir, 'static/system/initd/couchbase-server')
        self.couchebaseCert = os.path.join(Config.certFolder, 'couchbase.pem')
        self.common_lib_dir = os.path.join(Config.jetty_base, 'common/libs/couchbase')

    def install(self):
        self.extract_libs()

        if Config.couchbase_hostname == 'localhost':
            Config.couchbase_hostname = Config.hostname

        if not Config.get('couchebaseClusterAdmin'):
            Config.couchebaseClusterAdmin = 'admin'
            
        if Config.cb_install == InstallTypes.LOCAL:
            Config.isCouchbaseUserAdmin = True

        if not Config.get('couchbaseTrustStorePass'):
            Config.couchbaseTrustStorePass = self.getPW()
            Config.encoded_couchbaseTrustStorePass = self.obscure(Config.couchbaseTrustStorePass)

        if not Config.get('cb_query_node'):
            Config.cb_query_node = Config.couchbase_hostname

        if not Config.get('couchbase_bucket_prefix'):
            Config.couchbase_bucket_prefix = 'jans'

        if Config.cb_install == InstallTypes.LOCAL:
            Config.couchbase_hostname = Config.hostname

        self.dbUtils.set_cbm()

        if Config.cb_install == InstallTypes.LOCAL:
            self.add_couchbase_post_messages()
            self.couchbaseInstall()
            Config.pbar.progress(self.service_name, "Configuring Couchbase", incr=False)
            self.checkIfJansBucketReady()
            self.couchebaseCreateCluster()

        self.couchbaseSSL()

        self.create_couchbase_buckets()

        Config.pbar.progress(self.service_name, "Importing documents into Couchbase", incr=False)

        couchbase_mappings = self.getMappingType('couchbase')

        if Config.mapping_locations['default'] == 'couchbase':
            self.dbUtils.import_ldif(Config.couchbaseBucketDict['default']['ldif'], Config.couchbase_bucket_prefix)
        else:
            self.dbUtils.import_ldif([Config.ldif_base], force=BackendTypes.COUCHBASE)

        for group in couchbase_mappings:
            bucket = '{}_{}'.format(Config.couchbase_bucket_prefix, group)
            if Config.couchbaseBucketDict[group]['ldif']:
                self.dbUtils.import_ldif(Config.couchbaseBucketDict[group]['ldif'], bucket)

        self.couchbaseProperties()


    def couchbaseInstall(self):
        coucbase_package = None

        cb_package_list = glob.glob(os.path.join(self.couchbasePackageFolder, 'couchbase-server-enterprise*'))

        if not cb_package_list:
            err_msg = "Couchbase package not found at %s. Exiting with error..." % (self.couchbasePackageFolder)
            self.logIt(err_msg, True, True)

        package_name = max(cb_package_list)
        self.logIt("Found package '%s' for install" % package_name)
        Config.pbar.progress(self.service_name, "Importing Couchbase package", incr=False)
        if base.clone_type == 'deb':
            apt_path = shutil.which('apt')
            self.chown(self.couchbasePackageFolder, '_apt', 'nogroup', recursive=True)
            install_output = self.run([apt_path, 'install', '-y', package_name])
        else:
            if not self.check_installed('ncurses-compat-libs'):
                self.installNetPackage('ncurses-compat-libs')
            install_output = self.installPackage(package_name)

        Config.post_messages.append(install_output)


    def couchebaseCreateCluster(self):
        
        self.logIt("Initializing Couchbase Node")
        result = self.dbUtils.cbm.initialize_node()
        if result.ok:
            self.logIt("Couchbase Node was initialized")
        else:
            self.logIt("Failed to initilize Couchbase Node, reason: "+ result.text, errorLog=True)

        #wait a while for node initialization completed
        time.sleep(4)

        self.logIt("Renaming Couchbase Node")
        result = self.dbUtils.cbm.rename_node()
        if not result.ok:
            time.sleep(4)
            result = self.dbUtils.cbm.rename_node()

        if result.ok:
            self.logIt("Couchbase Node was renamed")
        else:
            self.logIt("Failed to rename Couchbase Node, reason: "+ result.text, errorLog=True)

        self.logIt("Setting Couchbase index storage mode")
        result = self.dbUtils.cbm.set_index_storage_mode()
        if result.ok:
            self.logIt("Couchbase index storage mode was set")
        else:
            self.logIt("Failed to set Couchbase index storage mode, reason: "+ result.text, errorLog=True)

        self.logIt("Setting Couchbase indexer memory quota to 1GB")
        result = self.dbUtils.cbm.set_index_memory_quta()
        if result.ok:
            self.logIt("Couchbase indexer memory quota was set to 1GB")
        else:
            self.logIt("Failed to set Couchbase indexer memory quota, reason: "+ result.text, errorLog=True)

        self.logIt("Setting up Couchbase Services")
        result = self.dbUtils.cbm.setup_services()
        if result.ok:
            self.logIt("Couchbase services were set up")
        else:
            self.logIt("Failed to setup Couchbase services, reason: "+ result.text, errorLog=True)

        self.logIt("Setting Couchbase Admin password")
        result = self.dbUtils.cbm.set_admin_password()
        if result.ok:
            self.logIt("Couchbase admin password  was set")
        else:
            self.logIt("Failed to set Couchbase admin password, reason: "+ result.text, errorLog=True)


    def couchebaseCreateBucket(self, bucketName, bucketType='couchbase', bucketRamsize=1024):
        result = self.dbUtils.cbm.add_bucket(bucketName, bucketRamsize, bucketType)
        self.logIt("Creating bucket {0} with type {1} and ramsize {2}".format(bucketName, bucketType, bucketRamsize))
        if result.ok:
            self.logIt("Bucket {} successfully created".format(bucketName))
        else:
            self.logIt("Failed to create bucket {}, reason: {}".format(bucketName, result.text), errorLog=True)
        #wait 1 second 
        time.sleep(1)

    def exec_n1ql_query(self, query):
        result = self.dbUtils.cbm.exec_query(query)
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
        
        Config.couchbase_buckets.append(bucket)
        couchbase_index_str = self.readFile(self.couchbaseIndexJson)
        couchbase_index_str = couchbase_index_str.replace('!bucket_prefix!', Config.couchbase_bucket_prefix)
        couchbase_index = json.loads(couchbase_index_str)

        self.logIt("Running Couchbase index creation for " + bucket + " bucket")


        index_list = couchbase_index.get(bucket,{})

        index_names = []
        for ind in index_list['attributes']:
            n1ql, index_name = self.couchbaseMakeIndex(bucket, ind)
            self.exec_n1ql_query(n1ql)
            index_names.append(index_name)

        for ind in index_list['static']:
            n1ql, index_name = self.couchbaseMakeIndex(bucket, ind)
            self.exec_n1ql_query(n1ql)

        if index_names:
            n1ql = 'BUILD INDEX ON `%s` (%s) USING GSI' % (bucket, ', '.join(index_names))
            self.exec_n1ql_query(n1ql)


    def checkIfJansBucketReady(self):

        for i in range(12):
            self.logIt("Checking if jans bucket is ready for N1QL query. Try %d ..." % (i+1))
            cbm_result = self.dbUtils.cbm.test_connection()
            if cbm_result.ok:
                return True
            else:
                time.sleep(5)
        print("Couchbase server was not ready. Giving up" + str(cbm_result.reason))
        sys.exit(1)

    def couchbaseSSL(self):
        self.logIt("Exporting Couchbase SSL certificate to " + self.couchebaseCert)
        
        for cb_host in base.re_split_host.findall(Config.couchbase_hostname):

            cbm_ = CBM(cb_host.strip(), Config.couchebaseClusterAdmin, Config.cb_password)
            cert = cbm_.get_certificate()
            self.writeFile(self.couchebaseCert, cert)

            cmd_args = [Config.cmd_keytool, "-import", "-trustcacerts", "-alias", "%s_couchbase" % cb_host, \
                      "-file", self.couchebaseCert, "-keystore", self.couchbaseTrustStoreFn, \
                      "-storepass", Config.couchbaseTrustStorePass, "-noprompt"]

            self.run(cmd_args)

    def couchbaseDict(self):
        prop_dict = {
                    'hostname': ','.join(base.re_split_host.findall(Config.couchbase_hostname)),
                    'couchbase_server_user': Config.couchebaseClusterAdmin,
                    'encoded_couchbase_server_pw': Config.encoded_cb_password,
                    'couchbase_buckets': ', '.join(Config.couchbase_buckets),
                    'default_bucket': Config.couchbase_bucket_prefix,
                    'encryption_method': 'SSHA-256',
                    'ssl_enabled': 'true',
                    'couchbaseTrustStoreFn': self.couchbaseTrustStoreFn,
                    'encoded_couchbaseTrustStorePass': Config.encoded_couchbaseTrustStorePass,
                    'certFolder': Config.certFolder,
                    'jansOptPythonFolder': Config.jansOptPythonFolder,
                    'couchbase_query_node': Config.cb_query_node
                    }

        couchbase_mappings = []

        for group in list(Config.couchbaseBucketDict.keys())[1:]:
            bucket = Config.couchbase_bucket_prefix if group == 'default' else Config.couchbase_bucket_prefix + '_' + group
            if bucket in Config.couchbase_buckets:
                cb_key = 'couchbase_{}_mapping'.format(group)
                if Config.mapping_locations[group] == 'couchbase':
                    if Config.couchbaseBucketDict[group]['mapping']:
                        couchbase_mappings.append('bucket.{}_{}.mapping: {}'.format(Config.couchbase_bucket_prefix, group, Config.couchbaseBucketDict[group]['mapping']))
                        Config.templateRenderingDict[cb_key] = Config.couchbaseBucketDict[group]['mapping']
                    else:
                         Config.templateRenderingDict[cb_key] = ''
                else:
                    Config.templateRenderingDict[cb_key] = ''

        prop_dict['couchbase_mappings'] = '\n'.join(couchbase_mappings)
        couchbase_test_mappings = [ 'config.' + mapping for mapping in couchbase_mappings ]
        prop_dict['couchbase_test_mappings'] = '\n'.join(couchbase_test_mappings)

        return prop_dict
        
    def couchbaseProperties(self):
        prop_file = os.path.basename(Config.jansCouchebaseProperties)
        prop = open(os.path.join(Config.templateFolder, prop_file)).read()
        prop_dict = self.couchbaseDict()
        prop = prop % prop_dict
        out_file = os.path.join(Config.output_dir, prop_file)
        self.writeFile(out_file, prop)
        self.writeFile(Config.jansCouchebaseProperties, prop)

    def create_couchbase_buckets(self):
        #Determine ram_size for buckets
        system_info = self.dbUtils.cbm.get_system_info()
        couchbaseClusterRamsize = (system_info['storageTotals']['ram']['quotaTotal'] - system_info['storageTotals']['ram']['quotaUsed']) / (1024*1024)

        couchbase_mappings = self.getMappingType('couchbase')

        min_cb_ram = 0
        
        for group in couchbase_mappings:
             min_cb_ram += Config.couchbaseBucketDict[group]['memory_allocation']
        
        min_cb_ram += Config.couchbaseBucketDict['default']['memory_allocation']

        if couchbaseClusterRamsize < min_cb_ram:
            print("Available quota on couchbase server is less than {} MB. Exiting installation".format(min_cb_ram))
            sys.exit(1)

        self.logIt("Ram size for Couchbase buckets was determined as {0} MB".format(couchbaseClusterRamsize))

        min_cb_ram = float(min_cb_ram)

        existing_buckets = []
        r = self.dbUtils.cbm.get_buckets()

        if r.ok:
            b_ = r.json()
            existing_buckets = [ bucket['name'] for bucket in b_ ]

        if not Config.couchbase_bucket_prefix in existing_buckets:

            if Config.mapping_locations['default'] != 'couchbase':
                self.couchebaseCreateBucket(Config.couchbase_bucket_prefix, bucketRamsize=100)
            else:
                bucketRamsize = int((Config.couchbaseBucketDict['default']['memory_allocation']/min_cb_ram)*couchbaseClusterRamsize)
                self.couchebaseCreateBucket(Config.couchbase_bucket_prefix, bucketRamsize=bucketRamsize)

        if Config.mapping_locations['default'] == 'couchbase':
            self.couchebaseCreateIndexes(Config.couchbase_bucket_prefix)

        for group in couchbase_mappings:
            bucket = '{}_{}'.format(Config.couchbase_bucket_prefix, group)
            if not bucket in existing_buckets:
                bucketRamsize = int((Config.couchbaseBucketDict[group]['memory_allocation']/min_cb_ram)*couchbaseClusterRamsize)
                self.couchebaseCreateBucket(bucket, bucketRamsize=bucketRamsize)
            else:
                self.logIt("Bucket {} already exists, not creating".format(bucket))

            self.couchebaseCreateIndexes(bucket)

    def add_couchbase_post_messages(self):
        Config.post_messages.append( 
                "Please note that you have to update your firewall configuration to\n"
                "allow connections to the following ports on Couchbase Server:\n"
                "4369, 28091 to 28094, 9100 to 9105, 9998, 9999, 11207, 11209 to 11211,\n"
                "11214, 11215, 18091 to 18093, and from 21100 to 21299."
            )
        (w, e) = ('', '') if Config.thread_queue else (colors.WARNING, colors.ENDC)
        Config.post_messages.append(
            w+"By using Couchbase Server you agree to the End User License Agreement.\n"
            "See /opt/couchbase/LICENSE.txt"+e
            )

    def extract_libs(self):
        self.logIt("Extracting {}".format(self.source_files[0][0]))
        if not os.path.exists(self.common_lib_dir):
            self.createDirs(self.common_lib_dir)
        shutil.unpack_archive(self.source_files[0][0], self.common_lib_dir)
        self.chown(os.path.join(Config.jetty_base, 'common'), Config.jetty_user, Config.jetty_user, True)

    def installed(self):

        if os.path.exists(self.couchebaseInstallDir):
            cb_install = InstallTypes.LOCAL
        elif os.path.exists(self.couchbaseTrustStoreFn):
            cb_install = InstallTypes.REMOTE
        else:
            cb_install = 0

        return cb_install
