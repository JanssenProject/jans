use std::collections::BTreeMap;
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

// TODO: Replace context with lighter map type
fn evaluate(statement: &Statement, context: &BTreeMap<Binding, cedar_policy::Decision>) -> cedar_policy::Decision {
	match statement {
		Statement::Binding(s) => context.get(s).unwrap_throw().clone(),
		Statement::Operation(function, arguments) => {
			let arguments: Vec<_> = arguments.into_iter().map(|s| evaluate(s, context)).collect();
			function(&arguments)
		}
	}
}

// supported tokens: Client, Application, User, Role, !, |, &, (, )
fn parse(tokens: &str) -> Statement {
	let mut start = 0;
	let mut o_stack: Vec<fn(&[cedar_policy::Decision]) -> cedar_policy::Decision> = vec![];

	// stores arguments
	let mut a_stack = vec![];
	let mut statements = vec![];

	for (index, _) in tokens.char_indices() {
		let slice = tokens.get(start..=index).expect_throw("Overflow encountered during string slice");

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
	let _ = parse("&(!(Client), Application, !(Application))");
	let _ = parse("&(Client, Application, User)");
	let _ = parse("Client");
}

#[wasm_bindgen_test::wasm_bindgen_test]
fn boolean_evaluator() {
	let _ = parse("&(!(Client), Application, !(Application))");
	let _ = parse("&(Client, Application, User)");
	let _ = parse("Client");
}
