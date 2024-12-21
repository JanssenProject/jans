use anyhow::{anyhow, Result};
use regex::Regex;
use std::io::{self, Write};

mod variable_assignment;

use variable_assignment::*;

pub struct Parser {
    var_name_regex: Regex,
}

impl Default for Parser {
    fn default() -> Self {
        let var_name_regex =
            Regex::new(VAR_NAME_REGEX_SRC).expect("Failed to compile regex for variable names");
        Self { var_name_regex }
    }
}

pub enum ParseResult {
    Quit,
    UnknownCommand(String),
    VariableAssignment(String, String),
}

impl Parser {
    pub fn parse(&self) -> Result<ParseResult> {
        // print the prompt
        print!("~~> ");
        io::stdout()
            .flush()
            .map_err(|e| anyhow!("Failed to flush stdout: {e}"))?;

        let mut input = String::new();
        io::stdin()
            .read_line(&mut input)
            .map_err(|e| anyhow!("Failed to read input: {e}"))?;
        let input = input.trim();

        // -=-=- parsing the input -=-=-

        if input == "quit()" {
            return Ok(ParseResult::Quit);
        }

        if let Some((var_name, value)) = parse_variable_assignment(&input, &self.var_name_regex) {
            return Ok(ParseResult::VariableAssignment(
                var_name.clone(),
                value.to_string(),
            ));
        } else {
            return Ok(ParseResult::UnknownCommand(input.to_string()));
        }
    }
}
