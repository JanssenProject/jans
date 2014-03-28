from org.xdi.oxauth.service.uma.authorization import IPolicyExternalAuthorization
from org.xdi.util import StringHelper

class PythonExternalAuthorization(IPolicyExternalAuthorization):

    def authorize(self, authorizationContext):

        print "authorizing..."

        if StringHelper.equalsIgnoreCase(authorizationContext.getUserClaim("locality"), "Austin"):
            print "authorized"
            return True

        return False
