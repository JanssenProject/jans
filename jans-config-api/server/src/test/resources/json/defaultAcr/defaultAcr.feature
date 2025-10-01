
Feature: Verify Default ACRS configuration endpoint

  	Background:
  	* def mainUrl = acrsUrl
  	
    @acrs-get-error
    Scenario: Retrieve ACRS configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response

 	@acrs-get
  	Scenario: Retrieve Default ACRS configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @ignore
    @acrs-put
  	Scenario: Update Default ACRS configuration
  	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def first_response = response 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request first_response 
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
    @ignore
    @acrs-error
    Scenario: Default Authentication Mode configuration cannot be null or blank
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.defaultAcr = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    