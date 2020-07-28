Feature: Openid connect clients

Scenario: Fetch all openid connect clients without bearer token
Given url openidclients_url
When method GET
Then status 401

Scenario: Fetch all openid connect clients
Given url openidclients_url
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200

Scenario: Fetch the first two openidconnect clients
Given url openidclients_url
And  header Authorization = 'Bearer ' + accessToken
And param limit = 2
When method GET
Then status 200
And assert response.length == 2

Scenario: Search openid connect clients given a serach pattern
Given url openidclients_url
And  header Authorization = 'Bearer ' + accessToken
And param pattern = 'oxTrust'
When method GET
Then status 200
