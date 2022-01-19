"""
 License terms and conditions for Gluu Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
 Installs and configures Couchbase
"""

from pathlib import Path
import shutil
import tarfile
from pygluu.kubernetes.kubeapi import Kubernetes
from pygluu.kubernetes.yamlparser import Parser
from pygluu.kubernetes.helpers import get_logger, exec_cmd
from pygluu.kubernetes.settings import ValuesHandler
from pygluu.kubernetes.pycert import setup_crts
import sys
import base64
import random
import os

logger = get_logger("gluu-couchbase     ")


def extract_couchbase_tar(tar_file):
    """
    Extracts couchbase kubernetes tar file
    :param tar_file:
    """
    extract_folder = Path("./couchbase-source-folder")
    logger.info("Extracting {} in {} ".format(tar_file, extract_folder))
    tr = tarfile.open(tar_file)
    tr.extractall(path=extract_folder)
    tr.close()


def set_memory_for_buckets(memory_quota, couchbase_bucket_prefix):
    def parse_couchbase_buckets(file, bucket_type, allbuckets):
        for bucket in allbuckets:
            metadata_name = "gluu"
            if bucket:
                metadata_name = "gluu-" + bucket
            parser = Parser(file, bucket_type, metadata_name)
            parser["spec"]["memoryQuota"] = str(memory_quota + 100) + "Mi"
            parser["spec"]["name"] = couchbase_bucket_prefix
            parser["metadata"]["name"] = couchbase_bucket_prefix
            if bucket:
                parser["spec"]["name"] = couchbase_bucket_prefix + "_" + bucket
                parser["metadata"]["name"] = couchbase_bucket_prefix + "-" + bucket
            parser.dump_it()

    buckets = ["", "site", "user"]
    ephemeral_buckets = ["cache", "token", "session"]
    parse_couchbase_buckets("./couchbase/couchbase-buckets.yaml",
                            "CouchbaseBucket", buckets)
    parse_couchbase_buckets("./couchbase/couchbase-ephemeral-buckets.yaml",
                            "CouchbaseEphemeralBucket", ephemeral_buckets)


def create_server_spec_per_cb_service(zones, number_of_cb_service_nodes, cb_service_name, mem_req, mem_limit,
                                      cpu_req, cpu_limit):
    """
    Creates the server spec section inside couchbase.yaml for each couchbase service
    :param zones:
    :param number_of_cb_service_nodes:
    :param cb_service_name:
    :param mem_req:
    :param mem_limit:
    :param cpu_req:
    :param cpu_limit:
    :return:
    """
    server_spec = []
    zones = zones
    number_of_zones = len(zones)
    size = dict()
    # Create defualt size 1 for all the zones available
    for n in range(number_of_cb_service_nodes):
        random_zone_index = random.randint(0, number_of_zones - 1)
        try:
            size[zones[random_zone_index]] = size[zones[random_zone_index]] + 1
        except KeyError:
            size[zones[random_zone_index]] = 1

    for k, v in size.items():
        node_zone = k
        name = "pvc-" + cb_service_name
        if cb_service_name == "analytics":
            name = ["pvc-" + cb_service_name]
        spec = {"name": cb_service_name + "-" + node_zone, "size": v, "serverGroups": [node_zone],
                "services": [cb_service_name],
                "resources": {"limits": {"cpu": str(cpu_limit) + "m", "memory": str(mem_limit) + "Mi"},
                              "requests": {"cpu": str(cpu_req) + "m", "memory": str(mem_req) + "Mi"}},
                "volumeMounts": {"default": "pvc-general", cb_service_name: name}
                }
        server_spec.append(spec)

    return server_spec


