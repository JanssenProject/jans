use std::collections::HashMap;

mod parsing;

use parsing::*;

#[derive(Default)]
struct Context {
    variables: HashMap<String, String>,
}

fn main() {
    let parser = Parser::default();
    let mut cli = Context::default();

    println!("===== Cedarling CLI =====");
    println!("Type `quit()` to exit the program.");
    println!("To assign a variable, use: <variable_name> = <value>\n");

    loop {
        let parse_result = match parser.parse() {
            Ok(result) => result,
            Err(e) => {
                eprintln!("[ERROR]: {e}");
                std::process::exit(1);
            },
        };

        match parse_result {
            ParseResult::Quit => break,
            ParseResult::VariableAssignment(name, val) => {
                println!("set `{name}` to `{val}`");
                cli.variables.insert(name, val);
            },
            ParseResult::UnknownCommand(input) => match cli.variables.get(&input) {
                Some(var) => println!("{input} = {var}"),
                None => println!("Invalid command or variable: {input}"),
            },
            ParseResult::EmptyString => {},
        }
    }
}
