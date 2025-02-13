"""
Copyright (c) 2025, Gluu, Inc. 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import marshmallow as ma
from marshmallow import EXCLUDE

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
    subject = SmartNested(SubjectSchema, required=True)
    resource = SmartNested(ResourceSchema, required=True)
    action = SmartNested(ActionSchema, required=True)
    context = ma.fields.Dict()

class DecisionSchema(BaseSchema):
    decision = ma.fields.Bool(required=True)
    context = ma.fields.Dict()

class WellKnownSchema(BaseSchema):
    access_evaluation_v1_endpoint = ma.fields.Str()
