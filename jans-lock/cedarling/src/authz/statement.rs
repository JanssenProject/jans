use crate::startup;
use wasm_bindgen::{throw_str, UnwrapThrowExt};

#[repr(u8)]
#[derive(Debug, PartialEq, Eq, PartialOrd, Ord)]
pub enum Binding {
	Client,
	Application,
	User,
}

#[derive(Debug)]
pub enum Statement {
	Binding(Binding),
	Operation(fn(&[cedar_policy::Decision]) -> cedar_policy::Decision, Vec<Statement>),
}

#[derive(Debug, Default)]
pub struct ExecCache {
	client: Option<cedar_policy::Decision>,
	user: Option<cedar_policy::Decision>,
	application: Option<cedar_policy::Decision>,
}

pub fn evaluate(
	// Lord have mercy on the number of parameters this takes
	statement: Statement,
	uids: &super::types::EntityUids,
	entities: &cedar_policy::Entities,
	input: &(cedar_policy::EntityUid, cedar_policy::EntityUid, cedar_policy::Context),
	cache: &mut ExecCache,
) -> cedar_policy::Decision {
	let schema = startup::SCHEMA.get();
	let policies = startup::POLICY_SET.get().expect_throw("POLICY_SET not initialized");

	match statement {
		Statement::Binding(s) => {
			// check cache
			if let Some(c) = match s {
				Binding::Client => cache.client,
				Binding::Application => cache.application,
				Binding::User => cache.user,
			} {
				return c;
			}

			// calculate result
			let (action, resource, context) = input;
			let principal = match s {
				Binding::Client => Some(uids.user.clone()),
				Binding::Application => uids.application.clone(),
				Binding::User => Some(uids.user.clone()),
			};

			let decision = cedar_policy::Request::new(principal, Some(action.clone()), Some(resource.clone()), context.clone(), schema).unwrap_throw();

			// create authorizer
			let authorizer = cedar_policy::Authorizer::new();
			authorizer.is_authorized(&decision, policies, &entities).decision()
		}
		Statement::Operation(function, arguments) => {
			let cb = |s| evaluate(s, uids, entities, input, cache);
			let arguments: Vec<_> = arguments.into_iter().map(cb).collect();
			function(&arguments)
		}
	}
}

// supported tokens: Client, Application, User, !, |, &, (, )
pub fn parse(tokens: &str) -> Statement {
	let mut start = 0;
	let mut o_stack: Vec<fn(&[cedar_policy::Decision]) -> cedar_policy::Decision> = vec![];

	// stores arguments
	let mut a_stack = vec![];
	let mut statements = vec![];

	for (index, _) in tokens.char_indices() {
		let slice = tokens.get(start..=index).expect_throw("Overflow encountered during string slice");

		// TODO: enable !Client pattern
		match slice {
			"Client" => statements.push(Statement::Binding(Binding::Client)),
			"Application" => statements.push(Statement::Binding(Binding::Application)),
			"User" => statements.push(Statement::Binding(Binding::User)),
			"!" => o_stack.push(operators::not),
			"|" => o_stack.push(operators::any),
			"&" => o_stack.push(operators::all),
			"(" => a_stack.push(statements.len()),
			")" => {
				let Some(start) = a_stack.pop() else { throw_str(&format!("Unmatched `)` at index: {}", index)) };
				let Some(operator) = o_stack.pop() else { throw_str("Operator call without function name") };

				let statement = Statement::Operation(operator, statements.drain(start..).collect());
				statements.push(statement);
			}
			"\n" | " " | "," => {
				// ignored inputs
			}
			// End of input, with unknown syntax
			_ if index == tokens.len() => match tokens.get(index..index + 1) {
				Some(s) => {
					let msg = format!("Unknown syntax: {}", s);
					throw_str(&msg)
				}
				None => throw_str("Syntax Error, encountered unknown tokens in boolean combine string"),
			},
			// expand window
			_ => continue,
		}

		start = index + 1;
	}

	// statements should have length of 1, since it's either an operation or a simple binding
	wasm_bindgen_test::console_log!("Statements: {:?}", statements);
	if statements.len() != 1 {
		throw_str("multiple statements found, possible syntax error")
	}

	statements.swap_remove(0)
}

mod operators {
	use wasm_bindgen::UnwrapThrowExt;

	// !
	pub fn not(input: &[cedar_policy::Decision]) -> cedar_policy::Decision {
		match input.first().expect_throw("`!` operation expects at least one input") {
			cedar_policy::Decision::Allow => cedar_policy::Decision::Deny,
			cedar_policy::Decision::Deny => cedar_policy::Decision::Allow,
		}
	}

	// |
	pub fn any(input: &[cedar_policy::Decision]) -> cedar_policy::Decision {
		match input.iter().any(|i| *i == cedar_policy::Decision::Allow) {
			true => cedar_policy::Decision::Allow,
			false => cedar_policy::Decision::Deny,
		}
	}

	// &
	pub fn all(input: &[cedar_policy::Decision]) -> cedar_policy::Decision {
		match input.iter().all(|i| *i == cedar_policy::Decision::Allow) {
			true => cedar_policy::Decision::Allow,
			false => cedar_policy::Decision::Deny,
		}
	}
}

#[wasm_bindgen_test::wasm_bindgen_test]
fn boolean_parser() {
	let _ = parse("&(Application, |(Client, Role))");
	let _ = parse("&(!(Client), Application, !(Application))");
	let _ = parse("&(Client, Application, User)");
	let _ = parse("Client");
}
