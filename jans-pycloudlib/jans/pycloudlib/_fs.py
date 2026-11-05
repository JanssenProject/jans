"""This module contains filesystem helpers."""

import os
import typing as _t


def open_private(path: str, mode: str = "w", **kwargs: _t.Any) -> _t.IO[_t.Any]:
    """Open a file for writing, restricted to owner-only (0600) permissions.

    The file is created with 0600 and an existing file is narrowed to 0600
    before any content is written.

    Args:
        path: Path to file.
        mode: Write mode, i.e. `w` or `wb`.
        **kwargs: Extra keyword arguments passed to `os.fdopen`.
    """
    fd = os.open(path, os.O_WRONLY | os.O_CREAT, 0o600)
    try:
        os.fchmod(fd, 0o600)
        os.ftruncate(fd, 0)
        return os.fdopen(fd, mode, **kwargs)
    except BaseException:
        os.close(fd)
        raise
