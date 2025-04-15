# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import Cedarling
from cedarling_python import EntityData, RequestUnsigned, authorize_errors
from config import load_bootstrap_config, TEST_FILES_PATH
from os.path import join
import json


# In python unit tests we not cover all possible scenarios, but most common.

# in fixture `load_bootstrap_config` we use policy store `policy-store_ok.json`
# The human-readable policy and schema file is located in next folder:
# `test_files\policy-store_ok_2.yaml`

POLICY_STORE_LOCATION = join(TEST_FILES_PATH, "policy-store_ok_2.yaml")


# Create resouce with type "Jans::Issue" from cedar-policy schema.
RESOURCE = EntityData.from_dict({
    "type": "Jans::Issue",
    "id": "random_id",
    "org_id": "some_long_id",
    "country": "US"
})


def test_authorize_unsigned():
    '''
    Test create correct cedarling requst with unsigned request for different principals.
    In this test we use in json rule cedar type name
    '''

    def config_cb(config: dict):
        '''
        Calback function to configure the bootstrap configuration
        '''
        json_rule = json.dumps({
            "and": [
                {"===": [{"var": "Jans::TestPrincipal1"}, "ALLOW"]},
                {"===": [{"var": "Jans::TestPrincipal2"}, "ALLOW"]},
                {"===": [{"var": "Jans::TestPrincipal3"}, "DENY"]}
            ]
        })

        config["CEDARLING_PRINCIPAL_BOOLEAN_OPERATION"] = json_rule

    instance = Cedarling(load_bootstrap_config(
        POLICY_STORE_LOCATION, config_cb=config_cb))

    request = RequestUnsigned(
        principals=[EntityData.from_dict({
            "id": "1",
            "type": "Jans::TestPrincipal1",
            "is_ok": True
        }),
            EntityData.from_dict({
                "id": "2",
                "type": "Jans::TestPrincipal2",
                "is_ok": True
            }),
            EntityData.from_dict({
                "id": "3",
                "type": "Jans::TestPrincipal3",
                "is_ok": False
            })],
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
        resource=RESOURCE,
    )

    authorize_result = instance.authorize_unsigned(request)
    assert authorize_result.is_allowed(), "request should be allowed"

    assert authorize_result.workload() is None, "workload should be None"
    assert authorize_result.person() is None, "person should be None"

    # check by principal type
    assert authorize_result.principal(
        "Jans::TestPrincipal1").decision.value == "ALLOW"
    assert authorize_result.principal(
        "Jans::TestPrincipal2").decision.value == "ALLOW"
    assert authorize_result.principal(
        "Jans::TestPrincipal3").decision.value == "DENY"

    # check by principal uid (type + id)
    assert authorize_result.principal(
        'Jans::TestPrincipal1::"1"').decision.value == "ALLOW"
    assert authorize_result.principal(
        'Jans::TestPrincipal2::"2"').decision.value == "ALLOW"
    assert authorize_result.principal(
        'Jans::TestPrincipal3::"3"').decision.value == "DENY"


def test_authorize_unsigned_json_rule_by_uid():
    '''
    Test create correct cedarling requst with unsigned request for different principals.
    In this test we use in json rule cedar type and id
    '''

    def config_cb(config: dict):
        '''
        Calback function to configure the bootstrap configuration
        '''
        json_rule = json.dumps({
            "and": [
                {"===": [{"var": 'Jans::TestPrincipal1::"1"'}, "ALLOW"]},
                {"===": [{"var": 'Jans::TestPrincipal2::"2"'}, "ALLOW"]},
                {"===": [{"var": 'Jans::TestPrincipal3::"3"'}, "DENY"]}
            ]
        })

        config["CEDARLING_PRINCIPAL_BOOLEAN_OPERATION"] = json_rule

    instance = Cedarling(load_bootstrap_config(
        POLICY_STORE_LOCATION, config_cb=config_cb))

    request = RequestUnsigned(
        principals=[EntityData.from_dict({
            "id": "1",
            "type": "Jans::TestPrincipal1",
            "is_ok": True
        }),
            EntityData.from_dict({
                "id": "2",
                "type": "Jans::TestPrincipal2",
                "is_ok": True
            }),
            EntityData.from_dict({
                "id": "3",
                "type": "Jans::TestPrincipal3",
                "is_ok": False
            })],
        action='Jans::Action::"UpdateForTestPrincipals"',
        context={},
        resource=RESOURCE,
    )

    authorize_result = instance.authorize_unsigned(request)
    assert authorize_result.is_allowed(), "request should be allowed"

    assert authorize_result.workload() is None, "workload should be None"
    assert authorize_result.person() is None, "person should be None"

    # check by principal type
    assert authorize_result.principal(
        "Jans::TestPrincipal1").decision.value == "ALLOW"
    assert authorize_result.principal(
        "Jans::TestPrincipal2").decision.value == "ALLOW"
    assert authorize_result.principal(
        "Jans::TestPrincipal3").decision.value == "DENY"

    # check by principal uid (type + id)
    assert authorize_result.principal(
        'Jans::TestPrincipal1::"1"').decision.value == "ALLOW"
    assert authorize_result.principal(
        'Jans::TestPrincipal2::"2"').decision.value == "ALLOW"
    assert authorize_result.principal(
        'Jans::TestPrincipal3::"3"').decision.value == "DENY"
