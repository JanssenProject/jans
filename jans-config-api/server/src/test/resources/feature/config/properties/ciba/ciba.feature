@ignore
Feature: Verify CIBA configuration endpoint

	Background:
  	* def mainUrl = cibaUrl
  	
	@ignore
  	@ciba-put-json
  	Scenario: Update CIBA configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('ciba.json')
    When method PUT
    Then status 200
    And print response
    And assert response.length != null

 	@ciba-get
  	Scenario: Retrieve CIBA configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

        
    @ciba-put
  	Scenario: Update CIBA configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    #Then set result.cibaMaxExpirationTimeAllowedSec = 1000
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response

    
    @ciba-error
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

    
    @ciba-error
  	Scenario: authDomain configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.authDomain = ''
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response

    
    @ciba-error
  	Scenario: databaseURL configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.databaseURL = ''
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @ciba-error
  	Scenario: projectId configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.projectId = ''
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @ciba-error
  	Scenario: storageBucket configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.storageBucket = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @ciba-error
  	Scenario: messagingSenderId configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.messagingSenderId = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @ciba-error
  	Scenario: appId configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.appId = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    
    @ciba-error
  	Scenario: notificationUrl configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.notificationUrl = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    
    @ciba-error
  	Scenario: notificationKey configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.notificationKey = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
   
   
    @ciba-error
  	Scenario: publicVapidKey configuration cannot be null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response 
    Then set result.publicVapidKey = null
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
   
   