/*
 * This software is available under the Apache-2.0 license.
 * See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
 *
 * Copyright (c) 2024, Gluu, Inc.
 */

use pyo3::prelude::*;

/// DropReason
/// ==========
///
/// Why a token was dropped from a multi-issuer authorization. Same shape as
/// the ``reason`` object in the core JSON.
///
/// Attributes
/// ----------
/// .. attribute:: kind
///     str: stable snake-case slug (e.g. ``"jwt_validation_failed"``).
/// .. attribute:: detail
///     str | None: stable detail slug for ``invalid_input``
///     (``"empty_mapping"`` / ``"empty_payload"``); ``None`` otherwise.
/// .. attribute:: message
///     str: claim-free human-readable message; non-empty for every reason.
#[derive(Debug, Clone, PartialEq, Eq)]
#[pyclass(eq, frozen, skip_from_py_object)]
pub struct DropReason {
    inner: cedarling::DropReason,
}

#[pymethods]
impl DropReason {
    #[getter]
    fn kind(&self) -> &'static str {
        self.inner.slug()
    }

    #[getter]
    fn detail(&self) -> Option<&'static str> {
        self.inner.detail()
    }

    #[getter]
    fn message(&self) -> String {
        self.inner.message()
    }

    fn __repr__(&self) -> String {
        format!(
            "DropReason(kind='{}', detail={:?}, message='{}')",
            self.inner.slug(),
            self.inner.detail(),
            self.inner.message()
        )
    }
}

/// DroppedToken
/// ============
///
/// A token the caller supplied that multi-issuer authorization did not use.
/// Identifies the entry by its input `mapping` and zero-based `index`, with a
/// claim-free `reason`.
///
/// Attributes
/// ----------
/// .. attribute:: mapping
///     str: the Cedar entity-type mapping of the dropped input.
/// .. attribute:: index
///     int: zero-based position in the request's ``tokens`` list.
/// .. attribute:: reason
///     DropReason: why the token was dropped.
#[pyclass(frozen)]
pub struct DroppedToken {
    inner: cedarling::DroppedToken,
}

#[pymethods]
impl DroppedToken {
    #[getter]
    fn mapping(&self) -> &str {
        &self.inner.mapping
    }

    #[getter]
    fn index(&self) -> usize {
        self.inner.index
    }

    #[getter]
    fn reason(&self) -> DropReason {
        DropReason {
            inner: self.inner.reason.clone(),
        }
    }

    fn __repr__(&self) -> String {
        format!(
            "DroppedToken(mapping='{}', index={}, reason='{}')",
            self.inner.mapping,
            self.inner.index,
            self.inner.reason.slug()
        )
    }
}

impl From<cedarling::DroppedToken> for DroppedToken {
    fn from(inner: cedarling::DroppedToken) -> Self {
        Self { inner }
    }
}
