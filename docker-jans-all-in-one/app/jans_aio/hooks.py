from pluggy import HookimplMarker
from pluggy import HookspecMarker

hookspec = HookspecMarker("jans_aio")
hookimpl = HookimplMarker("jans_aio")


class AioPlugin:
    @hookspec
    def add_supervisor_programs(self):
        """Return supervisor program configs to include."""

    @hookspec
    def add_nginx_includes(self):
        """Return nginx config snippets to include."""
