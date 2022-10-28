from prompt_toolkit.widgets import TextArea

class IntegerValidator:
    """A class for validating if entered text is integer for TextArea.
    Example:
        ta = TextArea()
        ta.buffer.on_text_insert=IntegerValidator(ta)
    """

    def __init__(self, me: TextArea) -> None:
        self.me = me

    def fire(self) -> None:
        """This fucntion is called when user enters a character on TextArea
        """
        cur_pos = self.me.buffer.cursor_position
        for c in self.me.text:
            if not c.isdigit():
                self.me.buffer._set_cursor_position(cur_pos-1)
                self.me.buffer.delete(1)
