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

from main.extensions import BlueprintApi
from main.v1.schema import EvaluationRequestSchema, DecisionSchema, WellKnownSchema
from flask.views import MethodView
from flask import request
from main.extensions import cedarling

blp = BlueprintApi("Evaluate",
                   __name__,
                   description="AuthZen evaluation endpoint")

@blp.route("/cedarling/evaluation")
class Evaluation(MethodView):
    @blp.arguments(EvaluationRequestSchema, location="json")
    @blp.response(200, DecisionSchema)
    def post(self, payload):
        """
        Evaluates whether a subject is authorized to perform a specific action on a resource.
        """
        auth_response = cedarling.authorize(
                                payload["subject"],
                                payload["action"],
                                payload["resource"],
                                payload.get("context", {})
                        )
        return auth_response

@blp.route("/.well-known/authzen-configuration")
class WellKnown(MethodView):
    @blp.response(200, WellKnownSchema)
    def get(self):
        """
        Returns authzen configuration endpoint
        """
        response = {
            "access_evaluation_v1_endpoint": f"{request.host_url}cedarling/evaluation"
        }
        return response
