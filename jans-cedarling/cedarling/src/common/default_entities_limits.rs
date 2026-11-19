// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use std::num::NonZeroUsize;

/// Error type for default entities limits validation
#[derive(Debug, thiserror::Error)]
pub enum DefaultEntitiesLimitsError {
    #[error("Maximum number of default entities ({max_entities}) exceeded, found {found}")]
    CountExceeded { max_entities: usize, found: usize },
}

/// Upper bound on the number of default entities.
///
/// `AtMost` holds a [`NonZeroUsize`], so a cap that rejects every store cannot
/// be built: the `0` that the bootstrap property accepts means [`Limit::Unlimited`].
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Limit {
    Unlimited,
    AtMost(NonZeroUsize),
}

impl Limit {
    /// `None` keeps `default`, `Some(0)` turns the check off.
    fn from_config(value: Option<usize>, default: NonZeroUsize) -> Self {
        match value {
            None => Self::AtMost(default),
            Some(value) => NonZeroUsize::new(value).map_or(Self::Unlimited, Self::AtMost),
        }
    }

    /// Returns the cap when `value` is above it.
    fn exceeded_by(self, value: usize) -> Option<usize> {
        match self {
            Self::AtMost(max) if value > max.get() => Some(max.get()),
            Self::AtMost(_) | Self::Unlimited => None,
        }
    }
}

/// Configuration for limiting default entities to prevent `DoS` and memory exhaustion attacks
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) struct DefaultEntitiesLimits {
    max_entities: Limit,
}

impl Default for DefaultEntitiesLimits {
    fn default() -> Self {
        Self::from_config(None)
    }
}

impl DefaultEntitiesLimits {
    const DEFAULT_MAX_ENTITIES: NonZeroUsize = NonZeroUsize::new(1000).unwrap();

    /// Builds limits from the `CEDARLING_MAX_DEFAULT_ENTITIES` value: `None`
    /// keeps the built-in default, `Some(0)` turns the check off.
    pub(crate) fn from_config(max_entities: Option<usize>) -> Self {
        Self {
            max_entities: Limit::from_config(max_entities, Self::DEFAULT_MAX_ENTITIES),
        }
    }

    /// Whether the entity count check is turned off, so startup can warn that
    /// the store is unbounded.
    pub(crate) fn is_count_unlimited(self) -> bool {
        self.max_entities == Limit::Unlimited
    }

    pub(super) fn validate_entities_count(
        self,
        entity_count: usize,
    ) -> Result<(), DefaultEntitiesLimitsError> {
        match self.max_entities.exceeded_by(entity_count) {
            Some(max_entities) => Err(DefaultEntitiesLimitsError::CountExceeded {
                max_entities,
                found: entity_count,
            }),
            None => Ok(()),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn entity_count_limit_is_inclusive() {
        let limits = DefaultEntitiesLimits::from_config(Some(2));

        limits
            .validate_entities_count(2)
            .expect("a count equal to the limit must pass");
        let err = limits
            .validate_entities_count(3)
            .expect_err("a count above the limit must be rejected");
        assert!(
            matches!(
                err,
                DefaultEntitiesLimitsError::CountExceeded {
                    max_entities: 2,
                    found: 3,
                }
            ),
            "the error must report the limit and the actual count, got: {err:?}"
        );
    }

    #[test]
    fn from_config_applies_builtin_default_for_none() {
        let limits = DefaultEntitiesLimits::from_config(None);

        assert_eq!(
            limits.max_entities,
            Limit::AtMost(DefaultEntitiesLimits::DEFAULT_MAX_ENTITIES),
            "None must resolve to the documented built-in default"
        );
        assert!(
            !limits.is_count_unlimited(),
            "the built-in default must keep the count check on"
        );
    }

    #[test]
    fn from_config_disables_limit_for_zero() {
        let limits = DefaultEntitiesLimits::from_config(Some(0));

        limits
            .validate_entities_count(usize::MAX)
            .expect("Some(0) must disable the entity count limit");
        assert!(
            limits.is_count_unlimited(),
            "a turned-off count check must be reported"
        );
    }

    #[test]
    fn from_config_uses_configured_value() {
        assert_eq!(
            DefaultEntitiesLimits::from_config(Some(1500)).max_entities,
            Limit::AtMost(NonZeroUsize::new(1500).expect("non-zero")),
            "a configured value must replace the built-in default"
        );
    }
}
