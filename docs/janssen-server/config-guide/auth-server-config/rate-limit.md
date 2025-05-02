---
tags:
  - administration
  - recipes
  - rate limit
---

# Rate Limit Configuration

AS has built-in rate limiting support. It is helping to prevent abuse and ensure system stability. 
Rate Limit is enabled by default. It can be enabled or disabled by `rate_limit` feature flag.

## Registration Endpoint Rate Limit

In AS, rate limiting is currently supported only for the Registration Endpoint ('/register'). 
Registration Endpoint is limited by: `redirect_uri` and `software_statement`. 
If none of these are present `no_key` key is used for limiting all Registration Endpoint requests globally.

The allowed number of requests is configured using the `rateLimitRegistrationRequestCount` property, while the time period (in seconds) over which these requests are counted is defined by the `rateLimitRegistrationPeriodInSeconds` property.

## Response

If rate limit is hit then error response `Too Many Requests` is returned

**Sample response**
```
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{"error": "Too many requests"}
```