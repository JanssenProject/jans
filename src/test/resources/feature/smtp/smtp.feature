@ignore
Feature: Configure STMP server

@CreateGetUpdateDelete
Scenario: Setup stmp configuration
Given url smtp_url
And header Authorization = 'Bearer ' + accessToken
And request read('classpath:smtp.json')
When method POST
Then status 201
Given url smtp_url
And header Authorization = 'Bearer ' + accessToken 
When method GET 
Then status 200 
#And assert response.fromEmailAddress == 'test@gmail.com'
And assert response.host == 'smtp.gmail.com'
Then def result = response
#Then set result.fromEmailAddress = 'gluuqa@gmail.com'
Given url smtp_url
And header Authorization = 'Bearer ' + accessToken
And request result
When method PUT
Then status 200
#And assert response.fromEmailAddress == 'gluuqa@gmail.com'
Given url smtp_url
And header Authorization = 'Bearer ' + accessToken
When method DELETE
Then status 204
