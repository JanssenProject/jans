import pytest


@pytest.mark.parametrize("given, expected", [
    ("", "clb"),  # default
    (1, "clb"),
    (2, "nlb"),
    (3, "alb"),
])
def test_aws_loadbalancer(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.aws import PromptAws

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("installer-settings.aws.arn.enabled", False)
    PromptAws(settings).prompt_aws_lb()
    assert settings.get("installer-settings.aws.lbType") == expected


def test_aws_vpccidr(monkeypatch, settings):
    from pygluu.kubernetes.terminal.aws import PromptAws

    monkeypatch.setattr("click.prompt", lambda x, default: "0.0.0.0/0")

    settings.set("installer-settings.aws.arn.enabled", True)
    settings.set("installer-settings.aws.vpcCidr", "")
    PromptAws(settings).prompt_aws_lb()
    assert settings.get("installer-settings.aws.vpcCidr") == "0.0.0.0/0"
