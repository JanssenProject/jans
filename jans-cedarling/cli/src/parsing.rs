use anyhow::Result;
use crossterm::event::{self, KeyEvent};
use regex::Regex;
use std::sync::mpsc::Sender;

mod hist;
mod parse_authz;
mod variable_assignment;

use hist::*;
use parse_authz::*;
use variable_assignment::*;

use crate::RenderEvent;

pub struct Parser {
    var_name_regex: Regex,
    fn_authz_regex: Regex,
    hist: InputHistory,
    renderer_tx: Sender<RenderEvent>,
}

impl Parser {
    pub fn new(renderer_tx: Sender<RenderEvent>) -> Self {
        let var_name_regex =
            Regex::new(VAR_NAME_REGEX_SRC).expect("Failed to compile regex for variable names");
        let fn_authz_regex =
            Regex::new(FN_AUTHZ_REGEX_SRC).expect("Failed to compile regex for authz(...)");
        Self {
            var_name_regex,
            fn_authz_regex,
            hist: InputHistory::default(),
            renderer_tx,
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
            event::KeyCode::Char(c) => {
                input.push(c);
            },
            event::KeyCode::Backspace => {
                if !input.is_empty() {
                    input.pop();
                }
            },
            event::KeyCode::Enter => {
                self.renderer_tx
                    .send(RenderEvent::UpdateLine(input.clone()))?;
                // self.renderer_tx
                //     .send(RenderEvent::WriteNewline(input.clone()))?;
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

        // send update to the renderer
        self.renderer_tx
            .send(RenderEvent::UpdateLine(input.clone()))?;

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
        }
        Ok(())
    }

    fn scrub_hist_next(&mut self, input: &mut String) -> Result<()> {
        if let Some(next) = self.hist.next() {
            *input = next.to_string();
        } else {
            input.clear();
        }
        Ok(())
    }

    fn handle_paste_event(&mut self, input: &mut String, pasted: String) -> Result<()> {
        input.push_str(&pasted);
        Ok(())
    }
}
