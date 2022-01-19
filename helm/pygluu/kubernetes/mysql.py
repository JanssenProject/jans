"""
pygluu.kubernetes.mysql
~~~~~~~~~~~~~~~~~~~~~~~
 License terms and conditions for Gluu Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
 Handles  MySQL operations
"""

from pygluu.kubernetes.helpers import get_logger, exec_cmd
from pygluu.kubernetes.kubeapi import Kubernetes
from pygluu.kubernetes.settings import ValuesHandler

logger = get_logger("gluu-mysql         ")


class MySQL(object):
    def __init__(self):
        self.settings = ValuesHandler()
        self.kubernetes = Kubernetes()
        self.timeout = 120

    def install_mysql(self):
        self.uninstall_mysql()
        self.kubernetes.create_namespace(name=self.settings.get("installer-settings.sql.namespace"),
                                         labels={"app": "mysql"})

        exec_cmd("helm repo add bitnami https://charts.bitnami.com/bitnami")
        exec_cmd("helm repo update")
        exec_cmd("helm install {} bitnami/mysql "
                 "--set auth.rootPassword={} "
                 "--set auth.database={} "
                 "--set auth.username={} "
                 "--set auth.password={} "
                 "--namespace={} ".format("gluu",
                                          self.settings.get("config.configmap.cnSqldbUserPassword"),
                                          self.settings.get("config.configmap.cnSqlDbName"),
                                          self.settings.get("config.configmap.cnSqlDbUser"),
                                          self.settings.get("config.configmap.cnSqldbUserPassword"),
                                          self.settings.get("installer-settings.sql.namespace")))

        if not self.settings.get("installer-settings.aws.lbType") == "alb":
            self.kubernetes.check_pods_statuses(self.settings.get("installer-settings.sql.namespace"), "app=mysql",
                                                self.timeout)

    def uninstall_mysql(self):
        logger.info("Removing gluu-mysql...")
        logger.info("Removing mysql...")
        exec_cmd("helm delete {} --namespace={}".format("gluu",
                                                        self.settings.get("installer-settings.sql.namespace")))
