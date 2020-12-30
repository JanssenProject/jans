@ignore
Feature: Couchbase connection configuration

  Background:
    * def mainUrl = couchbaseUrl

  Scenario: Retrieve Couchbase configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    When method GET
    Then status 200
    And print response
    And assert response.length != null

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

  Scenario: Update configuration
    Given url  mainUrl
    And header Authorization = 'Bearer ' + accessToken
    And request read('couchbase.json')
    When method PUT
    Then status 200
    And print response
    And print response.configId
    And print response.version

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