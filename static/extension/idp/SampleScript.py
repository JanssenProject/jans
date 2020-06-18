# oxShibboleth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Gluu
#
# Author: Yuriy Movchan
#

from org.gluu.service.cdi.util import CdiUtil
from org.gluu.model.custom.script.type.idp import IdpType
from org.gluu.util import StringHelper

import java

class IdpExtension(IdpType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Idp extension. Initialization"
        return True

    def destroy(self, configurationAttributes):
        print "Idp extension. Destroy"
        return True

    def getApiVersion(self):
        return 11

    def updateAttributes(self, context, configurationAttributes):
        print "Idp extension. Method: updateAttributes"
