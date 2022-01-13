@ignore
Feature: Verify jans-auth available endpoints.

	Background:
  	* def mainUrl = endpointsUrl
  	
  	@ignore
  	@endpoints-put-json
  	Scenario: Update available endpoints.
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('endpoints.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null

 	@endpoints-get
  	Scenario: Retrieve available endpoints.
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

        
    @endpoints-put
  	Scenario: Update available endpoints.
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response

	@endpoints-error
	Scenario: baseEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.baseEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
      
	@endpoints-error
	Scenario: authorizationEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.authorizationEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: tokenEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.tokenEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: tokenRevocationEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.tokenRevocationEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: userInfoEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.userInfoEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: clientInfoEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.clientInfoEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: endSessionEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.endSessionEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: registrationEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.registrationEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: openIdDiscoveryEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.openIdDiscoveryEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: openIdConfigurationEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.openIdConfigurationEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: idGenerationEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.idGenerationEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: introspectionEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.introspectionEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	@endpoints-error
	Scenario: umaConfigurationEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.umaConfigurationEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	@endpoints-error
	Scenario: oxElevenGenerateKeyEndpoint configuration cannot be null or empty
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
	And assert response.length != null
	Then def result = response 
	Then set result.oxElevenGenerateKeyEndpoint = null
	Given url  mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	And request result
	When method PUT
	Then status 400
	And print response
	
	
	