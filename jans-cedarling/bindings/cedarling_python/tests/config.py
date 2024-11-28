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
        "application_name":"TestApp",
        "policy_store_id":"asdasd123123",
        "policy_store_local_fn":policy_store_location,
        "jwt_sig_validation":"disabled",
        "jwt_status_validation":"disabled",
        "at_iss_validation":"disabled",
        "at_jti_validation":"disabled",
        "at_nbf_validation":"disabled",
        "idt_iss_validation":"disabled",
        "idt_sub_validation":"disabled",
        "idt_exp_validation":"disabled",
        "idt_iat_validation":"disabled",
        "idt_aud_validation":"disabled",
        "id_token_trust_mode":"none",
        "userinfo_iss_validation":"disabled",
        "userinfo_aud_validation":"disabled",
        "userinfo_sub_validation":"disabled",
        "userinfo_exp_validation":"disabled",
        "log_type":"std_out"
    })

    return bootstrap_config
