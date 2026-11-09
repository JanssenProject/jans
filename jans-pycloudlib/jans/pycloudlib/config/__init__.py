# noqa: D104
from jans.pycloudlib.config.kubernetes_config import KubernetesConfig  # noqa: F401
from jans.pycloudlib.config.google_config import GoogleConfig  # noqa: F401
from jans.pycloudlib.config.aws_config import AwsConfig  # noqa: F401
from jans.pycloudlib.config.file_config import FileConfig  # noqa: F401


def __getattr__(name: str):
    # Deferred import: python-consul depends on six, which Python 3.14 no longer ships.
    # Only import ConsulConfig when CN_CONFIG_ADAPTER=consul actually needs it.
    if name == "ConsulConfig":
        from jans.pycloudlib.config.consul_config import ConsulConfig
        return ConsulConfig
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


# avoid implicit reexport disabled error
__all__ = [
    "ConsulConfig",
    "KubernetesConfig",
    "GoogleConfig",
    "AwsConfig",
    "FileConfig",
]
