@ignore
Feature: Verify idToken configuration endpoint

  	Background:
  	* def mainUrl = idTokenUrl
  	
  	@idtoken-put-json
  	Scenario: Update idToken configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('idToken.json')
    When method PUT
    Then status 200
    And print response

 	@idtoken-get
  	Scenario: Retrieve idToken configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @idtoken-put
  	Scenario: Update idToken configuration
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
    
    @error
  	Scenario: idTokenSigningAlgValuesSupported configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.idTokenSigningAlgValuesSupported = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: idTokenEncryptionAlgValuesSupported configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.idTokenEncryptionAlgValuesSupported = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    
    @error
  	Scenario: idTokenEncryptionEncValuesSupported configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.idTokenEncryptionEncValuesSupported = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    