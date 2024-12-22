use anyhow::{anyhow, Result};
use crossterm::{cursor, execute, terminal};
use std::{cell::RefCell, io::Write, sync::mpsc::Receiver};

pub const PROMPT: &str = "~~>";

#[derive(Debug)]
pub enum RenderRequest {
    WriteNewline(String),
    UpdateLine(String),
    Backspace(String),
    MoveCursorLeft,
    MoveCursorRight,
    SetCursorPos(u16),
}

pub struct Renderer<W: Write> {
    buffer: RefCell<W>,
    event_rx: Receiver<RenderRequest>,
}

impl<W> Renderer<W>
where
    W: Write,
{
    pub fn new(buffer: W, event_rx: Receiver<RenderRequest>) -> Self {
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

    fn handle_event(&self, event: RenderRequest) -> Result<()> {
        let mut buf = self.buffer.borrow_mut();
        match event {
            RenderRequest::WriteNewline(message) => {
                Self::move_cursor_to_start(&mut buf)?;
                Self::clear_line(&mut buf)?;
                write!(buf, "{message}\n")?;
                Self::move_cursor_to_start(&mut buf)?;
                write!(buf, "{PROMPT} ")?;
            },
            RenderRequest::UpdateLine(input) => {
                Self::move_cursor_to_start(&mut buf)?;
                Self::clear_line(&mut buf)?;
                write!(buf, "{PROMPT} {input}").unwrap();
            },
            RenderRequest::Backspace(input) => {
                execute!(buf, cursor::SavePosition)?;
                Self::move_cursor_to_start(&mut buf)?;
                Self::clear_line(&mut buf)?;
                write!(buf, "{PROMPT} {input}").unwrap();
                execute!(buf, cursor::RestorePosition)?;
                execute!(buf, cursor::MoveLeft(1))?;
            },
            RenderRequest::MoveCursorLeft => Self::move_cursor_left(&mut buf)?,
            RenderRequest::MoveCursorRight => Self::move_cursor_right(&mut buf)?,
            RenderRequest::SetCursorPos(pos) => {
                let pos = u16::try_from(PROMPT.len())
                    .unwrap_or(u16::MAX)
                    .saturating_add(pos);
                execute!(buf, cursor::MoveToColumn(pos))?;
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

    fn move_cursor_left(buf: &mut W) -> Result<()> {
        execute!(buf, cursor::MoveLeft(1))
            .map_err(|e| anyhow!("Failed to move cursor to the left: {e}"))?;
        Ok(())
    }

    fn move_cursor_right(buf: &mut W) -> Result<()> {
        execute!(buf, cursor::MoveRight(1))
            .map_err(|e| anyhow!("Failed to move cursor to the left: {e}"))?;
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

        tx.send(RenderRequest::WriteNewline("Hello, world!".to_string()))
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

        tx.send(RenderRequest::UpdateLine("Hello, world!".to_string()))
            .unwrap();

        tx.send(RenderRequest::UpdateLine("Goodbye, world!".to_string()))
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
