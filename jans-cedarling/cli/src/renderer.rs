use anyhow::{anyhow, Result};
use crossterm::{cursor, execute, terminal};
use std::{cell::RefCell, io::Write, sync::mpsc::Receiver};

const PROMPT: &str = "~~>";

#[derive(Debug)]
pub enum RenderEvent {
    WriteNewline(String),
    UpdateLine(String),
}

pub struct Renderer<W: Write> {
    buffer: RefCell<W>,
    event_rx: Receiver<RenderEvent>,
}

impl<W> Renderer<W>
where
    W: Write,
{
    pub fn new(buffer: W, event_rx: Receiver<RenderEvent>) -> Self {
        Self {
            buffer: RefCell::new(buffer),
            event_rx,
        }
    }

    pub fn listen(&self) -> Result<()> {
        Self::print_prompt(&mut self.buffer.borrow_mut())?;
        loop {
            match self.event_rx.recv() {
                Ok(event) => self.handle_event(event)?,
                Err(_) => break,
            }
        }
        Ok(())
    }

    fn handle_event(&self, event: RenderEvent) -> Result<()> {
        let mut buf = self.buffer.borrow_mut();
        match event {
            RenderEvent::WriteNewline(message) => {
                Self::move_cursor_to_start(&mut buf)?;
                Self::clear_line(&mut buf)?;
                write!(buf, "{message}\n")?;
                Self::move_cursor_to_start(&mut buf)?;
                write!(buf, "{PROMPT} ")?;
            },
            RenderEvent::UpdateLine(input) => {
                Self::move_cursor_to_start(&mut buf)?;
                Self::clear_line(&mut buf)?;
                write!(buf, "{PROMPT} {input}").unwrap();
            },
        }
        buf.flush()?;
        Ok(())
    }

    fn print_prompt(buf: &mut W) -> Result<()> {
        write!(buf, "{PROMPT} ")?;
        Ok(())
    }

    fn clear_line(buf: &mut W) -> Result<()> {
        execute!(buf, terminal::Clear(terminal::ClearType::CurrentLine))
            .map_err(|e| anyhow!("Failed to clear line: {e}"))?;
        Ok(())
    }

    fn move_cursor_to_start(buf: &mut W) -> Result<()> {
        execute!(buf, cursor::MoveToColumn(0))
            .map_err(|e| anyhow!("Failed to move cursor to start: {e}"))?;
        Ok(())
    }

    #[allow(dead_code)]
    fn move_cursor_down(buf: &mut W) -> Result<()> {
        execute!(buf, cursor::MoveDown(1))
            .map_err(|e| anyhow!("Failed to move cursor down: {e}"))?;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::mpsc;

    #[test]
    fn test_print_line() {
        let (tx, rx) = mpsc::channel();
        let buffer = Vec::new();

        let renderer = Renderer::new(buffer, rx);

        tx.send(RenderEvent::WriteNewline("Hello, world!".to_string()))
            .unwrap();

        renderer.listen().unwrap();

        let output = String::from_utf8(renderer.buffer.borrow().to_vec()).unwrap();
        assert_eq!(output, "~~> \u{1b}[1B\u{1b}[1GHello, world!\n");
    }

    #[test]
    fn test_update_line() {
        let (tx, rx) = mpsc::channel();
        let buffer = Vec::new();

        let renderer = Renderer::new(buffer, rx);

        tx.send(RenderEvent::UpdateLine("Hello, world!".to_string()))
            .unwrap();

        tx.send(RenderEvent::UpdateLine("Goodbye, world!".to_string()))
            .unwrap();

        renderer.listen().unwrap();

        // Assert the buffer contains the correct output
        let output = String::from_utf8(renderer.buffer.borrow().to_vec()).unwrap();
        assert_eq!(
            output,
            "~~> \u{1b}[2K\u{1b}[1G~~> Hello, world!\u{1b}[2K\u{1b}[1G~~> Goodbye, world!"
        );
    }
}
