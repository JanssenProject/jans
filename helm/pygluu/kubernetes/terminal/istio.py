"""
pygluu.kubernetes.terminal.istio
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module contains helpers to interact with user's inputs for istio terminal prompts.

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import click
from pygluu.kubernetes.helpers import get_logger

logger = get_logger("gluu-prompt-istio  ")


class PromptIstio:
    """Prompt is used for prompting users for input used in deploying Gluu.
    """

    def __init__(self, settings):
        self.settings = settings

    def prompt_istio(self):
        """Prompt for Istio
        """
        if self.settings.get("global.istio.ingress") in (None, '') \
                and self.settings.get("global.storageClass.provisioner") not in \
                ("microk8s.io/hostpath", "k8s.io/minikube-hostpath"):
            self.settings.set("global.istio.ingress", click.confirm("[Alpha] Would you like to use "
                                                                    "Istio Ingress with Gluu ?"))
        if self.settings.get("global.istio.ingress"):
            self.settings.set("global.istio.enabled", True)

        if self.settings.get("global.istio.enabled") in (None, ''):
            logger.info("Please follow https://istio.io/latest/docs/ to learn more.")
            logger.info("Istio will auto inject side cars into all pods in Gluus namespace chosen. "
                        "The label istio-injection=enabled will be added to the namespace Gluu will be installed in "
                        "if the namespace does not exist. If it does please run "
                        "kubectl label namespace <namespace> istio-injection=enabled")
            self.settings.set("global.istio.enabled", click.confirm("[Alpha] Would you like to use Istio with Gluu ?"))

        if self.settings.get("global.istio.namespace") in (None, '') and self.settings.get("global.istio.enabled"):
            self.settings.set("global.istio.namespace", click.prompt("Istio namespace",
                                                                     default="istio-system"))

            if self.settings.get("config.configmap.lbAddr") in (None, ''):
                self.settings.set("config.configmap.lbAddr", click.prompt("Istio loadbalancer adderss(eks) or "
                                                                          "ip (gke, aks, digital ocean, local)", default=""))
