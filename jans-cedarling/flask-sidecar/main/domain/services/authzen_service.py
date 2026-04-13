from abc import ABC, abstractmethod
from main.domain.entities.authzen import CedarlingRequest, Decision


class AuthzenService(ABC):
    @abstractmethod
    def authorize(self, request: CedarlingRequest) -> Decision:
        """
        Performs authorization on request
        """
