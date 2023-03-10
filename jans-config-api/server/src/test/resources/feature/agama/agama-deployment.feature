
Feature: Agama Deployment

Background:
* def mainUrl = agama_deployment_url
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

Scenario: Fetch all Agama deployment without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 
	And print response


Scenario: Fetch all Agama deployment  
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200 
	And print response
	
Scenario: Fetch all Agama deployment  
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And header Authorization = 'Bearer ' + accessToken
    And param start = 0
	And param count = 3
	When method GET 
	Then status 200 
	And print response
	

	
