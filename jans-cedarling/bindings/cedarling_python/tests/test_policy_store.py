from cedarling_python import MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig
from cedarling_python import Cedarling
from config import TEST_FILES_PATH, sample_bootstrap_config
from os.path import join
import pytest

# In python unit tests we not cover all possible scenarios, but most common.
# also we try duplicate rust test cases


# test cases for load policy store with error
test_cases_err = [
    # cases with policy errors
    ("policy-store_policy_err_base64.json",
     "unable to decode policy_content as base64"),
    ("policy-store_policy_err_broken_utf8.json",
     "unable to decode policy_content to utf8 string"),
    ("policy-store_policy_err_broken_policy.json",
     'unable to decode policy with id: 840da5d85403f35ea76519ed1a18a33989f855bf1cf8, error: unable to decode policy_content from human redable format: unexpected token `)` at line 15 column 1'),
    # cases with schema errors
    ("policy-store_schema_err_base64.json",
     "unable to decode cedar policy schema base64"),
    ("policy-store_schema_err_json.json",
     "unable to unmarshal cedar policy schema json to the structure"),
    ("policy-store_schema_err_cedar_mistake.json",
     "Could not load policy: unable to parse cedar policy schema json: failed to resolve type: User_TypeNotExist at line 35 column 1"),
]


@ pytest.mark.parametrize("policy_file_name,expected_error", test_cases_err)
def test_load_policy_store(sample_bootstrap_config, policy_file_name, expected_error):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config

    with open(join(TEST_FILES_PATH, policy_file_name),
              mode="r", encoding="utf8") as f:
        policy_raw_json = f.read()
    # for now we support only json source
    policy_source = PolicyStoreSource(
        json=policy_raw_json)
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

        assert expected_error in error_message, "expected error message not found"
    else:
        raise Exception("expected error not found")


def test_load_policy_store_ok(sample_bootstrap_config):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config

    policy_raw_json = open(join(TEST_FILES_PATH, "policy-store_ok.json"),
                           mode="r", encoding="utf8").read()
    # for now we support only json source
    policy_source = PolicyStoreSource(json=policy_raw_json)
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
