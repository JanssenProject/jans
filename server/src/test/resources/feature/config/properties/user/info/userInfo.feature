@ignore
Feature: Verify UserInfo configuration endpoint

  	Background:
  	* def mainUrl = userInfoUrl

 	@userInfo-get
  	Scenario: Retrieve UserInfo configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
        
    @userInfo-put
  	Scenario: Update UserInfo configuration
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
    
    @userInfo-error
    Scenario: userInfoSigningAlgValuesSupported configuration is null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.userInfoSigningAlgValuesSupported = '' 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @userInfo-error
    Scenario: userInfoEncryptionAlgValuesSupported configuration is null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.userInfoEncryptionAlgValuesSupported = '' 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    @userInfo-error
    Scenario: userInfoEncryptionEncValuesSupported configuration is null or empty
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Then def result = response
    Then set result.userInfoEncryptionEncValuesSupported = '' 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 400
    And print response
    
    
   