import logging
import os
import typing as _t
from types import MappingProxyType

from jans.pycloudlib.secret.base_secret import BaseSecret
from jans.pycloudlib.schema import load_schema_from_file

logger = logging.getLogger(__name__)


class FileSecret(BaseSecret):
    def __init__(self) -> None:
        filepath = os.environ.get("CN_CONFIGURATOR_CONFIGURATION_FILE", "/etc/jans/conf/configuration.json")
        key_file = os.environ.get("CN_CONFIGURATOR_CONFIGURATION_KEY_FILE", "/etc/jans/conf/configuration.key")

        out, err, code = load_schema_from_file(filepath, exclude_configmap=True, key_file=key_file)
        if code != 0:
            logger.warning(f"Unable to load secrets from file {filepath}; error={err}; local secrets will be excluded")

        # set the data as immutable
        data = out.get("_secret") or {}
        self.data = MappingProxyType(data)

    def get(self, key: str, default: _t.Any = "") -> _t.Any:
        """Get specific secret.

        Args:
            key: Key name.
            default: Default value if key is not exist.

        Returns:
            Value based on given key or default one.
        """
        return self.data.get(key) or default

    def set(self, key: str, value: _t.Any) -> _t.Any:
        """Set specific secret.

        Args:
            key: Key name.
            value: Value of the key.

        Returns:
            A boolean to mark whether secret is set or not.
        """
        raise NotImplementedError(f"The {__class__.__name__}.set method is disabled.")

    def set_all(self, data: dict[str, _t.Any]) -> _t.Any:
        """Set all secret.

        Args:
            data: Key-value pairs.

        Returns:
            A boolean to mark whether secret is set or not.
        """
        raise NotImplementedError(f"The {__class__.__name__}.set_all method is disabled.")

    def get_all(self) -> dict[str, _t.Any]:
        """Get all secrets.

        Returns:
            A mapping of all secrets (if any).
        """
        return dict(self.data)
