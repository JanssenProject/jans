"""
Tests for the Data API functionality in Cedarling Python bindings.
"""
import pytest
from cedarling_python import Cedarling
from cedarling_python import data_errors_ctx
from config import load_bootstrap_config


def test_push_and_get_data_ctx():
    """Test basic push and get operations."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    # Push data without TTL
    instance.push_data_ctx("key1", "value1")
    result = instance.get_data_ctx("key1")
    assert result == "value1"

    # Push data with TTL
    instance.push_data_ctx("key2", {"nested": "data"}, ttl_secs=60)
    result = instance.get_data_ctx("key2")
    assert result == {"nested": "data"}

    # Push array
    instance.push_data_ctx("key3", [1, 2, 3])
    result = instance.get_data_ctx("key3")
    assert result == [1, 2, 3]

    instance.shut_down()


def test_get_data_entry_ctx():
    """Test getting data entry with metadata."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data_ctx("test_key", {"foo": "bar"})
    entry = instance.get_data_entry_ctx("test_key")

    assert entry is not None
    assert entry.key == "test_key"
    assert entry.value == {"foo": "bar"}
    from cedarling_python import CedarType
    assert isinstance(entry.data_type, CedarType)
    assert entry.data_type == CedarType.Record
    assert entry.access_count == 1
    from datetime import datetime
    # Verify created_at is a valid ISO 8601 timestamp
    # Python 3.10's fromisoformat() only supports up to microseconds (6 digits),
    # so truncate nanoseconds to microseconds if present
    timestamp = entry.created_at.replace('Z', '+00:00')
    # Truncate fractional seconds to 6 digits (microseconds) for Python 3.10 compatibility
    if '.' in timestamp:
        dot_idx = timestamp.index('.')
        # Find timezone offset start (+ or -) after the dot
        plus_idx = timestamp.find('+', dot_idx)
        minus_idx = timestamp.find('-', dot_idx)
        # Get the first timezone marker found (or -1 if neither found)
        tz_idx = min(
            plus_idx if plus_idx >= 0 else len(timestamp),
            minus_idx if minus_idx >= 0 else len(timestamp)
        )
        if tz_idx < len(timestamp):
            # Has timezone, truncate fractional part to 6 digits
            fractional = timestamp[dot_idx + 1:tz_idx]
            truncated_fractional = (fractional[:6] + '000000')[:6]  # Pad or truncate to 6
            timestamp = timestamp[:dot_idx + 1] + truncated_fractional + timestamp[tz_idx:]
        else:
            # No timezone, just truncate fractional part
            fractional = timestamp[dot_idx + 1:]
            truncated_fractional = (fractional[:6] + '000000')[:6]  # Pad or truncate to 6
            timestamp = timestamp[:dot_idx + 1] + truncated_fractional
    datetime.fromisoformat(timestamp)

    instance.shut_down()


def test_remove_data_ctx():
    """Test removing data."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data_ctx("to_remove", "data")
    assert instance.get_data_ctx("to_remove") == "data"

    removed = instance.remove_data_ctx("to_remove")
    assert removed is True

    assert instance.get_data_ctx("to_remove") is None

    # Try removing non-existent key
    removed = instance.remove_data_ctx("non_existent")
    assert removed is False

    instance.shut_down()


def test_clear_data_ctx():
    """Test clearing all data."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data_ctx("key1", "value1")
    instance.push_data_ctx("key2", "value2")
    instance.push_data_ctx("key3", "value3")

    assert instance.get_data_ctx("key1") == "value1"
    assert instance.get_data_ctx("key2") == "value2"
    assert instance.get_data_ctx("key3") == "value3"

    instance.clear_data_ctx()

    assert instance.get_data_ctx("key1") is None
    assert instance.get_data_ctx("key2") is None
    assert instance.get_data_ctx("key3") is None

    instance.shut_down()


def test_list_data_ctx():
    """Test listing all data entries."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    instance.push_data_ctx("key1", "value1")
    instance.push_data_ctx("key2", {"nested": "data"})
    instance.push_data_ctx("key3", [1, 2, 3])

    entries = instance.list_data_ctx()
    assert len(entries) == 3

    keys = {entry.key for entry in entries}
    assert keys == {"key1", "key2", "key3"}

    instance.shut_down()


def test_get_stats_ctx():
    """Test getting data store statistics."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    stats = instance.get_stats_ctx()
    assert stats.entry_count == 0

    instance.push_data_ctx("key1", "value1")
    instance.push_data_ctx("key2", "value2")

    stats = instance.get_stats_ctx()
    assert stats.entry_count == 2
    assert stats.metrics_enabled is True
    assert stats.total_size_bytes > 0

    instance.shut_down()


def test_data_error_invalid_key():
    """Test that empty key raises InvalidKey."""
    config = load_bootstrap_config()
    instance = Cedarling(config)

    with pytest.raises(data_errors_ctx.InvalidKey):
        instance.push_data_ctx("", "value")

    instance.shut_down()

