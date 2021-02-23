
Feature: Attributes 

Background:
* def mainUrl = attributes_url

Scenario: Fetch all attributes without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 


Scenario: Fetch all attributes 
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And print 'issuer = '+issuer
	#And header Authorization = 'Bearer ' + accessToken
	And header Authorization = 'Bearer ' + 'eyJraWQiOiJlMjMxZjM0My00NmU0LTRlNjEtODdjNy04OWQ3OGM1YjQzOGNfc2lnX2VzMjU2IiwidHlwIjoiSldUIiwiYWxnIjoiRVMyNTYifQ.eyJhdWQiOiJiNmNjOWFkYS1jYTBhLTQyOTQtOWJiZS0yMmNjZWQyOWU5MjEiLCJzdWIiOiJnT0pveU9xUDRrVkJQTDZmRktha1JQVTRrS2dlbExsN1FfNXRkNHgyS09ZIiwieDV0I1MyNTYiOiIiLCJjb2RlIjoiMWFlZTMwMjEtYmRjNS00ODNmLTliNmQtYjVmMmE0YzI2MWFkIiwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9hdHRyaWJ1dGVzLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9hY3JzLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9zY29wZXMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3NjcmlwdHMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2NsaWVudHMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3NtdHAucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2xvZ2dpbmcucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3VtYS9yZXNvdXJjZXMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2RhdGFiYXNlL2xkYXAucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2p3a3MucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2ZpZG8yLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9jYWNoZS5yZWFkb25seSIsImh0dHBzOi8vamFucy5pby9vYXV0aC9qYW5zLWF1dGgtc2VydmVyL2NvbmZpZy9wcm9wZXJ0aWVzLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9kYXRhYmFzZS9jb3VjaGJhc2UucmVhZG9ubHkiXSwiaXNzIjoiaHR0cHM6Ly9jZS1kZXY2LmdsdXUub3JnIiwidG9rZW5fdHlwZSI6ImJlYXJlciIsImV4cCI6MTYxNDA4NDkzNiwiaWF0IjoxNjE0MDgxMzM2LCJjbGllbnRfaWQiOiJiNmNjOWFkYS1jYTBhLTQyOTQtOWJiZS0yMmNjZWQyOWU5MjEifQ.D2iwTDrIGVPqLRuw1nncXbDZDAs8R9xQ-QDOLKkAB9RiFessdnshYl4mh5vuKRUrcamELXn4_DgQXuUPnZmLlA'
	#And header issuer = issuer  
	When method GET 
	Then status 200 
	And print response
	And assert response.length != null 
	And assert response.length >= 10 


Scenario: Fetch the first three attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken
	And param limit = 3 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 3 


Scenario: Search attributes given a search pattern 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param pattern = 'city' 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 1 


Scenario: Fetch the first three active attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	And param status = 'active' 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 3 
	And assert response[0].status == 'ACTIVE'
	And assert response[1].status == 'ACTIVE'
	And assert response[2].status == 'ACTIVE'	


Scenario: Fetch the first three inactive attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	And param status = 'inactive' 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 3 
	And assert response[0].status == 'INACTIVE'
	And assert response[1].status == 'INACTIVE'
	And assert response[2].status == 'INACTIVE'		


@CreateUpdateDelete 
Scenario: Create new attribute 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request read('attribute.json') 
	When method POST 
	Then status 201 
	Then def result = response 
	Then set result.displayName = 'UpdatedQAAddedAttribute' 
	Then def inum_before = result.inum 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And request result 
	When method PUT 
	Then status 200 
	And assert response.displayName == 'UpdatedQAAddedAttribute' 
	And assert response.inum == inum_before 
	Given url mainUrl + '/' +response.inum
	And header Authorization = 'Bearer ' + accessToken 
	When method DELETE 
	Then status 204 


Scenario: Delete a non-existion attribute by inum 
	Given url mainUrl + '/1402.66633-8675-473e-a749'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 
	

Scenario: Get an attribute by inum(unexisting attribute) 
	Given url mainUrl + '/53553532727272772'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 


Scenario: Get an attribute by inum 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 200 
	Given url mainUrl + '/' +response[0].inum
	And header Authorization = 'Bearer ' + accessToken
	When method GET 
	Then status 200
	And print response
	