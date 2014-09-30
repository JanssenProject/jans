from org.gluu.oxtrust.model.python import InumGeneratorType
from org.xdi.util import StringHelper
from java.lang import String
from org.xdi.util import INumGenerator
from org.gluu.oxtrust.util import Configuration

import java

class InumGenerator(InumGeneratorType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def generateInum(self,orgInum,prefix):
        if(StringHelper.isNotEmptyString(orgInum) and StringHelper.isNotEmptyString(prefix)):
            return orgInum + Configuration.inumDelimiter + prefix + Configuration.inumDelimiter + INumGenerator.generate()
        else:
            return ""