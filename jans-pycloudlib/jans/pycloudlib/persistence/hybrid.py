"""This module contains various helpers related to hybrid (LDAP + Couchbase) persistence."""

from jans.pycloudlib.persistence.utils import PersistenceMapper


def render_hybrid_properties(dest: str) -> None:
    """Render file contains properties to connect to hybrid persistence.

    Args:
        dest: Absolute path where generated file is located.
    """
    hybrid_storages = resolve_hybrid_storages(PersistenceMapper())

    out = "\n".join([
        f"{k}: {v}" for k, v in hybrid_storages.items()
    ])

    with open(dest, "w") as fw:
        fw.write(out)


def resolve_hybrid_storages(mapper: PersistenceMapper) -> dict[str, str]:
    """Resolve hybrid storage configuration.

    Args:
        mapper: Persistence mapper instance.
    """
    ctx = {
        # unique storage names
        "storages": ", ".join(sorted(set(
            mapper.mapping.values())
        )),
        "storage.default": mapper.mapping["default"],
    }

    for k, v in mapper.groups_with_rdn().items():
        # remove empty value (if any)
        values = [val for val in v if val]
        if not values:
            continue
        ctx[f"storage.{k}.mapping"] = ", ".join(values)
    return ctx
