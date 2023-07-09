
Feature: Verify Cache refresh configuration endpoint

	Background:cache-refresh
  	* def mainUrl = cacheRefreshUrl

	@ignore
	@cacherefresh-get-error
    Scenario: Retrieve Cache refresh configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
 
    @ignore
 	@cacherefresh-get
  	Scenario: Retrieve Cache refresh configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

	@ignore
    @cacherefresh-put
  	Scenario: Update Cache refresh configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request response
    When method PUT
    Then status 200
    And print response
           


   
   