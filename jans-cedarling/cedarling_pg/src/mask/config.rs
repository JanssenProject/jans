// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! Default mask registry (M4) and catalog lookup helpers.

use crate::mask::types::MaskType;

/// One explicit masking rule row from `cedarling.mask_rules`.
#[derive(Debug, Clone)]
pub(crate) struct MaskRule {
    pub(crate) column_name: String,
    pub(crate) mask_type: MaskType,
    pub(crate) condition_sql: Option<String>,
}

/// Return the built-in default mask for `col_name` when no explicit catalog rule exists.
///
/// Matching is a case-insensitive substring check against well-known sensitive field names.
pub(crate) fn default_mask_for_column(col_name: &str) -> Option<MaskType> {
    let lower = col_name.to_ascii_lowercase();
    if lower.contains("email") {
        return Some(MaskType::Partial { pattern: "***@***.com".to_string() });
    }
    if lower.contains("phone") {
        return Some(MaskType::Partial { pattern: "XXX-XXX-####".to_string() });
    }
    if lower.contains("ssn") {
        return Some(MaskType::Partial { pattern: "XXX-XX-####".to_string() });
    }
    if lower.contains("credit_card") || lower.contains("cc_number") {
        return Some(MaskType::Partial { pattern: "****-****-****-####".to_string() });
    }
    if lower.contains("password")
        || lower.contains("passwd")
        || lower.contains("secret")
        || lower.contains("api_key")
    {
        return Some(MaskType::Fixed { value: "[PROTECTED]".to_string() });
    }
    if lower.contains("salary") || lower.contains("income") {
        return Some(MaskType::Range { min: 50_000, max: 150_000 });
    }
    None
}

/// reject them when applying rules.
#[cfg(test)]
#[allow(clippy::unnecessary_wraps)]
pub(crate) fn lookup_explicit_rules(_table_name: &str) -> Result<Vec<MaskRule>, String> {
    // Unit tests run outside a Postgres backend; return no catalog rules so tests
    // validate pure masking behavior (default registry + type preservation) only.
    Ok(Vec::new())
}

/// Fetch explicit mask rules for `table_name` from `cedarling.mask_rules`.
///
/// Conditional rules (`condition_sql` non-empty) are returned in the result set; callers must
/// reject them when applying rules.
#[cfg(not(test))]
pub(crate) fn lookup_explicit_rules(table_name: &str) -> Result<Vec<MaskRule>, String> {
    let mut rules = Vec::new();
    pgrx::prelude::Spi::connect(|client| {
        let rows = client.select(
            "SELECT column_name, mask_type, mask_value, condition_sql
             FROM cedarling.mask_rules
             WHERE table_name = $1
             ORDER BY column_name",
            None,
            &[table_name.into()],
        )?;
        for row in rows {
            let Some(col) = row.get_by_name::<String, _>("column_name")? else {
                continue;
            };
            let mt = row
                .get_by_name::<String, _>("mask_type")?
                .unwrap_or_default();
            let mv = row.get_by_name::<String, _>("mask_value")?;
            let condition_sql = row.get_by_name::<String, _>("condition_sql")?;
            rules.push(MaskRule {
                column_name: col,
                mask_type: MaskType::from_parts(&mt, mv.as_deref()),
                condition_sql,
            });
        }
        Ok::<(), pgrx::spi::Error>(())
    })
    .map_err(|source| format!("mask rule catalog lookup failed for table '{table_name}': {source}"))?;
    Ok(rules)
}


#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn email_gets_partial_mask() {
        assert!(matches!(
            default_mask_for_column("user_email"),
            Some(MaskType::Partial { .. })
        ));
    }

    #[test]
    fn phone_gets_partial_mask() {
        assert!(matches!(
            default_mask_for_column("phone_number"),
            Some(MaskType::Partial { .. })
        ));
    }

    #[test]
    fn ssn_gets_partial_mask() {
        let m = default_mask_for_column("ssn");
        assert!(matches!(m, Some(MaskType::Partial { ref pattern }) if pattern.contains("XX")));
    }

    #[test]
    fn credit_card_gets_partial_mask() {
        assert!(matches!(
            default_mask_for_column("credit_card_number"),
            Some(MaskType::Partial { .. })
        ));
        assert!(matches!(
            default_mask_for_column("cc_number"),
            Some(MaskType::Partial { .. })
        ));
    }

    #[test]
    fn password_fields_get_fixed_mask() {
        for col in &["password", "user_passwd", "api_key", "client_secret"] {
            assert!(
                matches!(default_mask_for_column(col), Some(MaskType::Fixed { .. })),
                "column '{col}' should get Fixed mask"
            );
        }
    }

    #[test]
    fn salary_gets_range_mask() {
        assert!(matches!(
            default_mask_for_column("annual_salary"),
            Some(MaskType::Range { min: 50_000, max: 150_000 })
        ));
        assert!(matches!(
            default_mask_for_column("gross_income"),
            Some(MaskType::Range { .. })
        ));
    }

    #[test]
    fn unrecognized_column_gets_no_default() {
        assert!(default_mask_for_column("created_at").is_none());
        assert!(default_mask_for_column("description").is_none());
        assert!(default_mask_for_column("id").is_none());
    }
}
