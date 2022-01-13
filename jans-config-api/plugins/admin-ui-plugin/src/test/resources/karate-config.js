function() {
	var stream = read('classpath:karate.properties');
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
    
    karate.log('karate env :', env);
    karate.log('karate url :', url);
    karate.log('karate port :', port);
    karate.log('karate baseUrl :', baseUrl);

    var testStream = read('classpath:test.properties');
    var testProps = new java.util.Properties();
    testProps.load(testStream);
    karate.log(' testProps = '+testProps);

    var grantType = testProps.get('test.grant.type');
    var clientId = testProps.get('test.client.id');
    var clientSecret = testProps.get('test.client.secret');
    var scopes = testProps.get('test.scopes');
    var issuer = testProps.get('test.issuer');

    karate.log(' grantType = '+grantType);
    karate.log(' clientId = '+clientId);
    karate.log(' clientSecret = '+clientSecret);
    karate.log(' scopes = '+scopes);
    karate.log(' issuer = '+issuer);

    var config = {
        env: env,
        baseUrl: baseUrl,
        testProps: testProps,
        issuer: issuer,
        accessToken: '123',
        grantType: grantType,
        clientId: clientId,
        clientSecret: clientSecret,
        scopes: scopes,
        authzurl: issuer + '/jans-auth/authorize.htm',
        adminUIConfigURL: issuer + '/jans-config-api/admin-ui/oauth2/config',
        apiProtectionTokenURL: issuer + '/jans-config-api/admin-ui/oauth2/api-protection-token',
        checkLicenseURL: issuer + '/jans-config-api/admin-ui/license/checkLicense',
        getLicenseDetailsURL: issuer + '/jans-config-api/admin-ui/license/getLicenseDetails',
        getLicenseDetailsURL: issuer + '/jans-config-api/admin-ui/license/getLicenseDetails',
        getAuditLoggingURL: issuer + '/jans-config-api/admin-ui/logging/audit',
    };

    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);    
    
    var result = karate.callSingle('classpath:authzCode.feature', config);
    print(' result.response = '+result.response);
    //config.accessToken = result.response.access_token;
    
    return config;
}