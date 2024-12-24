
Feature: User endpoint

    Background:
    * def mainUrl = user_url
	* def funGetCustomAttributes =
"""
function(attributes_array,attribute_name) {
print(' attributes_array = '+attributes_array);
print(' attribute_name = '+attribute_name);
var attribute_value;
for (var i = 0; i < attributes_array.length; i++) {
print(' attributes_array[i] = '+attributes_array[i]);
if ( attributes_array[i].name == attribute_name ){
  	attribute_value = attributes_array[i].value;
  	print(' attribute_value= '+attribute_value);
}
}
return attribute_value;
}
"""

Scenario: Fetch all user without bearer token
Given url mainUrl
When method GET
Then status 401


Scenario: Fetch all user
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 200
And print response
And assert response.length != null


Scenario: Fetch the first three users
Given url mainUrl
And header Authorization = 'Bearer ' + accessToken
And param limit = 3
When method GET
Then status 200
And print response
#And assert response.length == 3


Scenario: Get an user by inum(unexisting user)
Given url mainUrl + '/53553532727272772'
And header Authorization = 'Bearer ' + accessToken
When method GET
And print response
Then status 404


Scenario: Delete a non-existion user by inum
Given url mainUrl + '/1402.66633-8675-473e-a749'
And header Authorization = 'Bearer ' + accessToken
When method GET
Then status 404
And print response

@CreateUpdateDelete 
Scenario: Create new user, patch and delete 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('user.json') 
	When method POST 
	Then status 201
    And print response    
	Then def result = response 
    And print result
    And assert result != null
	And assert result.customAttributes.length != null
    #Then def inum = funGetCustomAttributes(result.customAttributes,'inum')
	Then def inum = result.inum
    And print inum
    And assert inum != null
	And print result.userId
	And print 'Patching user ' + '-' +result.userId + '-' +inum
    Given url mainUrl + '/' +inum
	And header Authorization = 'Bearer ' + accessToken 
	And request read('user-patch.json') 
	When method PATCH 
	Then status 200
    And print response    
	Then def result = response 
	And print 'About to delete user ' + '-' +result.userId + '-' +inum
	Given url mainUrl + '/' +inum
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204 
	And print response
    And print 'User successfully deleted'

@ignore
@CreateUpdateDelete 
Scenario: Create new user, update and delete 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('user.json') 
	When method POST 
	Then status 201
    And print response    
	Then def result = response 
    And print result
    And assert result != null
	And assert result.customAttributes.length != null
    Then def inum = funGetCustomAttributes(result.customAttributes,'inum')
    And print inum
    And assert inum != null
    And print 'Updating user ' + '-' +inum
	And print result.userId
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And print result
	And request result 
	When method PUT 
	Then status 200 
    And print response
	And print 'Successfully updated user'
	And print response.userId
	And print 'Patching user ' + '-' +response.userId + '-' +inum
    Given url mainUrl + '/' +inum
	And header Authorization = 'Bearer ' + accessToken 
	And request read('user-patch.json') 
	When method PATCH 
	Then status 200
    And print response    
	Then def result = response 
	 And print 'About to delete user' + '-' +inum
	Given url mainUrl + '/' +inum
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204 
	And print response
    And print 'User successfully deleted'
