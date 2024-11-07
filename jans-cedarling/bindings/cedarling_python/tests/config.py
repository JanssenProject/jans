from os.path import join
from cedarling_python import MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig, JwtConfig
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
    # log_config = MemoryLogConfig(log_ttl=100)
    log_config = StdOutLogConfig()
    # log_config = DisabledLoggingConfig()

    # Create policy source configuration
    # NOTE yaml is only used for test fixtures. Real imports use json.
    with open(join(TEST_FILES_PATH, "policy-store_ok.yaml"),
              mode="r", encoding="utf8") as f:
        policy_raw = f.read()
    policy_source = PolicyStoreSource(yaml=policy_raw)

    policy_store_config = PolicyStoreConfig(
        source=policy_source)

    jwt_config = JwtConfig(enabled=False)

    # collect all in the BootstrapConfig
    bootstrap_config = BootstrapConfig(
        application_name="example_app_name",
        log_config=log_config,
        policy_store_config=policy_store_config,
        jwt_config=jwt_config
    )
    return bootstrap_config
