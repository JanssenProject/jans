use cedar_policy::*;

// SAFETY: Webassembly is single-threaded, so this is safe. The statics are also scoped
pub(crate) fn entities(swap: Option<Entities>) -> &'static Entities {
	static mut ENTITIES: Option<Entities> = None;

	unsafe {
		swap.map(|s| ENTITIES = Some(s));
		ENTITIES.as_ref().unwrap()
	}
}

pub(crate) fn policies(swap: Option<PolicySet>) -> &'static PolicySet {
	static mut ENTITIES: Option<PolicySet> = None;

	unsafe {
		swap.map(|s| ENTITIES = Some(s));
		ENTITIES.as_ref().unwrap()
	}
}

pub(crate) fn schema(swap: Option<Schema>) -> Option<&'static Schema> {
	static mut ENTITIES: Option<Schema> = None;

	unsafe {
		swap.map(|s| ENTITIES = Some(s));
		ENTITIES.as_ref()
	}
}

pub(crate) fn authorizer() -> &'static Authorizer {
	static mut AUTHORIZER: Option<Authorizer> = None;
	unsafe { AUTHORIZER.get_or_insert_with(|| Authorizer::new()) }
}
