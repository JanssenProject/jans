
@parallel=false
Feature: Verify LDAP configuration endpoint

  	Background:
  	* def mainUrl = ldapUrl
  	* def result = call read('get-ldap-config.feature');
  	* def funGetLdapConfig =
"""
function(ldap_array,ldap_id) {
print(' ldap_array = '+ldap_array);
print(' ldap_id = '+ldap_id);
var temp;
for (var i = 0; i < ldap_array.length; i++) {
print(' ldap_array[i] = '+ldap_array[i]);
if ( ldap_array[i].configId == ldap_id ){
  	temp = ldap_array[i];
  	print(' temp= '+temp);
}
}
return temp;
}
"""
  	
  	@ldap-config-get-without-bearer-token
    Scenario: Get LDAP configuration By Name without bearer token    
    Given url  mainUrl
    When method GET
    Then status 401
    And print response
    
 	    
    @ldap-config-get-by-name
  	Scenario: Get LDAP configuration By Name    
    And print result
    And def first_response = result.response
    And print first_response
    Given url  mainUrl + '/' +first_response[0].configId
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


    @ldap-config-put
  	Scenario: Update LDAP configuration
  	And print result
    And def first_response = result.response
    And print first_response
    And assert first_response.length != null
    Then def result = first_response[0] 
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response
    And print response.configId
    And print response.version
    
    
    @ignore   
    @ldap-config-delete-by-name-valid
    Scenario: Delete LDAP configuration
	And print result
    And def first_response = result.response
    And print first_response
    And assert first_response.length != null
    Then def data = funGetLdapConfig(first_response,'new_auth_ldap_server')
    And print data
    And match data != null
    And print data.configId
    Given url  mainUrl + '/' +data.configId
    And  header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 204
    And print response
    And assert response.length != null    
       
    
    @ldap-config-patch
	Scenario: Patch LDAP configuration
	And print result
    And def first_response = result.response
    And print first_response
    And assert first_response.length != null
    And print 'Patch - ' + first_response[0].configId
  	Given url  mainUrl + '/' +first_response[0].configId
    And  header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request "[ {\"op\":\"replace\", \"path\": \"/maxConnections\", \"value\": "+first_response[0].maxConnections+"} ]"
	Then print request
    When method PATCH
    Then status 200
    And print response
    
    @ignore
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
 
    @ignore 
    @ldap-config-post-same-name-ldap-error
  	Scenario: Add LDAP configuration with same name as existing
    Given url  mainUrl
    And  header Authorization = 'Bearer ' + accessToken
    And request read('ldap.json')
    When method POST
    Then status 406
    And print response
    
    @ignore
    @ldap-config-test
    Scenario: Test LDAP configuration
    #Given url  mainUrl
    Given url  mainUrl + '/' +'auth_ldap_server'
    And  header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    And def result = response
    And print result
  	Given url  mainUrl + '/test/'
    And  header Authorization = 'Bearer ' + accessToken
  	And request result
    When method POST
    Then status 200
    And print response
    
    
    