# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from cedarling_python import EntityData, RequestUnsigned
from cedarling_python import TokenInput, AuthorizeMultiIssuerRequest

# if you want to use errors
from cedarling_python import authorize_errors

import time
import yaml
import os


def load_yaml_to_env(yaml_path):
    with open(yaml_path, "r") as file:
        config = yaml.safe_load(file)

    for key, value in config.items():
        if value is not None:  # Skip null values
            os.environ[key] = str(value)


bootstrap_config = BootstrapConfig.load_from_file(
    "./example_files/sample_bootstrap_props.yaml"
)

# Create config from environment variables
# bootstrap_config = BootstrapConfig.from_env()

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


def authorize_without_token():
    # //// Execute authentication request without token ////

    # Create an entity data object using the EntityData class
    resource = EntityData.from_dict(
        {
            "cedar_entity_mapping": {
                "entity_type": "Jans::Application",
                "id": "some_id",
            },
            "app_id": "application_id",
            "name": "Some Application",
            "url": {
                "host": "jans.test",
                "path": "/protected-endpoint",
                "protocol": "http",
            },
        }
    )

    # Creating context for request
    context = {}

    # Creating cedarling request
    action = 'Jans::Action::"UpdateTestPrincipal"'

    request = RequestUnsigned(
        principals=[
            EntityData.from_dict(
                {
                    "cedar_entity_mapping": {
                        "entity_type": "Jans::TestPrincipal1",
                        "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0",
                    },
                    "is_ok": True,
                }
            ),
            EntityData.from_dict(
                {
                    "cedar_entity_mapping": {
                        "entity_type": "Jans::TestPrincipal2",
                        "id": "qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYkt1",
                    },
                    "is_ok": True,
                }
            ),
        ],
        action=action,
        resource=resource,
        context=context,
    )

    # Authorize call
    try:
        authorize_result = instance.authorize_unsigned(request)
    except authorize_errors.AuthorizeError as e:
        print(e)
        return
    # Print logs from MemoryLogConfig
    # print(*instance.pop_logs())

    # watch on the decision for TestPrincipal1
    principal1 = authorize_result.principal("Jans::TestPrincipal1")
    if principal1 is not None:
        print(f"Result of principal1 authorization: {principal1.decision}")

        # show diagnostic information
        workload_diagnostic = principal1.diagnostics
        print("Policy ID used:")
        for diagnostic in workload_diagnostic.reason:
            print(diagnostic)

        print(f"Errors during authorization: {len(workload_diagnostic.errors)}")
        for diagnostic in workload_diagnostic.errors:
            print(diagnostic)

        print()

    # watch on the decision for TestPrincipal2
    principal2 = authorize_result.principal("Jans::TestPrincipal2")
    if principal2 is not None:
        print(f"Result of principal2 authorization: {principal2.decision}")
        person_diagnostic = principal2.diagnostics
        print("Policy ID used:")
        for diagnostic in person_diagnostic.reason:
            print(diagnostic)

        print(f"Errors during authorization: {len(person_diagnostic.errors)}")
        for diagnostic in person_diagnostic.errors:
            print(diagnostic)

    """
    authorize_result.is_allowed() only returns true if 
    policies permit and json logic is satisfied.
    """
    assert authorize_result.is_allowed()

    print()


def authorize_multi_issuer():
    # //// Execute multi-issuer authorization request ////

    # Build resource entity
    resource = EntityData.from_dict(
        {
            "cedar_entity_mapping": {
                "entity_type": "Jans::Application",
                "id": "some_id",
            },
            "app_id": "application_id",
            "name": "Some Application",
            "url": {
                "host": "jans.test",
                "path": "/protected-endpoint",
                "protocol": "http",
            },
        }
    )

    # Build typed tokens coming potentially from different issuers
    # Use locally defined sample payloads so this example is self-contained
    token_a = "eyJhbGciOi...access_token_sample"
    token_b = "eyJhbGciOi...custom_token_sample"
    tokens = [
        TokenInput(mapping="Jans::Access_Token", payload=token_a),
        # Example of a second issuer/custom token type; replace payload accordingly
        TokenInput(mapping="Acme::DolphinToken", payload=token_b),
    ]

    action = 'Jans::Action::"Read"'
    context = {"location": "miami"}

    request = AuthorizeMultiIssuerRequest(
        tokens=tokens,
        action=action,
        resource=resource,
        context=context,
    )

    try:
        result = instance.authorize_multi_issuer(request)
    except authorize_errors.AuthorizeError as e:
        print("authorize_multi_issuer:", e)
        return

    # High-level allowed flag
    print(f"Multi-issuer allowed: {result.is_allowed()}")

    # Detailed cedar response
    resp = result.response()
    print(f"Decision: {resp.decision}")
    print("Policy IDs used:")
    for reason in resp.diagnostics.reason:
        print(reason)

    print(f"Errors during authorization: {len(resp.diagnostics.errors)}")
    for err in resp.diagnostics.errors:
        print(err)

    print(f"Request ID: {result.request_id()}")
    print()


if __name__ == "__main__":
    print("!!!\nRunning  authorize_without_token:")
    authorize_without_token()

    print("!!!\nRunning  authorize_multi_issuer:")
    authorize_multi_issuer()
