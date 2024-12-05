from os.path import join
from cedarling_python import BootstrapConfig
import pytest

ROOT_FOLDER_PATH = "../../"
TEST_FILES_PATH = join(ROOT_FOLDER_PATH, "test_files")

# in fixture `sample_bootstrap_config` we use policy store `policy-store_ok.json`
# The human-readable policy and schema file is located in next folder:
# `test_files\policy-store_ok`


@pytest.fixture
def sample_bootstrap_config():
    '''
    Fixture with correct data of BootstrapConfig.
    You can change some part in the specific test case.
    '''

    # Create policy source configuration
    # NOTE yaml is only used for test fixtures. Real imports use json.
    policy_store_location = join(TEST_FILES_PATH, "policy-store_ok.yaml")

    bootstrap_config = BootstrapConfig({
        "CEDARLING_APPLICATION_NAME": "TestApp",
        "CEDARLING_POLICY_STORE_ID": "a1bf93115de86de760ee0bea1d529b521489e5a11747",
        "CEDARLING_POLICY_STORE_LOCAL_FN": policy_store_location,
        "CEDARLING_USER_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_AUTHZ": "enabled",
        "CEDARLING_WORKLOAD_BOOLEAN_OPERATION": "AND",
        "CEDARLING_JWT_SIG_VALIDATION": "disabled",
        "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
        "CEDARLING_AT_ISS_VALIDATION": "disabled",
        "CEDARLING_AT_JTI_VALIDATION": "disabled",
        "CEDARLING_AT_NBF_VALIDATION": "disabled",
        "CEDARLING_AT_EXP_VALIDATION": "disabled",
        "CEDARLING_IDT_ISS_VALIDATION": "disabled",
        "CEDARLING_IDT_SUB_VALIDATION": "disabled",
        "CEDARLING_IDT_EXP_VALIDATION": "disabled",
        "CEDARLING_IDT_IAT_VALIDATION": "disabled",
        "CEDARLING_IDT_AUD_VALIDATION": "disabled",
        "CEDARLING_USERINFO_ISS_VALIDATION": "disabled",
        "CEDARLING_USERINFO_AUD_VALIDATION": "disabled",
        "CEDARLING_USERINFO_SUB_VALIDATION": "disabled",
        "CEDARLING_USERINFO_EXP_VALIDATION": "disabled",
        "CEDARLING_ID_TOKEN_TRUST_MODE": "none",
        "CEDARLING_LOG_TYPE": "std_out"
    })

    return bootstrap_config
