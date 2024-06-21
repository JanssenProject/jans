use cedar_policy::*;

use crate::types;

// SAFETY: Webassembly is single-threaded, so this is safe. The statics are also scoped
pub(crate) fn policies(swap: Option<PolicySet>) -> &'static PolicySet {
	static mut ENTITIES: Option<PolicySet> = None;

	unsafe {
		swap.map(|s| ENTITIES = Some(s));
		ENTITIES.as_ref().unwrap()
	}
}

pub(crate) fn trusted_issuers(swap: Option<Vec<types::TrustedIssuer>>) -> &'static [types::TrustedIssuer] {
	static mut ENTITIES: Vec<types::TrustedIssuer> = Vec::new();

	unsafe {
		swap.map(|s| ENTITIES = s);
		ENTITIES.as_slice()
	}
}

pub(crate) fn schema(swap: Option<Schema>) -> Option<&'static Schema> {
	static mut ENTITIES: Option<Schema> = None;

	unsafe {
		swap.map(|s| ENTITIES = Some(s));
		ENTITIES.as_ref()
	}
}
