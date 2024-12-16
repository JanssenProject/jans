from main.extensions import BlueprintApi
from main.v1.schema import EvaluationRequestSchema, DecisionSchema, WellKnownSchema
from flask.views import MethodView
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
            "access_evaluation_v1_endpoint": "http://127.0.0.1:5000/cedarling/evaluation"
        }
        return response
