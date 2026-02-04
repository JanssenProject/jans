"""
Tests for the Data API functionality in Cedarling Python bindings.
"""
import pytest
from cedarling_python import BootstrapConfig, Cedarling
from config import load_bootstrap_config


def test_push_and_get_data():
    """Test basic push and get operations."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    # Push data without TTL
    instance.push_data("key1", "value1")
    result = instance.get_data("key1")
    assert result == "value1"

    # Push data with TTL
    instance.push_data("key2", {"nested": "data"}, ttl_secs=60)
    result = instance.get_data("key2")
    assert result == {"nested": "data"}

    # Push array
    instance.push_data("key3", [1, 2, 3])
    result = instance.get_data("key3")
    assert result == [1, 2, 3]

    instance.shut_down()


def test_get_data_entry():
    """Test getting data entry with metadata."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data("test_key", {"foo": "bar"})
    entry = instance.get_data_entry("test_key")

    assert entry is not None
    assert entry.key == "test_key"
    assert entry.value() == {"foo": "bar"}
    assert entry.data_type is not None
    assert entry.access_count >= 0
    assert entry.created_at is not None

    instance.shut_down()


def test_remove_data():
    """Test removing data."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data("to_remove", "data")
    assert instance.get_data("to_remove") == "data"

    removed = instance.remove_data("to_remove")
    assert removed is True

    assert instance.get_data("to_remove") is None

    # Try removing non-existent key
    removed = instance.remove_data("non_existent")
    assert removed is False

    instance.shut_down()


def test_clear_data():
    """Test clearing all data."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data("key1", "value1")
    instance.push_data("key2", "value2")
    instance.push_data("key3", "value3")

    assert instance.get_data("key1") == "value1"
    assert instance.get_data("key2") == "value2"
    assert instance.get_data("key3") == "value3"

    instance.clear_data()

    assert instance.get_data("key1") is None
    assert instance.get_data("key2") is None
    assert instance.get_data("key3") is None

    instance.shut_down()


def test_list_data():
    """Test listing all data entries."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data("key1", "value1")
    instance.push_data("key2", {"nested": "data"})
    instance.push_data("key3", [1, 2, 3])

    entries = instance.list_data()
    assert len(entries) == 3

    keys = {entry.key for entry in entries}
    assert keys == {"key1", "key2", "key3"}

    instance.shut_down()


def test_get_stats():
    """Test getting data store statistics."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    stats = instance.get_stats()
    assert stats.entry_count == 0

    instance.push_data("key1", "value1")
    instance.push_data("key2", "value2")

    stats = instance.get_stats()
    assert stats.entry_count == 2
    assert stats.metrics_enabled is not None
    assert stats.total_size_bytes >= 0

    instance.shut_down()


def test_data_error_invalid_key():
    """Test that empty key raises DataError."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    with pytest.raises(Exception):  # Should be DataError
        instance.push_data("", "value")

    instance.shut_down()

