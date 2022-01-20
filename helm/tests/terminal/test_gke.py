def test_prompt_gke_account(monkeypatch, settings):
    from pygluu.kubernetes.terminal.gke import PromptGke

    monkeypatch.setattr("click.prompt", lambda x: "random@gmail.local")

    PromptGke(settings).prompt_gke()
    assert settings.get("GMAIL_ACCOUNT") == "random@gmail.local"


def test_prompt_gke_vol_type(monkeypatch, settings):
    from pygluu.kubernetes.terminal.gke import PromptGke

    class FakePopen:
        returncode = 0

        def __init__(self, *args, **kwargs):
            pass

        def communicate(self):
            return b"/home/random", b""

    monkeypatch.setattr("subprocess.Popen", FakePopen)

    settings.set("GMAIL_ACCOUNT", "random@gmail.local")
    settings.set("APP_VOLUME_TYPE", 11)
    settings.set("NODES_NAMES", ["node-1"])
    settings.set("NODES_ZONES", ["zone-1"])

    PromptGke(settings).prompt_gke()
    assert settings.get("GOOGLE_NODE_HOME_DIR") == "/home/random"
