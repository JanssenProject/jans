"""
pygluu.kubernetes.redis
~~~~~~~~~~~~~~~~~~~~~~~

 License terms and conditions for Gluu Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
 Handles Redis installation for testing.
"""

from pygluu.kubernetes.helpers import get_logger, exec_cmd
from pygluu.kubernetes.kubeapi import Kubernetes
from pygluu.kubernetes.settings import ValuesHandler


logger = get_logger("gluu-redis         ")


class Redis(object):
    def __init__(self):
        self.settings = ValuesHandler()
        self.kubernetes = Kubernetes()
        self.timeout = 120
        if "gke" in self.settings.get("installer-settings.volumeProvisionStrategy"):
            user_account, stderr, retcode = exec_cmd("gcloud config get-value core/account")
            user_account = str(user_account, "utf-8").strip()

            user, stderr, retcode = exec_cmd("whoami")
            user = str(user, "utf-8").strip()
            cluster_role_binding_name = "cluster-admin-{}".format(user)
            self.kubernetes.create_cluster_role_binding(cluster_role_binding_name=cluster_role_binding_name,
                                                        user_name=user_account,
                                                        cluster_role_name="cluster-admin")

    def install_redis(self):
        self.uninstall_redis()
        self.kubernetes.create_namespace(name=self.settings.get("installer-settings.redis.namespace"),
                                         labels={"app": "redis"})
        exec_cmd("helm repo add bitnami https://charts.bitnami.com/bitnami")
        exec_cmd("helm repo update")
        exec_cmd("helm install {} bitnami/redis-cluster "
                 "--set global.redis.password={} "
                 "--namespace={}".format("redis-cluster",
                                         self.settings.get("config.redisPassword"),
                                         self.settings.get("installer-settings.redis.namespace")))

        if not self.settings.get("installer-settings.aws.lbType") == "alb":
            self.kubernetes.check_pods_statuses(self.settings.get("installer-settings.namespace"), "app=redis-cluster",
                                                self.timeout)

    def uninstall_redis(self):
        logger.info("Removing gluu-redis-cluster...")
        logger.info("Removing redis...")
        exec_cmd("helm delete {} --namespace={}".format("redis-cluster",
                                                        self.settings.get("installer-settings.redis.namespace")))
