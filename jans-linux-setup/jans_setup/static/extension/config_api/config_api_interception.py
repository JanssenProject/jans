# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2018, Janssen
#
# Author: Puja Sharma
#
#

from io.jans.model.custom.script.type.configapi import ConfigApiType
from io.jans.util import StringHelper


class ConfigApiAuthorization(ConfigApiType):
    def __init__(self, current_time_millis):
        self.currentTimeMillis = current_time_millis

    def init(self, configuration_attributes):
        print("ConfigApiType script. Initializing ...")
        print("ConfigApiType script. Initialized successfully")
        return True

    def destroy(self, configuration_attributes):
        print("ConfigApiType script. Destroying ...")
        print("ConfigApiType script. Destroyed successfully")
        return True

    def getApiVersion(self):
        return 1


    # Returns boolean true or false depending on the process, if the client is authorized
    # or not.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # response_as_json_object - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalIntrospectionContext (in https://github.com/JanssenFederation/oxauth project, )
    def authorize(self, response_as_json_object, context):
        print(" response_as_json_object: %s" % response_as_json_object)
        print(" context: %s" % context)

        print("Config Authentication process")
        request = context.httpRequest
        response = context.httpResponse
        print(" request = : %s" % request)
        print(" response = : %s" % response)

        app_configuration = context.getApiAppConfiguration()
        custom_script_configuration = context.getScript()
        issuer = context.getRequestParameters().get("ISSUER")
        token =  context.getRequestParameters().get("TOKEN")
        method = context.getRequestParameters().get("METHOD")
        path = context.getRequestParameters().get("PATH")
      
        print(" requese2: %s" % request)
        print(" response2 new: %s" % response)
        print("ConfigApiType.app_configuration: %s" % app_configuration)
        print("ConfigApiType.custom_script_configuration: %s" % custom_script_configuration)
        print("ConfigApiType.issuer: %s" % issuer)
        print("ConfigApiType.token: %s" % token)
        print("ConfigApiType.method: %s" % method)
        print("ConfigApiType.path: %s" % path)

        #Example to validate method
        if ("GET" == StringHelper.toUpperCase(method) ):
          print("Validate method: %s" % method)
        
        if ("attributes" == StringHelper.toLowerCase(path) ):
          print("ConfigApiType.path: %s" % path)
  
        response_as_json_object.accumulate("key_from_script", "value_from_script")
        print(" final response_as_json_object: %s" % response_as_json_object)

        return True




