@ignore
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
    When method GET
    Then status 200
    #And print response
    Then def first_response = response 
    #And print first_response
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 200
    And print response
    
    
    @error
    Scenario: Error case for oxOpenIdConnectVersion configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.oxOpenIdConnectVersion = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
  
    @error
    Scenario: Error case for issuer configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.issuer = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
       
    @error
    Scenario: Error case for jwksUri configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.jwksUri = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
   

    @error
    Scenario: Error case for tokenEndpointAuthMethodsSupported configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.tokenEndpointAuthMethodsSupported = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
       
    
    @error
    Scenario: Error case for tokenEndpointAuthSigningAlgValuesSupported configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.tokenEndpointAuthSigningAlgValuesSupported = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for serviceDocumentation configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.serviceDocumentation = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    

    @error
    Scenario: Error case for uiLocalesSupported configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.uiLocalesSupported = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for opPolicyUri configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.opPolicyUri = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for opTosUri configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.opTosUri = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for checkSessionIFrame configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.checkSessionIFrame = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response

    
    @error
    Scenario: Error case for displayValuesSupported configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.displayValuesSupported = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response


    @error
    Scenario: Error case for claimTypesSupported configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.claimTypesSupported = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    
    
    @error
    Scenario: Error case for claimsLocalesSupported configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.claimsLocalesSupported = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
       
    
    @error
    Scenario: Error case for idTokenTokenBindingCnfValuesSupported configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.idTokenTokenBindingCnfValuesSupported = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
    

    @error
    Scenario: Error case for spontaneousScopeLifetime configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.spontaneousScopeLifetime = 0
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response
   
    
    @error
    Scenario: Error case for openidSubAttribute configuration validation
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    Then def first_response = response 
    Then set first_response.openidSubAttribute = null
    Given url mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response
    When method PUT
    Then status 400
    And print response

    