from main.extensions import BlueprintApi
from main.v1.schema import EvaluationRequestSchema, DecisionSchema 
from flask.views import MethodView

blp = BlueprintApi("Evaluate",
                   __name__,
                   description="AuthZen evaluation endpoint")

@blp.route("/evaluation")
class Evaluation(MethodView):
    @blp.arguments(EvaluationRequestSchema, location="json")
    @blp.response(200, DecisionSchema)
    def post(payload):
        """
        Evaluates whether a subject is authorized to perform a specific action on a resource.
        """
        pass
