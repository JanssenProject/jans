Feature: Verify Cors configuration filter endpoint

  	Background:
  	* def mainUrl = corsUrl

 	@cors-get
  	Scenario: Retrieve ResponseMode configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @cors-put
  	Scenario: Update ResponseMode configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('cors.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null
    
   