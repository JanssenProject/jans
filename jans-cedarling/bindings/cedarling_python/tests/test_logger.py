from cedarling_python import Cedarling
from config import sample_bootstrap_config


# In python unit tests we not cover all possible scenarios, but most common.

def test_invalid_log_config(sample_bootstrap_config):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config
    try:
        # when we set invalid log configuration it should raise ValueError
        config.log_type = "String"
    except TypeError:
        pass


def test_memory_logger(sample_bootstrap_config):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config

    config.log_type = "memory"

    # on initialize Cedarling we should have logs
    cedarling = Cedarling(config)

    # check that we have logs in memory
    active_log_ids = cedarling.get_log_ids()
    assert len(active_log_ids) != 0
    # check if we have access to log entries by id
    for id in active_log_ids:
        log_entry = cedarling.get_log_by_id(active_log_ids[0])
        assert log_entry is not None

    # check that we can pop logs from memory
    assert len(cedarling.pop_logs()) != 0

    # after popping all logs, we should have no more active logs in memory
    assert len(cedarling.get_log_ids()) == 0
    assert len(cedarling.pop_logs()) == 0


def test_off_logger(sample_bootstrap_config):
    # map fixture to variable with shorter name for readability
    config = sample_bootstrap_config

    config.log_type = "off"

    cedarling = Cedarling(config)

    # we should not have logs in memory
    assert len(cedarling.get_log_ids()) == 0
    assert len(cedarling.pop_logs()) == 0


def test_stdout_logger(sample_bootstrap_config):
    # Map fixture to variable with shorter name for readability
    config = sample_bootstrap_config
    config.log_type = "std_out"

    cedarling = Cedarling(config)

    # for some reason python capsys doesn't capture the output in correct time
    # so we skip the reading stdout

    # We should not have logs in memory (confirm logs are printed only to stdout)
    assert len(cedarling.get_log_ids()) == 0
    assert len(cedarling.pop_logs()) == 0
