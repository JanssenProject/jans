Feature: Verify Auth configuration endpoint

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

    
    
    
    
    