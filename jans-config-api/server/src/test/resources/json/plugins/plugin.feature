
Feature: Plugins

Background:
* def mainUrl = plugin_url

Scenario: Fetch all plugin without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all plugin_url
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null


Scenario: Fetch plugin based on name
Given url mainUrl + '/fido2'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response



