import pytest


@pytest.mark.parametrize("arch, vol_type", [
    ("kubernetes.io/aws-ebs", 7),
    ("kubernetes.io/gce-pd", 12),
    ("kubernetes.io/azure-disk", 17),
    ("dobs.csi.digitalocean.com", 22),
    ("openebs.io/local", 26),
])
def test_prompt_app_volume_type(monkeypatch, settings, arch, vol_type):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: vol_type)

    settings.set("global.cnPersistenceType", arch)
    settings.set("CN_APP_VOLUME_TYPE", vol_type)
    prompt = PromptVolumes(settings)
    prompt.prompt_app_volume_type()
    assert settings.get("CN_APP_VOLUME_TYPE") == vol_type


@pytest.mark.parametrize("vol_choice, vol_path", [
    (7, "awsEbsDynamic"),
])
def test_prompt_app_volume_choice_aws(monkeypatch, settings, vol_choice, vol_path):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: vol_choice)

    settings.set("global.storageClass.provisioner", "kubernetes.io/aws-ebs")
    settings.set("installer-settings.volumeProvisionStrategy", "awsEbsDynamic")
    prompt = PromptVolumes(settings)
    prompt.prompt_app_volume_type()
    assert settings.get("installer-settings.volumeProvisionStrategy") == vol_path


@pytest.mark.parametrize("vol_choice, vol_path", [
    (12, "gkePdDynamic"),
])
def test_prompt_app_volume_choice_gce(monkeypatch, settings, vol_choice, vol_path):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: vol_choice)

    settings.set("global.storageClass.provisioner", "kubernetes.io/gce-pd")
    settings.set("installer-settings.volumeProvisionStrategy", "gkePdDynamic")
    prompt = PromptVolumes(settings)
    prompt.prompt_app_volume_type()
    assert settings.get("installer-settings.volumeProvisionStrategy") == vol_path


@pytest.mark.parametrize("vol_choice, vol_path", [
    (17, "aksPdDynamic"),
])
def test_prompt_app_volume_choice_azure(monkeypatch, settings, vol_choice, vol_path):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: vol_choice)

    settings.set("global.storageClass.provisioner", "kubernetes.io/azure-disk")
    settings.set("installer-settings.volumeProvisionStrategy", "aksPdDynamic")
    prompt = PromptVolumes(settings)
    prompt.prompt_app_volume_type()
    assert settings.get("installer-settings.volumeProvisionStrategy") == vol_path


@pytest.mark.parametrize("vol_choice, vol_path", [
    (22, "doksPdDynamic"),
])
def test_prompt_app_volume_choice_do(monkeypatch, settings, vol_choice, vol_path):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: vol_choice)

    settings.set("global.storageClass.provisioner", "dobs.csi.digitalocean.com")
    settings.set("installer-settings.volumeProvisionStrategy", "doksPdDynamic")
    prompt = PromptVolumes(settings)
    prompt.prompt_app_volume_type()
    assert settings.get("installer-settings.volumeProvisionStrategy") == vol_path


@pytest.mark.parametrize("vol_choice, vol_path", [
    (26, "localOpenEbsHostPathDynamic"),
])
def test_prompt_app_volume_choice_local(monkeypatch, settings, vol_choice, vol_path):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: vol_choice)

    settings.set("global.storageClass.provisioner", "openebs.io/local")
    settings.set("installer-settings.volumeProvisionStrategy", "localOpenEbsHostPathDynamic")
    prompt = PromptVolumes(settings)
    prompt.prompt_app_volume_type()
    assert settings.get("installer-settings.volumeProvisionStrategy") == "localOpenEbsHostPathDynamic"


@pytest.mark.parametrize("persistence", ["ldap", "hybrid"])
def test_prompt_storage(monkeypatch, settings, persistence):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: "4Gi")

    settings.set("global.cnPersistenceType", persistence)
    settings.set("opendj.persistence.size", "")
    PromptVolumes(settings).prompt_storage()
    assert settings.get("opendj.persistence.size") == "4Gi"


@pytest.mark.parametrize("persistence", ["ldap", "hybrid"])
def test_prompt_storage_2(monkeypatch, settings, persistence):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: "4Gi")

    settings.set("global.cnPersistenceType", persistence)
    settings.set("opendj.persistence.size", "5Gi")
    PromptVolumes(settings).prompt_storage()
    assert settings.get("opendj.persistence.size") == "5Gi"


def test_prompt_volumes_microk8s(settings):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    settings.set("installer-settings.volumeProvisionStrategy", "microk8sDynamic")
    PromptVolumes(settings).prompt_volumes()
    assert settings.get("global.storageClass.provisioner") == "microk8s.io/hostpath"


def test_prompt_volumes_minikube(settings):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    settings.set("installer-settings.volumeProvisionStrategy", "minikubeDynamic")
    PromptVolumes(settings).prompt_volumes()
    assert settings.get("global.storageClass.provisioner") == "k8s.io/minikube-hostpath"


def test_prompt_volumes_global_azure_type(monkeypatch, settings):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: "StandardSSD_LRS")

    settings.set("installer-settings.volumeProvisionStrategy", "aksPdDynamic")
    settings.set("global.azureStorageAccountType", "")
    PromptVolumes(settings).prompt_volumes()
    assert settings.get("global.azureStorageAccountType") == "StandardSSD_LRS"


def test_prompt_volumes_global_aws_type(monkeypatch, settings):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: "io1")

    settings.set("installer-settings.volumeProvisionStrategy", "awsEbsDynamic")
    settings.set("global.awsStorageType", "")
    PromptVolumes(settings).prompt_volumes()
    assert settings.get("global.awsStorageType") == "io1"


def test_prompt_volumes_global_gke_type(monkeypatch, settings):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: "pd-ssd")

    settings.set("installer-settings.volumeProvisionStrategy", "gkePdDynamic")
    settings.set("global.gcePdStorageType", "")
    PromptVolumes(settings).prompt_volumes()
    assert settings.get("global.gcePdStorageType") == "pd-ssd"


def test_prompt_volumes_global_local_type(monkeypatch, settings):
    from pygluu.kubernetes.terminal.volumes import PromptVolumes

    monkeypatch.setattr("click.prompt", lambda x, default: "openebs.io/local")

    settings.set("installer-settings.volumeProvisionStrategy", "localOpenEbsHostPathDynamic")
    settings.set("global.storageClass.provisioner", "")
    PromptVolumes(settings).prompt_volumes()
    assert settings.get("global.storageClass.provisioner") == "openebs.io/local"
