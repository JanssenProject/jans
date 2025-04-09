---
tags:
  - cedarling
  - sidecar
---

# Sidecar Overview

The sidecar is a containerized Flask project that uses the `cedarling_python` binding and implements the [AuthZen](https://openid.github.io/authzen/) specification. This image can run alongside another service and uses cedarling to validate evaluation requests against a policy store. 

## Docker setup

- Ensure that you have installed [docker](https://docs.docker.com/engine/install/) 
- Create a file called `bootstrap.json`. You may use this [sample](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/flask-sidecar/secrets/bootstrap.json) file. 
- Modify the bootstrap file to your specifications. In particular you need to provide a link to your policy store in `CEDARLING_POLICY_STORE_URI`. The configuration keys are described [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/cedarling_python.pyi#L10).
- Pull the docker image:
	```
	docker pull ghcr.io/janssenproject/jans/cedarling-flask-sidecar:1.5.0-1
	```
- Run the docker image, replacing `</absolute/path/to/bootstrap.json>` with the absolute path to your bootstrap file: 

	```bash 
	docker run -d \
		-e APP_MODE='development' \
		-e CEDARLING_BOOTSTRAP_CONFIG_FILE=/bootstrap.json \
		-e SIDECAR_DEBUG_RESPONSE=False \
		--mount type=bind,src=</absolute/path/to/bootstrap.json>,dst=/bootstrap.json \
		-p 5000:5000\
		ghcr.io/janssenproject/jans/cedarling-flask-sidecar:1.5.0-1
	```

    - `SIDECAR_DEBUG_RESPONSE` is an option that will cause the sidecar to return extra diagnostic information for each query if set to `True`. This may be useful to check which policies are being used to reach a decision.
    - Take note of the output of the command. This is the container ID of the sidecar.
- The sidecar runs in the background on port 5000. OpenAPI documentation is available at `http://0.0.0.0:5000/swagger-ui`
- To stop the sidecar, run `docker container stop <container ID>`

## Usage

The sidecar has one endpoint: `/cedarling/evaluation`.

Example request to the evaluation endpoint:

```json
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

Cedarling requires OpenID Userinfo, Access, and ID tokens to construct the principal entity, as described [here](./cedarling-authz.md). These values are sent in the subject field's properties. 

Upon creating the principal, action, resource, and context entities, cedarling will evaluate these entities against the policies defined in the policy store. Then it will return a true/false decision. If the decision is false, the sidecar will analyze cedarling diagnostics and provide additional information for the admin.

Example of `true` case:

```json
{
  "decision": true
}
```

Example of `false` case:

```json
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

In this example both the person and workload evaluations were `DENY`, so the decision was false. Additional information is returned in the `context` field.
