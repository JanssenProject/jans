@ignore
Feature: Verify Metrics configuration endpoint

  	Background:
  	* def mainUrl = metricsUrl
  	
	@metrics-get  	
	Scenario: Retrieve metrics configuration
	Given url mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
    And assert response.length != null
    
    @metrics-put  	
	Scenario: Update metrics configuration
	Given url mainUrl
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
    
    @error
  	Scenario: metricReporterKeepDataDays cannot be less than 1
	Given url mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
    And assert response.length != null
    Then def result = response
    Then set result.metricReporterKeepDataDays = 0
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    
    @error
  	Scenario: metricReporterInterval cannot be less than 1
	Given url mainUrl
	And  header Authorization = 'Bearer ' + accessToken
	When method GET
	Then status 200
	And print response
    And assert response.length != null
    Then def result = response
    Then set result.metricReporterInterval = 0
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
  	