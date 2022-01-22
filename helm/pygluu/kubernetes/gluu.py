"""
pygluu.kubernetes.helm
~~~~~~~~~~~~~~~~~~~~~~

 License terms and conditions for Gluu Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
 Handles Helm Gluu Chart
"""

from pathlib import Path
from pygluu.kubernetes.yamlparser import Parser
from pygluu.kubernetes.helpers import get_logger, exec_cmd
from pygluu.kubernetes.kubeapi import Kubernetes
from pygluu.kubernetes.couchbase import Couchbase
from pygluu.kubernetes.settings import ValuesHandler
import time
import socket

logger = get_logger("gluu-helm          ")


class Gluu(object):
    def __init__(self):
        self.values_file = Path("./helm/gluu/override-values.yaml").resolve()
        self.upgrade_values_file = Path("./helm/gluu-upgrade/values.yaml").resolve()
        self.settings = ValuesHandler()
        self.kubernetes = Kubernetes()
        self.ldap_backup_release_name = self.settings.get("installer-settings.releaseName") + "-ldap-backup"
        if "gke" in self.settings.get("installer-settings.volumeProvisionStrategy"):
            # Clusterrolebinding needs to be created for gke with CB installed
            if self.settings.get("config.configmap.cnCacheType") == "REDIS" or \
                    self.settings.get("installer-settings.couchbase.install"):
                user_account, stderr, retcode = exec_cmd("gcloud config get-value core/account")
                user_account = str(user_account, "utf-8").strip()

                user, stderr, retcode = exec_cmd("whoami")
                user = str(user, "utf-8").strip()
                cluster_role_binding_name = "cluster-admin-{}".format(user)
                self.kubernetes.create_cluster_role_binding(cluster_role_binding_name=cluster_role_binding_name,
                                                            user_name=user_account,
                                                            cluster_role_name="cluster-admin")

    def prepare_alb(self):
        ingress_parser = Parser("./alb/ingress.yaml", "Ingress")
        ingress_parser["spec"]["rules"][0]["host"] = self.settings.get("global.fqdn")
        ingress_parser["metadata"]["annotations"]["alb.ingress.kubernetes.io/certificate-arn"] = \
            self.settings.get("installer-settings.aws.arn.arnAcmCert")
        if not self.settings.get("installer-settings.aws.arn.enabled"):
            del ingress_parser["metadata"]["annotations"]["alb.ingress.kubernetes.io/certificate-arn"]

        for path in ingress_parser["spec"]["rules"][0]["http"]["paths"]:
            service_name = path["backend"]["serviceName"]
            if self.settings.get("config.configmap.cnCasaEnabled") and service_name == "casa":
                path_index = ingress_parser["spec"]["rules"][0]["http"]["paths"].index(path)
                del ingress_parser["spec"]["rules"][0]["http"]["paths"][path_index]

            if self.settings.get("global.oxshibboleth.enabled") and service_name == "oxshibboleth":
                path_index = ingress_parser["spec"]["rules"][0]["http"]["paths"].index(path)
                del ingress_parser["spec"]["rules"][0]["http"]["paths"][path_index]

            if self.settings.get("config.configmap.cnPassportEnabled") and service_name == "oxpassport":
                path_index = ingress_parser["spec"]["rules"][0]["http"]["paths"].index(path)
                del ingress_parser["spec"]["rules"][0]["http"]["paths"][path_index]

            if self.settings.get("installer-settings.global.scim.enabled") and service_name == "jans-scim":
                path_index = ingress_parser["spec"]["rules"][0]["http"]["paths"].index(path)
                del ingress_parser["spec"]["rules"][0]["http"]["paths"][path_index]

            if self.settings.get("installer-settings.config-api.enabled") and service_name == "config-api":
                path_index = ingress_parser["spec"]["rules"][0]["http"]["paths"].index(path)
                del ingress_parser["spec"]["rules"][0]["http"]["paths"][path_index]

        ingress_parser.dump_it()

    def deploy_alb(self):
        alb_ingress = Path("./alb/ingress.yaml")
        self.kubernetes.create_objects_from_dict(alb_ingress, self.settings.get("installer-settings.namespace"))
        if self.settings.get("global.fqdn"):
            prompt = input("Please input the DNS of the Application load balancer  found on AWS UI: ")
            lb_hostname = prompt
            while True:
                try:
                    if lb_hostname:
                        break
                    lb_hostname = self.kubernetes.read_namespaced_ingress(
                        name="gluu", namespace="gluu").status.load_balancer.ingress[0].hostname
                except TypeError:
                    logger.info("Waiting for loadbalancer address..")
                    time.sleep(10)
            self.settings.set("config.configmap.lbAddr", lb_hostname)

    def wait_for_nginx_add(self):
        hostname_ip = None
        while True:
            try:
                if hostname_ip:
                    break
                if "aws" in self.settings.get("installer-settings.volumeProvisionStrategy"):
                    hostname_ip = self.kubernetes.read_namespaced_service(
                        name=self.settings.get(
                            'installer-settings.nginxIngress.releaseName') + "-ingress-nginx-controller",
                        namespace=self.settings.get(
                            "installer-settings.nginxIngress.releaseName")).status.load_balancer.ingress[
                        0].hostname
                    self.settings.set("config.configmap.lbAddr", hostname_ip)
                    if self.settings.get("installer-settings.aws.lbType") == "nlb":
                        try:
                            ip_static = socket.gethostbyname(str(hostname_ip))
                            if ip_static:
                                break
                        except socket.gaierror:
                            logger.info("Address has not received an ip yet.")
                elif "local" in self.settings.get("installer-settings.volumeProvisionStrategy"):
                    self.settings.set("config.configmap.lbAddr",
                                      self.settings.get('installer-settings.nginxIngress.releaseName') +
                                      "-nginx-ingress-controller." +
                                      self.settings.get("installer-settings.nginxIngress.releaseName") +
                                      ".svc.cluster.local")
                    break
                else:
                    hostname_ip = self.kubernetes.read_namespaced_service(
                        name=self.settings.get('installer-settings.nginxIngress.releaseName') + "-ingress-nginx-controller",
                        namespace=self.settings.get("installer-settings.nginxIngress.releaseName")).status.load_balancer.ingress[0].ip
                    self.settings.set("global.lbIp", hostname_ip)
            except (TypeError, AttributeError):
                logger.info("Waiting for address..")
                time.sleep(10)

    def check_install_nginx_ingress(self, install_ingress=True):
        """
        Helm installs nginx ingress or checks to recieve and ip or address
        :param install_ingress:
        """
        if install_ingress:
            self.kubernetes.delete_custom_resource("virtualservers.k8s.nginx.org")
            self.kubernetes.delete_custom_resource("virtualserverroutes.k8s.nginx.org")
            self.kubernetes.delete_cluster_role("ingress-nginx-nginx-ingress")
            self.kubernetes.delete_cluster_role_binding("ingress-nginx-nginx-ingress")
            self.kubernetes.create_namespace(name=self.settings.get("installer-settings.nginxIngress.releaseName"),
                                             labels={"app": "ingress-nginx"})
            self.kubernetes.delete_cluster_role(
                self.settings.get('installer-settings.nginxIngress.releaseName') + "-nginx-ingress-controller")
            self.kubernetes.delete_cluster_role_binding(
                self.settings.get('installer-settings.nginxIngress.releaseName') + "-nginx-ingress-controller")
            try:
                exec_cmd("helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx")
                exec_cmd("helm repo add stable https://charts.helm.sh/stable")
                exec_cmd("helm repo update")
            except FileNotFoundError:
                logger.error("Helm v3 is not installed. Please install it to continue "
                             "https://helm.sh/docs/intro/install/")
                raise SystemExit(1)
        command = "helm install {} ingress-nginx/ingress-nginx --namespace={} ".format(
            self.settings.get('installer-settings.nginxIngress.releaseName'),
            self.settings.get("installer-settings.nginxIngress.namespace"))
        if self.settings.get("installer-settings.volumeProvisionStrategy") == "minikubeDynamic":
            exec_cmd("minikube addons enable ingress")
        if "aws" in self.settings.get("installer-settings.volumeProvisionStrategy"):
            if self.settings.get("installer-settings.aws.lbType") == "nlb":
                if install_ingress:
                    nlb_override_values_file = Path("./nginx/aws/aws-nlb-override-values.yaml").resolve()
                    nlb_values = " --values {}".format(nlb_override_values_file)
                    exec_cmd(command + nlb_values)
            else:
                if self.settings.get("installer-settings.aws.arn.enabled"):
                    if install_ingress:
                        elb_override_values_file = Path("./nginx/aws/aws-elb-override-values.yaml").resolve()
                        elb_file_parser = Parser(elb_override_values_file, True)
                        elb_file_parser["controller"]["service"]["annotations"].update(
                            {"service.beta.kubernetes.io/aws-load-balancer-ssl-cert": self.settings.get(
                                "installer-settings.aws.arn.arnAcmCert")})
                        elb_file_parser["controller"]["config"]["proxy-real-ip-cidr"] = \
                            self.settings.get("installer-settings.aws.vpcCidr")
                        elb_file_parser.dump_it()
                        elb_values = " --values {}".format(elb_override_values_file)
                        exec_cmd(command + elb_values)
                else:
                    if install_ingress:
                        exec_cmd(command)
        volume_provision_strategy = self.settings.get("installer-settings.volumeProvisionStrategy")
        if "gke" in volume_provision_strategy or \
                "aks" in volume_provision_strategy or \
                "doks" in volume_provision_strategy:
            if install_ingress:
                cloud_override_values_file = Path("./nginx/cloud/cloud-override-values.yaml").resolve()
                cloud_values = " --values {}".format(cloud_override_values_file)
                exec_cmd(command + cloud_values)
        elif "local" in volume_provision_strategy:
            if install_ingress:
                baremetal_override_values_file = Path("./nginx/baremetal/baremetal-override-values.yaml").resolve()
                baremetal_values = " --values {}".format(baremetal_override_values_file)
                exec_cmd(command + baremetal_values)
        if self.settings.get("global.storageClass.provisioner") not in \
                ("microk8s.io/hostpath", "k8s.io/minikube-hostpath"):
            logger.info("Waiting for nginx to be prepared...")
            time.sleep(60)
            self.wait_for_nginx_add()

    def install_gluu(self, install_ingress=True):
        """
        Helm install Gluu
        :param install_ingress:
        """
        labels = {"app": "gluu"}
        if self.settings.get("global.istio.enabled"):
            labels = {"app": "gluu", "istio-injection": "enabled"}
        self.kubernetes.create_namespace(name=self.settings.get("installer-settings.namespace"), labels=labels)
        if self.settings.get("global.cnPersistenceType") != "ldap" and \
                self.settings.get("installer-settings.couchbase.install"):
            couchbase_app = Couchbase()
            couchbase_app.uninstall()
            couchbase_app = Couchbase()
            couchbase_app.install()
            self.settings = ValuesHandler()
        if self.settings.get("installer-settings.aws.lbType") == "alb":
            self.prepare_alb()
            self.deploy_alb()
        if self.settings.get("installer-settings.aws.lbType") != "alb" and \
                self.settings.get("global.istio.ingress"):
            self.check_install_nginx_ingress(install_ingress)
        try:
            exec_cmd("helm install {} -f {} ./helm/gluu --namespace={}".format(
                self.settings.get('installer-settings.releaseName'),
                self.values_file, self.settings.get("installer-settings.namespace")))

            if self.settings.get("global.cnPersistenceType") in ("hybrid", "ldap"):
                self.install_ldap_backup()

        except FileNotFoundError:
            logger.error("Helm v3 is not installed. Please install it to continue "
                         "https://helm.sh/docs/intro/install/")
            raise SystemExit(1)

    def install_ldap_backup(self):
        values_file = Path("./helm/ldap-backup/values.yaml").resolve()
        values_file_parser = Parser(values_file, True)
        values_file_parser["ldapPass"] = self.settings.get("config.ldapPassword")
        if self.settings.get("global.storageClass.provisioner") not in \
                ("microk8s.io/hostpath", "k8s.io/minikube-hostpath"):
            values_file_parser["gluuLdapSchedule"] = self.settings.get("installer-settings.ldap.backup.fullSchedule")
        if self.settings.get("opendj.multiCluster.enabled"):
            values_file_parser["multiCluster"]["enabled"] = True
            values_file_parser["multiCluster"]["ldapAdvertiseAdminPort"] = \
                self.settings.get("opendj.ports.tcp-admin.nodePort")
            values_file_parser["multiCluster"]["serfAdvertiseAddrSuffix"] = \
                self.settings.get("opendj.multiCluster.serfAdvertiseAddrSuffix")[:-6]
        values_file_parser.dump_it()
        exec_cmd("helm install {} -f ./helm/ldap-backup/values.yaml ./helm/ldap-backup --namespace={}".format(
            self.ldap_backup_release_name, self.settings.get("installer-settings.namespace")))

    def upgrade_gluu(self):
        values_file_parser = Parser(self.upgrade_values_file, True)
        values_file_parser["domain"] = self.settings.get("global.fqdn")
        values_file_parser["cnCacheType"] = self.settings.get("config.configmap.cnCacheType")
        values_file_parser["cnCouchbaseUrl"] = self.settings.get("config.configmap.cnCouchbaseUrl")
        values_file_parser["cnCouchbaseUser"] = self.settings.get("config.configmap.cnCouchbaseUser")
        values_file_parser["cnCouchbaseSuperUser"] = self.settings.get("config.configmap.cnCouchbaseSuperUser")
        values_file_parser["cnPersistenceLdapMapping"] = self.settings.get("global.cnPersistenceType")
        values_file_parser["cnPersistenceType"] = self.settings.get("config.configmap.cnPersistenceLdapMapping")
        values_file_parser["source"] = self.settings.get("installer-settings.currentVersion")
        values_file_parser["target"] = self.settings.get("installer-settings.upgrade.targetVersion")
        values_file_parser.dump_it()
        exec_cmd("helm install {} -f {} ./helm/gluu-upgrade --namespace={}".format(
            self.settings.get('installer-settings.releaseName'), self.values_file,
            self.settings.get("installer-settings.namespace")))

    def uninstall_gluu(self):
        exec_cmd("helm delete {} --namespace={}".format(self.settings.get('installer-settings.releaseName'),
                                                        self.settings.get("installer-settings.namespace")))
        exec_cmd("helm delete {} --namespace={}".format(self.ldap_backup_release_name,
                                                        self.settings.get("installer-settings.namespace")))

    def uninstall_nginx_ingress(self):
        exec_cmd("helm delete {} --namespace={}".format(self.settings.get('installer-settings.nginxIngress.releaseName'),
                                                        self.settings.get("installer-settings.nginxIngress.namespace")))
