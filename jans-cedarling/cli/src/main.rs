use anyhow::Result;
use std::{
    collections::HashMap,
    io,
    sync::mpsc::{self, Sender},
    thread,
};

mod parsing;
mod renderer;

use crossterm::terminal;
use parsing::*;
use renderer::*;

struct Context {
    variables: HashMap<String, String>,
    renderer_tx: Sender<RenderEvent>,
}

impl Context {
    fn new(renderer_tx: Sender<RenderEvent>) -> Self {
        Self {
            variables: HashMap::new(),
            renderer_tx,
        }
    }

    fn try_print_var(&self, name: &str) -> Result<()> {
        if let Some(val) = self.variables.get(name) {
            self.renderer_tx
                .send(RenderEvent::WriteNewline(format!("{name} = {val}")))?;
        } else {
            self.renderer_tx.send(RenderEvent::WriteNewline(format!(
                "Invalid command or variable: {name}"
            )))?;
        }
        Ok(())
    }

    fn set_var(&mut self, name: String, val: String) -> Result<()> {
        self.renderer_tx.send(RenderEvent::WriteNewline(format!(
            "set `{name}` to `{val}`"
        )))?;
        self.variables.insert(name, val);
        Ok(())
    }
}

fn main() {
    let (renderer_tx, renderer_rx) = mpsc::channel();

    terminal::enable_raw_mode().unwrap();

    let renderer = Renderer::new(io::stdout(), renderer_rx);
    let _renderer_handle = thread::spawn(move || renderer.listen());

    let mut ctx = Context::new(renderer_tx.clone());
    let mut parser = Parser::new(renderer_tx.clone());

    [
        "===== Cedarling CLI =====",
        "Type `quit()` to exit the program.",
        "To assign a variable, use: <variable_name> = <value>",
    ]
    .into_iter()
    .for_each(|s| {
        renderer_tx
            .send(RenderEvent::WriteNewline(s.to_string()))
            .unwrap()
    });

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
            ParsedCommand::VariableAssignment(name, val) => ctx.set_var(name, val).unwrap(),
            ParsedCommand::UnknownCommand(input) => ctx.try_print_var(input.as_str()).unwrap(),
            ParsedCommand::NoOp => {},
            ParsedCommand::FnAuthz(vec) => println!("Calling authz with params: {vec:?}"),
        }
    }

    terminal::disable_raw_mode().unwrap();
}
