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
    var config = {
	    env: env,
	    accessToken: 'c8dd2445-4734-4119-8dd1-4dbe91976202',
	    baseUrl: baseUrl,
	    healthUrl: baseUrl + '/health',
	    fido2Url: baseUrl + '/api/v1/fido2/config',
	    acrsUrl: baseUrl + '/api/v1/oxauth/acrs',
	    authConfigurationUrl: baseUrl + '/api/v1/oxauth/config/oxauth',
	    scriptsUrl: baseUrl + '/api/v1/oxauth/config/scripts',
	    cacheUrl: baseUrl + '/api/v1/oxauth/config/cache',
	    ldapUrl: baseUrl + '/api/v1/oxauth/config/database/ldap',
	    openidclients_url: baseUrl + '/api/v1/oxauth/openid/clients',
	    scopes_url: baseUrl + '/api/v1/oxauth/openid/scopes',
	    openidsectors_url: baseUrl + '/api/v1/oxauth/openid/sectoridentifiers',
	    umaresources_url: baseUrl + '/api/v1/oxauth/uma/resources',
	    attributes_url: baseUrl + '/api/v1/oxauth/attributes',
	    smtp_url: baseUrl + '/api/v1/oxauth/config/smtp',
    };

    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);
    
    return config;
}