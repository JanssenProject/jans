
Feature: Attributes 

Background:
* def mainUrl = attributes_url

@ignore
Scenario: Fetch all attributes without bearer token 
	Given url mainUrl 
	When method GET 
	Then status 401 


Scenario: Fetch all attributes 
	Given url mainUrl 
	And print 'accessToken = '+accessToken
	And print 'issuer = '+issuer
	And header Authorization = 'Bearer ' + 'eyJraWQiOiJiZTMwZTY4ZC1lZGI5LTRhNzAtOTU0OS03NDFiYmRiYWY4OGFfc2lnX3JzMjU2IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJiNmNjOWFkYS1jYTBhLTQyOTQtOWJiZS0yMmNjZWQyOWU5MjEiLCJzdWIiOiJnT0pveU9xUDRrVkJQTDZmRktha1JQVTRrS2dlbExsN1FfNXRkNHgyS09ZIiwieDV0I1MyNTYiOiIiLCJjb2RlIjoiYTAzMDhlNWYtM2ExMS00YzFkLWI2YzgtN2Y2MjY2OTJjOTZlIiwic2NvcGUiOlsiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9hdHRyaWJ1dGVzLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9hY3JzLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9zY29wZXMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3NjcmlwdHMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2NsaWVudHMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3NtdHAucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2xvZ2dpbmcucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3VtYS9yZXNvdXJjZXMucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2RhdGFiYXNlL2xkYXAucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2p3a3MucmVhZG9ubHkiLCJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL2ZpZG8yLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9jYWNoZS5yZWFkb25seSIsImh0dHBzOi8vamFucy5pby9vYXV0aC9qYW5zLWF1dGgtc2VydmVyL2NvbmZpZy9wcm9wZXJ0aWVzLnJlYWRvbmx5IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2NvbmZpZy9kYXRhYmFzZS9jb3VjaGJhc2UucmVhZG9ubHkiXSwiaXNzIjoiaHR0cHM6Ly9jZS1kZXY2LmdsdXUub3JnIiwidG9rZW5fdHlwZSI6ImJlYXJlciIsImV4cCI6MTYxMzY2MTg4NywiaWF0IjoxNjEzNjYxNTg3LCJjbGllbnRfaWQiOiJiNmNjOWFkYS1jYTBhLTQyOTQtOWJiZS0yMmNjZWQyOWU5MjEifQ.XZlcOzbYm_YfndBkF7zE5Ns96cbODlnF1UWra5u_L-ND045q9137QJa2qWhnWAn5b21fWF9zWM4M13Z6MFe1OB1o5hwB-JTPObQ7h2g_jQBaWh3X2qrEGU_arLwyO70nLbP37wfDEW15d2FpKhkzLEXt436XyEfrzZ2SzruC3qHSjkyDgua8MP6x5VFV1-iNVDrEddSushPSTK7sMhthE9a5oaV6cY0W0c2nSApigjxRsCEHgarukFiBlraHZhj34QoDs-RJYRuTvzfCMdmczbc_ERC_BRFqcQENEH2OviTiDVni3y6lACpy3wj3z1djk9TjQWQdXb4pRXAOMUdjpA'
	#And header issuer = issuer  
	When method GET 
	Then status 200 
	And print response
	And assert response.length != null 
	And assert response.length >= 10 

@ignore
Scenario: Fetch the first three attributes 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param limit = 3 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 3 

@ignore
Scenario: Search attributes given a search pattern 
	Given url mainUrl
	And header Authorization = 'Bearer ' + accessToken 
	And param pattern = 'city' 
	When method GET 
	Then status 200
	And print response 
	And assert response.length == 1 

@ignore
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

@ignore
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

@ignore
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

@ignore
Scenario: Delete a non-existion attribute by inum 
	Given url mainUrl + '/1402.66633-8675-473e-a749'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 
	
@ignore
Scenario: Get an attribute by inum(unexisting attribute) 
	Given url mainUrl + '/53553532727272772'
	And header Authorization = 'Bearer ' + accessToken 
	When method GET 
	Then status 404 

@ignore
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
	