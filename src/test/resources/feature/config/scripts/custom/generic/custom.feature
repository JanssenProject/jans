Feature: Verify Custom Script configuration endpoint

	Background:
  	* def mainUrl = scriptsUrl
  	
  	
 	@scripts-get
  	Scenario: Retrieve Custom Script configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

    @ignore    
    @scripts-put
  	Scenario: Update Custom Script configuration
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

     @ignore
    @scripts-error
  	Scenario: apiKey configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.apiKey = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
