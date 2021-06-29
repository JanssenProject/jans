@ignore
Feature: Verify ResponseTypes configuration endpoint

  	Background:
  	* def mainUrl = responsesTypesUrl

 	@responses_types-get
  	Scenario: Retrieve ResponseType configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @responses_types-put
  	Scenario: Update ResponseType configuration
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
    
   