import marshmallow as ma
from marshmallow import EXCLUDE, RAISE, ValidationError, validate, validates_schema

class BaseSchema(ma.Schema):
    class Meta(ma.Schema.Meta):
        unknown = EXCLUDE 
        ordered = False

class SmartNested(ma.fields.Nested):
    """Nested schema helper."""

    def _deserialize(self, *args, **kwargs):
        if hasattr(self.schema, "session"):
            self.schema.transient = self.root.transient
        return super()._deserialize(*args, **kwargs)

class SubjectSchema(BaseSchema):
    type_field = ma.fields.Str(required=True, data_key="type")
    id = ma.fields.Str(required=True)
    properties = ma.fields.Dict()

class ResourceSchema(SubjectSchema):
    pass

class ActionSchema(BaseSchema):
    name = ma.fields.Str(required=True)
    properties = ma.fields.Dict()

class EvaluationRequestSchema(BaseSchema):
    subject = SmartNested(SubjectSchema)
    resource = SmartNested(ResourceSchema)
    action = SmartNested(ActionSchema)
    context = ma.fields.Dict()

class DecisionSchema(BaseSchema):
    decision = ma.fields.Bool(required=True)
    context = ma.fields.Dict()
