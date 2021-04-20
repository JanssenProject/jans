@ignore
Feature: Verify Cors configuration filter endpoint

  	Background:
  	* def mainUrl = corsUrl

 	@cors-get
  	Scenario: Retrieve Cors configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @cors-put
  	Scenario: Update Cors configuration
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
    And assert response.length != null
    
   