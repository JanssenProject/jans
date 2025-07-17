# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from cedarling_python import BootstrapConfig
from cedarling_python import Cedarling
from config import TEST_FILES_PATH, load_bootstrap_config
from os.path import join
import pytest

# In python unit tests we not cover all possible scenarios, but most common.
# also we try duplicate rust test cases

# in fixture `load_bootstrap_config` we use policy store `policy-store_ok.json`
# The human-readable policy and schema file is located in next folder:
# `test_files\policy-store_ok`

# test cases for load policy store with error
test_cases_err = [
    # cases with policy errors
    ("policy-store_policy_err_base64.json",
     "missing required field 'name' in policy store entry"),
    ("policy-store_policy_err_broken_utf8.json",
     "missing required field 'name' in policy store entry"),
    ("policy-store_policy_err_broken_policy.yaml",
     "unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode policy_content from human readable format: unexpected token `)`"),
    # cases with schema errors
    ("policy-store_schema_err_base64.json",
     "missing required field 'name' in policy store entry"),
    ("policy-store_schema_err.yaml",
     "missing required field 'name' in policy store entry"),
    ("policy-store_schema_err_cedar_mistake.yaml",
     "missing required field 'name' in policy store entry"),
]


@ pytest.mark.parametrize("policy_file_name,expected_error", test_cases_err)
def test_load_policy_store(policy_file_name, expected_error):
    # map fixture to variable with shorter name for readability

    config = load_bootstrap_config(policy_store_location=join(TEST_FILES_PATH, policy_file_name))

    try:
        # initialize cedarling
        Cedarling(config)
    except ValueError as e:
        # check that error message is present
        # just to be sure that we are testing the right thing
        if expected_error == "":
            raise e

        error_message = str(e)

        assert error_message != "", "error message should not be empty"

        assert expected_error in error_message, "expected error message not found, but: {}".format(
            error_message)
    else:
        raise Exception("expected error not found")


def test_load_policy_store_ok():
    # map fixture to variable with shorter name for readability
    policy_store_location = join(TEST_FILES_PATH, "policy-store_ok.yaml")

    config = load_bootstrap_config(policy_store_location)

    # initialize cedarling
    Cedarling(config)


def test_policy_store_source_wrong_type():
    # map fixture to variable with shorter name for readability

    try:
        load_bootstrap_config("invalid_config.abc")
    except ValueError:
        pass
    else:
        assert False, "ValueError was not raised when a policy store has an unsupported file type"
