"""
pygluu.kubernetes.kubeapi
~~~~~~~~~~~~~~~~~~~~~~~~~

 License terms and conditions for Gluu Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
"""

import sys
import time

from kubernetes import client, utils, config

from pygluu.kubernetes.helpers import get_logger, check_microk8s_kube_config_file, exec_cmd
from pygluu.kubernetes.yamlparser import Parser

logger = get_logger("gluu-kubernetes-api")


def load_kubernetes_config(mute=True):
    """
    Loads kubernetes in cluster or from file configuration
    :param mute:
    """
    config_loaded = False
    try:
        config.load_incluster_config()
        config_loaded = True
    except config.config_exception.ConfigException:
        if not mute:
            logger.warning("Unable to load in-cluster configuration; trying to load from Kube config file")
        try:
            config.load_kube_config()
            config_loaded = True
        except (IOError, config.config_exception.ConfigException) as exc:
            if not mute:
                logger.warning("Unable to load Kube config; reason={}".format(exc))

    if not config_loaded:
        logger.error("Unable to load in-cluster or Kube config")
        sys.exit(1)


class Kubernetes(object):
    def __init__(self):
        check_microk8s_kube_config_file()
        load_kubernetes_config()
        self.api_client = client.ApiClient()
        self.custom_def_cli = client.CustomObjectsApi()
        self.core_cli = client.CoreV1Api()
        self.apps_cli = client.AppsV1Api()
        self.rbac_cli = client.RbacAuthorizationV1Api()
        self.extenstion_cli = client.ExtensionsV1beta1Api()
        self.crd_cli = client.ApiextensionsV1beta1Api()
        self.storage_cli = client.StorageV1Api()
        self.admission_cli = client.AdmissionregistrationV1beta1Api()
        self.delete_options = client.V1DeleteOptions()
        self.delete_options.grace_period_seconds = 2
        self.delete_options.propagation_policy = 'Foreground'
        self.core_cli.api_client.configuration.assert_hostname = False
        self.apps_cli.api_client.configuration.assert_hostname = False

    @staticmethod
    def check_error_and_response(starting_time, resp):
        end_time = time.time()
        running_time = end_time - starting_time
        if resp.status != 404 and resp.status:
            logger.info("Waiting for the kubernetes object to be fully terminated.")
            time.sleep(1)
            if running_time > 60:
                logger.exception(resp)
                return False
            return True
        else:
            # The kubernetes object has been removed or does not exist"
            return False

    @staticmethod
    def check_create_error_and_response(e, kind, name):
        """Checking create error """
        error = str(e)
        if "AlreadyExists" in error or "409" in error:
            logger.warning("Resource {}/{} already exists. Skipping...".format(kind, name))
            pass
        elif "Unauthorized" in error or "401" in error:
            logger.error("Unauthorized code status 401 while trying to create {}/{}.".format(
                kind, name))
            pass
        elif "Not Found" in error or "404" in error:
            logger.error("Not found code status 404 while trying to create {}/{}. Trying again..".format(
                kind, name))
            pass
        else:
            raise e

    @staticmethod
    def check_read_error_and_response(starting_time, resp):
        end_time = time.time()
        running_time = end_time - starting_time
        if resp.status == 404 and not resp.status:
            logger.info("Resource not found. Trying to read again...")
            time.sleep(1)
            if running_time > 40:
                logger.exception(resp)
                return False
            return True
        else:
            # The kubernetes object has been found"
            return False

    def delete_validating_webhook_configuration(self, name):
        """Delete validating webhook configuration with name"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.admission_cli.delete_validating_webhook_configuration(name, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info("validatingwebhookconfiguration/{} has been removed or does not exist".format(name))

    def delete_mutating_webhook_configuration(self, name):
        """Delete mutating webhook configuration with name"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.admission_cli.delete_mutating_webhook_configuration(name, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info("mutatingwebhookconfiguration/{} has been removed or does not exist".format(name))

    def delete_service(self, name, namespace="default"):
        """Delete service with name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.core_cli.delete_namespaced_service(name=name, namespace=namespace, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info("service/{} from namespace/{} has been removed or does not exist".format(name, namespace))

    def delete_deployment_using_name(self, name, namespace="default"):
        """Delete deployment using name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.apps_cli.delete_namespaced_deployment(name, namespace, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('deployment/{} in namespace/{} has been removed or does not exist'.format(name, namespace))

    def delete_secret(self, name, namespace="default"):
        """Delete secret using name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.core_cli.delete_namespaced_secret(name, namespace, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('secret/{} from namespace/{} has been removed or does not exist'.format(name, namespace))

    def delete_role(self, name, namespace="default"):
        """Delete role using name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.rbac_cli.delete_namespaced_role(name, namespace, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('role/{} in namespace/{} has been removed or does not exist'.format(name, namespace))

    def delete_role_binding(self, name, namespace="default"):
        """Delete role binding using name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.rbac_cli.delete_namespaced_role_binding(name, namespace, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('rolebinding/{} from namespace/{} has been removed or does not exist'.format(name, namespace))

    def delete_cluster_role(self, name):
        """Delete cluster role using name"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.rbac_cli.delete_cluster_role(name, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('role/{} has been removed or does not exist'.format(name))

    def delete_cluster_role_binding(self, name):
        """Delete cluster role binding using name"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.rbac_cli.delete_cluster_role_binding(name, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('clusterrolebinding/{} has been removed or does not exist'.format(name))

    def delete_service_account(self, name, namespace="default"):
        """Delete service account using name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.core_cli.delete_namespaced_service_account(name, namespace, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('serviceaccount/{} in namespace/{} has been removed or does not exist'.format(name, namespace))

    def delete_custom_resource(self, name):
        """Delete custom resource using name"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.crd_cli.delete_custom_resource_definition(name, body=self.delete_options)

            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('customeresource/{} has been removed or does not exist'.format(name))

    def delete_storage_class(self, name):
        """Delete storage class using name"""
        starting_time = time.time()
        response = True
        while response:
            try:
                resp = self.storage_cli.delete_storage_class(name, body=self.delete_options)
            except client.rest.ApiException as e:
                response = self.check_error_and_response(starting_time, e)
            else:
                response = self.check_error_and_response(starting_time, resp)
        logger.info('storageclass/{} has been removed or does not exist'.format(name))

    def delete_namespaced_custom_object(self, filepath, group, version, plural, namespace="default"):
        """Delete custom object using file in namespace"""
        starting_time = time.time()
        response = True
        while response:
            yaml_objects = Parser(filepath).return_manifests_dict
            for manifest in yaml_objects:
                try:
                    resp = self.custom_def_cli.delete_namespaced_custom_object(group=group,
                                                                               version=version,
                                                                               namespace=namespace,
                                                                               plural=plural,
                                                                               name=manifest["metadata"]["name"],
                                                                               body=manifest)

                    logger.info('Deleted {}/{} in namespace  {}'.format(manifest["kind"],
                                                                        manifest["metadata"]["name"], namespace))
                except client.rest.ApiException as e:
                    response = self.check_error_and_response(starting_time, e)
                else:
                    response = self.check_error_and_response(starting_time, resp)

    def delete_namespaced_custom_object_by_name(self, group, version, plural, name, namespace="default"):
        """Delete custom object using name in namespace"""
        try:
            resp = self.custom_def_cli.delete_namespaced_custom_object(group=group,
                                                                       version=version,
                                                                       namespace=namespace,
                                                                       plural=plural,
                                                                       name=name,
                                                                       body=self.delete_options)
            logger.info('Deleted {} in namespace  {}'.format(name, namespace))
        except client.rest.ApiException as e:
            if e.status == 404:
                logger.info('{} in namespace  {} not found.'.format(name, namespace))
            else:
                logger.error(e, resp)

    def create_namespace(self, name, labels=None):
        """Create namespace using name"""
        labels = labels or {}
        body = client.V1Secret()
        metadata = client.V1ObjectMeta()
        metadata.name = name
        metadata.labels = labels
        body.metadata = metadata
        try:
            self.core_cli.create_namespace(body=body, pretty="pretty")
            logger.info('Created namespace {}'.format(name))
            return True
        except client.rest.ApiException as e:
            self.check_create_error_and_response(e, "Namespace", name)
            return False

    def create_cluster_role_binding(self, cluster_role_binding_name, user_name, cluster_role_name):
        """Create role binding using name=role_binding_name in namespace
        connecting role_name using service_account_name"""
        metadata = client.V1ObjectMeta(name=cluster_role_binding_name)
        role = client.V1RoleRef(kind="ClusterRole", name=cluster_role_name, api_group="rbac.authorization.k8s.io")
        subject = client.V1Subject(kind="User", name=user_name)
        body = client.V1ClusterRoleBinding(subjects=[subject], metadata=metadata, role_ref=role)

        try:
            self.rbac_cli.create_cluster_role_binding(body=body)
            logger.info('Created cluster role binding {}'.format(cluster_role_binding_name))
            return True
        except client.rest.ApiException as e:
            self.check_create_error_and_response(e, "ClusterRoleBinding", cluster_role_binding_name)
            return False

    def create_namespaced_custom_object(self, filepath, group, version, plural, namespace="default"):
        """Create custom object using file in namespace"""
        yaml_objects = Parser(filepath).return_manifests_dict
        for manifest in yaml_objects:
            try:
                self.custom_def_cli.create_namespaced_custom_object(group=group,
                                                                    version=version,
                                                                    namespace=namespace,
                                                                    plural=plural,
                                                                    body=manifest)

                logger.info('Created {}/{} in namespace  {}'.format(manifest["kind"],
                                                                    manifest["metadata"]["name"], namespace))
            except (client.rest.ApiException, Exception) as e:
                self.check_create_error_and_response(e, manifest["kind"], manifest["metadata"]["name"])

    def patch_or_create_namespaced_secret(self, name, literal, value_of_literal, namespace="default",
                                          secret_type="Opaque", second_literal=None, value_of_second_literal=None,
                                          data=None):
        """Patch secret and if not exist create"""
        # Instantiate the Secret object
        body = client.V1Secret()
        metadata = client.V1ObjectMeta(name=name)
        body.data = data
        if not data:
            body.data = {literal: value_of_literal}
        body.metadata = metadata
        body.type = secret_type
        if second_literal:
            body.data = {literal: value_of_literal, second_literal: value_of_second_literal}
        try:
            self.core_cli.patch_namespaced_secret(name, namespace, body)
            logger.info('Secret  {} in namespace {} has been patched'.format(name, namespace))
            return
        except client.rest.ApiException as e:
            if e.status == 404 or not e.status:
                try:
                    self.core_cli.create_namespaced_secret(namespace=namespace, body=body)
                    logger.info('Created secret {} of type {} in namespace {}'.format(name, secret_type, namespace))
                    return True
                except client.rest.ApiException as e:
                    logger.exception(e)
                    return False
            logger.exception(e)
            return False

    def create_objects_from_dict(self, filepath, namespace=None):
        """Create kubernetes object from a yaml encapsulated inside a dictionary"""
        yaml_objects = Parser(filepath).return_manifests_dict
        for manifest in yaml_objects:
            try:
                # handle special cases of namespace injection
                if namespace:
                    manifest["metadata"]["namespace"] = namespace
                utils.create_from_dict(self.api_client, manifest)
                logger.info('Created {}/{}'.format(manifest["kind"], manifest["metadata"]["name"]))
            except (client.rest.ApiException, Exception) as e:
                # AttributeError: module 'kubernetes.client' has no attribute 'NetworkingIstioIoV1alpha3Api'
                if "module 'kubernetes.client' has no attribute 'NetworkingIstioIoV1alpha3Api'" in str(e):
                    logger.warning("Creating {} failed.".format(manifest["kind"]))
                    logger.info("Trying again using kubectl...")
                    exec_cmd("kubectl apply -f {} -n {}".format(filepath, namespace))
                    break
                self.check_create_error_and_response(e, manifest["kind"], manifest["metadata"]["name"])

    def list_pod_name_by_label(self, namespace="default", app_label=None):
        """List pods names with app label in namespace"""
        try:
            pods_name = []
            response = self.core_cli.list_namespaced_pod(namespace=namespace, label_selector=app_label, watch=False)
            number_of_pods = len(response.items)
            for i in range(number_of_pods):
                pods_name.append(response.items[i].metadata.name)
            return pods_name
        except client.rest.ApiException as e:
            logger.exception(e)

    def read_namespaced_service(self, name, namespace="default"):
        """Read service with name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                service = self.core_cli.read_namespaced_service(name=name, namespace=namespace)
                logger.info('Reading service {}'.format(name))
                return service
            except client.rest.ApiException as e:
                response = self.check_read_error_and_response(starting_time, e)

    def read_namespaced_ingress(self, name, namespace="default"):
        """Read service with name in namespace"""
        starting_time = time.time()
        response = True
        while response:
            try:
                ingress = self.extenstion_cli.read_namespaced_ingress(name=name, namespace=namespace)
                logger.info('Reading ingress {}'.format(name))
                return ingress
            except client.rest.ApiException as e:
                response = self.check_read_error_and_response(starting_time, e)

    def read_namespaced_pod_status(self, name, timeout, namespace="default"):
        """Read pod status with name in namespace"""
        starting_time = time.time()
        try:
            finished_prep_boolean = False
            while not finished_prep_boolean:
                end_time = time.time()
                running_time = end_time - starting_time
                time.sleep(5)
                response = self.core_cli.read_namespaced_pod_status(name=name, namespace=namespace)
                all_statuses = response.status.conditions
                try:
                    for status in all_statuses:
                        if status.type == "Ready":
                            try:
                                check_if_job = response.metadata.labels["job-name"]
                            except KeyError:
                                check_if_job = None
                            if check_if_job and status.reason == "PodCompleted":
                                finished_prep_boolean = True
                                break
                            elif not check_if_job and status.status == "True":
                                finished_prep_boolean = True
                                break
                except TypeError:
                    logger.warning("Pod might not exist or was evicted.")
                if running_time > timeout:
                    logger.warning("Timeout exceeded. This may not be an error. Please check pods statuses.")
                    return False
                logger.info("Waiting for pod {} to get ready".format(name))
        except client.rest.ApiException as e:
            logger.exception(e)

    def check_pods_statuses(self, namespace="default", app_label=None, timeout=300):
        """Loop through pod names and check statuses"""
        time.sleep(10)
        pods_name = self.list_pod_name_by_label(namespace, app_label)
        for pod_name in pods_name:
            self.read_namespaced_pod_status(name=pod_name, namespace=namespace, timeout=timeout)

    def list_nodes(self):
        """List all nodes"""
        try:
            nodes_list = self.core_cli.list_node(pretty="pretty")
            logger.info("Getting list of nodes")
            return nodes_list
        except client.rest.ApiException as e:
            logger.exception(e)
            return False

    def read_node(self, name):
        """Read node information"""
        try:
            node_data = self.core_cli.read_node(name)
            logger.info("Getting node {} data".format(name))
            return node_data
        except client.rest.ApiException as e:
            logger.exception(e)
            return False
