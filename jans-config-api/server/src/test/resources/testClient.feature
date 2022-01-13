@ignore
Feature: This Feature is to get token to test the test cases

Background:
* def mainUrl = test_url

Scenario: Get Token
Given url mainUrl
And print url
And request ''
When method POST
Then status 204
And print response
