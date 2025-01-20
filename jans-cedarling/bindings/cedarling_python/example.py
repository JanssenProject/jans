# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from cedarling_python import ResourceData, Request
import time

bootstrap_config = BootstrapConfig.load_from_file("./example_files/sample_bootstrap_props.yaml")

# initialize cedarling instance
# all values in the bootstrap_config is parsed and validated at this step.
instance = Cedarling(bootstrap_config)

# Getting logs from memory available only when `"log_type": "memory"`
# returns a list of all active log ids
# active_log_ids = instance.get_log_ids()

# get log entry by id
# log_entry = instance.get_log_by_id(active_log_ids[0])


# show logs; only applicable to MemoryLogConfig logger
# print("Logs stored in memory:")
# print(*instance.pop_logs())


# //// Execute authentication request ////

# field resource_type and id is mandatory
# other fields are attributes of the resource.
resource = ResourceData(resource_type="Jans::Application",
                        id="random_id",
                        app_id="application_id",
                        name="Some Application",
                        url={
                            "host": "jans.test",
                            "path": "/protected-endpoint",
                            "protocol": "http"
                        }
                        )

# or we can init resource using dict
resource = ResourceData.from_dict({
    "type": "Jans::Application",
    "id": "some_id",
    "app_id": "application_id",
    "name": "Some Application",
    "url": {
        "host": "jans.test",
        "path": "/protected-endpoint",
        "protocol": "http"
    }
})

access_token = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiY29kZSI6IjNlMmEyMDEyLTA5OWMtNDY0Zi04OTBiLTQ0ODE2MGMyYWIyNSIsImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhdWQiOiJkN2Y3MWJlYS1jMzhkLTRjYWYtYTFiYS1lNDNjNzRhMTFhNjIiLCJhY3IiOiJzaW1wbGVfcGFzc3dvcmRfYXV0aCIsIng1dCNTMjU2IjoiIiwibmJmIjoxNzMxOTUzMDMwLCJzY29wZSI6WyJyb2xlIiwib3BlbmlkIiwicHJvZmlsZSIsImVtYWlsIl0sImF1dGhfdGltZSI6MTczMTk1MzAyNywiZXhwIjoxNzMyMTIxNDYwLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6InVaVWgxaERVUW82UEZrQlBud3BHemciLCJ1c2VybmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjMwNiwidXJpIjoiaHR0cHM6Ly9qYW5zLnRlc3QvamFucy1hdXRoL3Jlc3R2MS9zdGF0dXNfbGlzdCJ9fX0.Pt-Y7F-hfde_WP7ZYwyvvSS11rKYQWGZXTzjH_aJKC5VPxzOjAXqI3Igr6gJLsP1aOd9WJvOPchflZYArctopXMWClbX_TxpmADqyCMsz78r4P450TaMKj-WKEa9cL5KtgnFa0fmhZ1ZWolkDTQ_M00Xr4EIvv4zf-92Wu5fOrdjmsIGFot0jt-12WxQlJFfs5qVZ9P-cDjxvQSrO1wbyKfHQ_txkl1GDATXsw5SIpC5wct92vjAVm5CJNuv_PE8dHAY-KfPTxOuDYBuWI5uA2Yjd1WUFyicbJgcmYzUSVt03xZ0kQX9dxKExwU2YnpDorfwebaAPO7G114Bkw208g"

"""
Payload of access_token:
{
  "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
  "code": "3e2a2012-099c-464f-890b-448160c2ab25",
  "iss": "https://account.gluu.org",
  "token_type": "Bearer",
  "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
  "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
  "acr": "simple_password_auth",
  "x5t#S256": "",
  "nbf": 1731953030,
  "scope": [
    "role",
    "openid",
    "profile",
    "email"
  ],
  "auth_time": 1731953027,
  "exp": 1732121460,
  "iat": 1731953030,
  "jti": "uZUh1hDUQo6PFkBPnwpGzg",
  "username": "Default Admin User",
  "status": {
    "status_list": {
      "idx": 306,
      "uri": "https://jans.test/jans-auth/restv1/status_list"
    }
  }
}
"""

