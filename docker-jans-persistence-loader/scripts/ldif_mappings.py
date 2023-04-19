from jans.pycloudlib.persistence.utils import PersistenceMapper


def get_ldif_mappings(group, optional_scopes=None):
    optional_scopes = optional_scopes or []

    def default_files():
        return [
            "base.ldif",
            "attributes.ldif",
            "scopes.ldif",
            "scripts.ldif",
            "configuration.ldif",
            "o_metric.ldif",
            "agama.ldif",
            "jans-auth/role-scope-mappings.ldif",
            "jans-cli/client.ldif",
            "jans-auth/configuration.ldif",
        ]

    def user_files():
        return [
            "jans-auth/people.ldif",
            "jans-auth/groups.ldif",
        ]

    def site_files():
        return ["o_site.ldif"]

    ldif_mappings = {
        "default": default_files(),
        "user": user_files(),
        "site": site_files(),
        "cache": [],
        "token": [],
        "session": [],
    }

    mapper = PersistenceMapper()
    ldif_mappings = {
        mapping: files for mapping, files in ldif_mappings.items()
        if mapping in mapper.groups()[group]
    }
    return ldif_mappings