class Couchbase(object):
    def __init__(self):
        self.settings = ValuesHandler()
        self.kubernetes = Kubernetes()
        self.storage_class_file = Path("./couchbase/storageclasses.yaml")
        self.couchbase_cluster_file = Path("./couchbase/couchbase-cluster.yaml")
        self.couchbase_buckets_file = Path("./couchbase/couchbase-buckets.yaml")
        self.couchbase_group_file = Path("./couchbase/couchbase-group.yaml")
        self.couchbase_user_file = Path("./couchbase/couchbase-user.yaml")
        self.couchbase_rolebinding_file = Path("./couchbase/couchbase-rolebinding.yaml")
        self.couchbase_ephemeral_buckets_file = Path("./couchbase/couchbase-ephemeral-buckets.yaml")
        self.couchbase_source_folder_pattern, self.couchbase_source_file = self.get_couchbase_files
        self.couchbase_custom_resource_definition_file = self.couchbase_source_file.joinpath("crd.yaml")
        self.couchbase_operator_dac_file = self.couchbase_source_file.joinpath("operator_dac.yaml")
        self.couchbase_admission_file = self.couchbase_source_file.joinpath("admission.yaml")
        self.couchbase_operator_backup_file = self.couchbase_source_file.joinpath("operator_dac_backup.yaml")
        self.filename = ""
        # @TODO: Remove flag after depreciation of couchbase operator 2.0
        self.old_couchbase = False

    @property
    def get_couchbase_files(self):
        """
        Returns the couchbase extracted package folder path containing manifests and the tar package file
        :return:
        """
        if self.settings.get("installer-settings.couchbase.install"):
            couchbase_tar_pattern = "couchbase-autonomous-operator-kubernetes_*.tar.gz"
            directory = Path('.')
            try:
                couchbase_tar_file = list(directory.glob(couchbase_tar_pattern))[0]
                if "_1." in str(couchbase_tar_file.resolve()):
                    logger.fatal("Couchbase Autonomous Operator version must be > 2.0")
                    sys.exit()
                # @TODO: Remove condition and underlying lines after depreciation of couchbase operator 2.0
                if "_2.0" in str(couchbase_tar_file.resolve()):
                    logger.warning("An newer version of the couchbase operator exists. "
                                   "Please consider canceling out and using it.https://www.couchbase.com/downloads")
                    self.old_couchbase = True

            except IndexError:
                logger.fatal("Couchbase package not found.")
                logger.info("Please download the couchbase kubernetes package and place it inside the same directory "
                            "containing the pygluu-kubernetes.pyz script.https://www.couchbase.com/downloads")
                sys.exit()
            extract_couchbase_tar(couchbase_tar_file)
            couchbase_source_folder_pattern = "./couchbase-source-folder/couchbase-autonomous-operator-kubernetes_*"
            couchbase_source_folder = list(directory.glob(couchbase_source_folder_pattern))[0]

            return couchbase_tar_file, couchbase_source_folder
        # Couchbase is installed.
        return Path("."), Path(".")

    def create_couchbase_gluu_cert_pass_secrets(self, encoded_ca_crt_string, encoded_cb_pass_string,
                                                encoded_cb_super_pass_string):
        """
        Create cor patch secret containing couchbase certificate authority crt and couchbase admin password
        :param encoded_ca_crt_string:
        :param encoded_cb_pass_string:
        :param encoded_cb_super_pass_string:
        """
        # Remove this if its not needed
        self.kubernetes.patch_or_create_namespaced_secret(name="cb-crt",
                                                          namespace=self.settings.get("installer-settings.namespace"),
                                                          literal="couchbase.crt",
                                                          value_of_literal=encoded_ca_crt_string)

        # Remove this if its not needed
        self.kubernetes.patch_or_create_namespaced_secret(name="cb-pass",
                                                          namespace=self.settings.get("installer-settings.namespace"),
                                                          literal="couchbase_password",
                                                          value_of_literal=encoded_cb_pass_string)

        self.kubernetes.patch_or_create_namespaced_secret(name="cb-super-pass",
                                                          namespace=self.settings.get("installer-settings.namespace"),
                                                          literal="couchbase_superuser_password",
                                                          value_of_literal=encoded_cb_super_pass_string)

    def setup_backup_couchbase(self):
        """
        Setups Couchbase backup strategy
        """
        couchbase_backup_file = Path("./couchbase/backup/couchbase-backup.yaml")
        parser = Parser(couchbase_backup_file, "CouchbaseBackup")
        parser["spec"]["full"]["schedule"] = self.settings.get("installer-settings.couchbase.backup.fullSchedule")
        parser["spec"]["incremental"]["schedule"] = self.settings.get(
            "installer-settings.couchbase.backup.incrementalSchedule")
        parser["spec"]["backupRetention"] = self.settings.get("installer-settings.couchbase.backup.retentionTime")
        parser["spec"]["size"] = self.settings.get("installer-settings.couchbase.backup.storageSize")
        parser.dump_it()
        self.kubernetes.create_namespaced_custom_object(filepath=couchbase_backup_file,
                                                        group="couchbase.com",
                                                        version="v2",
                                                        plural="couchbasebackups",
                                                        namespace=self.settings.get("installer-settings.couchbase.namespace"))

    @property
    def calculate_couchbase_resources(self):
        """
        Return a dictionary containing couchbase resource information calculated
        Alpha
        @todo: switch to preset values based on ranges for TPS and amount of users
        :return:
        """
        tps = int(self.settings.get("CN_EXPECTED_TRANSACTIONS_PER_SEC"))
        number_of_data_nodes = 0
        number_of_query_nodes = 0
        number_of_index_nodes = 0
        number_of_eventing_service_memory_nodes = 0
        user_ratio = int(self.settings.get("CN_NUMBER_OF_EXPECTED_USERS")) / 50000000
        tps_ratio = tps / 14000

        if self.settings.get("CN_USING_RESOURCE_OWNER_PASSWORD_CRED_GRANT_FLOW") == "Y":
            number_of_data_nodes += tps_ratio * 7 * user_ratio
            number_of_query_nodes += tps_ratio * 5 * user_ratio
            number_of_index_nodes += tps_ratio * 5 * user_ratio
            number_of_eventing_service_memory_nodes += tps_ratio * 4 * user_ratio

        if self.settings.get("CN_USING_CODE_FLOW") == "Y":
            number_of_data_nodes += tps_ratio * 14 * user_ratio
            number_of_query_nodes += tps_ratio * 12 * user_ratio
            number_of_index_nodes += tps_ratio * 13 * user_ratio
            number_of_eventing_service_memory_nodes += tps_ratio * 7 * user_ratio

        if self.settings.get("CN_USING_SCIM_FLOW") == "Y":
            number_of_data_nodes += tps_ratio * 7 * user_ratio
            number_of_query_nodes += tps_ratio * 5 * user_ratio
            number_of_index_nodes += tps_ratio * 5 * user_ratio
            number_of_eventing_service_memory_nodes += tps_ratio * 4 * user_ratio

        if not self.settings.get("CN_COUCHBASE_GENERAL_STORAGE"):
            self.settings.set("CN_COUCHBASE_GENERAL_STORAGE", str(int((tps_ratio * (
                int(self.settings.get("CN_NUMBER_OF_EXPECTED_USERS")) / 125000)) + 5)) + "Gi")
        if not self.settings.get("CN_COUCHBASE_DATA_STORAGE"):
            self.settings.set("CN_COUCHBASE_DATA_STORAGE", str(int((tps_ratio * (
                int(self.settings.get("CN_NUMBER_OF_EXPECTED_USERS")) / 100000)) + 5)) + "Gi")
        if not self.settings.get("CN_COUCHBASE_INDEX_STORAGE"):
            self.settings.set("CN_COUCHBASE_INDEX_STORAGE", str(int((tps_ratio * (
                int(self.settings.get("CN_NUMBER_OF_EXPECTED_USERS")) / 200000)) + 5)) + "Gi")
        if not self.settings.get("CN_COUCHBASE_QUERY_STORAGE"):
            self.settings.set("CN_COUCHBASE_QUERY_STORAGE", str(int((tps_ratio * (
                int(self.settings.get("CN_NUMBER_OF_EXPECTED_USERS")) / 200000)) + 5)) + "Gi")
        if not self.settings.get("CN_COUCHBASE_ANALYTICS_STORAGE"):
            self.settings.set("CN_COUCHBASE_ANALYTICS_STORAGE", str(int((tps_ratio * (
                int(self.settings.get("CN_NUMBER_OF_EXPECTED_USERS")) / 250000)) + 5)) + "Gi")

        if self.settings.get("CN_COUCHBASE_DATA_NODES"):
            number_of_data_nodes = self.settings.get("CN_COUCHBASE_DATA_NODES")
        if self.settings.get("CN_COUCHBASE_QUERY_NODES"):
            number_of_query_nodes = self.settings.get("CN_COUCHBASE_QUERY_NODES")
        if self.settings.get("CN_COUCHBASE_INDEX_NODES"):
            number_of_index_nodes = self.settings.get("CN_COUCHBASE_INDEX_NODES")
        if self.settings.get("CN_COUCHBASE_SEARCH_EVENTING_ANALYTICS_NODES"):
            number_of_eventing_service_memory_nodes = self.settings.get("CN_COUCHBASE_SEARCH_EVENTING_ANALYTICS_NODES")

        data_service_memory_quota = (tps_ratio * 40000 * user_ratio) + 512
        data_memory_request = data_service_memory_quota / 4
        data_memory_limit = data_memory_request
        data_cpu_request = data_service_memory_quota / 4
        data_cpu_limit = data_cpu_request

        query_memory_request = data_memory_request
        query_memory_limit = query_memory_request
        query_cpu_request = data_service_memory_quota / 4
        query_cpu_limit = query_cpu_request

        index_service_memory_quota = (tps_ratio * 25000 * user_ratio) + 256
        index_memory_request = index_service_memory_quota / 3
        index_memory_limit = index_memory_request
        index_cpu_request = index_service_memory_quota / 3
        index_cpu_limit = index_cpu_request

        search_service_memory_quota = (tps_ratio * 4000 * user_ratio) + 256
        eventing_service_memory_quota = (tps_ratio * 4000 * user_ratio) + 256
        analytics_service_memory_quota = (tps_ratio * 4000 * user_ratio) + 1024

        search_eventing_analytics_memory_quota_sum = (search_service_memory_quota + eventing_service_memory_quota +
                                                      analytics_service_memory_quota)
        search_eventing_analytics_memory_request = tps_ratio * 10000 * user_ratio
        search_eventing_analytics_memory_limit = search_eventing_analytics_memory_request
        search_eventing_analytics_cpu_request = tps_ratio * 6000 * user_ratio
        search_eventing_analytics_cpu_limit = search_eventing_analytics_cpu_request

        # Two services because query is assumed to take the same amount of mem quota
        total_mem_resources = \
            data_service_memory_quota + data_service_memory_quota + index_service_memory_quota + \
            search_eventing_analytics_memory_quota_sum

        total_cpu_resources = data_cpu_limit + query_cpu_limit + index_cpu_limit + search_eventing_analytics_cpu_limit

        resources_info = dict(CN_COUCHBASE_DATA_NODES=int(number_of_data_nodes),
                              CN_COUCHBASE_QUERY_NODES=int(number_of_query_nodes),
                              CN_COUCHBASE_INDEX_NODES=int(number_of_index_nodes),
                              CN_COUCHBASE_SEARCH_EVENTING_ANALYTICS_NODES=int(number_of_eventing_service_memory_nodes),
                              COUCHBASE_DATA_MEM_QUOTA=round(data_service_memory_quota),
                              COUCHBASE_DATA_MEM_REQUEST=round(data_memory_request),
                              COUCHBASE_DATA_MEM_LIMIT=round(data_memory_limit),
                              COUCHBASE_DATA_CPU_REQUEST=round(data_cpu_request),
                              COUCHBASE_DATA_CPU_LIMIT=round(data_cpu_limit),
                              COUCHBASE_QUERY_MEM_QUOTA=round(data_service_memory_quota),
                              COUCHBASE_QUERY_MEM_REQUEST=round(query_memory_request),
                              COUCHBASE_QUERY_MEM_LIMIT=round(query_memory_limit),
                              COUCHBASE_QUERY_CPU_REQUEST=round(query_cpu_request),
                              COUCHBASE_QUERY_CPU_LIMIT=round(query_cpu_limit),
                              COUCHBASE_INDEX_MEM_QUOTA=round(index_service_memory_quota),
                              COUCHBASE_INDEX_MEM_REQUEST=round(index_memory_request),
                              COUCHBASE_INDEX_MEM_LIMIT=round(index_memory_limit),
                              COUCHBASE_INDEX_CPU_REQUEST=round(index_cpu_request),
                              COUCHBASE_INDEX_CPU_LIMIT=round(index_cpu_limit),
                              COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_QUOTA=round(search_service_memory_quota),
                              COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_REQUEST=round(
                                  search_eventing_analytics_memory_request),
                              COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_LIMIT=round(
                                  search_eventing_analytics_memory_limit),
                              COUCHBASE_SEARCH_EVENTING_ANALYTICS_CPU_REQUEST=round(
                                  search_eventing_analytics_cpu_request),
                              COUCHBASE_SEARCH_EVENTING_ANALYTICS_CPU_LIMIT=round(search_eventing_analytics_cpu_limit),
                              TOTAL_RAM_NEEDED=round(total_mem_resources),
                              TOTAL_CPU_NEEDED=round(total_cpu_resources)
                              )
        self.settings.set("CN_COUCHBASE_DATA_NODES", number_of_data_nodes)
        self.settings.set("CN_COUCHBASE_QUERY_NODES", number_of_query_nodes)
        self.settings.set("CN_COUCHBASE_INDEX_NODES", number_of_index_nodes)
        self.settings.set("CN_COUCHBASE_SEARCH_EVENTING_ANALYTICS_NODES", number_of_eventing_service_memory_nodes)
        return resources_info

    def analyze_couchbase_cluster_yaml(self):
        """
        Dumps created calculated resources into couchbase.yaml file. ALso includes cloud zones.
        """
        parser = Parser("./couchbase/couchbase-cluster.yaml", "CouchbaseCluster")
        parser["metadata"]["name"] = self.settings.get("installer-settings.couchbase.clusterName")
        number_of_buckets = 5
        if self.settings.get("global.storageClass.provisioner") in ("microk8s.io/hostpath",
                                                                    "k8s.io/minikube-hostpath") or \
                self.settings.get("global.cloud.testEnviroment"):
            resources_servers = [{"name": "allServices", "size": 1,
                                  "services": ["data", "index", "query", "search", "eventing", "analytics"],
                                  "volumeMounts": {"default": "pvc-general",
                                                   "data": "pvc-data", "index": "pvc-index",
                                                   "analytics": ["pvc-analytics"]}}]
            data_service_memory_quota = 1024
            index_service_memory_quota = 512
            search_service_memory_quota = 512
            eventing_service_memory_quota = 512
            analytics_service_memory_quota = 1024
            memory_quota = 0
        else:
            resources = self.calculate_couchbase_resources
            data_service_memory_quota = resources["COUCHBASE_DATA_MEM_QUOTA"]
            index_service_memory_quota = resources["COUCHBASE_INDEX_MEM_QUOTA"]
            search_service_memory_quota = resources["COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_QUOTA"]
            eventing_service_memory_quota = resources["COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_QUOTA"]
            analytics_service_memory_quota = resources["COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_QUOTA"] + 1024
            memory_quota = ((resources["COUCHBASE_DATA_MEM_QUOTA"] - 500) / number_of_buckets)
            zones_list = self.settings.get("CN_NODES_ZONES")
            data_server_spec = create_server_spec_per_cb_service(zones_list, int(resources["CN_COUCHBASE_DATA_NODES"]),
                                                                 "data",
                                                                 str(resources["COUCHBASE_DATA_MEM_REQUEST"]),
                                                                 str(resources["COUCHBASE_DATA_MEM_LIMIT"]),
                                                                 str(resources["COUCHBASE_DATA_CPU_REQUEST"]),
                                                                 str(resources["COUCHBASE_DATA_CPU_LIMIT"]))

            query_server_spec = create_server_spec_per_cb_service(zones_list,
                                                                  int(resources["CN_COUCHBASE_QUERY_NODES"]),
                                                                  "query",
                                                                  str(resources["COUCHBASE_QUERY_MEM_REQUEST"]),
                                                                  str(resources["COUCHBASE_QUERY_MEM_LIMIT"]),
                                                                  str(resources["COUCHBASE_QUERY_CPU_REQUEST"]),
                                                                  str(resources["COUCHBASE_QUERY_CPU_LIMIT"]))

            index_server_spec = create_server_spec_per_cb_service(zones_list,
                                                                  int(resources["CN_COUCHBASE_INDEX_NODES"]), "index",
                                                                  str(resources["COUCHBASE_INDEX_MEM_REQUEST"]),
                                                                  str(resources["COUCHBASE_INDEX_MEM_LIMIT"]),
                                                                  str(resources["COUCHBASE_INDEX_CPU_REQUEST"]),
                                                                  str(resources["COUCHBASE_INDEX_CPU_LIMIT"]))

            search_eventing_analytics_server_spec = create_server_spec_per_cb_service(
                zones_list,
                int(resources["CN_COUCHBASE_SEARCH_EVENTING_ANALYTICS_NODES"]), "analytics",
                str(resources["COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_REQUEST"]),
                str(resources["COUCHBASE_SEARCH_EVENTING_ANALYTICS_MEM_LIMIT"]),
                str(resources["COUCHBASE_SEARCH_EVENTING_ANALYTICS_CPU_REQUEST"]),
                str(resources["COUCHBASE_SEARCH_EVENTING_ANALYTICS_CPU_LIMIT"]))

            resources_servers = \
                data_server_spec + query_server_spec + index_server_spec + \
                search_eventing_analytics_server_spec

        if self.settings.get("installer-settings.nodes.zones"):
            unique_zones = list(dict.fromkeys(self.settings.get("installer-settings.nodes.zones")))
            parser["spec"]["serverGroups"] = unique_zones
        parser["spec"]["cluster"]["dataServiceMemoryQuota"] = str(data_service_memory_quota) + "Mi"
        parser["spec"]["cluster"]["indexServiceMemoryQuota"] = str(index_service_memory_quota) + "Mi"
        parser["spec"]["cluster"]["searchServiceMemoryQuota"] = str(search_service_memory_quota) + "Mi"
        parser["spec"]["cluster"]["eventingServiceMemoryQuota"] = str(eventing_service_memory_quota) + "Mi"
        parser["spec"]["cluster"]["analyticsServiceMemoryQuota"] = str(analytics_service_memory_quota) + "Mi"

        set_memory_for_buckets(memory_quota, self.settings.get("config.configmap.cnCouchbaseBucketPrefix"))
        parser["metadata"]["name"] = self.settings.get("installer-settings.couchbase.clusterName")
        parser["spec"]["servers"] = resources_servers

        number_of_volume_claims = len(parser["spec"]["volumeClaimTemplates"])
        for i in range(number_of_volume_claims):
            name = parser["spec"]["volumeClaimTemplates"][i]["metadata"]["name"]
            if name == "pvc-general":
                parser["spec"]["volumeClaimTemplates"][i]["spec"]["resources"]["requests"]["storage"] = "5Gi"
            elif name == "pvc-data":
                parser["spec"]["volumeClaimTemplates"][i]["spec"]["resources"]["requests"]["storage"] = "5Gi"
            elif name == "pvc-index":
                parser["spec"]["volumeClaimTemplates"][i]["spec"]["resources"]["requests"]["storage"] = "5Gi"
            elif name == "pvc-query":
                parser["spec"]["volumeClaimTemplates"][i]["spec"]["resources"]["requests"]["storage"] = "5Gi"
            elif name == "pvc-analytics":
                parser["spec"]["volumeClaimTemplates"][i]["spec"]["resources"]["requests"]["storage"] = "5Gi"
        parser.dump_it()

    def install(self):
        """
        Installs Couchbase
        """
        self.kubernetes.create_namespace(name=self.settings.get("installer-settings.namespace"))
        if not self.settings.get("installer-settings.couchbase.customFileOverride"):
            try:
                self.analyze_couchbase_cluster_yaml()
            except Exception:
                # TODO remove this exception
                logger.error("Looks like some of the couchbase files were misconfigured. "
                             "If you wish to override the couchbase files please set "
                             " installer-settings.couchbase.customFileOverride to true`")
                sys.exit()
        cb_namespace = self.settings.get("installer-settings.couchbase.namespace")
        storage_class_file_parser = Parser(self.storage_class_file, "StorageClass")
        if self.settings.get('global.storageClass.provisioner') in ("kubernetes.io/gce-pd",
                                                                    "dobs.csi.digitalocean.com",
                                                                    "kubernetes.io/azure-disk"):
            try:
                del storage_class_file_parser["parameters"]["encrypted"]
            except KeyError:
                logger.info("Key not found")
            storage_class_file_parser["parameters"]["type"] = \
                self.settings.get("installer-settings.couchbase.volumeType")
        storage_class_file_parser["provisioner"] = self.settings.get('global.storageClass.provisioner')
        if self.settings.get('global.storageClass.provisioner') == "microk8s.io/hostpath":
            try:
                del storage_class_file_parser["allowVolumeExpansion"]
                del storage_class_file_parser["parameters"]
            except KeyError:
                logger.info("Key not found")
            storage_class_file_parser.dump_it()
        elif self.settings.get('global.storageClass.provisioner') == "k8s.io/minikube-hostpath":
            try:
                del storage_class_file_parser["allowVolumeExpansion"]
                del storage_class_file_parser["parameters"]
            except KeyError:
                logger.info("Key not found")
            storage_class_file_parser.dump_it()
        else:
            try:
                storage_class_file_parser["parameters"]["type"] = \
                    self.settings.get("installer-settings.couchbase.volumeType")
            except KeyError:
                logger.info("Key not found")
        storage_class_file_parser.dump_it()

        logger.info("Installing Couchbase...")
        couchbase_crts_keys = Path("couchbase_crts_keys")
        if not couchbase_crts_keys.exists():
            os.mkdir(couchbase_crts_keys)
        custom_cb_ca_crt = Path("./couchbase_crts_keys/ca.crt")
        custom_cb_crt = Path("./couchbase_crts_keys/chain.pem")
        custom_cb_key = Path("./couchbase_crts_keys/pkey.key")
        if not custom_cb_ca_crt.exists() and not custom_cb_crt.exists() and not custom_cb_key.exists():
            setup_crts(ca_common_name=self.settings.get("installer-settings.couchbase.commonName"),
                       cert_common_name="couchbase-server",
                       san_list=self.settings.get("installer-settings.couchbase.subjectAlternativeName"),
                       ca_cert_file="./couchbase_crts_keys/ca.crt",
                       ca_key_file="./couchbase_crts_keys/ca.key",
                       cert_file="./couchbase_crts_keys/chain.pem",
                       key_file="./couchbase_crts_keys/pkey.key")
        labels = {"app": "gluu-couchbase"}
        if self.settings.get("global.istio.enabled"):
            labels = {"app": "couchbase", "istio-injection": "enabled"}
        self.kubernetes.create_namespace(name=cb_namespace, labels=labels)
        chain_pem_filepath = Path("./couchbase_crts_keys/chain.pem")
        pkey_filepath = Path("./couchbase_crts_keys/pkey.key")
        tls_cert_filepath = Path("./couchbase_crts_keys/tls-cert-file")
        tls_private_key_filepath = Path("./couchbase_crts_keys/tls-private-key-file")
        ca_cert_filepath = Path("./couchbase_crts_keys/ca.crt")
        shutil.copyfile(ca_cert_filepath, Path("./couchbase_crts_keys/couchbase.crt"))
        shutil.copyfile(chain_pem_filepath, tls_cert_filepath)
        shutil.copyfile(pkey_filepath, tls_private_key_filepath)

        encoded_ca_crt_string = self.settings.get("config.configmap.cnCouchbaseCrt")
        if encoded_ca_crt_string in (None, ''):
            with open(ca_cert_filepath) as content_file:
                ca_crt_content = content_file.read()
                encoded_ca_crt_bytes = base64.b64encode(ca_crt_content.encode("utf-8"))
                encoded_ca_crt_string = str(encoded_ca_crt_bytes, "utf-8")
            self.settings.set("config.configmap.cnCouchbaseCrt", encoded_ca_crt_string)

        with open(chain_pem_filepath) as content_file:
            chain_pem_content = content_file.read()
            encoded_chain_bytes = base64.b64encode(chain_pem_content.encode("utf-8"))
            encoded_chain_string = str(encoded_chain_bytes, "utf-8")

        with open(pkey_filepath) as content_file:
            pkey_content = content_file.read()
            encoded_pkey_bytes = base64.b64encode(pkey_content.encode("utf-8"))
            encoded_pkey_string = str(encoded_pkey_bytes, "utf-8")

        self.kubernetes.patch_or_create_namespaced_secret(name="couchbase-server-tls",
                                                          namespace=cb_namespace,
                                                          literal=chain_pem_filepath.name,
                                                          value_of_literal=encoded_chain_string,
                                                          second_literal=pkey_filepath.name,
                                                          value_of_second_literal=encoded_pkey_string)
        self.kubernetes.patch_or_create_namespaced_secret(name="couchbase-operator-tls",
                                                          namespace=cb_namespace,
                                                          literal=ca_cert_filepath.name,
                                                          value_of_literal=encoded_ca_crt_string)

        encoded_cb_super_user_bytes = base64.b64encode(
            self.settings.get("config.configmap.cnCouchbaseSuperUser").encode("utf-8"))
        encoded_cb_super_user_string = str(encoded_cb_super_user_bytes, "utf-8")
        encoded_cb_pass_bytes = base64.b64encode(
            self.settings.get("config.configmap.cnCouchbasePassword").encode("utf-8"))
        encoded_cb_pass_string = str(encoded_cb_pass_bytes, "utf-8")
        encoded_cb_super_pass_bytes = base64.b64encode(
            self.settings.get("config.configmap.cnCouchbaseSuperUserPassword").encode("utf-8"))
        encoded_cb_super_pass_string = str(encoded_cb_super_pass_bytes, "utf-8")

        self.create_couchbase_gluu_cert_pass_secrets(encoded_ca_crt_string, encoded_cb_pass_string,
                                                     encoded_cb_super_pass_string)
        self.kubernetes.patch_or_create_namespaced_secret(name="gluu-couchbase-user-password",
                                                          namespace=self.settings.get(
                                                              "installer-settings.couchbase.namespace"),
                                                          literal="password",
                                                          value_of_literal=encoded_cb_pass_string)

        admission_command = "./{}/bin/cbopcfg generate admission --namespace {}".format(self.couchbase_source_file,
                                                                                        self.settings.get(
                                                                                            "installer-settings.couchbase.namespace"))
        operator_command = "./{}/bin/cbopcfg generate operator --namespace {}".format(self.couchbase_source_file,
                                                                                      self.settings.get(
                                                                                          "installer-settings.couchbase.namespace"))
        backup_command = "./{}/bin/cbopcfg generate backup --namespace {}".format(self.couchbase_source_file,
                                                                                  self.settings.get(
                                                                                      "installer-settings.couchbase.namespace"))
        # @TODO: Remove condition and operator_command override after depreciation of couchbase operator 2.0
        if self.old_couchbase:
            operator_command = "./{}/bin/cbopcfg -backup=true -namespace={}".format(self.couchbase_source_file,
                                                                                    self.settings.get(
                                                                                        "installer-settings.couchbase.namespace"))
        exec_cmd(operator_command, output_file=self.couchbase_operator_dac_file)
        # @TODO: Remove only the condition after depreciation of couchbase operator 2.0
        if not self.old_couchbase:
            exec_cmd(backup_command, output_file=self.couchbase_operator_backup_file)
            exec_cmd(admission_command, output_file=self.couchbase_admission_file)

        couchbase_cluster_parser = Parser(self.couchbase_cluster_file, "CouchbaseCluster")
        couchbase_cluster_parser["spec"]["networking"]["tls"]["static"]["serverSecret"] = "couchbase-server-tls"
        couchbase_cluster_parser["spec"]["networking"]["tls"]["static"]["operatorSecret"] = "couchbase-operator-tls"
        if self.settings.get("global.istio.enabled"):
            couchbase_cluster_parser["spec"]["networking"]["networkPlatform"] = "Istio"
        try:
            couchbase_cluster_parser["spec"]["security"]["rbac"]["selector"]["matchLabels"]["cluster"] = \
                self.settings.get("installer-settings.couchbase.clusterName")
            couchbase_cluster_parser["spec"]["security"]["rbac"]["managed"] = True
        except KeyError:
            logger.error("rbac section is missing or incorrect in couchbase-cluster.yaml."
                         " Please set spec --> security --> rbac --> managed : true"
                         " and set spec --> security --> rbac --> selector --> matchLabels --> "
                         "cluster --> to your cluster name")
            logger.info("As a result of the above the installation will exit "
                        "as the gluu user will not be created causing the communication between "
                        "Gluu server and Couchbase to fail.")
            sys.exit()
        if "localOpenEbsHostPathDynamic" in self.settings.get("installer-settings.volumeProvisionStrategy"):
            volume_claims = couchbase_cluster_parser["spec"]["volumeClaimTemplates"]
            for i, volume_claim in enumerate(volume_claims):
                couchbase_cluster_parser["spec"]["volumeClaimTemplates"][i]["spec"]["storageClassName"] = \
                    "openebs-hostpath"
        couchbase_cluster_parser.dump_it()

        self.kubernetes.create_objects_from_dict(self.couchbase_custom_resource_definition_file,
                                                 namespace=cb_namespace)

        self.kubernetes.create_objects_from_dict(self.couchbase_operator_dac_file,
                                                 namespace=cb_namespace)
        # @TODO: Remove only the condition after depreciation of couchbase operator 2.0
        if not self.old_couchbase:
            self.kubernetes.create_objects_from_dict(self.couchbase_admission_file,
                                                     namespace=cb_namespace)

            self.kubernetes.create_objects_from_dict(self.couchbase_operator_backup_file,
                                                     namespace=cb_namespace)

        self.kubernetes.check_pods_statuses(cb_namespace, "app=couchbase-operator", 700)

        self.kubernetes.patch_or_create_namespaced_secret(name="cb-auth",
                                                          namespace=cb_namespace,
                                                          literal="username",
                                                          value_of_literal=encoded_cb_super_user_string,
                                                          second_literal="password",
                                                          value_of_second_literal=encoded_cb_super_pass_string)

        self.kubernetes.create_objects_from_dict(self.storage_class_file, namespace=cb_namespace)
        self.kubernetes.create_namespaced_custom_object(filepath=self.couchbase_cluster_file,
                                                        group="couchbase.com",
                                                        version="v2",
                                                        plural="couchbaseclusters",
                                                        namespace=cb_namespace)
        self.kubernetes.create_namespaced_custom_object(filepath=self.couchbase_buckets_file,
                                                        group="couchbase.com",
                                                        version="v2",
                                                        plural="couchbasebuckets",
                                                        namespace=cb_namespace)
        self.kubernetes.create_namespaced_custom_object(filepath=self.couchbase_ephemeral_buckets_file,
                                                        group="couchbase.com",
                                                        version="v2",
                                                        plural="couchbaseephemeralbuckets",
                                                        namespace=cb_namespace)
        coucbase_group_parser = Parser(self.couchbase_group_file, "CouchbaseGroup")
        coucbase_group_parser["metadata"]["labels"]["cluster"] = \
            self.settings.get("installer-settings.couchbase.clusterName")
        permissions = ["query_select", "query_update", "query_insert", "query_delete"]
        allbuckets = ["", "site", "user", "cache", "token", "session"]
        roles = []
        for permission in permissions:
            for bucket in allbuckets:
                bucket_name = self.settings.get("config.configmap.cnCouchbaseBucketPrefix")
                if bucket:
                    bucket_name = bucket_name + "_" + bucket
                roles.append({"name": permission, "bucket": bucket_name})
        coucbase_group_parser["spec"]["roles"] = roles
        coucbase_group_parser.dump_it()
        coucbase_user_parser = Parser(self.couchbase_user_file, "CouchbaseUser")
        coucbase_user_parser["metadata"]["labels"]["cluster"] = \
            self.settings.get("installer-settings.couchbase.clusterName")
        coucbase_user_parser.dump_it()
        self.kubernetes.create_namespaced_custom_object(filepath=self.couchbase_group_file,
                                                        group="couchbase.com",
                                                        version="v2",
                                                        plural="couchbasegroups",
                                                        namespace=cb_namespace)
        self.kubernetes.create_namespaced_custom_object(filepath=self.couchbase_user_file,
                                                        group="couchbase.com",
                                                        version="v2",
                                                        plural="couchbaseusers",
                                                        namespace=cb_namespace)
        self.kubernetes.create_namespaced_custom_object(filepath=self.couchbase_rolebinding_file,
                                                        group="couchbase.com",
                                                        version="v2",
                                                        plural="couchbaserolebindings",
                                                        namespace=cb_namespace)
        self.kubernetes.check_pods_statuses(cb_namespace, "couchbase_service_analytics=enabled", 700)
        self.kubernetes.check_pods_statuses(cb_namespace, "couchbase_service_data=enabled", 700)
        self.kubernetes.check_pods_statuses(cb_namespace, "couchbase_service_eventing=enabled", 700)
        self.kubernetes.check_pods_statuses(cb_namespace, "couchbase_service_index=enabled", 700)
        self.kubernetes.check_pods_statuses(cb_namespace, "couchbase_service_query=enabled", 700)
        self.kubernetes.check_pods_statuses(cb_namespace, "couchbase_service_search=enabled", 700)
        # Setup couchbase backups
        if self.settings.get("global.storageClass.provisioner") not in ("microk8s.io/hostpath",
                                                                        "k8s.io/minikube-hostpath"):
            self.setup_backup_couchbase()
        shutil.rmtree(self.couchbase_source_folder_pattern, ignore_errors=True)

    def uninstall(self):
        """
        Uninstalls couchbase
        """
        logger.info("Deleting Couchbase...")
        self.kubernetes.delete_storage_class("couchbase-sc")
        self.kubernetes.delete_custom_resource("couchbaseclusters.couchbase.com")
        self.kubernetes.delete_validating_webhook_configuration("couchbase-operator-admission")
        self.kubernetes.delete_mutating_webhook_configuration("couchbase-operator-admission")
        self.kubernetes.delete_cluster_role_binding("couchbase-operator-admission")
        self.kubernetes.delete_cluster_role("couchbase-operator-admission")
        self.kubernetes.delete_role("couchbase-operator", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_secret("cb-auth", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_secret("gluu-couchbase-user-password", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_deployment_using_name("couchbase-operator", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_role_binding("couchbase-operator", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_service_account("couchbase-operator", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_service("couchbase-operator-admission", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_deployment_using_name("couchbase-operator-admission",
                                                     self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_service("couchbase-operator", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_custom_resource("couchbasebackuprestores.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbasebackups.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbasebuckets.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbaseephemeralbuckets.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbasereplications.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbaserolebindings.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbasegroups.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbasememcachedbuckets.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbaseusers.couchbase.com")
        self.kubernetes.delete_custom_resource("couchbaseautoscalers.couchbase.com")

        self.kubernetes.delete_service_account("couchbase-operator-admission",
                                               self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_secret("couchbase-operator-admission", self.settings.get("installer-settings.couchbase.namespace"))
        self.kubernetes.delete_secret("couchbase-operator-tls", self.settings.get("installer-settings.couchbase.namespace"))
        shutil.rmtree(Path("./couchbase-source-folder"), ignore_errors=True)
