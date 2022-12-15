---
tags:
  - administration
  - planning
  - benchmarking
---

Want confidence that your Janssen platform will perform with the latency and
concurrency you need in production? If so, then you MUST benchmark properly.
It's critical that your test data and transactions mock what you need to prove
in production. For cloud native deployments, benchmarking is also essential
to test auto-scaling and failover. Ideally, you'll test the same hardware
and network.

Remember that OAuth has different flows, which have a different number of steps,
and different requirements for compute and persistence. For example, the
OAuth Client Credential Grant is very short: there is one token request, and
one token response. The OpenID Code Flow has many more steps: it has a
request/response to the authorization, token, Userinfo and logout endpoints.
It's really important to test the exact flow you intend to use in production.

Benchmarking also has the benefit of testing the index configuration in your
database. Every database trades disk space for performance by using indexes.
If you miss an index, performance sometimes comes to a grinding halt. Your
service may even crash. Having confidence that all potential index scenario's
get executed in your benchmarking is essential.

Auth Server publishes [JUnit](https://junit.org) tests and some tools for
generating test data. Because each page rendered by Auth Server has a state,
you cannot use a static load generation page tool. Don't forget to gather data
on compute, storage, and memory. Also watch the resources consumed by the each
web service and system service (e.g. database). You can't find the bottleneck
without this data. System libraries and hardware choices may also impact your
results.

Benchmarking is an iterative process. Inevitably, things never go exactly how
you guessed they would. And you make changes to your environment and run the
benchmarking tests again--*ad naseum*. Leave enough time for benchmarking. How
ever much time you think it will take, triple your estimate.

Make sure you look at both short tests and long running tests. A slow memory
leak may only present after many hours of high load. Also, run some crazy tests,
just so you know what happens. Some organizations benchmark for ten times
normal volume. But when an incident happens, they see 1,000 times normal volume.
Think outside-the-box.  

It's tricky load testing some newer two-factor authentication services,
especially if they rely on new features of the browser. Hopefully the tools to
benchmark these will evolve.

Janssen Auth Server supports a number of deployment models--both VM and cloud
native. While cloud native architecture enables true horizontal scalability,
it comes at a cost. Benchmarking can help you understand if that cost is
justified.

## Load test

**Prerequisite**

1. Create OpenID Connect client with 
   1. Response Types: ['code', 'id_token]
   1. Grant Types: ['authorization_code', `implicit`, 'refresh_token']
   1. Redirect Uri: valid redirect uri which is resolvable by machine which runs this load test
1. Create users by pattern:
   1. username: `test_user1`, `test_user2`, ... `test_userN` 
   1. secret: `test_user_password`
   Following script can be used [add_sequenced_jans_user_rdbm.py](https://github.com/JanssenProject/jans/tree/main/demos/benchmarking/scripts/add_users_rdbm.py)

**User Selection Logic**

`JSR223 PreProcessor : identify user credentials` has following logic for user selection      

```java
double weight = 0.7d;

int min = 1;
int max = 1000;

int minX = 1001;
int maxX = 100000;

float chance = new Random().nextFloat();
if (chance <= weight) {
	min = minX;
	max = maxX;
}

int userNumber = new Random().nextInt(max - min + 1) + min;
String username = "test_user" + userNumber;

//log.info("username: " + username);

vars.put("username", username);
```

Please change it if needed.

**Threads&RampUp**

Configure test script threads properties in `<Root> -> Main` Thread Group inside script, like:

- Number Of Threads
- Ramp-Up Period (in seconds)
- Loop Count (or Forever)

**jmeter in non-GUI mode**

Don't run jmeter in GUI mode for real load. Use non-GUI mode.
```bash
jmeter -n -t Authorization_Code_Flow_jans.jmx
```

### Authorization Code Flow jmeter test

For load testing with Authorization Code Flow jmeter test is used located [here](https://github.com/JanssenProject/jans/blob/main/demos/load-testing/jmeter/test/Authorization%20Code%20Flow_jans.jmx)

1. Configure Script
   1. Open jmeter script by GUI
   1. Set Thread properties according to your needs: `Authorization Code Flow -> Main`
   1. Set Host: `"Authorization Code Flow" -> "User Defined Variables"`: `host`
   1. Update Location Regular Expression: `"Authorization Code Flow" -> "User Defined Variables"`: `location_regexp` - It must match `redirect_uri` correctly to extract `code` returned back by AS.
      Consider following location header: `Location: https://yuriyz-modern-gnat.gluu.info/?code=626651bf-9d3c-430c-b0ec-8724dd065742&scope=openid&session_state=6255bbb4443a76a0f509b604cf651ad281bacbbde241389f2d05859783e41e1f.87f6112c-cb2c-4fe5-bd49-80e46b47e41f`
      RegExp for it is : `Location: https:\/\/yuriyz-modern-gnat\.gluu\.info(.*)`
   1. Put Client details: `"Authorization Code Flow" -> "User Defined Variables"`: `client_id`, `client_secret`, `redirect_uri`
   1. Put User details: set user password to`test_user_password`. 
      If any modification or logic is required for setting username and password please change code in: `Authorization Code Flow` -> `Main` -> `Simple Controller` -> `/login` -> `JSR223 PreProcessor : identify user credentials`
               
If everything was done correctly you should see:
1. in jmeter log correctly parsed code:
```code
 15:04:31 INFO  - jmeter.extractor.JSR223PostProcessor: login post processor, code : , redirect_to:/jans-auth/restv1/authorize?scope=openid+profile+email+user_name&response_type=code&redirect_uri=https%3A%2F%2Fyuriyz-modern-gnat.gluu.info%2F&nonce=nonce&client_id=37d82f27-39f2-423d-85a0-1449b4378b26&sid=5b02e2b5-9403-40a3-a902-ac45e84c6f84 
2022/12/13 15:04:31 INFO  - jmeter.extractor.JSR223PostProcessor: login post processor, code : 626651bf-9d3c-430c-b0ec-8724dd065742, redirect_to:/?code=626651bf-9d3c-430c-b0ec-8724dd065742&scope=openid&session_state=6255bbb4443a76a0f509b604cf651ad281bacbbde241389f2d05859783e41e1f.87f6112c-cb2c-4fe5-bd49-80e46b47e41f 
```
2. `/token` step must be passed successfully (marked=passed which confirmed that `access_token` and `id_token` are present in response).

### Resource Owner Password Grant (ROPC) Flow jmeter test

For load testing with Resource Owner Password Grant (ROPC) Flow jmeter test is used located [here](https://github.com/JanssenProject/jans/blob/main/demos/load-testing/jmeter/test/ResourceOwnerPasswordCredentials_jans.jmx)

1. Configure Script
   1. Open jmeter script by GUI
   1. Set Thread properties according to your needs: `ROPC -> Main`
   1. Set Host: `"ROPC" -> "User Defined Variables"`: `host`
   1. Put Client details: `"ROPC" -> "User Defined Variables"`: `client_id`, `client_secret`
   1. Put User details: set user password to`test_user_password`. 
      If any modification or logic is required for setting username and password please change code in: `ROPC` -> `Main` -> `Simple Controller` -> `/token` -> `JSR223 PreProcessor : identify user credentials`

   
