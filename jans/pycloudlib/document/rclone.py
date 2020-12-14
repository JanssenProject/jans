"""
jans.pycloudlib.document.rclone
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This module consists of class to interact with RClone.
"""

import logging
import os

from jans.pycloudlib.utils import exec_cmd

logger = logging.getLogger(__name__)


class RClone:
    """This class interacts with RClone.
    """

    def __init__(self, url, username, password):
        self.url = f"{url}/repository/default"
        self.username = username
        self.password = password

    def configure(self) -> None:
        """Configure connection to Jackrabbit WebDAV.
        """
        conf_file = os.path.expanduser("~/.config/rclone/rclone.conf")
        if os.path.isfile(conf_file):
            return

        cmd = f"rclone config create jackrabbit webdav vendor other pass '{self.password}' user '{self.username}' url {self.url}"
        _, err, code = exec_cmd(cmd)

        if code != 0:
            errors = err.decode().splitlines()
            logger.warning(f"Unable to create webdav config; reason={errors}")

    def copy_from(self, remote: str, local: str) -> None:
        """Copy path from remote into local filesystem.

        :params remote: Remote path.
        :params local: Local path.
        """
        cmd = f"rclone copy jackrabbit:{remote} {local} --create-empty-src-dirs --ignore-size"
        _, err, code = exec_cmd(cmd)

        if code != 0:
            errors = err.decode().splitlines()
            logger.debug(
                f"Unable to sync files from remote directories; reason={errors}"
            )

    def copy_to(self, remote: str, local: str) -> None:
        """ Copy path from local to remote.

        :params remote: Remote path.
        :params local: Local path.
        """
        cmd = f"rclone copy {local} jackrabbit:{remote} --create-empty-src-dirs --ignore-size"
        _, err, code = exec_cmd(cmd)

        if code != 0:
            errors = err.decode().splitlines()
            logger.debug(f"Unable to sync files to remote directories; reason={errors}")

    def ready(self, path: str = "/") -> bool:
        """Check whether remote WebDAV is ready.

        :params path: Root path of remote.
        :returns: A ``bool`` to mark remote readiness.
        """
        cmd = f"rclone lsd jackrabbit:{path}"
        _, err, code = exec_cmd(cmd)

        if code != 0:
            errors = err.decode().splitlines()
            logger.debug(f"Unable to list remote directory {path}; reason={errors}")
            return False
        return True

    def ls(self, path: str) -> bytes:
        """List paths at remote.

        :params path: Remote path.
        :returns: List of paths.
        """
        cmd = f"rclone ls jackrabbit:{path}"
        out, err, code = exec_cmd(cmd)
        if code != 0:
            errors = err.decode().splitlines()
            logger.debug(f"Unable to list remote directory {path}; reason={errors}")
        return out
