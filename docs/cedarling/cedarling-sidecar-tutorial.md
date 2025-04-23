---
tags:
  - administration
  - Cedar
  - Cedarling
  - Quick start guide
  - Cedarling Hello World
---

# Cedarling Sidecar Tutorial

## Introduction

The goal of this tutorial is to show how to use the Cedarling Sidecar. To demonstrate this, 
we'll build a very simple Python Flask API Gateway that calls the Cedarling sidecar Authzen 
REST endpoint to authorize access based on the content of a JWT access token.

### Sequence diagram

```mermaid
sequenceDiagram
title API Gateway
actor User
participant Gateway
participant Sidecar

User->Gateway: GET /protected\nAuthorization: Bearer ...
Gateway->Sidecar: POST /cedarling/evaluation\n{"subject": {"type": "JWT", "id": "cedarling", "properties": {"access_token": "..."} }}
Sidecar->Sidecar: cedarling.authorize(access_token)

alt ALLOW
    Sidecar->Gateway: {"decision": true}
    Gateway->User: 200 {"result": "protected resource"}
else DENY
    Sidecar->Gateway: {"decision": false, "context": {...}}
    Gateway->User: 403 Forbidden
end

```

### Sample Authzen request

```json
{
	"subject": {
		"type": "token_bundle",
		"id": "some_id",
		"properties": {
			"access_token": ""
		}
	},
	"resource": {
		"type": "Jans::HTTP_Request",
		"id": "some_id",
		"properties": {
			"header": {},
			"url": {
				"protocol": "http",
				"host": "www.acme.tld",
				"path": "/protected"
			}
		}
	},
	"action": {
		"name": "Jans::Action::\"GET\""
	},
	"context": {}
}

```

## Prerequisites

Before you begin, make sure you have the following:

* `Docker` installed on your machine
* `Python 3.10` or higher
* `pipx` and `Poetry` installed if you're not using Docker

## Policy Store Setup (Agama Lab)

