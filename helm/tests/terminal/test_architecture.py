import pytest


@pytest.mark.parametrize("given, expected", [
    (1, "microk8s.io/hostpath"),
    (2, "k8s.io/minikube-hostpath"),
    (3, "kubernetes.io/aws-ebs"),
    (4, "kubernetes.io/gce-pd"),
    (5, "kubernetes.io/azure-disk"),
    (6, "dobs.csi.digitalocean.com"),
    (7, "openebs.io/local"),
    ("random", "microk8s.io/hostpath"),
])
def test_arch(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.architecture import PromptArch

    monkeypatch.setattr("click.prompt", lambda x, default: given)

    settings.set("global.storageClass.provisioner", "")
    prompt = PromptArch(settings)
    prompt.prompt_arch()
    assert settings.get("global.storageClass.provisioner") == expected
