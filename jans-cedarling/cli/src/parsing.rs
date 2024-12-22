use anyhow::Result;
use crossterm::event::{self, KeyEvent};
use regex::Regex;
use std::sync::mpsc::Sender;

mod hist;
mod key_events;
mod parse_authz;
mod variable_assignment;

use hist::*;
use parse_authz::*;
use variable_assignment::*;

use crate::RenderRequest;

pub struct Parser {
    var_name_regex: Regex,
    fn_authz_regex: Regex,
    hist: InputHistory,
    renderer_tx: Sender<RenderRequest>,
    cursor_pos: u16,
}

impl Parser {
    pub fn new(renderer_tx: Sender<RenderRequest>) -> Self {
        let var_name_regex =
            Regex::new(VAR_NAME_REGEX_SRC).expect("Failed to compile regex for variable names");
        let fn_authz_regex =
            Regex::new(FN_AUTHZ_REGEX_SRC).expect("Failed to compile regex for authz(...)");
        Self {
            var_name_regex,
            fn_authz_regex,
            hist: InputHistory::default(),
            renderer_tx,
            cursor_pos: 0,
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

impl Parser {
    pub fn parse(&mut self) -> Result<ParsedCommand> {
        let mut current_input = String::new();
        loop {
            if let Some(event) = event::read().ok() {
                match event {
                    event::Event::Key(key_event) => {
                        if let Some(cmd) = self.handle_key_event(&mut current_input, key_event)? {
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

    fn handle_key_event(
        &mut self,
        input: &mut String,
        key_event: KeyEvent,
    ) -> Result<Option<ParsedCommand>> {
        match key_event.code {
            event::KeyCode::Enter => {
                self.handle_enter(input)?;
                return Ok(Some(self.parse_cmd(input)));
            },
            event::KeyCode::Char(c) => self.insert_char(input, c)?,
            event::KeyCode::Backspace => self.backspace(input)?,
            event::KeyCode::Up => self.scrub_hist_prev(input)?,
            event::KeyCode::Down => self.scrub_hist_next(input)?,
            event::KeyCode::Left => self.move_cursor_left()?,
            event::KeyCode::Right => self.move_cursor_right(input.len())?,
            _ => (),
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
}
