
Feature: Scim config 

Background:
* def mainUrl = scim_config_url


Scenario: Fetch scim config without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 

    
@Get-all
Scenario: Fetch config by filter 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
  	When method GET 
	Then status 200
	And print response 
    

@patch-config-maxCount
Scenario: Patch config properties
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200
    And print response    
    And print response.maxCount
    Given url mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
	And def request_body = (response.maxCount == null ? "[ {\"op\":\"add\", \"path\": \"/maxCount\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/maxCount\", \"value\":\""+response.maxCount+"\" } ]")
    And print request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    And print 'SCIM App configuration'

	
@patch-config-loggingLevel
Scenario: Patch loggingLevel properties
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200
    And print response    
    And print response.loggingLevel
    Given url mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
	And def request_body = (response.loggingLevel == null ? "[ {\"op\":\"add\", \"path\": \"/loggingLevel\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\":\""+response.loggingLevel+"\" } ]")
    And print request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    And print 'SCIM App configuration'
	
@patch-config-userExtensionSchemaURI
Scenario: Patch userExtensionSchemaURI properties
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200
    And print response    
    And print response.userExtensionSchemaURI
    Given url mainUrl
  	And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
	And def request_body = (response.userExtensionSchemaURI == null ? "[ {\"op\":\"add\", \"path\": \"/userExtensionSchemaURI\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/userExtensionSchemaURI\", \"value\":\""+response.userExtensionSchemaURI+"\" } ]")
    And print request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    And print 'SCIM App configuration'