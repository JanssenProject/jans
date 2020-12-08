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
	    accessToken: '6b097721-c70a-4d05-9955-2c61544a4b51',
	    baseUrl: baseUrl,
	    
	    authConfigurationUrl: baseUrl + '/jans-config-api/api/v1/jans-auth-server/config',
	    
	    //openidclients_url: baseUrl + '/jans-config-api/api/v1/openid/clients',
	    //scopes_url: baseUrl + '/jans-config-api/api/v1/scopes',
	    
	    //umaresources_url: baseUrl + '/jans-config-api/api/v1/uma/resources',
	    
    };

    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);
    
    return config;
}