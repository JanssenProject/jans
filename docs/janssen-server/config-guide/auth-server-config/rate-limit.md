---
tags:
  - administration
  - recipes
  - rate limit
---

# Rate Limit Configuration

AS has built-in rate limiting support. This helps prevent abuse and ensure system stability.
Rate Limit is enabled by default. It can be enabled or disabled by `rate_limit` feature flag.
However it requires exact rate limit rules configuration to effectively apply rate limiting.

Sample rate limit configuration

```json
{
  "rateLimitConfiguration": {
    "rateLimitRules": [
      {
        "path": "/jans-auth/restv1/register",
        "methods": [
          "POST"
        ],
        "requestCount": 5,
        "periodInSeconds": 60,
        "keyExtractors": [
          {
            "source": "body",
            "parameterNames": [
              "software_statement"
            ]
          },
          {
            "source": "header",
            "parameterNames": [
              "X-Real-IP"
            ]
          }
        ]
      },
      {
        "path": "/jans-auth/restv1/register",
        "methods": [
          "POST"
        ],
        "requestCount": 10,
        "periodInSeconds": 60,
        "keyExtractors": [
          {
            "source": "header",
            "parameterNames": [
              "X-Real-IP"
            ]
          }
        ]
      }
    ]
  },
  ...
}
```

## Rate Limit Rules

Rate limiting is applied based on rules which consists of :
- **path** - path of the endpoint, e.g. `/jans-auth/restv1/register`
- **methods** - http methods: `POST`, `PUT`, `GET`, `DELETE`, `PATCH`, `OPTIONS`, `HEAD`
- **requestCount** - requests count
- **periodInSeconds** - period allowed for given requests count 
- **keyExtractors** - array of key extractors
  - **source** - source of key, possible values: `body`, `header`, `query`
  - **parameterNames** - name of parameters to extract

**Key** is value which is dynamically constructed from request by applying key extractors. Rate limiting is made by key.

Here is sample request
```http request
POST /jans-auth/restv1/register
X-ClientCert: test_cert
{ 
  "software_statement": "dummy_ssa", 
  "redirect_uris": ["https://client.example.com/callback", "https://client.example.com/callback2"]
} 
```

Key extractors are set as following

```json
  "keyExtractors": [
        {
          "source": "body",
          "parameterNames": [
            "redirect_uris"
          ]
        },
        {
          "source": "body",
          "parameterNames": [
            "software_statement"
          ]
        },
        {
          "source": "header",
          "parameterNames": [
            "X-ClientCert"
          ]
        }
      ]
```

AS will form following key by applying key extractors on sample request above:

`/jans-auth/restv1/register_[dummy_ssa]_[https://client.example.com/callback, https://client.example.com/callback2]__test_cert__`


## Response

If rate limit is hit then error response `Too Many Requests` is returned

**Sample response**
```
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{"error": "Too many requests"}
```
