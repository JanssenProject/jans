import logging
import os
import typing as _t
from types import MappingProxyType

from jans.pycloudlib.config.base_config import BaseConfig
from jans.pycloudlib.schema import load_schema_from_file

logger = logging.getLogger(__name__)


class FileConfig(BaseConfig):
    def __init__(self) -> None:
        filepath = os.environ.get("CN_CONFIGURATOR_CONFIGURATION_FILE", "/etc/jans/conf/configuration.json")

        out, err, code = load_schema_from_file(filepath, exclude_secret=True)
        if code != 0:
            logger.warning(f"Unable to load configmaps from file {filepath}; error={err}; local configmaps will be excluded")

        # set the data as immutable
        data = out.get("_configmap") or {}
        self.data = MappingProxyType(data)

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get specific config.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        return self.data.get(key) or default

    def set(self, key: str, value: _t.Any) -> _t.Any:
        """Set specific config.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether config is set or not.
        """
        raise NotImplementedError(f"The {__class__.__name__}.set method is disabled.")

    def set_all(self, data: dict[str, _t.Any]) -> _t.Any:
        """Set all config.

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether config is set or not.
        """
        raise NotImplementedError(f"The {__class__.__name__}.set_all method is disabled.")

    def get_all(self) -> dict[str, _t.Any]:
        """Get all configs.

        Returns:
            A mapping of all configs (if any).
        """
        return dict(self.data)
