import os
import typing as _t
from pathlib import Path
from contextlib import suppress

from jans.pycloudlib.utils import as_boolean

SUPERVISORD_PROGRAMS = {
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
        "mem_ratio": 0.15,
        "java_opts_env": "CN_CONFIG_API_JAVA_OPTIONS",
    },
    "jans-fido2": {
        "mem_ratio": 0.15,
        "java_opts_env": "CN_FIDO2_JAVA_OPTIONS",
    },
    "jans-scim": {
        "mem_ratio": 0.2,
        "java_opts_env": "CN_SCIM_JAVA_OPTIONS",
    },
    "casa": {
        "mem_ratio": 0.1,
        "java_opts_env": "GLUU_CASA_JAVA_OPTIONS",
    },
    "admin-ui": {
        "mem_ratio": 0,
    },
}


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


def get_apps_resources(max_memory: int, enabled_programs: _t.Optional[list[str]] = None) -> dict[str, int]:
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
        SUPERVISORD_PROGRAMS[program]["mem_ratio"]
        for program in enabled_programs
    ])

    ratio_multiplier = 1.0 + (1.0 - used_ratio) / used_ratio

    apps_mem_alloc = {}
    for program in enabled_programs:
        allowed_ratio = SUPERVISORD_PROGRAMS[program]["mem_ratio"] * ratio_multiplier
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


def get_enabled_programs():
    user_components = [
        _comp.strip()
        for _comp in os.environ.get("FLEX_COMPONENTS", "").split(",")
        if _comp
    ]

    components = list(SUPERVISORD_PROGRAMS.keys())
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
    if as_boolean(os.environ.get("FLEX_AIO_ENABLE_MONITOR")):
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


def main():
    enabled_programs = get_enabled_programs()
    apps_mem_alloc = get_apps_resources(get_max_memory(), enabled_programs)

    for program, memory in apps_mem_alloc.items():
        if "java_opts_env" not in SUPERVISORD_PROGRAMS[program]:
            continue

        render_java_program_conf(
            program,
            memory,
            SUPERVISORD_PROGRAMS[program]["java_opts_env"],
        )

    # main supervisord config
    render_supervisord_conf(list(apps_mem_alloc.keys()))

    # render /etc/nginx/http.d/default.conf
    render_nginx_default_conf(enabled_programs)


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


def render_nginx_default_conf(enabled_programs):
    # context for rendered templates
    ctx = {}

    # list of supported includes per program
    includes = [
        ("jans-auth", ["upstream", "location"]),
        ("jans-config-api", ["upstream", "location"]),
        ("jans-fido2", ["upstream", "location"]),
        ("jans-scim", ["upstream", "location"]),
        ("casa", ["upstream", "location"]),
        ("admin-ui", ["location"]),
    ]

    for program, types in includes:
        for type_ in types:
            file_no_ext = f"{program}-{type_}"

            key = file_no_ext.replace("-", "_")
            val = f"include /etc/nginx/flex-aio/{file_no_ext}.conf;"
            ctx[key] = f"{val}"

            if program not in enabled_programs:
                # disable the include by commenting out the line
                ctx[key] = f"# {val}"

    tmpl = Path("/app/templates/nginx/nginx-default.conf").read_text().strip()
    Path("/etc/nginx/http.d/default.conf").write_text(tmpl % ctx)


if __name__ == "__main__":
    main()
