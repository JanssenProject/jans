use anyhow::{anyhow, Result};
use crossterm::{
    cursor,
    event::{self, KeyEvent},
    terminal, ExecutableCommand,
};
use regex::Regex;
use std::io::{Stdout, Write};

mod hist;
mod parse_authz;
mod variable_assignment;

use hist::*;
use parse_authz::*;
use variable_assignment::*;

const PROMPT: &str = "~~>";

pub struct Parser<'a> {
    var_name_regex: Regex,
    fn_authz_regex: Regex,
    stdout: &'a Stdout,
    hist: InputHistory,
}

impl<'a> Parser<'a> {
    pub fn new(std_out: &'a Stdout) -> Self {
        let var_name_regex =
            Regex::new(VAR_NAME_REGEX_SRC).expect("Failed to compile regex for variable names");
        let fn_authz_regex =
            Regex::new(FN_AUTHZ_REGEX_SRC).expect("Failed to compile regex for authz(...)");
        Self {
            var_name_regex,
            fn_authz_regex,
            stdout: std_out,
            hist: InputHistory::default(),
        }
    }
}

pub enum ParsedCommand {
    Quit,
    UnknownCommand(String),
    VariableAssignment(String, String),
    NoOp,
    FnAuthz(Vec<String>),
}

impl Parser<'_> {
    pub fn parse(&mut self) -> Result<ParsedCommand> {
        self.print_prompt()?;

        let mut current_input = String::new();
        loop {
            if let Some(event) = event::read().ok() {
                match event {
                    event::Event::Key(key_event) => {
                        if let Some(cmd) = self.handle_key_event(&mut current_input, key_event)? {
                            self.print_input(&current_input)?;
                            self.hist.push(current_input);
                            return Ok(cmd);
                        }
                    },
                    event::Event::Paste(pasted) => {
                        self.handle_paste_event(&mut current_input, pasted)?;
                    },
                    _ => {},
                }
            }
        }
    }

    fn print_prompt(&mut self) -> Result<()> {
        self.move_cursor_to_start()?;
        print!("{PROMPT} ");
        self.flush()?;

        Ok(())
    }

    // prints the prompt and the given current input, overwriting the previous line
    fn print_input(&mut self, input: &str) -> Result<()> {
        self.clear_line()?;
        print!("{PROMPT} {input}");
        self.flush()?;
        self.move_cursor_to_start()?;

        Ok(())
    }

    fn handle_key_event(
        &mut self,
        input: &mut String,
        key_event: KeyEvent,
    ) -> Result<Option<ParsedCommand>> {
        match key_event.code {
            event::KeyCode::Char(c) => {
                input.push(c);
                self.print_input(&input)?;
            },
            event::KeyCode::Backspace => {
                if !input.is_empty() {
                    input.pop();
                    self.print_input(&input)?;
                }
            },
            event::KeyCode::Enter => {
                return Ok(Some(self.parse_cmd(input)));
            },
            event::KeyCode::Up => {
                self.scrub_hist_prev(input)?;
            },
            event::KeyCode::Down => {
                self.scrub_hist_next(input)?;
            },
            _ => {},
        }

        Ok(None)
    }

    fn parse_cmd(&self, input: &str) -> ParsedCommand {
        let mut cmd = ParsedCommand::UnknownCommand(input.to_string());

        if input.is_empty() {
            cmd = ParsedCommand::NoOp;
        }

        if input == "quit()" {
            cmd = ParsedCommand::Quit;
        }

        if let Some((var_name, value)) = parse_variable_assignment(input, &self.var_name_regex) {
            cmd = ParsedCommand::VariableAssignment(var_name.clone(), value.to_string());
        }

        if let Some(args) = parse_authz(input, &self.fn_authz_regex, &self.var_name_regex) {
            cmd = ParsedCommand::FnAuthz(args);
        }

        cmd
    }

    fn scrub_hist_prev(&mut self, input: &mut String) -> Result<()> {
        if let Some(prev) = self.hist.prev() {
            *input = prev.to_string();
            self.print_input(&input)?;
        }
        Ok(())
    }

    fn scrub_hist_next(&mut self, input: &mut String) -> Result<()> {
        if let Some(next) = self.hist.next() {
            *input = next.to_string();
        } else {
            input.clear();
        }
        self.print_input(&input)?;
        Ok(())
    }

    fn handle_paste_event(&mut self, input: &mut String, pasted: String) -> Result<()> {
        input.push_str(&pasted);
        self.print_input(&input)?;
        Ok(())
    }

    fn clear_line(&mut self) -> Result<()> {
        self.stdout
            .execute(cursor::MoveToColumn(0))
            .map_err(|e| anyhow!("Failed to move cursor to start: {e}"))?;
        self.stdout
            .execute(terminal::Clear(terminal::ClearType::CurrentLine))
            .map_err(|e| anyhow!("Failed to clear line: {e}"))?;
        Ok(())
    }

    fn move_cursor_to_start(&mut self) -> Result<()> {
        self.stdout
            .execute(cursor::MoveToColumn(0))
            .map_err(|e| anyhow!("Failed to move cursor: {e}"))?;
        Ok(())
    }

    fn flush(&mut self) -> Result<()> {
        self.stdout
            .flush()
            .map_err(|e| anyhow!("Failed to flush stdout: {e}"))?;
        Ok(())
    }
}
