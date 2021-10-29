@ignore
Feature: Verify RequestObject configuration endpoint

  	Background:
  	* def mainUrl = requestObjectUrl

 	@request_object-get
  	Scenario: Retrieve RequestObject configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @request_object-put
  	Scenario: Update RequestObject configuration
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
    
   