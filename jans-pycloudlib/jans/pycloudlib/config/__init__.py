# noqa: D104
from jans.pycloudlib.config.kubernetes_config import KubernetesConfig  # noqa: F401
from jans.pycloudlib.config.google_config import GoogleConfig  # noqa: F401
from jans.pycloudlib.config.aws_config import AwsConfig  # noqa: F401
from jans.pycloudlib.config.file_config import FileConfig  # noqa: F401


def __getattr__(name: str):
    if name == "ConsulConfig":
        from jans.pycloudlib.config.consul_config import ConsulConfig
        return ConsulConfig
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


__all__ = ["ConsulConfig", "KubernetesConfig", "GoogleConfig", "AwsConfig", "FileConfig"]
