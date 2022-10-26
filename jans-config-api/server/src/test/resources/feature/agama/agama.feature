
Feature: Agama flow

Background:
* def mainUrl = agama_url
* def funGetEncodedValue =
"""
function(strValue) {
print(' strValue = '+strValue);
if(strValue == null || strValue.length==0){
return strValue;
}
var URLEncoder = Java.type('java.net.URLEncoder');
var encodedStrValue = URLEncoder.encode(strValue, "UTF-8");
print(' encodedStrValue = '+encodedStrValue);
return encodedStrValue;
}
"""

Scenario: Fetch all agama without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 
	And print response


Scenario: Fetch all agama flows 
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200 
	And print response
	
@ignore
Scenario: Fetch agama flow by name 
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200 
	And print response
	And assert response.length != null 
 	And print response[0].qname 
	Then def flowName = response[0].qname 
 	And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	And print response

@ignore	
@CreateUpdateDelete 
Scenario: Create, update and delete agama flow
    #Create agama flow
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('agama.json') 
	When method POST 
    Then status 201 
	And print response
	And print response.qname 	
	Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName
	#Fetch agama flow by name and with source
    Given url mainUrl + '/' +encodedFlowName+ '?includeSource=true'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
	Then def result = response
    And print result
    And print 'Old revision ='+response.revision 
	Then set result.revision = response.revision+1
    And print result
    And print response.qname 	
	Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName
    #Update agama flow
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And print response
 	And print response.qname 	
    And print response.revision
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Fetch agama flow by name
	Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
 	And print response.qname 	
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Fetch agama flow by name and with source
	Given url mainUrl + '/' +encodedFlowName + '?includeSource=true'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Delete agama flow by name
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204
	And print response

@ignore
@CreateFlowWithDataInRequestBodyUpdateDelete
Scenario: Create agama flow with source data in request body
    #Create agama flow
    Then def flowName = 'test'
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
    And header Content-Type = 'text/plain'
    And header Accept = 'application/json'
	And request read('agama-source.txt') 
	When method POST 
    Then status 201 
	And print response
	Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName
	#Fetch agama flow by name and with source
    Given url mainUrl + '/' +encodedFlowName+ '?includeSource=true'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
    And print response.qname 	
	Then def flowName = response.qname
    And print flowName 
	Then def result = response
    And print 'Old revision ='+response.revision 
	Then set result.revision = response.revision+1
 	And print encodedFlowName 
    #Update agama flow
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And print response
 	And print response.qname 	
    And print response.revision
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Fetch agama flow by name
	Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
 	And print response.qname 	
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Delete agama flow by name
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204
	And print response

@ignore
@CreateAndUpdateFlowWithDataInRequestBodyUpdateDelete
Scenario: Create agama flow with source data in request body
    #Create agama flow
    Then def flowName = 'test'
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
    And header Content-Type = 'text/plain'
    And header Accept = 'application/json'
	And request read('agama-source.txt') 
	When method POST 
    Then status 201 
	And print response
	Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName
	#Fetch agama flow by name and with source
    Given url mainUrl + '/' +encodedFlowName+ '?includeSource=true'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
    And print response.qname 	
	Then def flowName = response.qname
    And print flowName 
	Then def result = response
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
    #Update agama flow
	Given url mainUrl + '/source/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
    And header Content-Type = 'text/plain'
    And header Accept = 'application/json'
	And request read('agama-source.txt') 
	When method PUT 
	Then status 200 
	And print response
 	And print response.qname 	
    And print response.revision
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Fetch agama flow by name
	Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
 	And print response.qname 	
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Delete agama flow by name
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204
	And print response

@ignore	
@CreateAndPatchFlowAndDelete
Scenario: Create and Patch agama flow
    #Create agama flow
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('agama.json') 
	When method POST 
    Then status 201 
	And print response
	Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName
	#Fetch agama flow by name and with source
    Given url mainUrl + '/' +encodedFlowName+ '?includeSource=true'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
    Then status 200 
	And print response
	Then def result = response
    And print response.qname 	
	Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName
    #Patch agama flow
	Then def revision_before = response.revision
	And print 'revision = '+revision_before
    Then def revision_updated = revision_before+1
	And print 'revision_updated = '+revision_updated
    And print result.jansHideOnDiscovery
    And def request_body = (result.revision == null ? "[ {\"op\":\"add\", \"path\": \"/revision\", \"value\":"+revision_updated+" } ]" : "[ {\"op\":\"replace\", \"path\": \"/revision\", \"value\":"+revision_updated+" } ]")
    And print 'request_body ='+request_body
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request request_body
	Then print request
    When method PATCH
    Then status 200
    And print response	
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
	#Delete agama flow by name
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204
	And print response
