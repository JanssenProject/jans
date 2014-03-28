from org.xdi.oxauth.service.uma.authorization import IPolicyExternalAuthorization
from org.xdi.util import StringHelper

#
# Authorization request may contain claims:
# {"rpt":"<rpt token here>","ticket":"<ticket here>","claims":{"locality":["Austin"]}}
#

class PythonExternalAuthorization(IPolicyExternalAuthorization):

    def authorize(self, authorizationContext):

        print "authorizing..."

        if StringHelper.equalsIgnoreCase(authorizationContext.getRequestClaim("locality"), "Austin"):
            print "authorized"
            return True

        return False