Feature: Openid connect clients

Scenario: Fetch all openid connect clients without bearer token
Given url openidclients_url
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200