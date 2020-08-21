Feature: Verify OpenId configuration endpoint

	Background:
  	* def mainUrl = openidUrl

 	@openid-get
  	Scenario: Retrieve OpenId configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    

    @openid-put
  	Scenario: Update OpenId configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('config.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    
    @error
    Scenario: Error case for oxOpenIdConnectVersion configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.oxOpenIdConnectVersion = null
    And request request_json
    When method PUT
    Then status 400
    And print response
   

    @error
    Scenario: Error case for issuer configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.issuer = null
    And request request_json
    When method PUT
    Then status 400
    And print response
   
   
    @error
    Scenario: Error case for jwksUri configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.jwksUri = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    

    @error
    Scenario: Error case for tokenEndpointAuthMethodsSupported configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.tokenEndpointAuthMethodsSupported = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for tokenEndpointAuthSigningAlgValuesSupported configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.tokenEndpointAuthSigningAlgValuesSupported = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for serviceDocumentation configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.serviceDocumentation = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    

    @error
    Scenario: Error case for uiLocalesSupported configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.uiLocalesSupported = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for opPolicyUri configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.opPolicyUri = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for opTosUri configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.opTosUri = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for checkSessionIFrame configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.checkSessionIFrame = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for displayValuesSupported configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.displayValuesSupported = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    

    @error
    Scenario: Error case for claimTypesSupported configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.claimTypesSupported = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for claimsLocalesSupported configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.claimsLocalesSupported = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for idTokenTokenBindingCnfValuesSupported configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.idTokenTokenBindingCnfValuesSupported = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for spontaneousScopeLifetime configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.spontaneousScopeLifetime = 0
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for openidSubAttribute configuration validation
 	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    Then def request_json = read('config.json') 
    Then set request_json.openidSubAttribute = null
    And request request_json
    When method PUT
    Then status 400
    And print response
    
    