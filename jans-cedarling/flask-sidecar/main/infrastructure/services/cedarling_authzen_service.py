from main.domain.entities.authzen import CedarlingRequest, Decision
from main.domain.services.authzen_service import AuthzenService
from main.base.cedarling.cedarling import CedarlingInstance 


class CedarlingAuthzenService(AuthzenService):
    def __init__(self, client: CedarlingInstance):
        self.client = client


    def authorize(self, request: CedarlingRequest) -> Decision:
        """
        Performs authorization on request
        """
        return self.client.authorize(request)
