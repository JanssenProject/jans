Feature: Verify backchannel configuration endpoint

Background:
  * def mainUrl = backchannelUrl
   
  @backchannel-get
  Scenario: Retrieve backchannel configuration
    Given url  mainUrl
    When method GET
    Then status 200
    #And print response
    And match response == 
     """
  { 
    backchannelClientId: '##string',
	backchannelRedirectUri: '##string',
    backchannelAuthenticationEndpoint: '##string',
	backchannelDeviceRegistrationEndpoint: '##string',
	backchannelTokenDeliveryModesSupported: '##[] #string',
	backchannelAuthenticationRequestSigningAlgValuesSupported: '##[] #string',
	backchannelUserCodeParameterSupported: '##boolean',
	backchannelBindingMessagePattern: '##string',
	backchannelAuthenticationResponseExpiresIn: '#number? _ >= 1 && _ <= 2147483647',
	backchannelAuthenticationResponseInterval: '#number? _ >= 1 && _ <= 2147483647',
	backchannelLoginHintClaims: '##[] #string'
  }
  """
   
   @backchannel-put
   Scenario: Update backchannel configuration
   Given url  mainUrl
   When method GET
   Then status 200
   #And print response
   Then def first_response = response 
   #And print first_response
   Given url  mainUrl
   And request first_response
   When method PUT
   Then status 200
   And print response
  
	