import os
import typing as _t
from pathlib import Path
from contextlib import suppress

from jans.pycloudlib.utils import as_boolean
from pluggy import PluginManager

from jans_aio.hooks import AioPlugin
from jans_aio.utils import import_from_string


def get_max_memory() -> int:
    """Calculate max. memory assigned to the container.

    The value is taken from either cgroup files or `os.sysconf` module.

    Returns:
        Max. memory in megabytes.
    """
    max_mem_bytes = _max_memory_from_cgroups() or _max_memory_from_sysconf()
    if not max_mem_bytes:
        raise RuntimeError("Unable to determine max. memory from cgroup files or os.sysconf module")
    return int(max_mem_bytes / (1024.**2))  # in MB


def get_apps_resources(
    max_memory: int,
    supervisor_programs: dict[str, _t.Any],
    enabled_programs: _t.Optional[list[str]] = None
) -> dict[str, int]:
    """Calculate max. memory allocated to all apps in total.

    The amount will be `85%` of given `max_memory`.

    Args:
        max_memory: Total memory of the machine.
        enabled_programs: List of enabled programs.

    Returns:
        Mapping of allocated memory for each app (in megabytes).
    """
    enabled_programs = enabled_programs or []
    apps_max_mem = int(max_memory * 0.85)  # - (len(enabled_programs) * 128)

    used_ratio = 0.001 + sum([
        supervisor_programs[program]["mem_ratio"]
        for program in enabled_programs
    ])

    ratio_multiplier = 1.0 + (1.0 - used_ratio) / used_ratio

    apps_mem_alloc = {}
    for program in enabled_programs:
        allowed_ratio = supervisor_programs[program]["mem_ratio"] * ratio_multiplier
        apps_mem_alloc[program] = int(round(allowed_ratio * int(apps_max_mem)))
    return apps_mem_alloc


def get_heap_sizes(mem_alloc: int) -> tuple[int, int]:
    """Calculate min. and max. heap size.

    Args:
        mem_alloc: Allocated memory in megabytes.

    Returns:
        A tuple consists of min. and max. heap size in megabytes.
    """
    min_heap_size = 256
    # max. heap is 75% of allocated memory?
    max_heap_size = mem_alloc

    if max_heap_size < min_heap_size:
        max_heap_size = min_heap_size
    return min_heap_size, max_heap_size


def get_enabled_programs(supervisor_programs):
    user_components = [
        _comp.strip()
        for _comp in os.environ.get("CN_AIO_COMPONENTS", "").split(",")
        if _comp
    ]

    components = list(supervisor_programs.keys())
    if user_components:
        components = [
            comp for comp in user_components
            if comp in components
        ]
    return components


def render_supervisord_conf(components: _t.Optional[list[str]] = None) -> None:
    """Render `/app/conf/supervisord.conf` template."""
    components = components or []

    # include aio-monitor only if enabled by user
    if as_boolean(os.environ.get("CN_AIO_ENABLE_MONITOR")):
        components.append("aio-monitor")

    includes = " ".join([f"supervisord.conf.d/{comp}.conf" for comp in components])

    conf_file = "/app/conf/supervisord.conf"
    tmpl = Path(conf_file).read_text()
    Path(conf_file).write_text(tmpl.format(supervisord_conf_includes=includes))


def render_java_program_conf(program: str, mem_alloc: int, java_opts_env: str = "") -> None:
    """Render Java-based program template located under `/app/conf/supervisord.conf.d` directory.

    Args:
        program: Name of the program (path of the file will be resolved from this name).
        mem_alloc: Allocated memory (in megabytes) to the program.
        java_opts_env: Name of the environment variable as extra Java options passed to the program.
    """
    conf_file = f"/app/conf/supervisord.conf.d/{program}.conf"
    tmpl = Path(conf_file).read_text()
    min_heap_size, max_heap_size = get_heap_sizes(mem_alloc)

    Path(conf_file).write_text(
        tmpl.format(
            min_heap_size=min_heap_size,
            max_heap_size=max_heap_size,
            java_options=os.environ.get(java_opts_env) or "",
        ),
    )


def _max_memory_from_cgroups() -> int:
    """Determine max. memory from cgroup.

    The value is taken from one of:

    - `/sys/fs/cgroup/memory.max` (cgroup v2)
    - `sys/fs/cgroup/memory.limit_in_bytes` (cgroup v1 -- for backward compatibility)


    Returns:
        Max. memory in kilobytes.
    """
    max_mem_bytes = 0
    cgroup_mem_files = [
        "/sys/fs/cgroup/memory.max",  # cgroup v2
        "/sys/fs/cgroup/memory.limit_in_bytes",  # cgroup v1
    ]

    for cgroup_mem_file in cgroup_mem_files:
        pth = Path(cgroup_mem_file)

        if pth.exists():
            with suppress(ValueError):
                max_mem_bytes = int(pth.read_text().strip())
                break
    return max_mem_bytes


