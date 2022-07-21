
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

	
@CreateUpdateDelete 
Scenario: Create new agama flow
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('agama.json') 
	When method POST 
    Then status 201 
	And print response
	Then def result = response
    And print 'Old transHash ='+response.transHash 
	Then set result.transHash = 'UpdatedAgamaFlowtransHash' 
    And print response.qname 	
	Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And print response
 	And print response.qname 	
    And print response.transHash
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204
	And print response

	
@CreateFlowWithDataInRequestBodyUpdateDelete
Scenario: Create new agama flow with source data in request body
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
    And print response.qname 	
	Then def flowName = response.qname
    And print flowName 
	Then def result = response
    And print 'Old transHash ='+response.transHash 
	Then set result.transHash = 'UpdatedAgamaFlowtransHash' 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And print response
 	And print response.qname 	
    And print response.transHash
    Then def flowName = response.qname
    And print flowName 
	Then def encodedFlowName = funGetEncodedValue(flowName)
 	And print encodedFlowName 
    Given url mainUrl + '/' +encodedFlowName
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204
	And print response
	
	
