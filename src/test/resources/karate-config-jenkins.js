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
    //karate.log('karate baseUrl:port =', baseUrl+':'+port);
    var config = {
	    env: env,

	    // default accessToken

	    accessToken: 'c8dd2445-4734-4119-8dd1-4dbe91976202',

	    // health endpoint
	    healthUrl: baseUrl + '/health',


	    // fido2 configuration endpoint
	    fido2Url: baseUrl + '/api/v1/fido2/config',

	    // ACRS - Default Authentication Mode configuration filter endpoint
	    acrsUrl: baseUrl + '/api/v1/oxauth/acrs',

	    // Auth configuration endpoints.
	    authConfigurationUrl: baseUrl + '/api/v1/oxauth/config/oxauth',

	    // Custom Script configuration endpoints.
	    scriptsUrl: baseUrl + '/api/v1/oxauth/config/scripts',

	    // Cache configuration endpoints.
	    cacheUrl: baseUrl + '/api/v1/oxauth/config/cache',

	    // OpenIdConnect Clients Endpoint
	    openidclients_url: baseUrl + '/api/v1/oxauth/clients',

	    // OpenIdConnect Scopes Endpoint
	    openidscopes_url: baseUrl + '/api/v1/oxauth/scopes',

	    // OpenIdConnect Sectors Endpoint
	    openidsectors_url: baseUrl + '/api/v1/oxauth/openid/sectoridentifiers',

	    // Uma scopes
	    umascopes_url: baseUrl + '/api/v1/oxauth/uma/scopes',

	    // Uma resources
	    umaresources_url: baseUrl + '/api/v1/oxauth/uma/resources',

	    // Uma resources
	    attributes_url: baseUrl + '/api/v1/oxauth/attributes',

	    // Person Scripts
	   // personscripts_url: baseUrl + '/api/v1/oxauth/scripts/person_authn',

	    // Person Scripts
	    smtp_url: baseUrl + '/api/v1/oxauth/config/smtp',

	    };

    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);

    return config;
}