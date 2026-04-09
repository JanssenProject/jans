from dataclasses import dataclass, field
import typing as _t


@dataclass
class Token:
    mapping: str
    payload: str


Context = _t.Dict[str, _t.Any]


@dataclass
class EntityMapping:
    entity_type: str
    id: str


@dataclass
class Resource:
    cedar_entity_mapping: EntityMapping
    attributes: _t.Dict[str, _t.Any] = field(default_factory=dict)


def resource_from_data(data: _t.Dict[str, _t.Any]) -> Resource:
    mapping = EntityMapping(**data["cedar_entity_mapping"])
    attrs = {k: v for k, v in data.items() if k != "cedar_entity_mapping"}
    return Resource(mapping, attrs)


@dataclass
class CedarlingRequest:
    tokens: _t.List[Token]
    action: str
    resource: Resource
    context: Context


@dataclass
class Decision:
    decision: bool
    context: Context
