import json
from functools import partial
from asyncio import Future

from prompt_toolkit.widgets import Button, Dialog
from prompt_toolkit.layout.dimension import D

class JansGDialog:
    def __init__(self, parent, title, body, buttons=[], width=None):
        self.future = Future()
        self.body = body
        self.myparent = parent

        if not width:
            width = int(parent.output.get_size().columns * 0.85)

        if not buttons:
            buttons = [Button(text="OK")]

        def do_handler(button_text, handler):
            if handler:
                handler(self)
            self.future.set_result(button_text)

        for button in buttons:
            button.handler = partial(do_handler, button.text, button.handler)

        self.dialog = Dialog(
            title=title,
            body=body,
            buttons=buttons,
            width=width,
            modal=True,
        )

    def __pt_container__(self):
        return self.dialog
