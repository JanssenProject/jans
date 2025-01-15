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
}

impl Config {
    pub fn new() -> Self {
        Config {
            max_items: 10_000,
            max_item_size: 500_000,
            max_ttl: Duration::seconds(60 * 60),
            default_ttl: Duration::seconds(5 * 60), // 5 minutes
            auto_clear_expired: true,
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
        assert_eq!(config.max_items, 10_000);
        assert_eq!(config.max_item_size, 500_000);
        assert_eq!(config.max_ttl, Duration::seconds(60 * 60));
        assert_eq!(config.default_ttl, Duration::seconds(5 * 60));
        assert!(config.auto_clear_expired);
    }
}
