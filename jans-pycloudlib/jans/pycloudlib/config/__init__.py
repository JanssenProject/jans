# noqa: D104


def __getattr__(name: str):
    _modules = {
        "ConsulConfig": "jans.pycloudlib.config.consul_config",
        "KubernetesConfig": "jans.pycloudlib.config.kubernetes_config",
        "GoogleConfig": "jans.pycloudlib.config.google_config",
        "AwsConfig": "jans.pycloudlib.config.aws_config",
        "FileConfig": "jans.pycloudlib.config.file_config",
    }
    if name in _modules:
        import importlib

        mod = importlib.import_module(_modules[name])
        return getattr(mod, name)
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


__all__ = ["ConsulConfig", "KubernetesConfig", "GoogleConfig", "AwsConfig", "FileConfig"]
