Feature: Verify backchannel configuration

Background:
  * def mainUrl = backchannelUrl
  
  Scenario: Get backchannel configuration
    Given url  mainUrl
    When method GET
    Then status 200
    #And print response.backchannelAuthenticationResponseInterval
    #And assert (response.backchannelAuthenticationResponseInterval != null && response.backchannelAuthenticationResponseInterval >= 0 && response.backchannelAuthenticationResponseInterval <= 2147483647 && response.backchannelAuthenticationResponseInterval & 1 == 0)
    And print response
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
	backchannelAuthenticationResponseExpiresIn: '#number? _ >= 1 && _ < 2147483647',
	backchannelAuthenticationResponseInterval: '#number? _ >= 1 && _ < 2147483647',
	backchannelLoginHintClaims: '##[] #string'
  }
  """
  
	