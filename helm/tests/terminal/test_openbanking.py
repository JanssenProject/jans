import pytest
from unittest.mock import patch, mock_open


@pytest.mark.parametrize("given, expected", [
    ("", "https://keystore.openbankingtest.org.uk/keystore/openbanking.jwks"),  # default
    ("random", "random"),
])
def test_ob_external_jwks(monkeypatch, settings, given, expected):
    from pygluu.kubernetes.terminal.openbanking import PromptOpenBanking

    monkeypatch.setattr("click.prompt", lambda x, default: given or expected)

    settings.set("global.cnObExtSigningJwksUri", "")
    settings.set("global.cnObExtSigningJwksCrt", "random")
    settings.set("global.cnObExtSigningJwksKey", "random")
    settings.set("global.cnObExtSigningJwksKeyPassPhrase", "random")
    settings.set("global.cnObExtSigningAlias", "random")
    settings.set("global.cnObStaticSigningKeyKid", "random")
    settings.set("global.cnObTransportCrt", "random")
    settings.set("global.cnObTransportKey", "random")
    settings.set("global.cnObTransportKeyPassPhrase", "random")
    settings.set("global.cnObTransportAlias", "random")
    settings.set("installer-settings.openbanking.hasCnObTransportTrustStore", True)
    settings.set("global.cnObTransportTrustStore", "random")

    prompt = PromptOpenBanking(settings)
    prompt.prompt_openbanking()

    assert settings.get("global.cnObExtSigningJwksUri") == expected
