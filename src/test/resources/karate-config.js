function() {
	
	var stream = read('classpath:karate.properties');
	var props = new java.util.Properties();
	props.load(stream);
	//karate.log('properties= ', props);
	 
    var env = props.get('karate.env'); // get java system property 'karate.env'
    var username = props.get('karate.user');
    var password = props.get('karate.pass');
    //karate.log('karate.env selected environment is:', env);
    //karate.log('karate user:pwd =', username+':'+password);
    karate.configure("ssl", true);
    
    if (!env) {
    env = 'dev'; //env can be anything: dev, qa, staging, etc.
    }
    
    var baseUrl = props.get('karate.test.url');
    var port = props.get('karate.test.port');
    //karate.log('karate baseUrl:port =', baseUrl+':'+port);
    var config = {
	    env: env,
	    
	    // default accessToken
	    
	    accessToken: 'c8dd2445-4734-4119-8dd1-4dbe91976202',
	    
	    // health endpoint
	    healthUrl: baseUrl + ':' + port + '/health',
	    
	    // backchannel endpoint
	    backchannelUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/backchannel',
	    
	    // Metrics endpoint
	    metricsUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/metrics',
	    
	    // DynamicRegistration endpoint
	    dynamicRegistrationUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/dyn_registration',
	    
	    // ResponsesTypes endpoint
	    responsesTypesUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/responses_types',
	    
	    // ResponseMode endpoint
	    responseModeUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/responses_modes',
	    
	    // JanssenPKCS endpoint
	    janssenPKCSUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/janssenpkcs',
	    
	    // UserInfo endpoint
	    userInfoUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/user_info',
	    
	    // RequestObject endpoint
	    requestObjectUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/request_object',
	    
	    // UmaConfiguration endpoint
	    umaConfigurationUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/uma',
	    
	    // idToken endpoint
	    idTokenUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/idtoken',

	    // SessionId endpoint
	    sessionIdUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/sessionid',
	    
	    // pairwise configuration endpoint
	    pairwiseUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/pairwise',
	    
	    // CIBA configuration endpoint
	    cibaUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/ciba',
	    
	    // fido2 configuration endpoint
	    fido2Url: baseUrl + ':' + port + '/api/v1/fido2/config',
	    
	    // CORS configuration filter endpoint
	    corsUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/cors',
	    
	    // ACRS - Default Authentication Mode configuration filter endpoint
	    acrsUrl: baseUrl + ':' + port + '/api/v1/oxauth/acrs',
	    
	    // Endpoints - available oxAuth endpoints.
	    endpointsUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/endpoints',

	    // Grant Type - Supported oxAuth Grant Type endpoints.
	    grantUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/grant',
	    
	    // OpenId - OpenID Connect configuration endpoints.
	    openidUrl: baseUrl + ':' + port + '/api/v1/oxauth/config/properties/openid',
	    
	    // OpenIdConnect Clients Endpoint
	    openidclients_url: baseUrl + ':' + port + '/api/v1/oxauth/clients',
	    
	    // OpenIdConnect Scopes Endpoint
	    openidscopes_url: baseUrl + ':' + port + '/api/v1/oxauth/scopes',
	    
	    // OpenIdConnect Sectors Endpoint
	    openidsectors_url: baseUrl + ':' + port + '/api/v1/oxauth/openid/sectoridentifiers',
	    
	    // Uma scopes
	    umascopes_url: baseUrl + ':' + port + '/api/v1/oxauth/uma/scopes',
	    
	    // Uma resources
	    umaresources_url: baseUrl + ':' + port + '/api/v1/oxauth/uma/resources',
	    
	    // Uma resources
	    attributes_url: baseUrl + ':' + port + '/api/v1/oxauth/attributes',
	    
	    // Person Scripts
	    personscripts_url: baseUrl + ':' + port + '/api/v1/oxauth/scripts/person_authn',
	    
	    };
    
    

    
    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);
    
    return config;
}