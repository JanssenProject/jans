@ignore
Feature: Verify supported Grant Type configuration endpoint

  	Background:
  	* def mainUrl = grantUrl

 	@grant-get
  	Scenario: Retrieve Grant Type configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @grant-put
  	Scenario: Update Grant Type configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('grant.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
   
   @error
   Scenario: Error case while updating grantTypesSupported configuration min validation
   Given url  mainUrl
   And  header Authorization = 'Bearer ' + accessToken
   When method GET
   Then status 200
   Then def first_response = response 
   Then set first_response.grantTypesSupported = null
   Given url mainUrl
   And  header Authorization = 'Bearer ' + accessToken
   And request first_response
   When method PUT
   Then status 400
   And print response
   
   
   