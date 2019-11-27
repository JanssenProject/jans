package org.gluu.model.custom.script.model;

public enum ScriptTemplate {

    AUTHEN( "from org.gluu.service.cdi.util import CdiUtil\n" +
            "from org.gluu.model.custom.script.type.auth import PersonAuthenticationType\n" +
            "\n" +
            "import java\n" +
            "\n" +
            "class SamplePersonScript(PersonAuthenticationType):\n" +
            "    def __init__(self, currentTimeMillis):\n" +
            "       \n" +
            "\n" +
            "    def init(self, configurationAttributes):\n" +
            "        return True   \n" +
            "\n" +
            "    def destroy(self, configurationAttributes):\n" +
            "        return True\n" +
            "\n" +
            "    def getApiVersion(self):\n" +
            "        return 1\n" +
            "\n" +
            "    def isValidAuthenticationMethod(self, usageType, configurationAttributes):\n" +
            "        return True\n" +
            "\n" +
            "    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):\n" +
            "        return None\n" +
            "\n" +
            "    def authenticate(self, configurationAttributes, requestParameters, step):\n" +
            "        return True\n" +
            "\n" +
            "    def prepareForStep(self, configurationAttributes, requestParameters, step):\n" +
            "        \n" +
            "\n" +
            "    def getExtraParametersForStep(self, configurationAttributes, step):\n" +
            "        return None\n" +
            "\n" +
            "    def getCountAuthenticationSteps(self, configurationAttributes):\n" +
            "        return 1\n" +
            "\n" +
            "    def getPageForStep(self, configurationAttributes, step):\n" +
            "        return \"\"\n" +
            "\n" +
            "    def logout(self, configurationAttributes, requestParameters):\n" +
            "        return True\n"),
    NO_AUTHEN("from org.gluu.service.cdi.util import CdiUtil\n" +
            "from org.gluu.oxauth.security import Identity\n" +
            "from org.gluu.model.custom.script.type.authz import ConsentGatheringType\n" +
            "from org.gluu.util import StringHelper\n" +
            "\n" +
            "import java\n" +
            "import random\n" +
            "\n" +
            "class SampleScript(ConsentGatheringType):\n" +
            "\n" +
            "    def __init__(self, currentTimeMillis):\n" +
            "\n" +
            "    def init(self, configurationAttributes):\n" +
            "        return True\n" +
            "\n" +
            "    def destroy(self, configurationAttributes):\n" +
            "        return True\n" +
            "\n" +
            "    def getApiVersion(self):\n" +
            "        return 1\n" +
            "\n" +
            "    \n" +
            "    def authorize(self, step, context): \n" +
            "        return True\n" +
            "\n" +
            "    def getNextStep(self, step, context):\n" +
            "        return -1\n" +
            "\n" +
            "    def prepareForStep(self, step, context):\n" +
            "        return True\n" +
            "\n" +
            "    def getStepsCount(self, context):\n" +
            "        return 2\n" +
            "\n" +
            "    def getPageForStep(self, step, context):\n" +
            "        return \"\"\n");


    private final String value;


    ScriptTemplate(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
