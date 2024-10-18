from os.path import join
from cedarling_python import MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig, JwtConfig
import pytest

ROOT_FOLDER_PATH = "../../"
TEST_FILES_PATH = join(ROOT_FOLDER_PATH, "cedarling/src/init/test_files")

# in `sample_bootstrap_config` we use policy store `policy-store_ok.json`
# The human-readable policy and schema file is located in next folder:
# `../..cedarling/src/init/test_files/policy-store_ok`


@pytest.fixture
def sample_bootstrap_config():
    '''
    Fixture with correct data of BootstrapConfig.
    You can change some part in the specific test case.
    '''
    # log_config = MemoryLogConfig(log_ttl=100)
    log_config = StdOutLogConfig()
    # log_config = DisabledLoggingConfig()

    # Create policy source configuration
    with open(join(TEST_FILES_PATH, "policy-store_ok.json"),
              mode="r", encoding="utf8") as f:
        policy_raw_json = f.read()
    # for now we support only json source
    policy_source = PolicyStoreSource(json=policy_raw_json)

    policy_store_config = PolicyStoreConfig(
        source=policy_source, store_id=None)

    jwt_config = JwtConfig(enabled=False)

    # collect all in the BootstrapConfig
    bootstrap_config = BootstrapConfig(
        application_name="example_app_name",
        log_config=log_config,
        policy_store_config=policy_store_config,
        jwt_config=jwt_config
    )
    return bootstrap_config
