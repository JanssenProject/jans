# noqa: D104


def __getattr__(name: str):
    _modules = {
        "KubernetesSecret": "jans.pycloudlib.secret.kubernetes_secret",
        "VaultSecret": "jans.pycloudlib.secret.vault_secret",
        "GoogleSecret": "jans.pycloudlib.secret.google_secret",
        "AwsSecret": "jans.pycloudlib.secret.aws_secret",
        "FileSecret": "jans.pycloudlib.secret.file_secret",
    }
    if name in _modules:
        import importlib

        mod = importlib.import_module(_modules[name])
        return getattr(mod, name)
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


__all__ = ["KubernetesSecret", "VaultSecret", "GoogleSecret", "AwsSecret", "FileSecret"]
