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


Scenario: Get an openid connect client by inum(unexisting client)
Given url openidclients_url + '/53553532727272772'
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404

Scenario: Get an openid connect client by inum
Given url openidclients_url + '/1402.aa2b6d33-8ad5-473e-a749-d54b72dc525e'
And  header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
