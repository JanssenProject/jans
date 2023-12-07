from pluggy import HookimplMarker
from pluggy import HookspecMarker

hookspec = HookspecMarker("jans_aio")
hookimpl = HookimplMarker("jans_aio")


class AioPlugin:
    @hookspec
    def add_supervisor_programs(self):
        ...

    @hookspec
    def add_nginx_includes(self):
        ...
