
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
    
	
    @auth-config-dcr-patch
  	Scenario: Patch dcrSignatureValidationEnabled configuration
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
    #And request "[ {\"op\":\"replace\", \"path\": \"/dcrSignatureValidationEnabled\", \"value\": "+(response.dcrSignatureValidationEnabled == null ? false : response.dcrSignatureValidationEnabled)+" } ,{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationSoftwareStatementJwksURIClaim\", \"value\": "+(response.dcrSignatureValidationSoftwareStatementJwksURIClaim == null ? null : response.dcrSignatureValidationSoftwareStatementJwksURIClaim)+" } ,{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationSoftwareStatementJwksClaim\", \"value\": \+(response.dcrSignatureValidationSoftwareStatementJwksClaim == null ? null : response.dcrSignatureValidationSoftwareStatementJwksClaim)+} ,{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationJwksUri\", \"value\": +(response.dcrSignatureValidationJwksUri == null ? null : response.dcrSignatureValidationJwksUri)+},{\"op\":\"replace\", \"path\": \"/dcrSignatureValidationJwks\", \"value\": +(response.dcrSignatureValidationJwks == null ? null : response.dcrSignatureValidationJwks)+} }, {\"op\":\"replace\", \"path\": \"/dcrAuthorizationWithClientCredentials\", \"value\": +(response.dcrAuthorizationWithClientCredentials == null ? false : response.dcrAuthorizationWithClientCredentials)+" }]"	
    And def request_body = (response.dcrSignatureValidationEnabled == null ? "[ {\"op\":\"add\", \"path\": \"/dcrSignatureValidationEnabled\", \"value\":false } ]" : "[ {\"op\":\"replace\", \"path\": \"/dcrSignatureValidationEnabled\", \"value\":"+response.dcrSignatureValidationEnabled+" } ]")
    And print 'request_body ='+request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    

    Scenario: Patch dcrSignatureValidationSoftwareStatementJwksURIClaim configuration
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
    And def request_body = (response.dcrSignatureValidationSoftwareStatementJwksURIClaim == null ? "[ {\"op\":\"add\", \"path\": \"/dcrSignatureValidationSoftwareStatementJwksURIClaim\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/dcrSignatureValidationSoftwareStatementJwksURIClaim\", \"value\":\""+response.dcrSignatureValidationSoftwareStatementJwksURIClaim+"\" } ]")
    And print 'request_body ='+request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    

    @auth-config-softwareStatementValidationType-patch
  	Scenario: Patch softwareStatementValidationType Auth configuration
  	Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
  	Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    #And request "[ {\"op\":\"replace\", \"path\": \"/softwareStatementValidationType\", \"value\": +(response.softwareStatementValidationType == null ? null : response.softwareStatementValidationType)+ } ,{\"op\":\"replace\", \"path\": \"/softwareStatementValidationClaimName\", \"value\": +(response.softwareStatementValidationClaimName == null ? null : response.softwareStatementValidationClaimName)+ } ]"
    And def request_body = (response.softwareStatementValidationType == null ? "[ {\"op\":\"add\", \"path\": \"/softwareStatementValidationType\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/softwareStatementValidationType\", \"value\":\""+response.softwareStatementValidationType+"\" } ]")
    And print 'request_body ='+request_body
    And request request_body
    Then print request
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    
    @auth-config-cleanServiceBaseDns-patch
  	Scenario: Patch cleanServiceBaseDns Auth configuration  	
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
    And def request_body = (response.cleanServiceBaseDns == null ? "[ {\"op\":\"add\", \"path\": \"/cleanServiceBaseDns\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/cleanServiceBaseDns\", \"value\":"+response.cleanServiceBaseDns+" } ]")
    And print 'request_body ='+request_body
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response
    
    
    @auth-config-statistical-patch
  	Scenario: Patch statistical Auth configuration
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
    #And request "[ {\"op\":\"replace\", \"path\": \"/statEnabled\", \"value\": \"true\" },{\"op\":\"replace\", \"path\": \"/statTimerIntervalInSeconds\", \"value\": \"200\" },{\"op\":\"replace\", \"path\": \"/statWebServiceIntervalLimitInSeconds\", \"value\": \"800\" }  ]"
    And request "[ {\"op\":\"replace\", \"path\": \"/statEnabled\", \"value\": "+response.statEnabled+" },{\"op\":\"replace\", \"path\": \"/statTimerIntervalInSeconds\", \"value\": "+response.statTimerIntervalInSeconds+" },{\"op\":\"replace\", \"path\": \"/statWebServiceIntervalLimitInSeconds\", \"value\": "+response.statWebServiceIntervalLimitInSeconds+" }  ]"
    Then print request
    When method PATCH
    Then status 200
    And print response
    
        
    @auth-config-patch
  	Scenario: Patch loggingLevel Auth configuration
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
    And request "[ {\"op\":\"replace\", \"path\": \"/loggingLevel\", \"value\": \"DEBUG\"  } ]"
    Then print request
    When method PATCH
    Then status 200
    And print response
    

    @auth-config-patch
  	Scenario: Patch cibaEnabled Auth configuration
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
 	And request "[ {\"op\":\"replace\", \"path\": \"/cibaEnabled\", \"value\":"+response.cibaEnabled+" } ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    
    @auth-config-patch
  	Scenario: Patch clientBlackList Auth configuration
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
    #And request "[ {\"op\":\"replace\", \"path\": \"/clientBlackList\", \"value\": ['/*.attacker.com/*','/*.hackers.com/*']\" } ]"
    And def request_body = (response.clientBlackList == null ? "[ {\"op\":\"add\", \"path\": \"/clientBlackList\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/clientBlackList\", \"value\":"+response.clientBlackList+" } ]")
    And print 'request_body ='+request_body
    And request request_body
	Then print request
    When method PATCH
    Then status 200
    And print response    
   

    @auth-config-patch
  	Scenario: Patch clientAuthenticationFilters Auth configuration
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
    #And request "[ {\"op\":\"replace\", \"path\": \"/clientAuthenticationFilters\", \"value\": read('clientAuthenticationFilters.json')\" } ]"
    And def request_body = (response.clientAuthenticationFilters == null ? "[ {\"op\":\"add\", \"path\": \"/clientAuthenticationFilters\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/clientAuthenticationFilters\", \"value\":"+response.clientAuthenticationFilters+" } ]")
    And print 'request_body ='+request_body
    And request request_body
	Then print request
    When method PATCH
    Then status 200
    And print response
    

    @auth-config-patch
  	Scenario: Patch authenticationFilters Auth configuration
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
    And def request_body = (response.authenticationFilters == null ? "[ {\"op\":\"add\", \"path\": \"/authenticationFilters\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/authenticationFilters\", \"value\":"+response.authenticationFilters+"} ]")
    And print 'request_body ='+request_body
    And request request_body
	Then print request
    When method PATCH
    Then status 200
    And print response
    
   
   @auth-config-patch
    Scenario: Patch keyAlgsAllowedForGeneration Auth configuration
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
    #And def request_body = (response.keyAlgsAllowedForGeneration == null ? "[ {\"op\":\"add\", \"path\": \"/keyAlgsAllowedForGeneration\", \"value\":[\"RS256\"\,\"PS256\"] } ]" : "[ {\"op\":\"replace\", \"path\": \"/keyAlgsAllowedForGeneration\", \"value\":"+response.keyAlgsAllowedForGeneration+"} ]")
    And def request_body = (response.keyAlgsAllowedForGeneration == null ? "[ {\"op\":\"add\", \"path\": \"/keyAlgsAllowedForGeneration\", \"value\":null } ]" : "[ {\"op\":\"replace\", \"path\": \"/keyAlgsAllowedForGeneration\", \"value\":"+response.keyAlgsAllowedForGeneration+"} ]")
    And print 'request_body ='+request_body
    And request request_body
    When method PATCH
    Then status 200
    And print response
    
    