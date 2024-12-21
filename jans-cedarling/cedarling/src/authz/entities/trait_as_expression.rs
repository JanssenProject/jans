// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

use cedar_policy::RestrictedExpression;

/// Trait to cast type to [`RestrictedExpression`]
pub(crate) trait AsExpression {
    fn to_expression(self) -> RestrictedExpression;
}

impl AsExpression for i64 {
    fn to_expression(self) -> RestrictedExpression {
        RestrictedExpression::new_long(self)
    }
}

impl AsExpression for String {
    fn to_expression(self) -> RestrictedExpression {
        RestrictedExpression::new_string(self)
    }
}

impl AsExpression for bool {
    fn to_expression(self) -> RestrictedExpression {
        RestrictedExpression::new_bool(self)
    }
}
