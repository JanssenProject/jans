---
tags:
  - administration
  - load balancer
  - nginx
  - apache HTTPD
  - istio
---

Janssen Auth Server is stateless, so you can spool up as many instances as your
load requires. But as there is only one hostname, you need a way to distribute
the requests. Typically, this is where a load balancer comes in.  The load
balancer also terminates the SSL connection, which offloads the cryptographic
compute from the application. In cloud native jargon, load balancing is called
HTTP Ingress.

In your load balancer configuration, you can use any routing algorithm. There
is no need for "sticky sessions", or whatever the load balancer you are using
calls that. For example, it's ok to use even round robin. Although modern HTTP
ingress controllers enable some very flexibility routing options to do A/B
testing and zero downtime updates.

As Janssen Auth Server doesn't really care what load balancer you use, some
options you might want to consider are:

1. **Apache HTTPD**

1. **nginx**

1. **istio**

1. **f5**

1. **Amazon Elastic Load Balancing**

1. **Google Cloud Load Balancing**
