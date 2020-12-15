Feature: Attributes 

@ignore
Scenario: Fetch all attributes without bearer token 
	Given url attributes_url 
	When method GET 
	Then status 401 

Scenario: Fetch all attributes 
	Given url attributes_url 
	And  header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	And assert response.length != null 
	And assert response.length >= 10 

@ignore
Scenario: Fetch the first three attributes 
	Given url attributes_url 
	And  header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	When method GET 
	Then status 200 
	And assert response.length == 3 

@ignore
Scenario: Search attributes given a search pattern 
	Given url attributes_url 
	And  header Authorization = 'Bearer ' + accessToken 
	And param pattern = 'city' 
	When method GET 
	Then status 200 
	And assert response.length == 1 

@ignore
Scenario: Fetch the first three active attributes 
	Given url attributes_url 
	And  header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	And param status = 'active' 
	When method GET 
	Then status 200 
	And assert response.length == 3 
	And assert response[0].status == 'ACTIVE'
	And assert response[1].status == 'ACTIVE'
	And assert response[2].status == 'ACTIVE'	


Scenario: Fetch the first three inactive attributes 
	Given url attributes_url 
	And  header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	And param status = 'inactive' 
	When method GET 
	Then status 200 
	And assert response.length == 3 
	And assert response[0].status == 'INACTIVE'
	And assert response[1].status == 'INACTIVE'
	And assert response[2].status == 'INACTIVE'		

@ignore
@CreateUpdateDelete 
Scenario: Create new attribute 
	Given url attributes_url 
	And header Authorization = 'Bearer ' + accessToken 
	And request read('attribute.json') 
	When method POST 
	Then status 201 
	Then def result = response 
	Then set result.displayName = 'UpdatedQAAddedAttribute' 
	Then def inum_before = result.inum 
	Given url attributes_url 
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And assert response.displayName == 'UpdatedQAAddedAttribute' 
	And assert response.inum == inum_before 
	Given url attributes_url + '/' +response.inum 
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204 

@ignore
Scenario: Delete a non-existion attribute by inum 
	Given url attributes_url + '/1402.66633-8675-473e-a749' 
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 
	
@ignore
Scenario: Get an attribute by inum(unexisting attribute) 
	Given url attributes_url + '/53553532727272772' 
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 

@ignore
Scenario: Get an attribute by inum 
	Given url attributes_url 
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	Given url attributes_url + '/' +response[0].inum 
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200