id_token = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiYnhhQ1QwWlFYYnY0c2J6alNEck5pQSIsInN1YiI6InF6eG4xU2NyYjlsV3RHeFZlZE1Da3ktUWxfSUxzcFphUUE2Znl1WWt0dzAiLCJhbXIiOltdLCJpc3MiOiJodHRwczovL2FjY291bnQuZ2x1dS5vcmciLCJub25jZSI6IjI1YjJiMTZiLTMyYTItNDJkNi04YThlLWU1ZmE5YWI4ODhjMCIsInNpZCI6IjZkNDQzNzM0LWI3YTItNGVkOC05ZDNhLTE2MDZkMmY5OTI0NCIsImphbnNPcGVuSURDb25uZWN0VmVyc2lvbiI6Im9wZW5pZGNvbm5lY3QtMS4wIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYWNyIjoic2ltcGxlX3Bhc3N3b3JkX2F1dGgiLCJjX2hhc2giOiJWOGg0c085Tnp1TEthd1BPLTNETkxBIiwibmJmIjoxNzMxOTUzMDMwLCJhdXRoX3RpbWUiOjE3MzE5NTMwMjcsImV4cCI6MTczMTk1NjYzMCwiZ3JhbnQiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJpYXQiOjE3MzE5NTMwMzAsImp0aSI6ImlqTFpPMW9vUnlXcmdJbjdjSWROeUEiLCJzdGF0dXMiOnsic3RhdHVzX2xpc3QiOnsiaWR4IjozMDcsInVyaSI6Imh0dHBzOi8vamFucy50ZXN0L2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19.Nw7MRaJ5LtDak_LdEjrICgVOxDwd1p1I8WxD7IYw0_mKlIJ-J_78rGPski9p3L5ZNCpXiHtVbnhc4lJdmbh-y6mrD3_EY_AmjK50xpuf6YuUuNVtFENCSkj_irPLkIDG65HeZherWsvH0hUn4FVGv8Sw9fjny9Doi-HGHnKg9Qvphqre1U8hCphCVLQlzXAXmBkbPOC8tDwId5yigBKXP50cdqDcT-bjXf9leIdGgq0jxb57kYaFSElprLN9nUygM4RNCn9mtmo1l4IsdTlvvUb3OMAMQkRLfMkiKBjjeSF3819mYRLb3AUBaFH16ZdHFBzTSB6oA22TYpUqOLihMg"

"""
Payload of id_token:
{
  "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
  "code": "3e2a2012-099c-464f-890b-448160c2ab25",
  "iss": "https://account.gluu.org",
  "token_type": "Bearer",
  "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
  "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
  "acr": "simple_password_auth",
  "x5t#S256": "",
  "nbf": 1731953030,
  "scope": [
    "role",
    "openid",
    "profile",
    "email"
  ],
  "auth_time": 1731953027,
  "exp": 1732121460,
  "iat": 1731953030,
  "jti": "uZUh1hDUQo6PFkBPnwpGzg",
  "username": "Default Admin User",
  "status": {
    "status_list": {
      "idx": 306,
      "uri": "https://jans.test/jans-auth/restv1/status_list"
    }
  }
}
"""

