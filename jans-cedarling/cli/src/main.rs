use std::{collections::HashMap, io};

mod parsing;

use crossterm::{cursor, terminal, ExecutableCommand};
use parsing::*;

#[derive(Default)]
struct Context {
    variables: HashMap<String, String>,
}

fn main() {
    let mut ctx = Context::default();

    println!("===== Cedarling CLI =====");
    println!("Type `quit()` to exit the program.");
    println!("To assign a variable, use: <variable_name> = <value>\n");

    terminal::enable_raw_mode().unwrap();
    let mut stdout = io::stdout();
    stdout.execute(cursor::Hide).unwrap();

    let mut parser = Parser::new(&stdout);

    loop {
        let parsed_cmd = match parser.parse() {
            Ok(result) => result,
            Err(e) => {
                eprintln!("[ERROR]: {e}");
                std::process::exit(1);
            },
        };

        match parsed_cmd {
            ParsedCommand::Quit => break,
            ParsedCommand::VariableAssignment(name, val) => {
                println!("set `{name}` to `{val}`");
                ctx.variables.insert(name, val);
            },
            ParsedCommand::UnknownCommand(input) => match ctx.variables.get(&input) {
                Some(var) => println!("{input} = {var}"),
                None => println!("Invalid command or variable: {input}"),
            },
            ParsedCommand::NoOp => {},
            ParsedCommand::FnAuthz(vec) => println!("Calling authz with params: {vec:?}"),
        }
    }

    terminal::disable_raw_mode().unwrap();
    stdout.execute(cursor::Show).unwrap();
}
