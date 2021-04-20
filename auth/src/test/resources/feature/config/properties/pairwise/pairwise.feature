@ignore
Feature: Verify Pairwise configuration endpoint

  	Background:
  	* def mainUrl = pairwiseUrl

 	@pairwise-get
  	Scenario: Retrieve pairwise configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    
    @pairwise-put
  	Scenario: Update pairwise configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    #Then set result.pairwiseIdType = 'algorithmic'
    #Then set result.pairwiseCalculationKey = 'YQmxW1ciznJW0SojsddI5ksk'
    #Then set result.pairwiseCalculationSalt = 'xewqsr9U3bO7HRAJYmu25'
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response
    
    @error
  	Scenario: pairwiseIdType configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.pairwiseIdType = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: pairwiseIdType configuration cannot be other than persistent or algorithmic
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.pairwiseIdType = 'xyz'
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: pairwiseCalculationKey configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.pairwiseCalculationKey = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @error
  	Scenario: pairwiseCalculationSalt configuration cannot be null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.pairwiseCalculationSalt = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    