Feature: Verify Auth configuration endpoint

  	Background:
  	* def mainUrl = authConfigurationUrl

 	@auth-config-get
  	Scenario: Retrieve Auth configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @ignore
    @auth-config-patch
  	Scenario: Patch loggingLevel Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\": \"DEBUG\" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @ignore
    @auth-config-patch
  	Scenario: Patch cibaEnabled Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
 	And request "[ {\"op\":\"replace\", \"path\": \"/cibaEnabled\", \"value\": \"true\" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
	@ignore
    @auth-config-patch
  	Scenario: Patch clientBlackList Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/clientBlackList\", \"value\": ['/*.attacker.com/*','/*.hackers.com/*']\" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response    
   
    @ignore
    @auth-config-patch
  	Scenario: Patch clientAuthenticationFilters Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/clientBlackList\", \"value\": read('clientAuthenticationFilters.json')\" } ]"
    #And request read('clientAuthenticationFilters.json')
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @ignore
    @auth-config-patch
  	Scenario: Patch authenticationFilters Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Given path 'authenticationFilters'
    And request read('authenticationFilters.json')
	Then print request
    When method PATCH
    Then status 200
    And print response
    
   