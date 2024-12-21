mod parsing;

use parsing::*;

fn main() {
    let parser = Parser::default();

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
                println!("set `{name}` to `{val}`")
            },
            _ => {},
        }
    }
}
