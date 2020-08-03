Feature: Openid connect clients

Scenario: Fetch all openid connect scopes without bearer token
Given url openidscopes_url
When method GET
Then status 401

Scenario: Fetch all openid connect scopes
Given url openidscopes_url
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And assert response.length != null

Scenario: Fetch the first three openidconnect scopes
Given url openidscopes_url
And  header Authorization = 'Bearer ' + accessToken
And param limit = 3
When method GET
Then status 200
And assert response.length == 3

Scenario: Search openid connect scopes given a serach pattern
Given url openidscopes_url
And  header Authorization = 'Bearer ' + accessToken
And param pattern = 'openid'
When method GET
Then status 200
And assert response.length == 1