
Feature: Verify Organization configuration endpoint

  	Background:
  	* def mainUrl = org_configuration_url
  	
  	@auth-config-get-error
    Scenario: Retrieve Organization configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response

 	@auth-config-get
  	Scenario: Retrieve Organization configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    	
    @auth-config-patch
  	Scenario: Patch Organization configuration
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
	And print response.description
    #And def request_body = (response.description == null ? "[ {\"op\":\"add\", \"path\": \"/description\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/description\", \"value\":"+response.description+" } ]")
	And def request_body = (response.description == null ? "[ {\"op\":\"add\", \"path\": \"/description\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/description\", \"value\":\""+response.description+"\" } ]")
	And print request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    
  