function() {
	
	var stream = read('classpath:karate.properties');
	var props = new java.util.Properties();
	props.load(stream);
	 karate.log('properties= ', props);
	 
    var env = props.get('karate.env'); // get java system property 'karate.env'
    var username = props.get('karate.user');
    var password = props.get('karate.pass');
    karate.log('karate.env selected environment is:', env);
    karate.log('karate user:pwd =', username+':'+password);
    karate.configure("ssl", true);
    
    if (!env) {
    env = 'dev'; //env can be anything: dev, qa, staging, etc.
    }
    
    var baseUrl = props.get('karate.test.url');
    var port = props.get('karate.test.port');
    karate.log('karate baseUrl:port =', baseUrl+':'+port);
    var config = {
	    env: env,
	    
	    //#1 - metrics endpoint
	    metricsUrl: baseUrl + ':' + port + '/api/v1/oxauth/metrics',

	    //#11 - sessionId endpoint
	    sessionidUrl: baseUrl + ':' + port + '/api/v1/oxauth/sessionid',
	    
	    };

    
    karate.configure('connectTimeout', 30000);
    karate.configure('readTimeout', 60000);
    
    return config;
}