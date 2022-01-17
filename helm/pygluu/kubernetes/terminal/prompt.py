"""
pygluu.kubernetes.terminal.prompt
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to initialize all terminal prompts to
interact with user's inputs for terminal installations.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""

from pygluu.kubernetes.settings import ValuesHandler
from pygluu.kubernetes.terminal.confirmsettings import PromptConfirmSettings
from pygluu.kubernetes.terminal.volumes import PromptVolumes
from pygluu.kubernetes.terminal.configuration import PromptConfiguration
from pygluu.kubernetes.terminal.jackrabbit import PromptJackrabbit
from pygluu.kubernetes.terminal.istio import PromptIstio
from pygluu.kubernetes.terminal.replicas import PromptReplicas
from pygluu.kubernetes.terminal.couchbase import PromptCouchbase
from pygluu.kubernetes.terminal.architecture import PromptArch
from pygluu.kubernetes.terminal.namespace import PromptNamespace
from pygluu.kubernetes.terminal.optionalservices import PromptOptionalServices
from pygluu.kubernetes.terminal.testenv import PromptTestEnvironment
from pygluu.kubernetes.terminal.aws import PromptAws
from pygluu.kubernetes.terminal.helpers import gather_ip
from pygluu.kubernetes.terminal.persistencebackend import PromptPersistenceBackend
from pygluu.kubernetes.terminal.ldap import PromptLdap
from pygluu.kubernetes.terminal.images import PromptImages
from pygluu.kubernetes.terminal.cache import PromptCache
from pygluu.kubernetes.terminal.backup import PromptBackup
from pygluu.kubernetes.terminal.license import PromptLicense
from pygluu.kubernetes.terminal.version import PromptVersion
from pygluu.kubernetes.terminal.sql import PromptSQL
from pygluu.kubernetes.terminal.google import PromptGoogle
from pygluu.kubernetes.terminal.openbanking import PromptOpenBanking
from pygluu.kubernetes.terminal.distribution import PromptDistribution
from pygluu.kubernetes.terminal.helm import PromptHelm
from pathlib import Path


class Prompt:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self):
        self.settings = ValuesHandler()

    def load_settings(self):
        self.settings = ValuesHandler()
        self.settings.store_override_file()

    def license(self):
        self.load_settings()
        PromptLicense(self.settings)

    def versions(self):
        self.load_settings()
        PromptVersion(self.settings)

    def arch(self):
        self.load_settings()
        arch = PromptArch(self.settings)
        arch.prompt_arch()

    def namespace(self):
        self.load_settings()
        namespace = PromptNamespace(self.settings)
        namespace.prompt_gluu_namespace()

    def optional_services(self):
        self.load_settings()
        optional_services = PromptOptionalServices(self.settings)
        optional_services.prompt_optional_services()

    def jackrabbit(self):
        self.load_settings()
        jackrabbit = PromptJackrabbit(self.settings)
        jackrabbit.prompt_jackrabbit()

    def istio(self):
        self.load_settings()
        istio = PromptIstio(self.settings)
        istio.prompt_istio()

    def test_enviornment(self):
        self.load_settings()
        test_environment = PromptTestEnvironment(self.settings)
        if not self.settings.get("global.cloud.testEnviroment") and \
                self.settings.get("global.storageClass.provisioner") not in ("microk8s.io/hostpath",
                                                                             "k8s.io/minikube-hostpath"):
            test_environment.prompt_test_environment()

    def network(self):
        if self.settings.get("global.lbIp") in ('None', ''):
            ip = gather_ip()
            self.load_settings()
            self.settings.set("global.lbIp", ip)

            if "aws" in self.settings.get("installer-settings.volumeProvisionStrategy") and \
                    not self.settings.get("global.istio.enabled"):
                aws = PromptAws(self.settings)
                aws.prompt_aws_lb()

    def persistence_backend(self):
        self.load_settings()
        persistence_backend = PromptPersistenceBackend(self.settings)
        persistence_backend.prompt_persistence_backend()

    def ldap(self):
        self.load_settings()
        if self.settings.get("global.cnPersistenceType") == "hybrid":
            ldap = PromptLdap(self.settings)
            ldap.prompt_hybrid_ldap_held_data()

    def volumes(self):
        self.load_settings()
        volumes = PromptVolumes(self.settings)
        if self.settings.get("global.cnPersistenceType") in ("hybrid", "ldap") or \
                self.settings.get("global.jackrabbit.enabled"):
            volumes.prompt_volumes()
        volumes.prompt_storage()

    def couchbase(self):
        self.load_settings()
        couchbase = PromptCouchbase(self.settings)
        if self.settings.get("global.cnPersistenceType") in ("hybrid", "couchbase"):
            couchbase.prompt_couchbase()

    def cache(self):
        self.load_settings()
        cache = PromptCache(self.settings)
        cache.prompt_cache_type()

    def backup(self):
        self.load_settings()
        if self.settings.get("global.storageClass.provisioner") not in ("microk8s.io/hostpath",
                                                                        "k8s.io/minikube-hostpath"):
            backup = PromptBackup(self.settings)
            backup.prompt_backup()

    def configuration(self):
        self.load_settings()
        configuration = PromptConfiguration(self.settings)
        configuration.prompt_config()

    def images(self):
        self.load_settings()
        images = PromptImages(self.settings)
        images.prompt_image_name_tag()

    def replicas(self):
        self.load_settings()
        replicas = PromptReplicas(self.settings)
        replicas.prompt_replicas()

    def distribution(self):
        self.load_settings()
        dist = PromptDistribution(self.settings)
        dist.prompt_distribution()

    def helm(self):
        self.load_settings()
        helm = PromptHelm(self.settings)
        helm.prompt_helm()

    def openbanking(self):
        self.load_settings()
        if self.settings.get("global.distribution") == "openbanking":
            # Disable all optional services from openbanking distribution
            self.settings.set("global.cr-rotate.enabled", False)
            self.settings.set("global.auth-server-key-rotation.enabled", False)
            self.settings.set("config.configmap.cnPassportEnabled", False)
            self.settings.set("global.oxshibboleth.enabled", False)
            self.settings.set("config.configmap.cnCasaEnabled", False)
            self.settings.set("global.client-api.enabled", False)
            self.settings.set("global.fido2.enabled", False)
            self.settings.set("global.scim.enabled", False)
            self.settings.set("installer-settings.volumeProvisionStrategy", "microk8sDynamic")
            # Jackrabbit might be enabled for this distribution later
            self.settings.set("global.jackrabbit.enabled", False)
            ob = PromptOpenBanking(self.settings)
            ob.prompt_openbanking()

    def sql(self):
        self.load_settings()
        if self.settings.get("global.cnPersistenceType") == "sql":
            spanner = PromptSQL(self.settings)
            spanner.prompt_sql()

    def google(self):
        self.load_settings()
        if self.settings.get("global.cnPersistenceType") == "spanner":
            spanner = PromptGoogle(self.settings)
            spanner.prompt_google()

    def confirm_settings(self):
        self.load_settings()
        if not self.settings.get("installer-settings.confirmSettings"):
            confirm_settings = PromptConfirmSettings(self.settings)
            confirm_settings.confirm_params()

    def prompt(self):
        """Main property: called to setup all prompts and returns prompts in settings file.

        :return:
        """
        # Check if override file by customer exists if not empty the new one
        if not Path("./override-values.yaml").exists():
            self.settings.reset_data()
        self.license()
        self.versions()
        self.arch()
        self.distribution()
        self.openbanking()
        self.namespace()
        self.optional_services()
        if self.settings.get("global.distribution") != "openbanking":
            self.jackrabbit()
        self.istio()
        self.test_enviornment()
        self.network()
        self.persistence_backend()
        self.ldap()
        if self.settings.get("global.distribution") != "openbanking":
            self.volumes()
        self.sql()
        self.google()
        self.couchbase()
        self.cache()
        self.backup()
        self.configuration()
        self.images()
        self.replicas()
        self.helm()
        self.confirm_settings()
        self.settings.remove_empty_keys()