userinfo_token = "eyJraWQiOiJjb25uZWN0X2Y5YTAwN2EyLTZkMGItNDkyYS05MGNkLWYwYzliMWMyYjVkYl9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJxenhuMVNjcmI5bFd0R3hWZWRNQ2t5LVFsX0lMc3BaYVFBNmZ5dVlrdHcwIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInJvbGUiOlsiQ2FzYUFkbWluIl0sImlzcyI6Imh0dHBzOi8vYWNjb3VudC5nbHV1Lm9yZyIsImdpdmVuX25hbWUiOiJBZG1pbiIsIm1pZGRsZV9uYW1lIjoiQWRtaW4iLCJpbnVtIjoiYTZhNzAzMDEtYWY0OS00OTAxLTk2ODctMGJjZGNmNGUzNGZhIiwiY2xpZW50X2lkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwiYXVkIjoiZDdmNzFiZWEtYzM4ZC00Y2FmLWExYmEtZTQzYzc0YTExYTYyIiwidXBkYXRlZF9hdCI6MTczMTY5ODEzNSwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsIm5pY2tuYW1lIjoiQWRtaW4iLCJmYW1pbHlfbmFtZSI6IlVzZXIiLCJqdGkiOiJPSW4zZzFTUFNEU0tBWUR6RU5Wb3VnIiwiZW1haWwiOiJhZG1pbkBqYW5zLnRlc3QiLCJqYW5zQWRtaW5VSVJvbGUiOlsiYXBpLWFkbWluIl19.CIahQtRpoTkIQx8KttLPIKH7gvGG8OmYCMzz7wch6k792DVYQG1R7q3sS9Ema1rO5Fm_GgjOsR0yTTMKsyhHDLBwkDd3cnMLgsh2AwVFZvxtpafTlUAPfjvMAy9YTtkPcY6rNUhsYLSSOA83kt6pHdIv5nI-G6ybqgg-bLBRpwZDoOV0TulRhmuukdiuugTXHT6Bb-K3ZeYs8CwewztnxoFTSDghSzq7VZIraV8SLTBLx5_xswn9mefamyB2XNN3o6vXuMyf4BEbYSCuJ3pu6YtNgfyWwt9cF8PYe4PVLoXZuJKN-cy4qrtgy43QXPCg96jSQUJqgLb5ZL5_3udm2Q"

"""
Payload of userinfo_token:
{
  "sub": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
  "email_verified": true,
  "role": [
    "CasaAdmin"
  ],
  "iss": "https://account.gluu.org",
  "given_name": "Admin",
  "middle_name": "Admin",
  "inum": "a6a70301-af49-4901-9687-0bcdcf4e34fa",
  "client_id": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
  "aud": "d7f71bea-c38d-4caf-a1ba-e43c74a11a62",
  "updated_at": 1731698135,
  "name": "Default Admin User",
  "nickname": "Admin",
  "family_name": "User",
  "jti": "OIn3g1SPSDSKAYDzENVoug",
  "email": "admin@jans.test",
  "jansAdminUIRole": [
    "api-admin"
  ]
}
"""

"""
Policies used:
@444da5d85403f35ea76519ed1a18a33989f855bf1cf8
permit(
    principal is Jans::Workload,
    action in [Jans::Action::"Read"],
    resource is Jans::Application
)when{
    resource.name == "Some Application"
};

@840da5d85403f35ea76519ed1a18a33989f855bf1cf8
permit(
    principal is Jans::User,
    action in [Jans::Action::"Read"],
    resource is Jans::Application
)when{
    resource.name == "Some Application"
};
"""

# Creating context for request
context = {
    "current_time": int(time.time()),
    "device_health": ["Healthy"],
    "fraud_indicators": ["Allowed"],
    "geolocation": ["America"],
    "network": "127.0.0.1",
    "network_type": "Local",
    "operating_system": "Linux",
    "user_agent": "Linux"
}

# Creating cedarling request

action = 'Jans::Action::"Read"'

request = Request(
    tokens={
        "access_token": access_token,
        "id_token": id_token,
        "userinfo_token": userinfo_token,
    }
    action=action,
    resource=resource, context=context)

# Authorize call
authorize_result = instance.authorize(request)

# Print logs from MemoryLogConfig
# print(*instance.pop_logs())

"""
authorize_result.is_allowed() only returns true if 
policies permit both User and Workload authorization
"""
assert authorize_result.is_allowed()


# watch on the decision for workload
workload_result = authorize_result.workload()
print(f"Result of workload authorization: {workload_result.decision}")

# show diagnostic information
workload_diagnostic = workload_result.diagnostics
print("Policy ID used:")
for diagnostic in workload_diagnostic.reason:
    print(diagnostic)

print(f"Errors during authorization: {len(workload_diagnostic.errors)}")
for diagnostic in workload_diagnostic.errors:
    print(diagnostic)

print()

# watch on the decision for person
person_result = authorize_result.person()
print(f"Result of person authorization: {person_result.decision}")
person_diagnostic = person_result.diagnostics
print("Policy ID used:")
for diagnostic in person_diagnostic.reason:
    print(diagnostic)

print(f"Errors during authorization: {len(person_diagnostic.errors)}")
for diagnostic in person_diagnostic.errors:
    print(diagnostic)

print()
