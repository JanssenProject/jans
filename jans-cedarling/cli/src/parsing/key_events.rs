use super::Parser;
use crate::RenderRequest;
use anyhow::Result;

impl Parser {
    pub fn handle_enter(&mut self, input: &mut String) -> Result<()> {
        self.renderer_tx
            .send(RenderRequest::UpdateLine(input.clone()))?;
        self.cursor_pos = 0;
        Ok(())
    }

    pub fn insert_char(&mut self, input: &mut String, c: char) -> Result<()> {
        input.push(c);
        self.cursor_pos += 1;
        self.renderer_tx
            .send(RenderRequest::UpdateLine(input.clone()))?;
        Ok(())
    }

    pub fn backspace(&mut self, input: &mut String) -> Result<()> {
        if input.is_empty() {
            return Ok(());
        }

        if self.cursor_pos > 0 {
            self.cursor_pos -= 1;
            input.remove(self.cursor_pos.into());
        } else {
            input.clear();
        }
        self.renderer_tx
            .send(RenderRequest::Backspace(input.clone()))?;

        Ok(())
    }

    pub fn move_cursor_left(&mut self) -> Result<()> {
        if self.cursor_pos > 0 {
            self.cursor_pos -= 1;
            self.renderer_tx.send(RenderRequest::MoveCursorLeft)?;
        }
        return Ok(());
    }

    pub fn move_cursor_right(&mut self, input_len: usize) -> Result<()> {
        if input_len > self.cursor_pos.into() {
            self.cursor_pos += 1;
            self.renderer_tx.send(RenderRequest::MoveCursorRight)?;
        }
        return Ok(());
    }

    pub fn scrub_hist_prev(&mut self, input: &mut String) -> Result<()> {
        if let Some(prev) = self.hist.prev() {
            *input = prev.to_string();
            self.cursor_pos = u16::try_from(input.len()).unwrap_or(u16::MAX);
            self.renderer_tx
                .send(RenderRequest::UpdateLine(input.clone()))?;
            self.renderer_tx
                .send(RenderRequest::SetCursorPos(self.cursor_pos))?;
        }
        Ok(())
    }

    pub fn scrub_hist_next(&mut self, input: &mut String) -> Result<()> {
        if let Some(next) = self.hist.next() {
            *input = next.to_string();
            self.cursor_pos = u16::try_from(input.len()).unwrap_or(u16::MAX);
            self.renderer_tx
                .send(RenderRequest::UpdateLine(input.clone()))?;
            self.renderer_tx
                .send(RenderRequest::SetCursorPos(self.cursor_pos))?;
        } else {
            input.clear();
            self.cursor_pos = 0;
            self.renderer_tx
                .send(RenderRequest::UpdateLine(input.clone()))?;
            self.renderer_tx
                .send(RenderRequest::SetCursorPos(self.cursor_pos))?;
        }
        Ok(())
    }

    pub fn handle_paste_event(&mut self, input: &mut String, pasted: String) -> Result<()> {
        input.push_str(&pasted);
        self.renderer_tx
            .send(RenderRequest::UpdateLine(input.clone()))?;
        Ok(())
    }
}
