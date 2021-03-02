
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
    @auth-config-dcr-patch
  	Scenario: Patch DCR Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/dcrSignatureValidationEnabled\", \"value\": \"true\" } ,{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationSoftwareStatementJwksURIClaim\", \"value\": \"jwks_uri\" } ,{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationSoftwareStatementJwksClaim\", \"value\": \"https://pujavs.jans.server2/jans-auth/restv1/jwks\" } ,{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationJwksUri\", \"value\": \"https://pujavs.jans.server2/jans-auth/restv1/jwks\" },{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationJwks\", \"value\": \"key\" }]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @ignore
    @auth-config-softwareStatementValidationType-patch
  	Scenario: Patch softwareStatementValidationType Auth configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/softwareStatementValidationType\", \"value\": \"jwks_uri\" } ,{\"op\":\"replace\", \"path\": \"/softwareStatementValidationClaimName\", \"value\": \"jwks_uri\" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    
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
    
   