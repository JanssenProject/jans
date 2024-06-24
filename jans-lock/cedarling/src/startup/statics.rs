//! SAFETY: Webassembly is single-threaded, so this is safe. The statics are also scoped and the references are read only

use super::types;
use cedar_policy::*;

pub(crate) fn policies(swap: Option<PolicySet>) -> &'static PolicySet {
	static mut POLICIES: Option<PolicySet> = None;

	unsafe {
		swap.map(|s| POLICIES = Some(s));
		POLICIES.as_ref().unwrap()
	}
}

pub(crate) fn trusted_issuers(swap: Option<Vec<types::TrustedIssuer>>) -> &'static [types::TrustedIssuer] {
	static mut ISSUERS: Vec<types::TrustedIssuer> = Vec::new();

	unsafe {
		swap.map(|s| ISSUERS = s);
		ISSUERS.as_slice()
	}
}

pub(crate) fn schema(swap: Option<Schema>) -> Option<&'static Schema> {
	static mut SCHEMA: Option<Schema> = None;

	unsafe {
		swap.map(|s| SCHEMA = Some(s));
		SCHEMA.as_ref()
	}
}
