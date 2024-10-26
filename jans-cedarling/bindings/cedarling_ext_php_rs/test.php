<?php

$token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s";

$payload_str = "some_long_id";

var_dump(cedarling_authorize_test($token,$payload_str));//we pass ID of Organization into $payload_str parameter. 


/*
Later, within rust code we check : principal.org_id == resource.org_id  from cedar policy:

permit(
    principal is Jans::Workload,
    action in [Jans::Action::"Update"],
    resource is Jans::Issue
)when{
    principal.org_id == resource.org_id
};

Value ,"org_id":"some_long_id" is passwed in access token which is base64 encoded

Decoded value of $token:


decoded access_token = 
{"alg":"HS256","typ":"JWT"}{"sub":"boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0","code":"bf1934f6-3905-420a-8299-6b2e3ffddd6e","iss":"https://admin-ui-test.gluu.org","token_type":"Bearer","client_id":"5b4487c4-8db1-409d-a653-f907b8094039","aud":"5b4487c4-8db1-409d-a653-f907b8094039","acr":"basic","x5t#S256":"","scope":["openid","profile"],"org_id":"some_long_id","auth_time":1724830746,"exp":1724945978,"iat":1724832259,"jti":"lxTmCVRFTxOjJgvEEpozMQ","name":"Default Admin User","status":{"status_list":{"idx":201,"uri":"https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"}}}



*/



