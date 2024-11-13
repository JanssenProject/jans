from cedarling_python import MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig
from cedarling_python import Cedarling
from config import TEST_FILES_PATH, sample_bootstrap_config
from os.path import join
import pytest

# In python unit tests we not cover all possible scenarios, but most common.
# also we try duplicate rust test cases

# in fixture `sample_bootstrap_config` we use policy store `policy-store_ok.json`
# The human-readable policy and schema file is located in next folder:
# `test_files\policy-store_ok`

# test cases for load policy store with error
test_cases_err = [
    # cases with policy errors
    ("policy-store_policy_err_base64.json",
     "Failed to decode Base64 encoded string"),
    ("policy-store_policy_err_broken_utf8.json",
     "invalid utf-8 sequence"),
    ("policy-store_policy_err_broken_policy.yaml",
     "Failed to load policy store from YAML: unexpected token `)`"),
    # cases with schema errors
    ("policy-store_schema_err_base64.json",
     "Failed to decode Base64 encoded string"),
    ("policy-store_schema_err.yaml",
     "Failed to load policy store from YAML: unexpected end of input"),
    ("policy-store_schema_err_cedar_mistake.yaml",
     "Failed to load policy store from YAML: Failed to load Schema from JSON: Schema(TypeNotDefined(TypeNotDefinedError"
    ),
]


@ pytest.mark.parametrize("policy_file_name,expected_error", test_cases_err)
def test_load_policy_store(sample_bootstrap_config, policy_file_name, expected_error):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config

    with open(join(TEST_FILES_PATH, policy_file_name),
              mode="r", encoding="utf8") as f:
        policy_raw = f.read()
    if policy_file_name.endswith("json"):
        policy_source = PolicyStoreSource(json=policy_raw)
    elif policy_file_name.endswith("yaml"):
        policy_source = PolicyStoreSource(yaml=policy_raw)
    else:
        raise f"unknown file extension {policy_file_name}"

    config.policy_store_config = PolicyStoreConfig(
        source=policy_source)
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


def test_load_policy_store_ok(sample_bootstrap_config):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config

    policy_raw = open(join(TEST_FILES_PATH, "policy-store_ok.yaml"),
                           mode="r", encoding="utf8").read()
    policy_source = PolicyStoreSource(yaml=policy_raw)
    config.policy_store_config = PolicyStoreConfig(source=policy_source)

    # initialize cedarling
    Cedarling(config)


def test_policy_store_source_wrong_type(sample_bootstrap_config):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config

    try:
        config.policy_store_config = PolicyStoreConfig(source="wrong type")
    except TypeError:
        pass
