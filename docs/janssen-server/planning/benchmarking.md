---
tags:
  - administration
  - planning
  - benchmarking
---
# Benchmarking

Want confidence that your Janssen platform will perform with the latency and
concurrency you need in production? If so, then you MUST benchmark properly.
It's critical that your test data and transactions mock what you need to prove
in production. For cloud-native deployments, benchmarking is also essential
to test auto-scaling and failover. Ideally, you'll test the same hardware
and network.

Remember that OAuth has different flows, which have a different number of steps,
and different requirements for compute and persistence. For example, the
OAuth Client Credential Grant is very short: there is one token request and
one token response. The OpenID Code Flow has many more steps: it has a
request/response to the authorization, token, Userinfo and logout endpoints.
It's really important to test the exact flow you intend to use in production.

Benchmarking also has the benefit of testing the index configuration in your
database. Every database trades disk space for performance by using indexes.
If you miss an index, performance sometimes comes to a grinding halt. Your
service may even crash. Having confidence that all potential index scenarios
get executed in your benchmarking is essential.

Auth Server publishes [JUnit](https://junit.org) tests and some tools for
generating test data. Because each page rendered by Auth Server has a state,
you cannot use a static load generation page tool. Don't forget to gather data
on compute, storage, and memory. Also, watch the resources consumed by each
web service and system service (e.g. database). You can't find the bottleneck
without this data. System libraries and hardware choices may also impact your
results.

Benchmarking is an iterative process. Inevitably, things never go exactly how
you guessed they would. And you make changes to your environment and run the
benchmarking tests again--*ad nauseam*. Leave enough time for benchmarking. However 
much time you think it will take, triple your estimate.

Make sure you look at both short tests and long running tests. A slow memory
leak may only present after many hours of high load. Also, run some crazy tests,
just so you know what happens. Some organizations benchmark for ten times the
normal volume. But when an incident happens, they see 1,000 times the normal volume.
Think outside-the-box.  

It's tricky load testing some newer two-factor authentication services,
especially if they rely on new features of the browser. Hopefully the tools to
benchmark these will evolve.

Janssen Auth Server supports a number of deployment models--both VM and cloud-native. 
While cloud-native architecture enables true horizontal scalability,
it comes at a cost. Benchmarking can help you understand if that cost is
justified.

## Load test

In cloud-native architecture, the load testing is executed via k8s pods. 

### Authorization Code Flow jmeter load test

For load testing with Authorization Code Flow jmeter test, the following [script](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/demos/benchmarking/docker-jans-loadtesting-jmeter/scripts/tests/authorization_code_flow.jmx) is used.

See [Authorization code flow recipe](../recipes/benchmark.md#authorization-code-flow) for details.

### Resource Owner Password Grant (ROPC) Flow jmeter load test

For load testing with Resource Owner Password Grant (ROPC) Flow jmeter test, the following [script](https://github.com/JanssenProject/jans/blob/vreplace-janssen-version/demos/benchmarking/docker-jans-loadtesting-jmeter/scripts/tests/resource_owner_password_credentials.jmx) is used.  

See [ROPC flow recipe](../recipes/benchmark.md#resource-owner-password-credentials-ropc-flow) for details.
