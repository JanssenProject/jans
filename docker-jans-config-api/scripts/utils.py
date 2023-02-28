import json


def get_config_api_scope_mapping(path="/app/static/config-api-rs-protect.json"):
    scope_mapping = {}
    scope_levels = ["scopes", "groupScopes", "superScopes"]

    with open(path) as f:
        scope_defs = json.loads(f.read())

    for resource in scope_defs["resources"]:
        for condition in resource["conditions"]:
            for scope_level in scope_levels:
                scope_mapping.update({
                    scope["inum"]: {
                        "name": scope["name"],
                        "level": scope_level,
                    }
                    for scope in condition.get(scope_level, [])
                    if scope.get("inum") and scope.get("name")
                })
    return scope_mapping
