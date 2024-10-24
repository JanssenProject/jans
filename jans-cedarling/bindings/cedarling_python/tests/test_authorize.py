from cedarling_python import Cedarling
from config import sample_bootstrap_config
from cedarling_python import ResourceData, Request, authorize_errors


# In python unit tests we not cover all possible scenarios, but most common.

# JSON payload of access token
# {
#   "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
#   "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
#   "iss": "https://admin-ui-test.gluu.org",
#   "token_type": "Bearer",
#   "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
#   "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
#   "acr": "basic",
#   "x5t#S256": "",
#   "scope": [
#     "openid",
#     "profile"
#   ],
#   "org_id": "some_long_id",
#   "auth_time": 1724830746,
#   "exp": 1724945978,
#   "iat": 1724832259,
#   "jti": "lxTmCVRFTxOjJgvEEpozMQ",
#   "name": "Default Admin User",
#   "status": {
#     "status_list": {
#       "idx": 201,
#       "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
#     }
#   }
# }
ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s"
# currently id token is unused and it just valid JWT file
ID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s"

# string representation of the authorize decision result
ALLOW_DECISION_STR = "ALLOW"
DENY_DECISION_STR = "DENY"


def test_authorize_ok(sample_bootstrap_config):
    '''
    Test create correct cedarling requst where Resource.org_id is same as Workload.org_id.
    Workload is created from ACCESS_TOKEN
    '''
    instance = Cedarling(sample_bootstrap_config)

    # Create resouce with type "Jans::Issue" from cedar-policy schema.
    resource = ResourceData.from_dict({
        "type": "Jans::Issue",
        "id": "random_id",
        "org_id": "some_long_id"
    })

    request = Request(
        ACCESS_TOKEN,
        ID_TOKEN,
        action='Jans::Action::"Update"',
        context={}, resource=resource)

    authorize_result = instance.authorize(request)
    assert authorize_result.is_allowed(), "request should be allowed"

    workload_result = authorize_result.workload()

    decision = workload_result.decision
    # check that Decision type converts to the string correctly
    assert str(decision) == ALLOW_DECISION_STR
    # check that Decision.value is correct
    assert decision.value == ALLOW_DECISION_STR

    diagnostics = workload_result.diagnostics
    # reason it is just set of strings with ID of policy
    assert diagnostics.reason == {
        '840da5d85403f35ea76519ed1a18a33989f855bf1cf8'}

    assert len(diagnostics.errors) == 0


# function that we will call in tests where check error handling
def raise_authorize_error(sample_bootstrap_config):
    '''
        The function attempts to make an authorized request but raises an error0 (authorize_errors.ResourceEntityError)
        because, in the cedar-policy type Jans::Issue, field `org_id` should be a string.
    '''
    instance = Cedarling(sample_bootstrap_config)

    resource = ResourceData.from_dict({
        "type": "Jans::Issue",
        "id": "random_id",
        "org_id": 1  # it should be string according to schema in policy store
    })

    request = Request(
        ACCESS_TOKEN,
        ID_TOKEN,
        action='Jans::Action::"Update"',
        context={}, resource=resource)

    instance.authorize(request)


def test_resource_entity_error(sample_bootstrap_config):
    '''
    Test catch authorize_errors.ResourceEntityError.
    This error (authorize_errors.ResourceEntityError) is inherited from authorize_errors.AuthorizeError.
    '''
    try:
        raise_authorize_error(sample_bootstrap_config)
    except authorize_errors.ResourceEntityError as e:
        assert str(e) == "could not create resource entity: could not get attribute value from token data error: could not convert json field with key: org_id to: String, got: number"


def test_authorize_error(sample_bootstrap_config):
    '''
    Test catch authorize_errors.AuthorizeError.
    It is the base error for all errors that can be caused by the authorize method of Cedarling.
    '''

    try:
        raise_authorize_error(sample_bootstrap_config)
    except authorize_errors.AuthorizeError as e:
        assert str(e) == "could not create resource entity: could not get attribute value from token data error: could not convert json field with key: org_id to: String, got: number"
