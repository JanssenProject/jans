@ignore
Feature: Verify DynamicRegistration configuration endpoint

  	Background:
  	* def mainUrl = dynamicRegistrationUrl
  	
  	@ignore
   	@dynamicRegistration-put-json
  	Scenario: Update Dynamic Registration configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('registration.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null

 	@dynamicRegistration-get
  	Scenario: Retrieve Dynamic Registration configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @dynamicRegistration-put
  	Scenario: Update Dynamic Registration configuration
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
  	Scenario: dynamicRegistrationCustomObjectClass configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.dynamicRegistrationCustomObjectClass = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: defaultSubjectType configuration cannot be other than public or pairwise
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.defaultSubjectType = 'abc'
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: dynamicRegistrationExpirationTime configuration should be int and not decimal
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.dynamicRegistrationExpirationTime = 20.5
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: dynamicGrantTypeDefault configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.dynamicGrantTypeDefault = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response

    
    @error
  	Scenario: dynamicRegistrationCustomAttributes configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.dynamicRegistrationCustomAttributes = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    