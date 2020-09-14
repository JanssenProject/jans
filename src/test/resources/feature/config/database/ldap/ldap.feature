@parallel=false
Feature: Verify LDAP configuration endpoint

  	Background:
  	* def mainUrl = ldapUrl
  	
  	@ignore
  	@ldap-config-delete
  	Scenario: Retrieve LDAP configuration
	Given url  mainUrl + '/new_auth_ldap_server'
    And  header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 204
    And print response
    And assert response.length != null   

 	@ldap-config-get
  	Scenario: Retrieve LDAP configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    
    @ldap-config-get-by-name
  	Scenario: Get LDAP configuration By Name
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response[0].configId
    And print response[0].version
    Given url  mainUrl + '/' +response[0].configId
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null 
    
    @ldap-config-get-by-name-invalid
  	Scenario: Get Non-existing LDAP configuration By Name
    Given url  mainUrl + '/' +'Non-existing-ldap'
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 404
    And print response
    And assert response.length != null    
    
    @ldap-config-delete-by-name-invalid
  	Scenario: Delete Non-existing LDAP configuration By Name
    Given url  mainUrl + '/' +'Non-existing-ldap-XYZ'
    And  header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 404
    And print response
    And assert response.length != null     
    
    @ldap-config-post
  	Scenario: Add LDAP configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('ldap.json')
    When method POST
    Then status 201
    And print response
    And assert response.length != null
    And print response.configId
    And print response.version
    Given url  mainUrl + '/' +response.configId
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.configId
    And print response.version
    Given url  mainUrl + '/' +response.configId
    And  header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 204
    And print response
    And assert response.length != null   
    
    @ldap-config-put
  	Scenario: Update LDAP configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('ldap.json')
    When method POST
    Then status 201
    And print response
    And assert response.length != null
    And print response.configId
    And print response.version
    Given url  mainUrl + '/' +response.configId
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response.configId
    And print response.version
    Then def result = response 
    Then set result.maxConnections = 25
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response
    And print response.configId
    And print response.version
    Given url  mainUrl + '/' +response.configId
    And  header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 204
    And print response
    
    @ldap-config-patch
	Scenario: Patch LDAP configuration
	Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response[0].configId
    And print response[0].version
  	Given url  mainUrl + '/' +response[0].configId
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/maxConnections\", \"value\": 8} ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @ldap-config-test
    Scenario: Test LDAP configuration
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And print response[0].configId
    And print response[0].version
    And def result = response[0] 
  	Given url  mainUrl + '/test/'
    And  header Authorization = 'Bearer ' + accessToken
  	And request result
    When method POST
    Then status 200
    And print response
    
    
    