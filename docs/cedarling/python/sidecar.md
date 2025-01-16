---
tags:
  - cedarling
  - python
  - sidecar
---

# Flask Sidecar

The sidecar is a containerized Flask project that uses the `cedarling_python` binding and implements the [AuthZen](https://openid.github.io/authzen/) specification. This image can run alongside another service and uses cedarling to validate evaluation requests against a policy store. 

## Docker setup

- Ensure that you have installed [docker](https://docs.docker.com/engine/install/) and [docker compose](https://docs.docker.com/compose/install/). 
- Clone the [Janssen](https://github.com/JanssenProject/jans) repository
- Navigate to `jans/jans-cedarling/flask-sidecar`
- Edit the provided `secrets/bootstrap.json` file to your specifications. The configuration keys are described [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/cedarling_python.pyi#L10). 
- Run `docker compose up`
  - For cloud deployments, please use the provided Dockerfile and pass your bootstrap configuration via the environment variable `CEDARLING_BOOTSTRAP_CONFIG_FILE`.
- The sidecar runs on port 5000. OpenAPI documentation is available at `http://0.0.0.0:5000/swagger-ui`

## Usage

The sidecar has one endpoint: `/cedarling/evaluation`.

Example request to the evaluation endpoint:

```
{
	"subject": {
		"type": "JWT",
		"id": "cedarling",
		"properties": {
			"access_token": "",
			"id_token": "",
			"userinfo_token": ""
		}
	},
	"resource": {
		"type": "Jans::Application",
		"id": "some_id",
		"properties": {
			"app_id": "application_id",
			"name": "Some Application",
			"url": {
				"host": "jans.test",
				"path": "/protected-endpoint",
				"protocol": "http"
			}
		}
	},
	"action": {
		"name": "Jans::Action::\"Read\""
	},
	"context": {
		"device_health": [
			"Healthy"
		],
		"fraud_indicators": [
			"Allowed"
		],
		"geolocation": [
			"America"
		],
		"network": "127.0.0.1",
		"network_type": "Local",
		"operating_system": "Linux",
		"user_agent": "Linux",
		"current_time": 1
	}
}
```

Cedarling requires OpenID Userinfo, Access, and ID tokens to construct the principal entity, as described [here](../cedarling-authz.md). As per AuthZen specification, these values are sent in the `context` field of the payload. Conversely, the `subject` field is currently not used by cedarling. These 3 tokens are subsequently removed from the context object before it is passed to cedarling.

Upon creating the principal, action, resource, and context entities, cedarling will evaluate these entities against the policies defined in the policy store. Then it will return a true/false decision. If the decision is false, the sidecar will analyze cedarling diagnostics and provide additional information for the admin.

Example of `true` case:

```
{
  "decision": true
}
```

Example of `false` case:

```
{
  "context": {
    "reason_admin": {
      "person diagnostics": [],
      "person error": [],
      "person evaluation": "DENY",
      "workload diagnostics": [],
      "workload evaluation": "DENY",
      "workload_error": []
    }
  },
  "decision": false
}
```

In this example both the person and workload evaluations were `DENY`, so the decision was false. Additional information is returned in the `context` field as per AuthZen specification.
