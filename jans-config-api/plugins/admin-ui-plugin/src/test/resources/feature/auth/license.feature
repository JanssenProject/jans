# new feature
# Tags: optional

Feature: Test Audit logging endpoints of admin-ui

  Scenario: Testing 'checkLicense' GET endpoint
    Given url checkLicenseURL
    When method GET
    Then status 200
    And print response
