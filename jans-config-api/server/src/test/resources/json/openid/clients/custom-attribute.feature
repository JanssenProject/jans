@ignore
Feature: Openid connect clients to test Custom Attribute for Client Claim Management

Background:
* def mainUrl = openidclients_url
* def customAttributes_url = attributes_url


Scenario: Connect Client with CustomAttribute Testing for Client Claim Management
#Check if custom attribute is present
Given url customAttributes_url 
And header Authorization = 'Bearer ' + accessToken
And param pattern = 'customTest' 
And param limit = 1 
When method GET 
Then status 200
And print response
Then print response[0].name
Then assert responseStatus == 200
And eval if( response.length == 0 ||response[0].name != 'customTest' ) karate.abort()
# Create Client with CustomAttribute
Given url openidclients_url
And header Authorization = 'Bearer ' + accessToken
And request read('openid-client.json')
When method POST
Then status 201
And print response
And print response.inum
# Patch OpenId Connect Client with CustomAttribute
Given url openidclients_url + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
And header Content-Type = 'application/json-patch+json'
And header Accept = 'application/json'
And request read('client_custom_attribute_patch.json')
When method PATCH
Then status 200
And print response
# Delete OpenId Connect Client with CustomAttribute
Given url openidclients_url + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204

