from main.extensions import BlueprintApi
from main.v1.schema import EvaluationRequestSchema, DecisionSchema 
from flask.views import MethodView
from main.extensions import cedarling

blp = BlueprintApi("Evaluate",
                   __name__,
                   description="AuthZen evaluation endpoint")

@blp.route("/evaluation")
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
