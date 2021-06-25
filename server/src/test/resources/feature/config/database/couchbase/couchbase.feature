
Feature: Couchbase connection configuration

  Background:
    * def mainUrl = couchbaseUrl

  Scenario: Retrieve Couchbase configuration without bearer token
    Given url  mainUrl
    When method GET
    Then status 401
    And print response


  Scenario: Retrieve Couchbase configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response

    
  Scenario: Retrieve Couchbase configuration by name
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And param name = response.name
    When method GET
    Then status 200
    And print response
    And assert response.length != null

@ignore
    @CreateGetUpdateDelete
    Scenario: Setup CB configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken 
    When method GET
    Then status 200
    And print response
    And def cbConf = (response.length != null ? response : read('couchbase.json'))
    And print cbConf
    Given url mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And request cbConf
    When method POST
    Then status 201
    And print response
    Then def result = response
    Given url mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And request result
    When method PUT
    Then status 200
    And print response
    Given url mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And param name = response.name
    When method DELETE
    Then status 204
    And print response
   
@ignore
    @Patch
    Scenario: Patchs CB configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken 
    When method GET
    Then status 200
    And print response
    And def request_body = "[ {\"op\":\"replace\", \"path\": \"/connectTimeout\", \"value\":"+response.connectTimeout+"} ]"
    And print 'request_body ='+request_body
    Given url  mainUrl
    And param name = response.name
    And header Authorization = 'Bearer ' + accessToken
    And header Content-Type = 'application/json-patch+json'
    And header Accept = 'application/json'
    And request request_body
    Then print request
    When method PATCH
    Then status 200
    And print response

@ignore
  Scenario: Add configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And request read('couchbase.json')
    When method POST
    Then status 201
    Then def result = response.configId
    And print response
    And assert response.length != null
    And print response.configId
    And print response.version

@ignore
  Scenario: Update configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And request read('couchbase.json')
    When method PUT
    Then status 200
    And print response
    And print response.configId
    And print response.version

    
@ignore
  Scenario: Delete configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And request read('couchbase_delete.json')
    When method POST
    Then status 201
    Then def result = response.configId
    And print response
    And assert response.length != null
    And print response.configId
    And print response.version
    Given url  mainUrl + '/couchbase_server_delete'
    And header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 204
    And print response
    And assert response.length != null

  Scenario: Delete Non-existing configuration
    Given url  mainUrl + '/Non-existing-ldap-XYZ'
    And header Authorization = 'Bearer ' + accessToken
    When method DELETE
    Then status 404
    And print response
    And assert response.length != null