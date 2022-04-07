function() {

	var stream = read('classpath:karate_jenkins.properties');
	var props = new java.util.Properties();
	props.load(stream);

    var env = props.get('karate.env'); // get java system property 'karate.env'
    karate.configure("ssl", true);
    
    if (!env) {
        env = 'dev'; //env can be anything: dev, qa, staging, etc.
    }
    
    var url = props.get('karate.test.url');
    var port = props.get('karate.test.port');
    var baseUrl = url + (port ? ':' + port : '');
    
    karate.log('karate_jenkins env :', env);
    karate.log('karate_jenkins url :', url);
    karate.log('karate_jenkins port :', port);    
    karate.log('karate_jenkins baseUrl :', baseUrl);
    
    var testStream = read('classpath:test.properties');
    var testProps = new java.util.Properties();
    testProps.load(testStream);
    karate.log(' testProps = '+testProps);
    var testClientId = testProps.get('test.client.id');
    var testClientSecret = testProps.get('test.client.secret');
    var tokenEndpoint = testProps.get('token.endpoint');
    var testScopes = testProps.get('test.scopes');
    var issuer = testProps.get('test.issuer');
    karate.log(' testClientId = '+testClientId);
    karate.log(' testClientSecret = '+testClientSecret);
    karate.log(' tokenEndpoint = '+tokenEndpoint);
    karate.log(' testScopes = '+testScopes);
    karate.log(' issuer = '+issuer);
    
    
    var config = {
        env: env,
        baseUrl: baseUrl,
        testProps: testProps,
        issuer: issuer,
        accessToken: '123',
       
        //scim
        scim_user_url: baseUrl + '/jans-config-api/scim/resource/user',
		scim_config_url: baseUrl + '/jans-config-api/scim/config',
    };

    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);    
    
    var result = karate.callSingle('classpath:token.feature', config);
    print(' result.response = '+result.response);
    config.accessToken = result.response.access_token;
    
    return config;
}