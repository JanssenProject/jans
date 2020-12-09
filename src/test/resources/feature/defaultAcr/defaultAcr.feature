@ignore
Feature: Verify Default ACRS configuration endpoint

  	Background:
  	* def mainUrl = acrsUrl

 	@acrs-get
  	Scenario: Retrieve Default ACRS configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @acrs-put
  	Scenario: Update Default ACRS configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('defaultAcr.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
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
    
    @acrs-error
    Scenario: Default setOxTrust Authentication Mode configuration cannot be null or blank
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.oxtrustAcr = ''
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
   