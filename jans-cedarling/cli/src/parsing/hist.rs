pub struct InputHistory {
    cmds: Vec<String>,
    cursor: usize,
    capacity: usize,
}

impl Default for InputHistory {
    fn default() -> Self {
        Self {
            cmds: Vec::with_capacity(3000),
            cursor: 0,
            capacity: 3000,
        }
    }
}

impl InputHistory {
    pub fn push(&mut self, cmd: impl ToString) {
        if self.cmds.len() == self.capacity {
            self.cmds[self.cursor % self.capacity] = cmd.to_string();
        } else {
            self.cmds.push(cmd.to_string());
        }
        self.cursor = self.cmds.len();
    }

    pub fn prev(&mut self) -> Option<&str> {
        if self.cursor > 0 {
            self.cursor -= 1;
            Some(&self.cmds[self.cursor])
        } else {
            None
        }
    }

    pub fn next(&mut self) -> Option<&str> {
        if self.cursor < self.cmds.len() {
            self.cursor += 1;
            Some(&self.cmds[self.cursor - 1])
        } else {
            None
        }
    }
}
