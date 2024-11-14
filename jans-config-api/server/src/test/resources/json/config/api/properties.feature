
Feature: Verify Auth configuration endpoint

  	Background:
  	* def mainUrl = api_config_url
  	
  	@config-get-error
    Scenario: Retrieve configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response

 	@config-get
  	Scenario: Retrieve configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    	
    @config-patch
  	Scenario: Patch configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And print 'response.loggingLevel = '+response.loggingLevel
    And def request_body = (response.loggingLevel == null ? "[ {\"op\":\"add\", \"path\": \"/loggingLevel\", \"value\":\"DEBUG\" } ]" : "[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\":\"DEBUG\" } ]")
    And print 'request_body ='+request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    
  