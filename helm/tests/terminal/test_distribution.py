import pytest


@pytest.mark.parametrize("given, expected", [
    ("", "default"),  # default
    (1, "default"),
    (2, "openbanking"),
])
def test_distribution(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.distribution import PromptDistribution

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("global.distribution", "")
    PromptDistribution(settings).prompt_distribution()
    assert settings.get("global.distribution") == expected
