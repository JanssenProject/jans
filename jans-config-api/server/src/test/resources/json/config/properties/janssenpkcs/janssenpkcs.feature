@ignore
Feature: Verify JanssenPKCS configuration endpoint

  	Background:
  	* def mainUrl = janssenPKCSUrl

 	@janssenPKCS-get
  	Scenario: Retrieve JanssenPKCS configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @janssenPKCS-put
  	Scenario: Update JanssenPKCS configuration
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
    
   