def _max_memory_from_sysconf() -> int:
    """Determine max. memory from os.sysconf.

    Returns:
        Max. memory in kilobytes.
    """
    return os.sysconf("SC_PAGE_SIZE") * os.sysconf("SC_PHYS_PAGES")


def render_nginx_default_conf(enabled_programs, nginx_includes):
    upstream_includes = []
    location_includes = []

    for program, types in nginx_includes.items():
        for type_ in types:
            file_no_ext = f"{program}-{type_}"

            val = f"include /etc/nginx/jans-aio/{file_no_ext}.conf;"

            if program not in enabled_programs:
                # disable the include by commenting out the line
                val = f"# {val}"

            if type_ == "upstream":
                upstream_includes.append(val)
            else:
                location_includes.append(val)

    # context for rendered templates
    ctx = {
        "upstream_includes": "\n".join(upstream_includes),
        "location_includes": "\n\t".join(location_includes),
    }
    tmpl = Path("/app/templates/nginx/nginx-default.conf").read_text().strip()
    Path("/etc/nginx/http.d/default.conf").write_text(tmpl % ctx)


class App:
    def __init__(self):
        self.plugin_manager = PluginManager("jans_aio")
        self.plugin_manager.add_hookspecs(AioPlugin)

    def get_supervisor_programs(self):
        programs = {
            "configurator": {
                "mem_ratio": 0,
            },
            "persistence-loader": {
                "mem_ratio": 0,
            },
            "jans-auth": {
                "mem_ratio": 0.25,
                "java_opts_env": "CN_AUTH_JAVA_OPTIONS",
            },
            "jans-config-api": {
                "mem_ratio": 0.10,
                "java_opts_env": "CN_CONFIG_API_JAVA_OPTIONS",
            },
            "jans-fido2": {
                "mem_ratio": 0.08,
                "java_opts_env": "CN_FIDO2_JAVA_OPTIONS",
            },
            "jans-scim": {
                "mem_ratio": 0.15,
                "java_opts_env": "CN_SCIM_JAVA_OPTIONS",
            },
            "jans-casa": {
                "mem_ratio": 0.10,
                "java_opts_env": "CN_CASA_JAVA_OPTIONS",
            },
            "jans-saml": {
                "mem_ratio": 0.10,
                "java_opts_env": "CN_SAML_JAVA_OPTIONS",
            },
            # #@NOTE: jans-link and jans-keycloak-link support is temporarily disabled
            # "jans-link": {
            #     "mem_ratio": 0.08,
            #     "java_opts_env": "CN_LINK_JAVA_OPTIONS",
            # },
            # "jans-keycloak-link": {
            #     "mem_ratio": 0.08,
            #     "java_opts_env": "CN_KEYCLOAK_LINK_JAVA_OPTIONS",
            # },
        }

        plugin_programs = self.plugin_manager.hook.add_supervisor_programs()
        for program in plugin_programs:
            programs.update(program)

        # merged supervisor programs
        return programs

    def get_nginx_includes(self):
        includes = {
            "jans-auth": ["upstream", "location"],
            "jans-config-api": ["upstream", "location"],
            "jans-fido2": ["upstream", "location"],
            "jans-scim": ["upstream", "location"],
            "jans-casa": ["upstream", "location"],
            "jans-saml": ["upstream", "location"],
            # @NOTE: jans-link and jans-keycloak-link support is temporarily disabled
            # "jans-link": ["upstream", "location"],
            # "jans-keycloak-link": ["upstream", "location"],
        }

        plugin_includes = self.plugin_manager.hook.add_nginx_includes()
        for include in plugin_includes:
            includes.update(include)

        # merged nginx includes
        return includes

    def discover_plugins(self) -> None:
        plugin_names = [
            name for name in os.environ.get("CN_AIO_PLUGINS", "").split(",")
            if name
        ]

        for name in plugin_names:
            plugin = import_from_string(name)
            self.plugin_manager.register(plugin())

    def bootstrap(self) -> None:
        self.discover_plugins()

        supervisor_programs = app.get_supervisor_programs()
        nginx_includes = app.get_nginx_includes()

        enabled_programs = get_enabled_programs(supervisor_programs)

        apps_mem_alloc = get_apps_resources(get_max_memory(), supervisor_programs, enabled_programs)

        for program, memory in apps_mem_alloc.items():
            if "java_opts_env" not in supervisor_programs[program]:
                continue

            render_java_program_conf(
                program,
                memory,
                supervisor_programs[program]["java_opts_env"],
            )

        # main supervisord config
        render_supervisord_conf(list(apps_mem_alloc.keys()))

        # render /etc/nginx/http.d/default.conf
        render_nginx_default_conf(enabled_programs, nginx_includes)


if __name__ == "__main__":
    app = App()
    app.bootstrap()
