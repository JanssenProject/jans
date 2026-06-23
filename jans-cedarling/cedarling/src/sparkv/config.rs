/*
 * This software is available under the MIT License
 * See https://github.com/uzyn/sparkv/blob/main/LICENSE for full text.
 *
 * Copyright (c) 2024 U-Zyn Chua
 */

use chrono::Duration;

#[derive(Debug, PartialEq, Clone, Copy)]
pub struct Config {
    pub max_items: usize,
    pub max_item_size: usize,
    pub max_ttl: Duration,
    pub default_ttl: Duration,
    pub auto_clear_expired: bool,
    pub earliest_expiration_eviction: bool,
}

impl Config {
    #[must_use]
    pub fn new() -> Self {
        Config {
            max_items: 10_000,
            max_item_size: 500_000,
            max_ttl: Duration::seconds(60 * 60),
            default_ttl: Duration::seconds(5 * 60), // 5 minutes
            auto_clear_expired: true,
            earliest_expiration_eviction: false,
        }
    }
}

impl Default for Config {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_new() {
        let config: Config = Config::new();
        assert_eq!(
            config.max_items, 10_000,
            "default max_items should be 10_000"
        );
        assert_eq!(
            config.max_item_size, 500_000,
            "default max_item_size should be 500_000"
        );
        assert_eq!(
            config.max_ttl,
            Duration::seconds(60 * 60),
            "default max_ttl should be 1 hour"
        );
        assert_eq!(
            config.default_ttl,
            Duration::seconds(5 * 60),
            "default default_ttl should be 5 minutes"
        );
        assert!(
            config.auto_clear_expired,
            "auto_clear_expired should default to true"
        );
        assert!(
            !config.earliest_expiration_eviction,
            "earliest_expiration_eviction should default to false"
        );
    }
}