To begin using Cedarling, you need to set up a policy store. We’ll use this [Agama Lab](https://cloud.gluu.org/agama-lab/login) for this purpose.

1. Sign in to [Agama Lab](https://cloud.gluu.org/agama-lab/login) using your GitHub account and click on Policy Designer.
   ![image](../assets/cedarling-policy-designer.png)
2. Choose a repository to store your Cedarling policies and schemas. Ensure that the repository has at least one commit on the default branch.
   ![image](../assets/cedarling-select-repo.png)
3. After initialization, create a policy store named `gatewayDemo`.
   ![image](../assets/cedarling-policy-store-name.png)
4. Open the policy store and navigate to Policies.
5. Click `Add Policy`, select `Text Editor`.
   ![image](../assets/cedarling-add-policy.png)
6. Paste the following Cedar policy:

    ```bash
    @id("allow_one")
    permit(
      principal is Jans::Workload,
      action == Jans::Action::"GET",
      resource is Jans::HTTP_Request
    )
    when {
      principal has access_token.scope &&
      principal.access_token.scope.contains("openid")
    };
    ```

7. Click `Save`. Agama Lab will validate your policy.
8. Next, click on `Trusted Issuers` and add the following issuer:
  
    * Name: `Gluu`
    * Description: `Gluu`
    * OpenID Configuration Endpoint: `https://account.gluu.org/.well-known/openid-configuration`
    * Copy the URL for your policy store; you'll need it for the sidecar setup.
    ![image](../assets/cedarling-copylink.png)

## Deploy Cedarling Sidecar

### Docker Instructions

Create a file named `bootstrap.json`. You may use the [sample](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/flask-sidecar/secrets/bootstrap.json) file.

* Set `CEDARLING_POLICY_STORE_URI` to the URL you copied from Agama Lab.
* Set `CEDARLING_USER_AUTHZ` to "disabled"
* Set `CEDARLING_TOKEN_CONFIGS` to the following value:

```json
 {
  "access_token": {
    "entity_type_name": "Jans::Access_token",
    "iss": "disabled",
    "aud": "disabled",
    "sub": "disabled",
    "nbf": "disabled",
    "exp": "disabled",
    "jti": "disabled"
   },
  "id_token": {
    "entity_type_name": "Jans::id_token",
    "iss": "disabled",
    "aud": "disabled",
    "sub": "disabled",
    "nbf": "disabled",
    "exp": "disabled",
    "jti": "disabled"
  },
  "userinfo_token": {
    "entity_type_name": "Jans::Userinfo_token",
    "iss": "disabled",
    "aud": "disabled",
    "sub": "disabled",
    "nbf": "disabled",
    "exp": "disabled",
    "jti": "disabled"
  }
}
```

* Set `CEDARLING_MAPPING_WORKLOAD` to `Jans::Workload`

* Set `CEDARLING_ID_TOKEN_TRUST_MODE` to "none"

 Pull the Docker image:

 ```bash
 docker pull ghcr.io/janssenproject/jans/cedarling-flask-sidecar:0.0.0-nightly
 ```

 Run the Docker image, replacing `</absolute/path/to/bootstrap.json>` with the absolute path to your bootstrap file:

```bash
docker run -e APP_MODE='development' -e CEDARLING_BOOTSTRAP_CONFIG_FILE=/bootstrap.json -e SIDECAR_DEBUG_RESPONSE=True --mount type=bind,src=</absolute/path/to/bootstrap.json>,dst=/bootstrap.json -p 5000:5000 -d ghcr.io/janssenproject/jans/cedarling-flask-sidecar:0.0.0-nightly
```

The sidecar is now running on [http://127.0.0.1:5000](http://127.0.0.1:5000). Keep track of the output of the previous command, 
it is your Docker container ID.

### Python instructions (if Docker is unavailable)

* Clone the Janssen repository and navigate to `jans/jans-cedarling/flask-sidecar`.
* Run `poetry install` to install dependencies.
* Download and install the latest Cedarling nightly wheel:

```bash
wget https://github.com/JanssenProject/jans/releases/download/nightly/cedarling_python-0.0.0-cp310-cp310-manylinux_2_31_x86_64.whl
```

* Install the nightly wheel:

```bash
poetry run pip install cedarling_python-0.0.0-cp310-cp310-manylinux_2_31_x86_64.whl
```

* Modify `secrets/bootstrap.json` to your specifications.

* Navigate to `jans/jans-cedarling/flask-sidecar/main`

* Create a file called `.env` and paste in the following content. Alternatively, set the following environment variables:

```
APP_MODE=development
CEDARLING_BOOTSTRAP_CONFIG_FILE=../secrets/bootstrap.json
SIDECAR_DEBUG_RESPONSE=False
```

* Run the sidecar: `poetry run flask run`

* The sidecar is now running on [http://127.0.0.1:5000](http://127.0.0.1:5000)

## Setup Test Gateway

We will use Flask to create a simple API Gateway.

* Create a folder called `demo` and navigate to it.
* Create a virtual environment and activate it:

 ```bash
 python -m venv venv
 source venv/bin/activate
 ```

* Install Flask and requests.

```bash
pip install flask requests

```

* Create a file called `gateway.py` with the following content:

```python
from flask import Flask, abort, request
import requests

app = Flask(__name__)

@app.route("/protected")
def protected():
    token = request.headers.get("Authorization")
    if token is None:
        abort(403)
    token_jwt = token.split(" ")[1]
    payload = {
        "subject": {
            "type": "token_bundle",
            "id": "some_id",
            "properties": {
                "access_token": token_jwt
            }
        },
        "resource": {
            "type": "Jans::HTTP_Request",
            "id": "some_id",
            "properties": {
                "header": {},
                "url": {
                    "protocol": "http",
                    "host": "www.acme.tld",
                    "path": "/protected"
                }
            }
        },
        "action": {
            "name": "Jans::Action::\"GET\""
        },
        "context": {}
    }
    response = requests.post("http://127.0.0.1:5000/cedarling/evaluation", json=payload)
    if response.ok and response.json()["decision"] == True:
        return {"protected_content": "secret"}
    abort(403)

if __name__ == '__main__':
    app.run(port=5001)

```

* Run the gateway:

```bash
flask --app gateway run --port 5001
```

## Test the Setup

* Access the protected endpoint via browser: [http://127.0.0.1:5001/protected](http://127.0.0.1:5001/protected). 
You should get a 403 Forbidden response.

* Access the protected endpoint via curl with the provided JWT:

```bash
curl http://127.0.0.1:5001/protected -H "Authorization: Bearer eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g"
```

* If the JWT is valid and the policy allows access, you should receive the following response:

```json
{
  "protected_content": "secret"
}
```

## Check Logs

The cedarling decision log will be outputted by the Docker container or directly by the API to stdout. In the case of Docker, this can be retrieved like so:

```bash
$ docker logs <container ID>
```

```json
{
  "request_id": "0194cdbc-b8c7-798d-8cc8-fb483448e6fa",
  "timestamp": "2025-02-03T21:34:44.935Z",
  "log_kind": "Decision",
  "pdp_id": "e122cc37-14f7-4033-a547-6791f339218b",
  "policystore_id": "c92b24bb010b772e7702811ae0725986bf7d3b39656e",
  "policystore_version": "1.6.15",
  "principal": "Workload",
  "Workload": {"client_id": "slkacm3cv5-vat1u~elog"},
  "diagnostics": {
    "reason": ["840da5d85403f35ea76519ed1a18a33989f855bf1cf8"],
    "errors": []
  },
  "action": "Jans::Action::\"GET\"",
  "resource": "Jans::HTTP_Request::\"some_id\"",
  "decision": "ALLOW",
  "tokens": {
    "access_token": {
      "jti": "uZUh1hDUQo6PFkBPnwpGzg"
    }
  },
  "decision_time_ms": 0
}
```

## Customizing the policy

Let's add one more check in the policy. This time we want to check if the `acr`
in the access token matches a particular value, for example `simple_password_auth`. 
This change will essentially involve two changes.

1. Update the schema
2. Update the policy

Let's update the schema first using steps the below:

1. Go to the `Policy Designer`.
2. Navigate to `Schema` → `Entity Types`.
3. Edit the `access_token` entity type.
4. Add a new field `acr` with the type set to `String`.
5. Save the schema.

Once the schema has been updated, your new updated Access_token entity type 
json should be similar to the one below:

```json
{
  "shape": {
    "type": "Record",
    "attributes": {
      "aud": {
        "type": "EntityOrCommon",
        "name": "String"
      },
      "exp": {
        "type": "EntityOrCommon",
        "name": "Long"
      },
      "iat": {
        "type": "EntityOrCommon",
        "name": "Long"
      },
      "iss": {
        "type": "EntityOrCommon",
        "name": "TrustedIssuer"
      },
      "jti": {
        "type": "EntityOrCommon",
        "required": false,
        "name": "String"
      },
      "nbf": {
        "type": "EntityOrCommon",
        "required": false,
        "name": "Long"
      },
      "scope": {
        "type": "Set",
        "required": false,
        "element": {
          "type": "EntityOrCommon",
          "name": "String"
        }
      },
      "acr": {
        "type": "String",
        "required": false
      }
    }
  }
}
```

Let's make the corresponding changes in the policy.

```cedar
@id("allow_one")
permit(
  principal is Jans::Workload,
  action == Jans::Action::"GET",
  resource is Jans::HTTP_Request
)
when {
  principal has access_token.acr &&
  principal.acr == "simple_password_auth"
};
```

The policy now checks whether the `acr` value in the access token is present and matches `simple_password_auth`.
