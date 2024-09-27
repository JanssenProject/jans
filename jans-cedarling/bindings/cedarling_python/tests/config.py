from os.path import join
from cedarling_python import AuthzConfig, MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig
import pytest

ROOT_FOLDER_PATH = "../../"
TEST_FILES_PATH = join(ROOT_FOLDER_PATH, "cedarling/src/init/test_files")


# we use fixture with correct data
# and can change some part in the specific test case
@pytest.fixture
def sample_bootstrap_config():
    authz_config = AuthzConfig(application_name="example_app_name")

    # log_config = MemoryLogConfig(log_ttl=100)
    # log_config = StdOutLogConfig()
    log_config = DisabledLoggingConfig()

    # Create policy source configuration
    with open(join(TEST_FILES_PATH, "policy-store_ok.json"),
              mode="r", encoding="utf8") as f:
        policy_raw_json = f.read()
    # for now we support only json source
    policy_source = PolicyStoreSource(json=policy_raw_json)

    policy_store_config = PolicyStoreConfig(
        source=policy_source, store_id=None)

    # collect all in the BootstrapConfig
    bootstrap_config = BootstrapConfig(
        authz_config=authz_config,
        log_config=log_config,
        policy_store_config=policy_store_config)
    return bootstrap_config
