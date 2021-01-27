
Feature: Openid connect clients

@ignore
Scenario: Fetch all openid connect clients without bearer token
Given url openidclients_url
When method GET
Then status 401

Scenario: Fetch all openid connect clients
Given url openidclients_url
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null

@ignore
Scenario: Fetch the first three openidconnect clients
Given url openidclients_url
And header Authorization = 'Bearer ' + accessToken
And param limit = 3
When method GET
Then status 200
And print response
And assert response.length == 3

@ignore
Scenario: Search openid connect clients given a serach pattern
Given url openidclients_url
And header Authorization = 'Bearer ' + accessToken
And param pattern = 'oxTrust'
When method GET
Then status 200
And print response
And assert response.length == 1

@ignore
Scenario: Get an openid connect client by inum(unexisting client)
Given url openidclients_url + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404

@ignore
Scenario: Get an openid connect client by inum
Given url openidclients_url
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
Given url openidclients_url + '/' +response[0].inum
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response

@ignore
@CreateUpdateDelete
Scenario: Create new OpenId Connect Client
Given url openidclients_url
And header Authorization = 'Bearer ' + accessToken
And request read('client.json')
When method POST
Then status 201
Then def result = response
Then set result.displayName = 'UpdatedQAAddedClient'
Given url openidclients_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
And assert response.displayName == 'UpdatedQAAddedClient'
Given url openidclients_url + '/' +response.inum
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204

@ignore
Scenario: Delete a non-existion openid connect client by inum
Given url openidclients_url + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
